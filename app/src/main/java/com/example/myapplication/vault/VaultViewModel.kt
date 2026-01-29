/**
 * 保险箱 ViewModel
 */
package com.example.myapplication.vault

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.storage.InternalImageStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import java.util.UUID

class VaultViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = VaultRepository(application)
    private val context = application.applicationContext
    private val authManager = VaultAuthManager(application)
    
    // 搜索查询
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery
    
    // 物品列表（根据搜索查询动态变化）
    val items: Flow<List<VaultItem>> = _searchQuery.flatMapLatest { query ->
        if (query.isBlank()) {
            repository.observeAllItems()
        } else {
            repository.searchItems(query)
        }
    }
    
    // 物品数量
    val itemCount: Flow<Int> = repository.observeItemCount()
    
    /**
     * 更新搜索查询
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    /**
     * 添加物品
     */
    fun addItem(
        name: String,
        note: String?,
        imagePath: String?,
        expiryDateEpochMs: Long?,
        currentQuantity: Int,
        minQuantity: Int
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val item = VaultItem(
                id = UUID.randomUUID().toString(),
                name = name,
                note = note,
                imagePath = imagePath,
                expiryDateEpochMs = expiryDateEpochMs,
                currentQuantity = currentQuantity,
                minQuantity = minQuantity
            )
            repository.addItem(item)
        }
    }
    
    /**
     * 更新物品
     */
    fun updateItem(
        itemId: String,
        name: String,
        note: String?,
        imagePath: String?,
        expiryDateEpochMs: Long?,
        currentQuantity: Int,
        minQuantity: Int
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val item = VaultItem(
                id = itemId,
                name = name,
                note = note,
                imagePath = imagePath,
                expiryDateEpochMs = expiryDateEpochMs,
                currentQuantity = currentQuantity,
                minQuantity = minQuantity
            )
            repository.updateItem(item)
        }
    }
    
    /**
     * 删除物品
     */
    fun deleteItem(itemId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            // 获取物品以删除关联图片
            val item = repository.getItem(itemId)
            item?.imagePath?.let { path ->
                InternalImageStore.delete(context, path)
            }
            repository.deleteItem(itemId)
        }
    }
    
    /**
     * 保存图片到保险箱目录
     */
    suspend fun saveVaultImage(uri: Uri): String? {
        return InternalImageStore.persistFromUri(context, uri)
    }
    
    /**
     * 创建临时相机 URI
     */
    fun createTempCameraUri(): Uri {
        return InternalImageStore.createTempCameraUri(context)
    }
    
    /**
     * 持久化拍照图片
     */
    suspend fun persistCapturedPhoto(tempUri: Uri): String? {
        return InternalImageStore.persistFromUri(context, tempUri)
    }
    
    // ============ 认证相关 ============
    
    /**
     * 检查保险箱是否已初始化
     */
    fun isVaultInitialized(): Boolean = authManager.isVaultInitialized()
    
    /**
     * 设置保险箱密码
     */
    fun setVaultPassword(password: String) {
        authManager.setVaultPassword(password)
    }
    
    /**
     * 验证密码
     */
    fun verifyPassword(password: String): Boolean = authManager.verifyPassword(password)
    
    /**
     * 是否支持生物识别
     */
    fun isBiometricAvailable(): Boolean = authManager.isBiometricAvailable()
    
    /**
     * 获取认证管理器（用于生物识别）
     */
    fun getAuthManager(): VaultAuthManager = authManager
}
