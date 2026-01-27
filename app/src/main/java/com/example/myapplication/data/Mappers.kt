package com.example.myapplication.data

import androidx.compose.ui.geometry.Offset
import com.example.myapplication.Item
import com.example.myapplication.Space
import com.example.myapplication.Spot
import com.example.myapplication.Tag

fun SpaceSummaryRow.toDomain(): Space {
    return Space(
        id = id,
        name = name,
        coverImagePath = coverImagePath,
        spots = emptyList()
    )
}

fun TagEntity.toDomain(): Tag = Tag(id = id, name = name, parentId = parentId)

fun ItemWithTags.toDomain(): Item {
    val item = item
    return Item(
        id = item.id,
        name = item.name,
        note = item.note,
        imagePath = item.imagePath,
        expiryDateEpochMs = item.expiryDateEpochMs,
        lastUsedAtEpochMs = item.lastUsedAtEpochMs,
        currentQuantity = item.currentQuantity,
        minQuantity = item.minQuantity,
        tags = tags.map { it.toDomain() }
    )
}

fun SpaceWithSpots.toDomain(): Space {
    return Space(
        id = space.id,
        name = space.name,
        coverImagePath = space.coverImagePath,
        spots = spots.map { spotWithItems ->
            Spot(
                id = spotWithItems.spot.id,
                name = spotWithItems.spot.name,
                position = Offset(spotWithItems.spot.x, spotWithItems.spot.y),
                items = spotWithItems.items.map { it.toDomain() }
            )
        }
    )
}

