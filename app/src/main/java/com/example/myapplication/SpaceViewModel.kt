/**
 * 应用的核心业务逻辑处理类（ViewModel）。
 *
 * 职责：
 * - 连接数据层（Room DAO）和 UI 层（Compose Screens）。
 * - 处理各种用户交互逻辑（增删改查空间、物品、清单、标签等）。
 * - 管理搜索状态和图片持久化。
 * - 初始化演示数据和模板。
 *
 * 上层用途：
 * - 被 `MainActivity` 实例化，并作为单一真相来源（Single Source of Truth）提供给所有 UI 页面。
 */
package com.example.myapplication

import kotlinx.coroutines.Dispatchers

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.AppDatabase
import com.example.myapplication.data.ItemEntity
import com.example.myapplication.data.ItemSearchResultRow
import com.example.myapplication.data.PackingListEntity
import com.example.myapplication.data.PackingListItemEntity
import com.example.myapplication.data.AppRepository
import com.example.myapplication.data.SpaceEntity
import com.example.myapplication.data.SpaceSummaryRow
import com.example.myapplication.data.SpotEntity
import com.example.myapplication.data.TagEntity
import com.example.myapplication.data.toDomain
import com.example.myapplication.settings.SettingsRepository
import com.example.myapplication.settings.UserSettings
import com.example.myapplication.storage.InternalImageStore
import com.example.myapplication.templates.SpaceTemplate
import com.example.myapplication.templates.Templates
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import java.util.concurrent.TimeUnit

