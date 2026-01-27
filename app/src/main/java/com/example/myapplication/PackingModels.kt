/**
 * 清单领域模型定义。
 *
 * 职责：
 * - 定义 `PackingList`（清单）和 `PackingListItem`（清单条目）的数据结构。
 *
 * 上层用途：
 * - 作为清单功能页面的统一业务模型。
 */
package com.example.myapplication

data class PackingList(
    val id: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
)

data class PackingListItem(
    val id: String,
    val listId: String,
    val name: String,
    val checked: Boolean,
    val linkedItemId: String? = null,
    val quantityNeeded: Int? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

