package com.example.myapplication

data class ItemSearchResult(
    val itemId: String,
    val itemName: String,
    val note: String?,
    val imagePath: String?,
    val spaceId: String,
    val spaceName: String,
    val spotId: String,
    val spotName: String,
) {
    val path: String
        get() = "$spaceName > $spotName"
}

