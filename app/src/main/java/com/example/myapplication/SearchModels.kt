/**
 * 搜索结果模型定义。
 *
 * 职责：
 * - 定义搜索结果项的数据结构。
 *
 * 上层用途：
 * - 供搜索页面展示匹配到的物品及位置信息。
 */
package com.example.myapplication

data class ItemSearchResult(
    val itemId: String,
    val itemName: String,
    val note: String?,
    val imagePath: String?,
    val locationId: String,
    val locationName: String,
    val folderId: String,
    val folderName: String,
) {
    val path: String
        get() = "$locationName > $folderName"
}