data class SpaceCard(
    val id: String,
    val name: String,
    val coverImagePath: String?,
    val itemCount: Int,
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SpaceViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.get(application)
    private val dao = db.dao()
    private val repo = AppRepository(db)
    private val settingsRepo = SettingsRepository(application)

    val settings: StateFlow<UserSettings> =
        settingsRepo.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserSettings())

    val spaces: StateFlow<List<SpaceCard>> = dao.observeSpaceSummaries()
        .map { rows -> rows.map { it.toCard() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val tags: StateFlow<List<Tag>> = dao.observeTags()
        .map { list -> list.map { it.toDomain() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val searchQuery = MutableStateFlow("")
    val searchResults: StateFlow<List<ItemSearchResult>> = searchQuery
        .debounce(200)
        .map { it.trim() }
        .distinctUntilChanged()
        .flatMapLatest { q ->
            if (q.isBlank()) {
                MutableStateFlow(emptyList())
            } else {
                dao.observeSearchResults(q).map { rows -> rows.map { it.toDomain() } }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val lists: StateFlow<List<PackingList>> = dao.observeLists()
        .map { list -> list.map { it.toDomain() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val expiringItemsCount: StateFlow<Int> = flow {
        // 使用一次性计算替代死循环，数据变化时 DAO Flow 会自动刷新
        val now = System.currentTimeMillis()
        val sevenDaysLater = now + TimeUnit.DAYS.toMillis(7)
        emitAll(dao.observeExpiringItemsCount(now, sevenDaysLater))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun observeListItems(listId: String): Flow<List<PackingListItem>> {
        return dao.observeListItems(listId).map { it.map { row -> row.toDomain() } }
    }

    init {
        viewModelScope.launch {
            if (dao.countSpaces() == 0) seedInitialData()
        }
    }

    fun setSearchQuery(q: String) {
        searchQuery.value = q
    }

    fun observeSpace(spaceId: String): Flow<Space?> {
        return dao.observeSpaceWithSpots(spaceId).map { list -> list.firstOrNull()?.toDomain() }
    }

    fun addSpace(name: String, coverImagePath: String?, templateId: String?) {
        viewModelScope.launch {
            createSpaceSuspend(name, coverImagePath, templateId)
        }
    }

    fun removeSpace(spaceId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            // Cleanup images
            val space = dao.getSpace(spaceId)
            val items = dao.getItemsInSpace(spaceId)
            
            space?.coverImagePath?.let { path -> InternalImageStore.delete(getApplication(), path) }
            items.forEach { item ->
                item.imagePath?.let { path -> InternalImageStore.delete(getApplication(), path) }
            }

            dao.deleteSpace(spaceId)
        }
    }

    fun addSpot(spaceId: String, name: String, position: Offset) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            dao.upsertSpot(
                SpotEntity(
                    id = UUID.randomUUID().toString(),
                    spaceId = spaceId,
                    name = name.trim(),
                    x = position.x,
                    y = position.y,
                    createdAt = now,
                    updatedAt = now
                )
            )
        }
    }

    fun updateSpotPosition(spaceId: String, spotId: String, newPosition: Offset) {
        viewModelScope.launch {
            val spot = dao.getSpot(spotId) ?: return@launch
            dao.updateSpot(
                spot.copy(
                    x = newPosition.x,
                    y = newPosition.y,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun removeSpot(spaceId: String, spotId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            // Cleanup images for items in this spot
            val items = dao.getItemsInSpot(spotId)
            items.forEach { item ->
                item.imagePath?.let { InternalImageStore.delete(getApplication(), it) }
            }
            dao.deleteSpot(spotId)
        }
    }

    fun addItemToSpot(
        spaceId: String,
        spotId: String,
        itemName: String,
        note: String?,
        imagePath: String?,
        expiryDateEpochMs: Long?,
        currentQuantity: Int,
        minQuantity: Int,
        tagIds: List<String>,
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val itemId = UUID.randomUUID().toString()
            dao.upsertItem(
                ItemEntity(
                    id = itemId,
                    spotId = spotId,
                    name = itemName.trim(),
                    note = note?.trim().takeIf { !it.isNullOrBlank() },
                    imagePath = imagePath,
                    expiryDateEpochMs = expiryDateEpochMs,
                    lastUsedAtEpochMs = null,
                    currentQuantity = currentQuantity.coerceAtLeast(0),
                    minQuantity = minQuantity.coerceAtLeast(0),
                    createdAt = now,
                    updatedAt = now
                )
            )
            repo.setTagsForItem(itemId, tagIds)
        }
    }

    fun addItemsBatch(
        spaceId: String,
        spotId: String,
        names: List<String>,
        defaultTagIds: List<String> = emptyList(),
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            for (raw in names) {
                val name = raw.trim()
                if (name.isBlank()) continue
                val itemId = UUID.randomUUID().toString()
                dao.upsertItem(
                    ItemEntity(
                        id = itemId,
                        spotId = spotId,
                        name = name,
                        note = null,
                        imagePath = null,
                        expiryDateEpochMs = null,
                        lastUsedAtEpochMs = null,
                        currentQuantity = 1,
                        minQuantity = 0,
                        createdAt = now,
                        updatedAt = now
                    )
                )
                if (defaultTagIds.isNotEmpty()) repo.setTagsForItem(itemId, defaultTagIds)
            }
        }
    }

    fun renameItem(spaceId: String, spotId: String, itemId: String, newName: String) {
        viewModelScope.launch { updateItem(itemId) { it.copy(name = newName.trim()) } }
    }

    fun updateItemNote(itemId: String, note: String?) {
        viewModelScope.launch {
            updateItem(itemId) { it.copy(note = note?.trim().takeIf { !it.isNullOrBlank() }) }
        }
    }

    fun updateItemImage(spaceId: String, spotId: String, itemId: String, imagePath: String?) {
        viewModelScope.launch { updateItem(itemId) { it.copy(imagePath = imagePath) } }
    }

    fun updateItemFull(
        itemId: String,
        name: String,
        note: String?,
        expiryDateEpochMs: Long?,
        currentQuantity: Int,
        minQuantity: Int,
        imagePath: String?,
        tagIds: List<String>,
        spotId: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = dao.getItem(itemId) ?: return@launch
            val updated = current.copy(
                name = name,
                note = note,
                expiryDateEpochMs = expiryDateEpochMs,
                currentQuantity = currentQuantity,
                minQuantity = minQuantity,
                imagePath = imagePath,
                spotId = spotId,
                updatedAt = System.currentTimeMillis()
            )
            dao.updateItem(updated)
            repo.setTagsForItem(itemId, tagIds)
        }
    }

    fun updateItemExpiry(itemId: String, expiryDateEpochMs: Long?) {
        viewModelScope.launch { updateItem(itemId) { it.copy(expiryDateEpochMs = expiryDateEpochMs) } }
    }

    fun updateItemQuantities(itemId: String, currentQuantity: Int, minQuantity: Int) {
        viewModelScope.launch {
            updateItem(itemId) {
                it.copy(
                    currentQuantity = currentQuantity.coerceAtLeast(0),
                    minQuantity = minQuantity.coerceAtLeast(0)
                )
            }
        }
    }

    fun setItemTags(itemId: String, tagIds: List<String>) {
        viewModelScope.launch { repo.setTagsForItem(itemId, tagIds) }
    }

    fun removeItem(spaceId: String, spotId: String, itemId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val item = dao.getItem(itemId)
            item?.imagePath?.let { path -> InternalImageStore.delete(getApplication(), path) }
            dao.deleteItem(itemId)
        }
    }

    fun addTag(name: String, parentId: String?) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            dao.upsertTag(
                TagEntity(
                    id = UUID.randomUUID().toString(),
                    name = name.trim(),
                    parentId = parentId,
                    createdAt = now
                )
            )
        }
    }

    fun setTagParent(tagId: String, parentId: String?) {
        viewModelScope.launch { dao.setTagParent(tagId, parentId) }
    }

    fun deleteTag(tagId: String) {
        viewModelScope.launch {
            dao.clearTagParentForChildren(tagId)
            dao.deleteTag(tagId)
        }
    }

    fun setHasSeenOnboarding(seen: Boolean) {
        viewModelScope.launch { settingsRepo.setHasSeenOnboarding(seen) }
    }

    fun completeOnboarding(addDemoData: Boolean, onDone: () -> Unit) {
        viewModelScope.launch {
            if (addDemoData) {
                addDemoDataSuspend()
            }
            settingsRepo.setHasSeenOnboarding(true)
            onDone()
        }
    }

    fun addDemoData() {
        viewModelScope.launch {
            addDemoDataSuspend()
        }
    }

    fun setRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setRemindersEnabled(enabled) }
    }

    fun setDaysBeforeExpiry(days: Int) {
        viewModelScope.launch { settingsRepo.setDaysBeforeExpiry(days) }
    }

    fun createList(name: String) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            dao.upsertList(
                PackingListEntity(
                    id = UUID.randomUUID().toString(),
                    name = name.trim(),
                    createdAt = now,
                    updatedAt = now
                )
            )
        }
    }

    fun toggleListItemChecked(item: PackingListItem) {
        viewModelScope.launch {
            dao.updateListItem(
                PackingListItemEntity(
                    id = item.id,
                    listId = item.listId,
                    name = item.name,
                    checked = !item.checked,
                    linkedItemId = item.linkedItemId,
                    quantityNeeded = item.quantityNeeded,
                    createdAt = item.createdAt,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun addListItem(listId: String, name: String) {
        viewModelScope.launch {
            addListItemSuspend(listId, name)
        }
    }

    suspend fun addListItemSuspend(listId: String, name: String) {
        val trimmed = name.trim()
        if (listId.isBlank() || trimmed.isBlank()) return

        dao.getList(listId) ?: error("清单不存在")
        val now = System.currentTimeMillis()
        dao.upsertListItems(
            listOf(
                PackingListItemEntity(
                    id = UUID.randomUUID().toString(),
                    listId = listId,
                    name = trimmed,
                    checked = false,
                    linkedItemId = null,
                    quantityNeeded = null,
                    createdAt = now,
                    updatedAt = now
                )
            )
        )
        val updated = dao.updateListUpdatedAt(listId, now)
        if (updated == 0) error("清单不存在")
    }

    fun deleteList(listId: String) {
        viewModelScope.launch { dao.deleteList(listId) }
    }

    fun deleteListItem(listItemId: String) {
        viewModelScope.launch { dao.deleteListItem(listItemId) }
    }

    fun generateRestockList() {
        viewModelScope.launch {
            val candidates = dao.listRestockCandidates()
            if (candidates.isEmpty()) return@launch
            val now = System.currentTimeMillis()
            val listId = UUID.randomUUID().toString()
            dao.upsertList(
                PackingListEntity(
                    id = listId,
                    name = "补货清单 " + formatEpochMsToDate(now),
                    createdAt = now,
                    updatedAt = now
                )
            )
            val listItems = candidates.map { item ->
                PackingListItemEntity(
                    id = UUID.randomUUID().toString(),
                    listId = listId,
                    name = item.name,
                    checked = false,
                    linkedItemId = item.id,
                    quantityNeeded = (item.minQuantity - item.currentQuantity).coerceAtLeast(1),
                    createdAt = now,
                    updatedAt = now
                )
            }
            dao.upsertListItems(listItems)
        }
    }

    fun persistBitmap(bitmap: Bitmap): String? = InternalImageStore.persistBitmap(getApplication(), bitmap)

    fun persistGalleryUri(uri: android.net.Uri): String? = InternalImageStore.copyFromGalleryToInternal(getApplication(), uri)

    fun createTempCameraUri(): android.net.Uri = InternalImageStore.createTempCameraUri(getApplication())

    fun persistCapturedPhoto(tempUri: android.net.Uri): String? = InternalImageStore.persistFromUri(getApplication(), tempUri)

    private suspend fun updateItem(itemId: String, transform: (ItemEntity) -> ItemEntity) {
        val current = dao.getItem(itemId) ?: return
        dao.updateItem(transform(current).copy(updatedAt = System.currentTimeMillis()))
    }

    private fun SpaceSummaryRow.toCard(): SpaceCard = SpaceCard(
        id = id,
        name = name,
        coverImagePath = coverImagePath,
        itemCount = itemCount
    )

    private fun ItemSearchResultRow.toDomain(): ItemSearchResult = ItemSearchResult(
        itemId = itemId,
        itemName = itemName,
        note = note,
        imagePath = imagePath,
        spaceId = spaceId,
        spaceName = spaceName,
        spotId = spotId,
        spotName = spotName
    )

    private fun PackingListEntity.toDomain(): PackingList = PackingList(id, name, createdAt, updatedAt)

    private fun PackingListItemEntity.toDomain(): PackingListItem = PackingListItem(
        id = id,
        listId = listId,
        name = name,
        checked = checked,
        linkedItemId = linkedItemId,
        quantityNeeded = quantityNeeded,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private suspend fun seedInitialData() {
        val now = System.currentTimeMillis()
        val app = getApplication<Application>()

        fun saveCover(bitmap: Bitmap): String? = InternalImageStore.persistBitmap(app, bitmap)

        val livingCover = saveCover(SampleCovers.livingRoom)
        val bedroomCover = saveCover(SampleCovers.bedroom)
        val officeCover = saveCover(SampleCovers.office)

        val livingId = "space_living"
        val bedroomId = "space_bedroom"
        val officeId = "space_office"

        val spaces = listOf(
            SpaceEntity(livingId, "客厅", livingCover, now, now),
            SpaceEntity(bedroomId, "卧室", bedroomCover, now, now),
            SpaceEntity(officeId, "办公室", officeCover, now, now),
        )
        spaces.forEach { dao.upsertSpace(it) }

        val spots = listOf(
            SpotEntity("spot_sofa", livingId, "沙发区", 60f, 140f, now, now),
            SpotEntity("spot_table", livingId, "茶几", 200f, 220f, now, now),
            SpotEntity("spot_tv", livingId, "电视柜", 140f, 60f, now, now),
            SpotEntity("spot_bedside", bedroomId, "床头", 80f, 160f, now, now),
            SpotEntity("spot_closet", bedroomId, "衣柜", 230f, 110f, now, now),
            SpotEntity("spot_desk", bedroomId, "书桌", 180f, 230f, now, now),
            SpotEntity("spot_office_desk", officeId, "书桌", 120f, 140f, now, now),
            SpotEntity("spot_cabinet", officeId, "储物柜", 240f, 200f, now, now),
        )
        dao.upsertSpots(spots)

        val items = listOf(
            ItemEntity("i1", "spot_sofa", "沙发", null, null, null, null, 1, 0, now, now),
            ItemEntity("i2", "spot_sofa", "抱枕", null, null, null, null, 2, 0, now, now),
            ItemEntity("i3", "spot_sofa", "落地灯", null, null, null, null, 1, 0, now, now),
            ItemEntity("i4", "spot_table", "茶几", null, null, null, null, 1, 0, now, now),
            ItemEntity("i5", "spot_table", "香薰", null, null, null, null, 1, 0, now, now),
            ItemEntity("i6", "spot_tv", "电视", null, null, null, null, 1, 0, now, now),
            ItemEntity("i7", "spot_tv", "音响", null, null, null, null, 1, 0, now, now),
            ItemEntity("i8", "spot_bedside", "床头灯", null, null, null, null, 1, 0, now, now),
            ItemEntity("i9", "spot_bedside", "香薰机", null, null, null, null, 1, 0, now, now),
            ItemEntity("i10", "spot_closet", "衣柜收纳", null, null, null, null, 1, 0, now, now),
            ItemEntity("i11", "spot_desk", "手账本", null, null, null, null, 1, 0, now, now),
            ItemEntity("i12", "spot_office_desk", "显示器", null, null, null, null, 1, 0, now, now),
            ItemEntity("i13", "spot_office_desk", "键盘", null, null, null, null, 1, 0, now, now),
            ItemEntity("i14", "spot_office_desk", "笔记本", null, null, null, null, 1, 0, now, now),
            ItemEntity("i15", "spot_cabinet", "文件盒", null, null, null, null, 1, 0, now, now),
            ItemEntity("i16", "spot_cabinet", "充电器", null, null, null, null, 1, 0, now, now),
        )
        items.forEach { dao.upsertItem(it) }
    }

    private suspend fun createSpaceSuspend(
        name: String,
        coverImagePath: String?,
        templateId: String?,
    ): String {
        val now = System.currentTimeMillis()
        val spaceId = UUID.randomUUID().toString()
        dao.upsertSpace(
            SpaceEntity(
                id = spaceId,
                name = name.trim(),
                coverImagePath = coverImagePath,
                createdAt = now,
                updatedAt = now
            )
        )

        val template = templateId?.let { id -> Templates.all.firstOrNull { it.id == id } }
        if (template != null) {
            ensureTemplateTags(template)
            val spots = template.spotNames.mapIndexed { idx, spotName ->
                val pos = templateSpotPosition(idx, template.spotNames.size)
                SpotEntity(
                    id = UUID.randomUUID().toString(),
                    spaceId = spaceId,
                    name = spotName,
                    x = pos.x,
                    y = pos.y,
                    createdAt = now,
                    updatedAt = now
                )
            }
            dao.upsertSpots(spots)
        }
        return spaceId
    }

    private suspend fun addDemoDataSuspend() {
        val existingNames = dao.listAllSpaces().map { it.name }.toSet()
        val existingListNames = dao.listAllLists().map { it.name }.toSet()
        val now = System.currentTimeMillis()

        val demoSpaces = listOf(
            Triple("演示-药箱", "medicine", SampleCovers.livingRoom),
            Triple("演示-衣柜", "closet", SampleCovers.bedroom),
            Triple("演示-工具箱", "tools", SampleCovers.office),
        )

        val nameToSpaceId = mutableMapOf<String, String>()
        for ((spaceName, templateId, coverBitmap) in demoSpaces) {
            if (existingNames.contains(spaceName)) continue
            val coverPath = InternalImageStore.persistBitmap(getApplication(), coverBitmap)
            val spaceId = createSpaceSuspend(spaceName, coverPath, templateId)
            nameToSpaceId[spaceName] = spaceId
        }

        val tagsByName = dao.listAllTags().associateBy { it.name }.mapValues { it.value.id }.toMutableMap()
        suspend fun ensureTag(name: String): String {
            val existing = tagsByName[name]
            if (existing != null) return existing
            val id = UUID.randomUUID().toString()
            dao.upsertTag(TagEntity(id = id, name = name, parentId = null, createdAt = now))
            tagsByName[name] = id
            return id
        }

        fun futureDays(days: Int): Long = now + TimeUnit.DAYS.toMillis(days.toLong())

        suspend fun addItem(
            spaceId: String,
            spotName: String,
            name: String,
            note: String?,
            expiryInDays: Int?,
            currentQty: Int,
            minQty: Int,
            tagNames: List<String>,
        ) {
            val spots = dao.listSpotsForSpace(spaceId)
            val spotId = spots.firstOrNull { it.name == spotName }?.id ?: spots.firstOrNull()?.id ?: return
            val itemId = UUID.randomUUID().toString()
            dao.upsertItem(
                ItemEntity(
                    id = itemId,
                    spotId = spotId,
                    name = name,
                    note = note,
                    imagePath = null,
                    expiryDateEpochMs = expiryInDays?.let { futureDays(it) },
                    lastUsedAtEpochMs = null,
                    currentQuantity = currentQty,
                    minQuantity = minQty,
                    createdAt = now,
                    updatedAt = now
                )
            )
            val tagIds = tagNames.map { ensureTag(it) }
            repo.setTagsForItem(itemId, tagIds)
        }

        nameToSpaceId["演示-药箱"]?.let { spaceId ->
            addItem(spaceId, "常用药", "布洛芬", "发烧/疼痛", expiryInDays = 2, currentQty = 1, minQty = 1, tagNames = listOf("退烧"))
            addItem(spaceId, "外用药", "创可贴", "小伤口", expiryInDays = null, currentQty = 4, minQty = 10, tagNames = listOf("外伤"))
            addItem(spaceId, "器材", "体温计", "测温", expiryInDays = null, currentQty = 1, minQty = 1, tagNames = listOf("器材"))
        }
        nameToSpaceId["演示-衣柜"]?.let { spaceId ->
            addItem(spaceId, "抽屉", "袜子", "冬季厚袜", expiryInDays = null, currentQty = 2, minQty = 6, tagNames = listOf("袜子"))
            addItem(spaceId, "中层", "T恤", "白色/黑色", expiryInDays = null, currentQty = 5, minQty = 0, tagNames = listOf("上衣"))
            addItem(spaceId, "上层", "外套", "不常用", expiryInDays = null, currentQty = 2, minQty = 0, tagNames = listOf("外套"))
        }
        nameToSpaceId["演示-工具箱"]?.let { spaceId ->
            addItem(spaceId, "耗材", "AA 电池", "遥控器/玩具", expiryInDays = null, currentQty = 0, minQty = 4, tagNames = listOf("耗材"))
            addItem(spaceId, "螺丝刀", "十字螺丝刀", "PH2", expiryInDays = null, currentQty = 1, minQty = 1, tagNames = listOf("五金"))
        }

        if (!existingListNames.contains("演示-旅行清单")) {
            val listId = UUID.randomUUID().toString()
            dao.upsertList(PackingListEntity(id = listId, name = "演示-旅行清单", createdAt = now, updatedAt = now))
            val listItems = listOf(
                "身份证/护照",
                "充电器",
                "数据线",
                "牙刷牙膏",
                "换洗衣物",
            ).map { itemName ->
                PackingListItemEntity(
                    id = UUID.randomUUID().toString(),
                    listId = listId,
                    name = itemName,
                    checked = false,
                    linkedItemId = null,
                    quantityNeeded = null,
                    createdAt = now,
                    updatedAt = now
                )
            }
            dao.upsertListItems(listItems)
        }
    }

    private suspend fun ensureTemplateTags(template: SpaceTemplate) {
        val existing = dao.listAllTags()
        val nameToId = existing.associateBy { it.name }.mapValues { it.value.id }.toMutableMap()
        val now = System.currentTimeMillis()

        suspend fun upsertTag(name: String, parentId: String?): String {
            val id = nameToId[name] ?: UUID.randomUUID().toString().also { nameToId[name] = it }
            dao.upsertTag(TagEntity(id = id, name = name, parentId = parentId, createdAt = now))
            return id
        }

        val parentNameToId = mutableMapOf<String, String>()
        for (tag in template.defaultTags) {
            val parentId = tag.parentName?.let { parent ->
                parentNameToId[parent] ?: upsertTag(parent, null).also { parentNameToId[parent] = it }
            }
            upsertTag(tag.name, parentId)
        }
    }

    private fun templateSpotPosition(index: Int, total: Int): Offset {
        if (total <= 0) return Offset(140f, 140f)
        val cols = 3
        val row = index / cols
        val col = index % cols
        val x = 70f + col * 120f
        val y = 90f + row * 110f
        return Offset(x, y)
    }

    companion object {
        fun parseDateToEpochMs(text: String): Long? {
            val trimmed = text.trim()
            if (trimmed.isBlank()) return null
            return try {
                val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
                fmt.parse(trimmed)?.time
            } catch (_: Throwable) {
                null
            }
        }

        fun formatEpochMsToDate(epochMs: Long?): String {
            if (epochMs == null) return ""
            return formatDateOnly(epochMs)
        }

        private fun formatDateOnly(epochMs: Long): String {
            return try {
                val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                fmt.format(Date(epochMs))
            } catch (_: Throwable) {
                ""
            }
        }
    }
}
