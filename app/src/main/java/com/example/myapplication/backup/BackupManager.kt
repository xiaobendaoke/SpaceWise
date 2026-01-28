/**
 * 备份与还原管理器。
 *
 * 职责：
 * - 将数据库数据与图片打包成 ZIP 导出。
 * - 从备份 ZIP 中解析并还原数据及图片。
 *
 * 上层用途：
 * - 在设置页面为用户提供数据导出和恢复功能。
 */
package com.example.myapplication.backup

import android.content.Context
import android.net.Uri
import com.example.myapplication.data.AppDatabase
import com.example.myapplication.data.AppRepository
import com.example.myapplication.data.FolderEntity
import com.example.myapplication.data.ItemEntity
import com.example.myapplication.data.ItemTagCrossRef
import com.example.myapplication.data.LocationEntity
import com.example.myapplication.data.PackingListEntity
import com.example.myapplication.data.PackingListItemEntity
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
        val locations = dao.listAllLocations()
        val folders = dao.listAllFolders()
        val items = dao.listAllItems()
        val tags = dao.listAllTags()
        val itemTags = dao.listAllItemTags()
        val lists = dao.listAllLists()
        val listItems = dao.listAllListItems()

        val json = JSONObject().apply {
            put("version", 2)  // 新版本
            put("locations", locations.toJsonArray { it.toJson() })
            put("folders", folders.toJsonArray { it.toJson() })
            put("items", items.toJsonArray { it.toJson() })
            put("tags", tags.toJsonArray { it.toJson() })
            put("itemTags", itemTags.toJsonArray { it.toJson() })
            put("lists", lists.toJsonArray { it.toJson() })
            put("listItems", listItems.toJsonArray { it.toJson() })
        }

        val imagesToInclude = (
            locations.mapNotNull { it.coverImagePath } + 
            folders.mapNotNull { it.coverImagePath } + 
            items.mapNotNull { it.imagePath }
        ).distinct().mapNotNull { path ->
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
        require(version >= 1) { "不支持的备份版本: $version" }

        clearInternalImages(context)
        for ((path, bytes) in images) {
            val outFile = File(context.filesDir, path)
            outFile.parentFile?.mkdirs()
            outFile.outputStream().use { it.write(bytes) }
        }

        val db = AppDatabase.get(context)
        val dao = db.dao()
        AppRepository(db).wipeAllData()

        if (version >= 2) {
            // 新版本格式
            val locations = root.getJSONArray("locations").toLocations()
            val folders = root.getJSONArray("folders").toFolders()
            val items = root.getJSONArray("items").toItems()
            val tags = root.getJSONArray("tags").toTags()
            val itemTags = root.getJSONArray("itemTags").toItemTags()
            val lists = root.getJSONArray("lists").toLists()
            val listItems = root.getJSONArray("listItems").toListItems()

            for (location in locations) dao.upsertLocation(location)
            for (folder in folders) dao.upsertFolder(folder)
            for (item in items) dao.upsertItem(item)
            for (tag in tags) dao.upsertTag(tag)
            dao.addItemTags(itemTags)
            for (list in lists) dao.upsertList(list)
            dao.upsertListItems(listItems)
        } else {
            // 旧版本格式 - 忽略数据（已使用销毁性迁移，无法恢复旧格式）
            // 用户需要重新开始
        }
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

    private fun LocationEntity.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("icon", icon)
        put("coverImagePath", coverImagePath)
        put("sortOrder", sortOrder)
        put("createdAt", createdAt)
        put("updatedAt", updatedAt)
    }

    private fun FolderEntity.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("locationId", locationId)
        put("parentId", parentId)
        put("name", name)
        put("icon", icon)
        put("coverImagePath", coverImagePath)
        put("enableMapView", enableMapView)
        put("mapX", mapX?.toDouble())
        put("mapY", mapY?.toDouble())
        put("sortOrder", sortOrder)
        put("createdAt", createdAt)
        put("updatedAt", updatedAt)
    }

    private fun ItemEntity.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("folderId", folderId)
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

    private fun JSONArray.toLocations(): List<LocationEntity> = (0 until length()).map { idx ->
        val o = getJSONObject(idx)
        LocationEntity(
            id = o.getString("id"),
            name = o.getString("name"),
            icon = o.optString("icon").takeIf { it.isNotBlank() },
            coverImagePath = o.optString("coverImagePath").takeIf { it.isNotBlank() },
            sortOrder = o.optInt("sortOrder", 0),
            createdAt = o.getLong("createdAt"),
            updatedAt = o.getLong("updatedAt"),
        )
    }

    private fun JSONArray.toFolders(): List<FolderEntity> = (0 until length()).map { idx ->
        val o = getJSONObject(idx)
        FolderEntity(
            id = o.getString("id"),
            locationId = o.getString("locationId"),
            parentId = o.optString("parentId").takeIf { it.isNotBlank() },
            name = o.getString("name"),
            icon = o.optString("icon").takeIf { it.isNotBlank() },
            coverImagePath = o.optString("coverImagePath").takeIf { it.isNotBlank() },
            enableMapView = o.optBoolean("enableMapView", false),
            mapX = if (o.isNull("mapX")) null else o.getDouble("mapX").toFloat(),
            mapY = if (o.isNull("mapY")) null else o.getDouble("mapY").toFloat(),
            sortOrder = o.optInt("sortOrder", 0),
            createdAt = o.getLong("createdAt"),
            updatedAt = o.getLong("updatedAt"),
        )
    }

    private fun JSONArray.toItems(): List<ItemEntity> = (0 until length()).map { idx ->
        val o = getJSONObject(idx)
        ItemEntity(
            id = o.getString("id"),
            folderId = o.getString("folderId"),
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
