/**
 * 应用的核心业务逻辑处理类（ViewModel）。
 *
 * 职责：
 * - 连接数据层（Room DAO）和 UI 层（Compose Screens）。
 * - 处理各种用户交互逻辑（增删改查场所、文件夹、物品、清单、标签等）。
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
import com.example.myapplication.data.FolderEntity
import com.example.myapplication.data.ItemEntity
import com.example.myapplication.data.ItemSearchResultRow
import com.example.myapplication.data.LocationEntity
import com.example.myapplication.data.PackingListEntity
import com.example.myapplication.data.PackingListItemEntity
import com.example.myapplication.data.AppRepository
import com.example.myapplication.data.TagEntity
import com.example.myapplication.data.toDomain
import com.example.myapplication.settings.SettingsRepository
import com.example.myapplication.settings.UserSettings
import com.example.myapplication.storage.InternalImageStore
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

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SpaceViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.get(application)
    private val dao = db.dao()
    private val repo = AppRepository(db)
    private val settingsRepo = SettingsRepository(application)

    val settings: StateFlow<UserSettings> =
        settingsRepo.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserSettings())

    // ==================== 场所 (Location) ====================

    val locations: StateFlow<List<Location>> = dao.observeLocationSummaries()
        .map { rows -> rows.map { it.toDomain() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addLocation(name: String, icon: String?, coverImagePath: String?) {
        require(name.trim().isNotBlank()) { "Location name cannot be blank" }
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val maxOrder = dao.listAllLocations().maxOfOrNull { it.sortOrder } ?: 0
            dao.upsertLocation(
                LocationEntity(
                    id = UUID.randomUUID().toString(),
                    name = name.trim(),
                    icon = icon,
                    coverImagePath = coverImagePath,
                    sortOrder = maxOrder + 1,
                    createdAt = now,
                    updatedAt = now
                )
            )
        }
    }

    fun updateLocation(locationId: String, name: String? = null, icon: String? = null, coverImagePath: String? = null) {
        require(name == null || name.trim().isNotBlank()) { "Location name cannot be blank" }
        viewModelScope.launch {
            val location = dao.getLocation(locationId) ?: return@launch
            dao.upsertLocation(
                location.copy(
                    name = name?.trim() ?: location.name,
                    icon = icon ?: location.icon,
                    coverImagePath = coverImagePath ?: location.coverImagePath,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun removeLocation(locationId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            // 清理图片
            val location = dao.getLocation(locationId)
            location?.coverImagePath?.let { InternalImageStore.delete(getApplication(), it) }
            
            // 清理文件夹封面和物品图片
            val folders = dao.listFoldersInLocation(locationId)
            for (folder in folders) {
                folder.coverImagePath?.let { InternalImageStore.delete(getApplication(), it) }
                val items = dao.getItemsInFolder(folder.id)
                items.forEach { item ->
                    item.imagePath?.let { InternalImageStore.delete(getApplication(), it) }
                }
            }
            
            dao.deleteLocation(locationId)
        }
    }

    fun observeLocation(locationId: String): Flow<Location?> {
        return dao.observeLocationSummaries().map { list ->
            list.firstOrNull { it.id == locationId }?.toDomain()
        }
    }

    // ==================== 文件夹 (Folder) ====================

    fun observeFolders(locationId: String, parentId: String?): Flow<List<Folder>> {
        return dao.observeFoldersByParent(locationId, parentId).map { rows ->
            rows.map { it.toDomain() }
        }
    }

    fun observeFolder(folderId: String): Flow<Folder?> {
        return dao.observeFolder(folderId).map { it?.toDomain() }
    }

    fun addFolder(
        locationId: String,
        parentId: String?,
        name: String,
        icon: String? = null,
        coverImagePath: String? = null,
        enableMapView: Boolean = false
    ) {
        require(name.trim().isNotBlank()) { "Folder name cannot be blank" }
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val siblings = dao.listFoldersByParent(locationId, parentId)
            val maxOrder = siblings.maxOfOrNull { it.sortOrder } ?: 0
            dao.upsertFolder(
                FolderEntity(
                    id = UUID.randomUUID().toString(),
                    locationId = locationId,
                    parentId = parentId,
                    name = name.trim(),
                    icon = icon,
                    coverImagePath = coverImagePath,
                    enableMapView = enableMapView,
                    mapX = null,
                    mapY = null,
                    sortOrder = maxOrder + 1,
                    createdAt = now,
                    updatedAt = now
                )
            )
        }
    }

    fun updateFolder(
        folderId: String,
        name: String? = null,
        icon: String? = null,
        coverImagePath: String? = null,
        enableMapView: Boolean? = null,
        mapPosition: Offset? = null
    ) {
        viewModelScope.launch {
            val folder = dao.getFolder(folderId) ?: return@launch
            dao.updateFolder(
                folder.copy(
                    name = name?.trim() ?: folder.name,
                    icon = icon ?: folder.icon,
                    coverImagePath = coverImagePath ?: folder.coverImagePath,
                    enableMapView = enableMapView ?: folder.enableMapView,
                    mapX = mapPosition?.x ?: folder.mapX,
                    mapY = mapPosition?.y ?: folder.mapY,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun updateFolderMapPosition(folderId: String, position: Offset) {
        viewModelScope.launch {
            val folder = dao.getFolder(folderId) ?: return@launch
            dao.updateFolder(
                folder.copy(
                    mapX = position.x,
                    mapY = position.y,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun removeFolder(folderId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            // 递归清理所有子区域的图片
            cleanupFolderResources(folderId)
            
            // 删除文件夹（外键级联会自动删除所有子区域和物品）
            dao.deleteFolder(folderId)
        }
    }
    
    /**
     * 递归清理区域及其所有子区域的图片资源
     */
    private suspend fun cleanupFolderResources(folderId: String) {
        // 获取当前区域
        val folder = dao.getFolder(folderId) ?: return
        
        // 清理当前区域的封面图
        folder.coverImagePath?.let { InternalImageStore.delete(getApplication(), it) }
        
        // 清理当前区域内的物品图片
        val items = dao.getItemsInFolder(folderId)
        items.forEach { item ->
            item.imagePath?.let { InternalImageStore.delete(getApplication(), it) }
        }
        
        // 递归清理所有子区域
        val subFolders = dao.listFoldersByParent(folder.locationId, folderId)
        subFolders.forEach { subFolder ->
            cleanupFolderResources(subFolder.id)
        }
    }

    // ==================== 面包屑导航 ====================

    fun getBreadcrumbs(locationId: String, folderId: String?): Flow<List<BreadcrumbItem>> {
        return flow {
            val breadcrumbs = mutableListOf<BreadcrumbItem>()
            
            // 添加场所
            val location = dao.getLocation(locationId)
            if (location != null) {
                breadcrumbs.add(BreadcrumbItem(location.id, location.name, isLocation = true))
            }
            
            // 添加文件夹路径（带循环检测和深度限制）
            if (folderId != null) {
                val path = mutableListOf<BreadcrumbItem>()
                val visited = mutableSetOf<String>()
                var currentId: String? = folderId
                var depth = 0
                val maxDepth = 100  // 防御性限制，正常情况下不会超过 10 层
                
                while (currentId != null && depth < maxDepth) {
                    // 检测循环引用
                    if (currentId in visited) {
                        // 循环检测到，停止遍历
                        break
                    }
                    visited.add(currentId)
                    
                    val folder = dao.getFolder(currentId)
                    if (folder != null) {
                        path.add(0, BreadcrumbItem(folder.id, folder.name, isLocation = false))
                        currentId = folder.parentId
                    } else {
                        break
                    }
                    
                    depth++
                }
                breadcrumbs.addAll(path)
            }
            
            emit(breadcrumbs)
        }
    }

    // ==================== 物品 (Item) ====================

    fun observeItemsInFolder(folderId: String): Flow<List<Item>> {
        return dao.observeItemsInFolder(folderId).map { list ->
            list.map { it.toDomain() }
        }
    }

    fun addItemToFolder(
        folderId: String,
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
                    folderId = folderId,
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
        folderId: String,
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
                        folderId = folderId,
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

    fun renameItem(itemId: String, newName: String) {
        viewModelScope.launch { updateItem(itemId) { it.copy(name = newName.trim()) } }
    }

    fun updateItemNote(itemId: String, note: String?) {
        viewModelScope.launch {
            updateItem(itemId) { it.copy(note = note?.trim().takeIf { !it.isNullOrBlank() }) }
        }
    }

    fun updateItemImage(itemId: String, imagePath: String?) {
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
        folderId: String
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
                folderId = folderId,
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

    fun removeItem(itemId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val item = dao.getItem(itemId)
            item?.imagePath?.let { path -> InternalImageStore.delete(getApplication(), path) }
            dao.deleteItem(itemId)
        }
    }

    private suspend fun updateItem(itemId: String, transform: (ItemEntity) -> ItemEntity) {
        val current = dao.getItem(itemId) ?: return
        dao.updateItem(transform(current).copy(updatedAt = System.currentTimeMillis()))
    }

    // ==================== 标签 (Tag) ====================

    val tags: StateFlow<List<Tag>> = dao.observeTags()
        .map { list -> list.map { it.toDomain() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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

    // ==================== 搜索 ====================

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

    fun setSearchQuery(q: String) {
        searchQuery.value = q
    }

    private fun ItemSearchResultRow.toDomain(): ItemSearchResult = ItemSearchResult(
        itemId = itemId,
        itemName = itemName,
        note = note,
        imagePath = imagePath,
        locationId = locationId,
        locationName = locationName,
        folderId = folderId,
        folderName = folderName
    )

    // ==================== 过期物品 ====================

    val expiringItemsCount: StateFlow<Int> = flow {
        val now = System.currentTimeMillis()
        val sevenDaysLater = now + TimeUnit.DAYS.toMillis(7)
        emitAll(dao.observeExpiringItemsCount(now, sevenDaysLater))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    // ==================== 清单 (List) ====================

    val lists: StateFlow<List<PackingList>> = dao.observeLists()
        .map { list -> list.map { it.toDomain() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun observeListItems(listId: String): Flow<List<PackingListItem>> {
        return dao.observeListItems(listId).map { it.map { row -> row.toDomain() } }
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

    // ==================== 设置 ====================

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

    // ==================== 图片工具 ====================

    fun persistBitmap(bitmap: Bitmap): String? = InternalImageStore.persistBitmap(getApplication(), bitmap)

    fun persistGalleryUri(uri: android.net.Uri): String? = InternalImageStore.copyFromGalleryToInternal(getApplication(), uri)

    fun createTempCameraUri(): android.net.Uri = InternalImageStore.createTempCameraUri(getApplication())

    fun persistCapturedPhoto(tempUri: android.net.Uri): String? = InternalImageStore.persistFromUri(getApplication(), tempUri)

    // ==================== 初始数据 ====================

    init {
        viewModelScope.launch {
            if (dao.countLocations() == 0) seedInitialData()
        }
    }

    private suspend fun seedInitialData() {
        val now = System.currentTimeMillis()
        
        // 创建默认场所
        val homeId = UUID.randomUUID().toString()
        dao.upsertLocation(
            LocationEntity(
                id = homeId,
                name = "我的家",
                icon = "🏠",
                coverImagePath = null,
                sortOrder = 1,
                createdAt = now,
                updatedAt = now
            )
        )
        
        // 创建一些默认文件夹
        val livingRoomId = UUID.randomUUID().toString()
        val bedroomId = UUID.randomUUID().toString()
        dao.upsertFolder(FolderEntity(livingRoomId, homeId, null, "客厅", "🛋️", null, false, null, null, 1, now, now))
        dao.upsertFolder(FolderEntity(bedroomId, homeId, null, "卧室", "🛏️", null, false, null, null, 2, now, now))
        dao.upsertFolder(FolderEntity(UUID.randomUUID().toString(), homeId, null, "厨房", "🍳", null, false, null, null, 3, now, now))
        
        // 在客厅创建子文件夹
        dao.upsertFolder(FolderEntity(UUID.randomUUID().toString(), homeId, livingRoomId, "电视柜", "📺", null, false, null, null, 1, now, now))
        dao.upsertFolder(FolderEntity(UUID.randomUUID().toString(), homeId, livingRoomId, "书架", "📚", null, false, null, null, 2, now, now))
    }

    private suspend fun addDemoDataSuspend() {
        val now = System.currentTimeMillis()
        
        // 检查是否已有演示数据
        val existingLocations = dao.listAllLocations().map { it.name }.toSet()
        if (existingLocations.contains("演示-办公室")) return
        
        // 创建演示场所
        val officeId = UUID.randomUUID().toString()
        dao.upsertLocation(
            LocationEntity(
                id = officeId,
                name = "演示-办公室",
                icon = "🏢",
                coverImagePath = null,
                sortOrder = 100,
                createdAt = now,
                updatedAt = now
            )
        )
        
        // 创建文件夹
        val deskId = UUID.randomUUID().toString()
        dao.upsertFolder(FolderEntity(deskId, officeId, null, "书桌", "🪑", null, false, null, null, 1, now, now))
        dao.upsertFolder(FolderEntity(UUID.randomUUID().toString(), officeId, null, "储物柜", "🗄️", null, false, null, null, 2, now, now))
        
        // 添加物品
        dao.upsertItem(ItemEntity(UUID.randomUUID().toString(), deskId, "显示器", null, null, null, null, 1, 1, now, now))
        dao.upsertItem(ItemEntity(UUID.randomUUID().toString(), deskId, "键盘", null, null, null, null, 1, 1, now, now))
        dao.upsertItem(ItemEntity(UUID.randomUUID().toString(), deskId, "鼠标", null, null, null, null, 1, 1, now, now))
        
        // 创建演示清单
        val existingListNames = dao.listAllLists().map { it.name }.toSet()
        if (!existingListNames.contains("演示-旅行清单")) {
            val listId = UUID.randomUUID().toString()
            dao.upsertList(PackingListEntity(id = listId, name = "演示-旅行清单", createdAt = now, updatedAt = now))
            val listItems = listOf("身份证/护照", "充电器", "数据线", "牙刷牙膏", "换洗衣物").map { itemName ->
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
