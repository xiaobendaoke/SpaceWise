/**
 * 数据库持久化实体定义。
 *
 * 职责：
 * - 定义 Room 数据库表结构和关联关系。
 * - 定义复杂查询的结果行模型。
 *
 * 上层用途：
 * - 供 `AppDao` 使用，用于底层的 SQL 映射和操作。
 */
package com.example.myapplication.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation

/**
 * 场所实体 - 最顶层容器（如：我的家、办公室、父母家）
 */
@Entity(tableName = "locations")
data class LocationEntity(
    @PrimaryKey val id: String,
    val name: String,
    val icon: String?,            // emoji 图标
    val coverImagePath: String?,
    val sortOrder: Int,
    val createdAt: Long,
    val updatedAt: Long,
)

/**
 * 文件夹实体 - 支持无限嵌套的容器（如：房间、家具、抽屉等）
 */
@Entity(
    tableName = "folders",
    foreignKeys = [
        ForeignKey(
            entity = LocationEntity::class,
            parentColumns = ["id"],
            childColumns = ["locationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("locationId"), Index("parentId")]
)
data class FolderEntity(
    @PrimaryKey val id: String,
    val locationId: String,       // 所属场所
    val parentId: String?,        // 父文件夹ID，null=场所根目录
    val name: String,
    val icon: String?,            // emoji 图标
    val coverImagePath: String?,
    val enableMapView: Boolean,   // 是否启用平面图模式
    val mapX: Float?,             // 平面图坐标X（可选）
    val mapY: Float?,             // 平面图坐标Y（可选）
    val sortOrder: Int,
    val createdAt: Long,
    val updatedAt: Long,
)

/**
 * 物品实体
 */
@Entity(
    tableName = "items",
    foreignKeys = [
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("folderId"),
        Index("expiryDateEpochMs"),
    ]
)
data class ItemEntity(
    @PrimaryKey val id: String,
    val folderId: String,         // 所属文件夹
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

/**
 * 标签实体
 */
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

/**
 * 物品-标签关联表
 */
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

/**
 * 清单实体
 */
@Entity(tableName = "lists")
data class PackingListEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
)

/**
 * 清单条目实体
 */
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

// ==================== 查询结果模型 ====================

/**
 * 场所摘要行
 */
data class LocationSummaryRow(
    val id: String,
    val name: String,
    val icon: String?,
    val coverImagePath: String?,
    val folderCount: Int,
    val itemCount: Int,
)

/**
 * 文件夹摘要行
 */
data class FolderSummaryRow(
    val id: String,
    val locationId: String,
    val parentId: String?,
    val name: String,
    val icon: String?,
    val coverImagePath: String?,
    val enableMapView: Boolean,
    val mapX: Float?,
    val mapY: Float?,
    val subFolderCount: Int,
    val itemCount: Int,
)

/**
 * 物品及其标签
 */
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

/**
 * 文件夹及其物品
 */
data class FolderWithItems(
    @Embedded val folder: FolderEntity,
    @Relation(
        entity = ItemEntity::class,
        parentColumn = "id",
        entityColumn = "folderId"
    )
    val items: List<ItemWithTags>,
)

/**
 * 物品搜索结果行
 */
data class ItemSearchResultRow(
    val itemId: String,
    val itemName: String,
    val note: String?,
    val imagePath: String?,
    val folderId: String,
    val folderName: String,
    val locationId: String,
    val locationName: String,
)

/**
 * 即将过期物品行
 */
data class ExpiringItemRow(
    val itemId: String,
    val itemName: String,
    val expiryDateEpochMs: Long,
    val folderName: String,
    val locationName: String,
)

/**
 * 清单及其条目
 */
data class ListWithItems(
    @Embedded val list: PackingListEntity,
    @Relation(parentColumn = "id", entityColumn = "listId")
    val items: List<PackingListItemEntity>,
)

/**
 * 面包屑导航项
 */
data class BreadcrumbRow(
    val id: String,
    val name: String,
    val isLocation: Boolean,
)
