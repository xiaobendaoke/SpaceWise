package com.example.myapplication.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(
    tableName = "spaces",
)
data class SpaceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val coverImagePath: String?,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "spots",
    foreignKeys = [
        ForeignKey(
            entity = SpaceEntity::class,
            parentColumns = ["id"],
            childColumns = ["spaceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("spaceId")]
)
data class SpotEntity(
    @PrimaryKey val id: String,
    val spaceId: String,
    val name: String,
    val x: Float,
    val y: Float,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "items",
    foreignKeys = [
        ForeignKey(
            entity = SpotEntity::class,
            parentColumns = ["id"],
            childColumns = ["spotId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("spotId"),
        Index("expiryDateEpochMs"),
    ]
)
data class ItemEntity(
    @PrimaryKey val id: String,
    val spotId: String,
    val name: String,
    val note: String?,
    val imagePath: String?,
    val expiryDateEpochMs: Long?,
    val lastUsedAtEpochMs: Long?,
    val currentQuantity: Int,
    val minQuantity: Int,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "tags",
    indices = [Index("parentId")]
)
data class TagEntity(
    @PrimaryKey val id: String,
    val name: String,
    val parentId: String?,
    val createdAt: Long,
)

@Entity(
    tableName = "item_tags",
    primaryKeys = ["itemId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("itemId"),
        Index("tagId"),
    ]
)
data class ItemTagCrossRef(
    val itemId: String,
    val tagId: String,
)

@Entity(tableName = "lists")
data class PackingListEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "list_items",
    foreignKeys = [
        ForeignKey(
            entity = PackingListEntity::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("listId"), Index("linkedItemId")]
)
data class PackingListItemEntity(
    @PrimaryKey val id: String,
    val listId: String,
    val name: String,
    val checked: Boolean,
    val linkedItemId: String?,
    val quantityNeeded: Int?,
    val createdAt: Long,
    val updatedAt: Long,
)

data class SpaceSummaryRow(
    val id: String,
    val name: String,
    val coverImagePath: String?,
    val itemCount: Int,
)

data class ItemWithTags(
    @Embedded val item: ItemEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = ItemTagCrossRef::class,
            parentColumn = "itemId",
            entityColumn = "tagId"
        )
    )
    val tags: List<TagEntity>,
)

data class SpotWithItems(
    @Embedded val spot: SpotEntity,
    @Relation(
        entity = ItemEntity::class,
        parentColumn = "id",
        entityColumn = "spotId"
    )
    val items: List<ItemWithTags>,
)

data class SpaceWithSpots(
    @Embedded val space: SpaceEntity,
    @Relation(
        entity = SpotEntity::class,
        parentColumn = "id",
        entityColumn = "spaceId"
    )
    val spots: List<SpotWithItems>,
)

data class ItemSearchResultRow(
    val itemId: String,
    val itemName: String,
    val note: String?,
    val imagePath: String?,
    val spotId: String,
    val spotName: String,
    val spaceId: String,
    val spaceName: String,
)

data class ExpiringItemRow(
    val itemId: String,
    val itemName: String,
    val expiryDateEpochMs: Long,
    val spotName: String,
    val spaceName: String,
)

data class ListWithItems(
    @Embedded val list: PackingListEntity,
    @Relation(parentColumn = "id", entityColumn = "listId")
    val items: List<PackingListItemEntity>,
)

