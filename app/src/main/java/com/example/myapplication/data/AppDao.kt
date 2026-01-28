/**
 * 数据库访问对象（DAO）。
 *
 * 职责：
 * - 定义所有的 SQL 查询操作（增删改查）。
 * - 使用 `Flow` 提供响应式数据流。
 *
 * 上层用途：
 * - 被 `AppRepository` 和 `SpaceViewModel` 调用以执行具体的数据操作。
 */
package com.example.myapplication.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {

    // ==================== 场所 (Location) ====================

    @Query(
        """
        SELECT l.id, l.name, l.icon, l.coverImagePath,
               (SELECT COUNT(*) FROM folders f WHERE f.locationId = l.id) AS folderCount,
               (SELECT COUNT(*) FROM items i 
                JOIN folders f2 ON i.folderId = f2.id 
                WHERE f2.locationId = l.id) AS itemCount
        FROM locations l
        ORDER BY l.sortOrder ASC, l.createdAt DESC
        """
    )
    fun observeLocationSummaries(): Flow<List<LocationSummaryRow>>

    @Query("SELECT * FROM locations WHERE id = :locationId LIMIT 1")
    suspend fun getLocation(locationId: String): LocationEntity?

    @Query("SELECT COUNT(*) FROM locations")
    suspend fun countLocations(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLocation(location: LocationEntity): Long

    @Query("DELETE FROM locations WHERE id = :locationId")
    suspend fun deleteLocation(locationId: String): Int

    @Query("SELECT * FROM locations ORDER BY sortOrder ASC, createdAt DESC")
    suspend fun listAllLocations(): List<LocationEntity>

    // ==================== 文件夹 (Folder) ====================

    @Query(
        """
        SELECT f.id, f.locationId, f.parentId, f.name, f.icon, f.coverImagePath, 
               f.enableMapView, f.mapX, f.mapY,
               (SELECT COUNT(*) FROM folders sub WHERE sub.parentId = f.id) AS subFolderCount,
               (SELECT COUNT(*) FROM items i WHERE i.folderId = f.id) AS itemCount
        FROM folders f
        WHERE f.locationId = :locationId AND f.parentId IS :parentId
        ORDER BY f.sortOrder ASC, f.createdAt DESC
        """
    )
    fun observeFoldersByParent(locationId: String, parentId: String?): Flow<List<FolderSummaryRow>>

    @Query(
        """
        SELECT f.id, f.locationId, f.parentId, f.name, f.icon, f.coverImagePath, 
               f.enableMapView, f.mapX, f.mapY,
               (SELECT COUNT(*) FROM folders sub WHERE sub.parentId = f.id) AS subFolderCount,
               (SELECT COUNT(*) FROM items i WHERE i.folderId = f.id) AS itemCount
        FROM folders f
        WHERE f.id = :folderId
        """
    )
    fun observeFolder(folderId: String): Flow<FolderSummaryRow?>

    @Query("SELECT * FROM folders WHERE id = :folderId LIMIT 1")
    suspend fun getFolder(folderId: String): FolderEntity?

    @Query("SELECT * FROM folders WHERE locationId = :locationId AND parentId IS :parentId ORDER BY sortOrder ASC, createdAt DESC")
    suspend fun listFoldersByParent(locationId: String, parentId: String?): List<FolderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFolder(folder: FolderEntity): Long

    @Update
    suspend fun updateFolder(folder: FolderEntity): Int

    @Query("DELETE FROM folders WHERE id = :folderId")
    suspend fun deleteFolder(folderId: String): Int

    @Query("SELECT * FROM folders")
    suspend fun listAllFolders(): List<FolderEntity>

    @Query("SELECT * FROM folders WHERE locationId = :locationId")
    suspend fun listFoldersInLocation(locationId: String): List<FolderEntity>

    @Transaction
    @Query("SELECT * FROM folders WHERE id = :folderId")
    fun observeFolderWithItems(folderId: String): Flow<FolderWithItems?>

    // ==================== 物品 (Item) ====================

    @Transaction
    @Query("SELECT * FROM items WHERE folderId = :folderId ORDER BY updatedAt DESC")
    fun observeItemsInFolder(folderId: String): Flow<List<ItemWithTags>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItem(item: ItemEntity): Long

    @Update
    suspend fun updateItem(item: ItemEntity): Int

    @Query("SELECT * FROM items WHERE id = :itemId LIMIT 1")
    suspend fun getItem(itemId: String): ItemEntity?

    @Query("DELETE FROM items WHERE id = :itemId")
    suspend fun deleteItem(itemId: String): Int

    @Query("SELECT * FROM items")
    suspend fun listAllItems(): List<ItemEntity>

    @Query("SELECT * FROM items WHERE folderId = :folderId")
    suspend fun getItemsInFolder(folderId: String): List<ItemEntity>

    // ==================== 标签 (Tag) ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTag(tag: TagEntity): Long

    @Query("SELECT * FROM tags ORDER BY createdAt DESC")
    fun observeTags(): Flow<List<TagEntity>>

    @Query("UPDATE tags SET parentId = :parentId WHERE id = :tagId")
    suspend fun setTagParent(tagId: String, parentId: String?): Int

    @Query("UPDATE tags SET parentId = NULL WHERE parentId = :tagId")
    suspend fun clearTagParentForChildren(tagId: String): Int

    @Query("DELETE FROM tags WHERE id = :tagId")
    suspend fun deleteTag(tagId: String): Int

    @Query("SELECT * FROM tags")
    suspend fun listAllTags(): List<TagEntity>

    // ==================== 物品-标签关联 ====================

    @Query("DELETE FROM item_tags WHERE itemId = :itemId")
    suspend fun clearTagsForItem(itemId: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addItemTags(refs: List<ItemTagCrossRef>): List<Long>

    @Query("SELECT * FROM item_tags")
    suspend fun listAllItemTags(): List<ItemTagCrossRef>

    // ==================== 搜索 ====================

    @Query(
        """
        SELECT i.id AS itemId, i.name AS itemName, i.note AS note, i.imagePath AS imagePath,
               f.id AS folderId, f.name AS folderName,
               l.id AS locationId, l.name AS locationName
        FROM items i
        JOIN folders f ON f.id = i.folderId
        JOIN locations l ON l.id = f.locationId
        LEFT JOIN item_tags it ON it.itemId = i.id
        LEFT JOIN tags t ON t.id = it.tagId
        WHERE
            i.name LIKE '%' || :q || '%'
            OR COALESCE(i.note, '') LIKE '%' || :q || '%'
            OR f.name LIKE '%' || :q || '%'
            OR l.name LIKE '%' || :q || '%'
            OR COALESCE(t.name, '') LIKE '%' || :q || '%'
        GROUP BY i.id
        ORDER BY i.updatedAt DESC
        LIMIT 200
        """
    )
    fun observeSearchResults(q: String): Flow<List<ItemSearchResultRow>>

    // ==================== 过期物品 ====================

    @Query(
        """
        SELECT i.id AS itemId, i.name AS itemName, i.expiryDateEpochMs AS expiryDateEpochMs,
               f.name AS folderName, l.name AS locationName
        FROM items i
        JOIN folders f ON f.id = i.folderId
        JOIN locations l ON l.id = f.locationId
        WHERE i.expiryDateEpochMs IS NOT NULL AND i.expiryDateEpochMs <= :upperBoundEpochMs
        ORDER BY i.expiryDateEpochMs ASC
        """
    )
    suspend fun listExpiringItems(upperBoundEpochMs: Long): List<ExpiringItemRow>

    @Query("SELECT COUNT(*) FROM items WHERE expiryDateEpochMs IS NOT NULL AND expiryDateEpochMs <= :upperBoundEpochMs AND expiryDateEpochMs > :lowerBoundEpochMs")
    fun observeExpiringItemsCount(lowerBoundEpochMs: Long, upperBoundEpochMs: Long): Flow<Int>

    // ==================== 补货候选 ====================

    @Query(
        """
        SELECT i.*
        FROM items i
        WHERE i.currentQuantity < i.minQuantity
        ORDER BY (i.minQuantity - i.currentQuantity) DESC, i.updatedAt DESC
        """
    )
    suspend fun listRestockCandidates(): List<ItemEntity>

    // ==================== 清单 (List) ====================

    @Query("SELECT * FROM lists ORDER BY updatedAt DESC")
    fun observeLists(): Flow<List<PackingListEntity>>

    @Query("SELECT * FROM list_items WHERE listId = :listId ORDER BY createdAt ASC")
    fun observeListItems(listId: String): Flow<List<PackingListItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertList(list: PackingListEntity): Long

    @Query("SELECT * FROM lists WHERE id = :listId LIMIT 1")
    suspend fun getList(listId: String): PackingListEntity?

    @Query("UPDATE lists SET updatedAt = :updatedAt WHERE id = :listId")
    suspend fun updateListUpdatedAt(listId: String, updatedAt: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertListItems(items: List<PackingListItemEntity>): List<Long>

    @Update
    suspend fun updateListItem(item: PackingListItemEntity): Int

    @Query("DELETE FROM list_items WHERE id = :listItemId")
    suspend fun deleteListItem(listItemId: String): Int

    @Query("DELETE FROM lists WHERE id = :listId")
    suspend fun deleteList(listId: String): Int

    @Query("SELECT * FROM lists")
    suspend fun listAllLists(): List<PackingListEntity>

    @Query("SELECT * FROM list_items")
    suspend fun listAllListItems(): List<PackingListItemEntity>

    // ==================== 清空数据 ====================

    @Query("DELETE FROM item_tags")
    suspend fun clearAllItemTags(): Int

    @Query("DELETE FROM tags")
    suspend fun clearAllTags(): Int

    @Query("DELETE FROM items")
    suspend fun clearAllItems(): Int

    @Query("DELETE FROM folders")
    suspend fun clearAllFolders(): Int

    @Query("DELETE FROM locations")
    suspend fun clearAllLocations(): Int

    @Query("DELETE FROM list_items")
    suspend fun clearAllListItems(): Int

    @Query("DELETE FROM lists")
    suspend fun clearAllLists(): Int
}
