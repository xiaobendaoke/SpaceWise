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

