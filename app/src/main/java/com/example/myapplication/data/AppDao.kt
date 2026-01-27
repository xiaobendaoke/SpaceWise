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
    @Query(
        """
        SELECT s.id AS id, s.name AS name, s.coverImagePath AS coverImagePath,
               COUNT(i.id) AS itemCount
        FROM spaces s
        LEFT JOIN spots sp ON sp.spaceId = s.id
        LEFT JOIN items i ON i.spotId = sp.id
        GROUP BY s.id
        ORDER BY s.createdAt DESC
        """
    )
    fun observeSpaceSummaries(): Flow<List<SpaceSummaryRow>>

    @Transaction
    @Query("SELECT * FROM spaces WHERE id = :spaceId")
    fun observeSpaceWithSpots(spaceId: String): Flow<List<SpaceWithSpots>>

    @Query("SELECT COUNT(*) FROM spaces")
    suspend fun countSpaces(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSpace(space: SpaceEntity): Long

    @Query("DELETE FROM spaces WHERE id = :spaceId")
    suspend fun deleteSpace(spaceId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSpots(spots: List<SpotEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSpot(spot: SpotEntity): Long

    @Update
    suspend fun updateSpot(spot: SpotEntity): Int

    @Query("SELECT * FROM spots WHERE id = :spotId LIMIT 1")
    suspend fun getSpot(spotId: String): SpotEntity?

    @Query("DELETE FROM spots WHERE id = :spotId")
    suspend fun deleteSpot(spotId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItem(item: ItemEntity): Long

    @Update
    suspend fun updateItem(item: ItemEntity): Int

    @Query("SELECT * FROM items WHERE id = :itemId LIMIT 1")
    suspend fun getItem(itemId: String): ItemEntity?

    @Query("DELETE FROM items WHERE id = :itemId")
    suspend fun deleteItem(itemId: String): Int

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

    @Query("DELETE FROM item_tags WHERE itemId = :itemId")
    suspend fun clearTagsForItem(itemId: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addItemTags(refs: List<ItemTagCrossRef>): List<Long>

    @Query(
        """
        SELECT i.id AS itemId, i.name AS itemName, i.note AS note, i.imagePath AS imagePath,
               sp.id AS spotId, sp.name AS spotName,
               s.id AS spaceId, s.name AS spaceName
        FROM items i
        JOIN spots sp ON sp.id = i.spotId
        JOIN spaces s ON s.id = sp.spaceId
        LEFT JOIN item_tags it ON it.itemId = i.id
        LEFT JOIN tags t ON t.id = it.tagId
        WHERE
            i.name LIKE '%' || :q || '%'
            OR COALESCE(i.note, '') LIKE '%' || :q || '%'
            OR sp.name LIKE '%' || :q || '%'
            OR s.name LIKE '%' || :q || '%'
            OR COALESCE(t.name, '') LIKE '%' || :q || '%'
        GROUP BY i.id
        ORDER BY i.updatedAt DESC
        LIMIT 200
        """
    )
    fun observeSearchResults(q: String): Flow<List<ItemSearchResultRow>>

    @Query(
        """
        SELECT i.id AS itemId, i.name AS itemName, i.expiryDateEpochMs AS expiryDateEpochMs,
               sp.name AS spotName, s.name AS spaceName
        FROM items i
        JOIN spots sp ON sp.id = i.spotId
        JOIN spaces s ON s.id = sp.spaceId
        WHERE i.expiryDateEpochMs IS NOT NULL AND i.expiryDateEpochMs <= :upperBoundEpochMs
        ORDER BY i.expiryDateEpochMs ASC
        """
    )
    suspend fun listExpiringItems(upperBoundEpochMs: Long): List<ExpiringItemRow>

    @Query(
        """
        SELECT i.*
        FROM items i
        WHERE i.currentQuantity < i.minQuantity
        ORDER BY (i.minQuantity - i.currentQuantity) DESC, i.updatedAt DESC
        """
    )
    suspend fun listRestockCandidates(): List<ItemEntity>

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

    @Query("DELETE FROM item_tags")
    suspend fun clearAllItemTags(): Int

    @Query("DELETE FROM tags")
    suspend fun clearAllTags(): Int

    @Query("DELETE FROM items")
    suspend fun clearAllItems(): Int

    @Query("DELETE FROM spots")
    suspend fun clearAllSpots(): Int

    @Query("DELETE FROM spaces")
    suspend fun clearAllSpaces(): Int

    @Query("DELETE FROM list_items")
    suspend fun clearAllListItems(): Int

    @Query("DELETE FROM lists")
    suspend fun clearAllLists(): Int

    @Query("SELECT * FROM spaces")
    suspend fun listAllSpaces(): List<SpaceEntity>

    @Query("SELECT * FROM spots")
    suspend fun listAllSpots(): List<SpotEntity>

    @Query("SELECT * FROM spots WHERE spaceId = :spaceId")
    suspend fun listSpotsForSpace(spaceId: String): List<SpotEntity>

    @Query("SELECT * FROM items")
    suspend fun listAllItems(): List<ItemEntity>

    @Query("SELECT * FROM tags")
    suspend fun listAllTags(): List<TagEntity>

    @Query("SELECT * FROM item_tags")
    suspend fun listAllItemTags(): List<ItemTagCrossRef>

    @Query("SELECT * FROM lists")
    suspend fun listAllLists(): List<PackingListEntity>

    @Query("SELECT * FROM list_items")
    suspend fun listAllListItems(): List<PackingListItemEntity>
}
