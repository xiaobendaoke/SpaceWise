/**
 * 保险箱数据仓库
 */
package com.example.myapplication.vault

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class VaultRepository(context: Context) {
    
    private val database = VaultDatabase.getInstance(context)
    private val dao = database.vaultDao()
    
    /**
     * 观察所有物品
     */
    fun observeAllItems(): Flow<List<VaultItem>> {
        return dao.observeAllItems().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    /**
     * 搜索物品
     */
    fun searchItems(query: String): Flow<List<VaultItem>> {
        return dao.searchItems(query).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    /**
     * 获取物品数量
     */
    fun observeItemCount(): Flow<Int> = dao.observeItemCount()
    
    /**
     * 添加物品
     */
    suspend fun addItem(item: VaultItem) {
        dao.insertItem(item.toEntity())
    }
    
    /**
     * 更新物品
     */
    suspend fun updateItem(item: VaultItem) {
        val existing = dao.getItemById(item.id)
        if (existing != null) {
            dao.updateItem(item.toEntity(createdAt = existing.createdAt))
        }
    }
    
    /**
     * 删除物品
     */
    suspend fun deleteItem(itemId: String) {
        dao.deleteItem(itemId)
    }
    
    /**
     * 获取物品（用于编辑）
     */
    suspend fun getItem(itemId: String): VaultItem? {
        return dao.getItemById(itemId)?.toDomain()
    }
    
    /**
     * 获取所有物品（用于导出）
     */
    suspend fun getAllItemsForExport(): List<VaultItemEntity> {
        return dao.getAllItemsForExport()
    }
    
    /**
     * 清空所有数据
     */
    suspend fun clearAll() {
        dao.clearAll()
    }
    
    /**
     * 批量导入物品
     */
    suspend fun importItems(items: List<VaultItemEntity>) {
        items.forEach { dao.insertItem(it) }
    }
}
