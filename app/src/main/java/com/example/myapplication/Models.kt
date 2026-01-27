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
