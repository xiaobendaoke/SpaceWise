/**
 * 保险箱数据库实体定义
 */
package com.example.myapplication.vault

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 保险箱物品实体
 */
@Entity(
    tableName = "vault_items",
    indices = [Index("expiryDateEpochMs")]
)
data class VaultItemEntity(
    @PrimaryKey val id: String,
    val name: String,
    val note: String?,
    val imagePath: String?,
    val expiryDateEpochMs: Long?,
    val currentQuantity: Int,
    val minQuantity: Int,
    val createdAt: Long,
    val updatedAt: Long,
)

/**
 * 保险箱物品领域模型
 */
data class VaultItem(
    val id: String,
    val name: String,
    val note: String? = null,
    val imagePath: String? = null,
    val expiryDateEpochMs: Long? = null,
    val currentQuantity: Int = 1,
    val minQuantity: Int = 0,
)

/**
 * 实体转领域模型
 */
fun VaultItemEntity.toDomain(): VaultItem = VaultItem(
    id = id,
    name = name,
    note = note,
    imagePath = imagePath,
    expiryDateEpochMs = expiryDateEpochMs,
    currentQuantity = currentQuantity,
    minQuantity = minQuantity,
)

/**
 * 领域模型转实体
 */
fun VaultItem.toEntity(createdAt: Long = System.currentTimeMillis()): VaultItemEntity = VaultItemEntity(
    id = id,
    name = name,
    note = note,
    imagePath = imagePath,
    expiryDateEpochMs = expiryDateEpochMs,
    currentQuantity = currentQuantity,
    minQuantity = minQuantity,
    createdAt = createdAt,
    updatedAt = System.currentTimeMillis(),
)
