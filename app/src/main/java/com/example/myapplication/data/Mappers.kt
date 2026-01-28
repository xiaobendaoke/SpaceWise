/**
 * 数据转换层（Mappers）。
 *
 * 职责：
 * - 实现数据库实体（Entities）与领域模型（Models）之间的互转逻辑。
 *
 * 上层用途：
 * - 被 `AppDao` 或 `SpaceViewModel` 调用，确保 UI 层不直接依赖底层数据库结构。
 */
package com.example.myapplication.data

import androidx.compose.ui.geometry.Offset
import com.example.myapplication.BreadcrumbItem
import com.example.myapplication.Folder
import com.example.myapplication.Item
import com.example.myapplication.Location
import com.example.myapplication.Tag

fun LocationSummaryRow.toDomain(): Location {
    return Location(
        id = id,
        name = name,
        icon = icon,
        coverImagePath = coverImagePath,
        folderCount = folderCount,
        itemCount = itemCount,
    )
}

fun LocationEntity.toDomain(): Location {
    return Location(
        id = id,
        name = name,
        icon = icon,
        coverImagePath = coverImagePath,
    )
}

fun FolderSummaryRow.toDomain(): Folder {
    return Folder(
        id = id,
        locationId = locationId,
        parentId = parentId,
        name = name,
        icon = icon,
        coverImagePath = coverImagePath,
        enableMapView = enableMapView,
        mapPosition = if (mapX != null && mapY != null) Offset(mapX, mapY) else null,
        subFolderCount = subFolderCount,
        itemCount = itemCount,
    )
}

fun FolderEntity.toDomain(): Folder {
    return Folder(
        id = id,
        locationId = locationId,
        parentId = parentId,
        name = name,
        icon = icon,
        coverImagePath = coverImagePath,
        enableMapView = enableMapView,
        mapPosition = if (mapX != null && mapY != null) Offset(mapX, mapY) else null,
    )
}

fun FolderWithItems.toDomain(): Folder {
    return Folder(
        id = folder.id,
        locationId = folder.locationId,
        parentId = folder.parentId,
        name = folder.name,
        icon = folder.icon,
        coverImagePath = folder.coverImagePath,
        enableMapView = folder.enableMapView,
        mapPosition = if (folder.mapX != null && folder.mapY != null) Offset(folder.mapX, folder.mapY) else null,
        itemCount = items.size,
        items = items.map { it.toDomain() },
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

fun BreadcrumbRow.toDomain(): BreadcrumbItem {
    return BreadcrumbItem(
        id = id,
        name = name,
        isLocation = isLocation,
    )
}
