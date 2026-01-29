/**
 * 保险箱数据访问对象
 */
package com.example.myapplication.vault

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultDao {
    
    /**
     * 获取所有物品
     */
    @Query("SELECT * FROM vault_items ORDER BY updatedAt DESC")
    fun observeAllItems(): Flow<List<VaultItemEntity>>
    
    /**
     * 搜索物品
     */
    @Query("""
        SELECT * FROM vault_items 
        WHERE name LIKE '%' || :query || '%' 
           OR note LIKE '%' || :query || '%'
        ORDER BY updatedAt DESC
    """)
    fun searchItems(query: String): Flow<List<VaultItemEntity>>
    
    /**
     * 根据ID获取物品
     */
    @Query("SELECT * FROM vault_items WHERE id = :itemId")
    suspend fun getItemById(itemId: String): VaultItemEntity?
    
    /**
     * 插入物品
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: VaultItemEntity): Long
    
    /**
     * 更新物品
     */
    @Update
    suspend fun updateItem(item: VaultItemEntity): Int
    
    /**
     * 删除物品
     */
    @Query("DELETE FROM vault_items WHERE id = :itemId")
    suspend fun deleteItem(itemId: String): Int
    
    /**
     * 获取物品数量
     */
    @Query("SELECT COUNT(*) FROM vault_items")
    fun observeItemCount(): Flow<Int>
    
    /**
     * 获取所有物品（用于导出）
     */
    @Query("SELECT * FROM vault_items ORDER BY createdAt ASC")
    suspend fun getAllItemsForExport(): List<VaultItemEntity>
    
    /**
     * 清空所有数据
     */
    @Query("DELETE FROM vault_items")
    suspend fun clearAll(): Int
}

