/**
 * 领域模型定义。
 *
 * 职责：
 * - 定义应用核心概念：`Location`（场所）、`Folder`（文件夹）、`Item`（物品）、`Tag`（标签）。
 * - 提供基础的数据封装。
 *
 * 上层用途：
 * - 作为 UI 层展示和业务逻辑处理的统一模型，由 `data/Mappers.kt` 从数据库实体转换而来。
 */
package com.example.myapplication

import androidx.compose.ui.geometry.Offset

/**
 * 场所 - 最顶层容器，如家、办公室等
 */
data class Location(
    val id: String,
    val name: String,
    val icon: String? = null,
    val coverImagePath: String? = null,
    val folderCount: Int = 0,
    val itemCount: Int = 0,
)

/**
 * 文件夹 - 可无限嵌套的容器，如房间、家具、抽屉等
 */
data class Folder(
    val id: String,
    val locationId: String,
    val parentId: String? = null,
    val name: String,
    val icon: String? = null,
    val coverImagePath: String? = null,
    val enableMapView: Boolean = false,
    val mapPosition: Offset? = null,
    val subFolderCount: Int = 0,
    val itemCount: Int = 0,
    val items: List<Item> = emptyList(),   // 用于详情页展示
)

/**
 * 物品
 */
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

/**
 * 标签
 */
data class Tag(
    val id: String,
    val name: String,
    val parentId: String? = null
)

/**
 * 面包屑导航项
 */
data class BreadcrumbItem(
    val id: String,
    val name: String,
    val isLocation: Boolean = false,
)
