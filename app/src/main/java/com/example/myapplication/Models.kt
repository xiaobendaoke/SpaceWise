/**
 * 领域模型定义。
 *
 * 职责：
 * - 定义应用核心概念：`Item`（物品）、`Spot`（点位）、`Tag`（标签）、`Space`（空间）。
 * - 提供基础的数据封装。
 *
 * 上层用途：
 * - 作为 UI 层展示和业务逻辑处理的统一模型，由 `data/Mappers.kt` 从数据库实体转换而来。
 */
package com.example.myapplication

import androidx.compose.ui.geometry.Offset

data class Item(
    val id: String,
    val name: String,
    val note: String? = null,
    val imagePath: String? = null,
    val expiryDateEpochMs: Long? = null,
    val lastUsedAtEpochMs: Long? = null,
    val currentQuantity: Int = 1,
    val minQuantity: Int = 0,
    val tags: List<Tag> = emptyList()
)

data class Spot(
    val id: String,
    val name: String,
    var position: Offset,
    val items: List<Item> = emptyList()
)

data class Tag(
    val id: String,
    val name: String,
    val parentId: String? = null
)

data class Space(
    val id: String,
    val name: String,
    val coverImagePath: String? = null,
    val spots: List<Spot> = emptyList()
) {
    val itemCount: Int
        get() = spots.sumOf { it.items.size }
}
