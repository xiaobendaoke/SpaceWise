package com.example.myapplication.backup

import android.content.Context
import android.net.Uri
import com.example.myapplication.data.AppDatabase
import com.example.myapplication.data.AppRepository
import com.example.myapplication.data.ItemEntity
import com.example.myapplication.data.ItemTagCrossRef
import com.example.myapplication.data.PackingListEntity
import com.example.myapplication.data.PackingListItemEntity
import com.example.myapplication.data.SpaceEntity
import com.example.myapplication.data.SpotEntity
import com.example.myapplication.data.TagEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object BackupManager {
    private const val DATA_JSON = "data.json"

    suspend fun exportToZip(context: Context, destination: Uri) {
        val dao = AppDatabase.get(context).dao()
        val spaces = dao.listAllSpaces()
        val spots = dao.listAllSpots()
        val items = dao.listAllItems()
        val tags = dao.listAllTags()
        val itemTags = dao.listAllItemTags()
        val lists = dao.listAllLists()
        val listItems = dao.listAllListItems()

        val json = JSONObject().apply {
            put("version", 1)
            put("spaces", spaces.toJsonArray { it.toJson() })
            put("spots", spots.toJsonArray { it.toJson() })
            put("items", items.toJsonArray { it.toJson() })
            put("tags", tags.toJsonArray { it.toJson() })
            put("itemTags", itemTags.toJsonArray { it.toJson() })
            put("lists", lists.toJsonArray { it.toJson() })
            put("listItems", listItems.toJsonArray { it.toJson() })
        }

        val imagesToInclude = (spaces.mapNotNull { it.coverImagePath } + items.mapNotNull { it.imagePath })
            .distinct()
            .mapNotNull { path ->
                val file = File(context.filesDir, path)
                if (file.exists() && file.isFile) path else null
            }

        context.contentResolver.openOutputStream(destination)?.use { outputStream ->
            ZipOutputStream(BufferedOutputStream(outputStream)).use { zip ->
                zip.putNextEntry(ZipEntry(DATA_JSON))
                zip.write(json.toString().toByteArray(Charsets.UTF_8))
                zip.closeEntry()

                for (relativePath in imagesToInclude) {
                    val file = File(context.filesDir, relativePath)
                    zip.putNextEntry(ZipEntry(relativePath))
                    file.inputStream().use { input -> input.copyTo(zip) }
                    zip.closeEntry()
                }
            }
        } ?: error("无法写入导出文件")
    }

    suspend fun importFromZip(context: Context, source: Uri) {
        val resolver = context.contentResolver
        val zipInput = resolver.openInputStream(source) ?: error("无法读取导入文件")

        val extractedJson = StringBuilder()
        val images = mutableMapOf<String, ByteArray>()

        ZipInputStream(BufferedInputStream(zipInput)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    if (entry.name == DATA_JSON) {
                        extractedJson.append(zip.readBytes().toString(Charsets.UTF_8))
                    } else {
                        images[entry.name] = zip.readBytes()
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }

        val root = JSONObject(extractedJson.toString())
        val version = root.optInt("version", 1)
        require(version == 1) { "不支持的备份版本: $version" }

        clearInternalImages(context)
        for ((path, bytes) in images) {
            val outFile = File(context.filesDir, path)
            outFile.parentFile?.mkdirs()
            outFile.outputStream().use { it.write(bytes) }
        }

        val db = AppDatabase.get(context)
        val dao = db.dao()
        AppRepository(db).wipeAllData()

        val spaces = root.getJSONArray("spaces").toSpaces()
        val spots = root.getJSONArray("spots").toSpots()
        val items = root.getJSONArray("items").toItems()
        val tags = root.getJSONArray("tags").toTags()
        val itemTags = root.getJSONArray("itemTags").toItemTags()
        val lists = root.getJSONArray("lists").toLists()
        val listItems = root.getJSONArray("listItems").toListItems()

        for (space in spaces) dao.upsertSpace(space)
        dao.upsertSpots(spots)
        for (item in items) dao.upsertItem(item)
        for (tag in tags) dao.upsertTag(tag)
        dao.addItemTags(itemTags)
        for (list in lists) dao.upsertList(list)
        dao.upsertListItems(listItems)
    }

    private fun clearInternalImages(context: Context) {
        val imagesDir = File(context.filesDir, "images")
        if (!imagesDir.exists()) return
        imagesDir.deleteRecursively()
    }

    private inline fun <T> List<T>.toJsonArray(toJson: (T) -> JSONObject): JSONArray {
        val array = JSONArray()
        for (item in this) array.put(toJson(item))
        return array
    }

    private fun SpaceEntity.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("coverImagePath", coverImagePath)
        put("createdAt", createdAt)
        put("updatedAt", updatedAt)
    }

    private fun SpotEntity.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("spaceId", spaceId)
        put("name", name)
        put("x", x.toDouble())
        put("y", y.toDouble())
        put("createdAt", createdAt)
        put("updatedAt", updatedAt)
    }

    private fun ItemEntity.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("spotId", spotId)
        put("name", name)
        put("note", note)
        put("imagePath", imagePath)
        put("expiryDateEpochMs", expiryDateEpochMs)
        put("lastUsedAtEpochMs", lastUsedAtEpochMs)
        put("currentQuantity", currentQuantity)
        put("minQuantity", minQuantity)
        put("createdAt", createdAt)
        put("updatedAt", updatedAt)
    }

    private fun TagEntity.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("parentId", parentId)
        put("createdAt", createdAt)
    }

    private fun ItemTagCrossRef.toJson(): JSONObject = JSONObject().apply {
        put("itemId", itemId)
        put("tagId", tagId)
    }

    private fun PackingListEntity.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("createdAt", createdAt)
        put("updatedAt", updatedAt)
    }

    private fun PackingListItemEntity.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("listId", listId)
        put("name", name)
        put("checked", checked)
        put("linkedItemId", linkedItemId)
        put("quantityNeeded", quantityNeeded)
        put("createdAt", createdAt)
        put("updatedAt", updatedAt)
    }

    private fun JSONArray.toSpaces(): List<SpaceEntity> = (0 until length()).map { idx ->
        val o = getJSONObject(idx)
        SpaceEntity(
            id = o.getString("id"),
            name = o.getString("name"),
            coverImagePath = o.optString("coverImagePath").takeIf { it.isNotBlank() },
            createdAt = o.getLong("createdAt"),
            updatedAt = o.getLong("updatedAt"),
        )
    }

    private fun JSONArray.toSpots(): List<SpotEntity> = (0 until length()).map { idx ->
        val o = getJSONObject(idx)
        SpotEntity(
            id = o.getString("id"),
            spaceId = o.getString("spaceId"),
            name = o.getString("name"),
            x = o.getDouble("x").toFloat(),
            y = o.getDouble("y").toFloat(),
            createdAt = o.getLong("createdAt"),
            updatedAt = o.getLong("updatedAt"),
        )
    }

    private fun JSONArray.toItems(): List<ItemEntity> = (0 until length()).map { idx ->
        val o = getJSONObject(idx)
        ItemEntity(
            id = o.getString("id"),
            spotId = o.getString("spotId"),
            name = o.getString("name"),
            note = o.optString("note").takeIf { it.isNotBlank() },
            imagePath = o.optString("imagePath").takeIf { it.isNotBlank() },
            expiryDateEpochMs = o.optLongOrNull("expiryDateEpochMs"),
            lastUsedAtEpochMs = o.optLongOrNull("lastUsedAtEpochMs"),
            currentQuantity = o.optInt("currentQuantity", 1),
            minQuantity = o.optInt("minQuantity", 0),
            createdAt = o.getLong("createdAt"),
            updatedAt = o.getLong("updatedAt"),
        )
    }

    private fun JSONArray.toTags(): List<TagEntity> = (0 until length()).map { idx ->
        val o = getJSONObject(idx)
        TagEntity(
            id = o.getString("id"),
            name = o.getString("name"),
            parentId = o.optString("parentId").takeIf { it.isNotBlank() },
            createdAt = o.getLong("createdAt"),
        )
    }

    private fun JSONArray.toItemTags(): List<ItemTagCrossRef> = (0 until length()).map { idx ->
        val o = getJSONObject(idx)
        ItemTagCrossRef(
            itemId = o.getString("itemId"),
            tagId = o.getString("tagId"),
        )
    }

    private fun JSONArray.toLists(): List<PackingListEntity> = (0 until length()).map { idx ->
        val o = getJSONObject(idx)
        PackingListEntity(
            id = o.getString("id"),
            name = o.getString("name"),
            createdAt = o.getLong("createdAt"),
            updatedAt = o.getLong("updatedAt"),
        )
    }

    private fun JSONArray.toListItems(): List<PackingListItemEntity> = (0 until length()).map { idx ->
        val o = getJSONObject(idx)
        PackingListItemEntity(
            id = o.getString("id"),
            listId = o.getString("listId"),
            name = o.getString("name"),
            checked = o.getBoolean("checked"),
            linkedItemId = o.optString("linkedItemId").takeIf { it.isNotBlank() },
            quantityNeeded = if (o.isNull("quantityNeeded")) null else o.getInt("quantityNeeded"),
            createdAt = o.getLong("createdAt"),
            updatedAt = o.getLong("updatedAt"),
        )
    }

    private fun JSONObject.optLongOrNull(key: String): Long? {
        return if (isNull(key)) null else optLong(key)
    }
}
