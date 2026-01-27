/**
 * 数据仓库层。
 *
 * 职责：
 * - 封装跨表的复杂数据库操作（如更新物品标签、清空全部数据）。
 * - 提供事务支持。
 *
 * 上层用途：
 * - 被 `SpaceViewModel` 调用，简化数据的维护逻辑。
 */
package com.example.myapplication.data

import androidx.room.withTransaction

class AppRepository(private val db: AppDatabase) {
    private val dao = db.dao()

    suspend fun setTagsForItem(itemId: String, tagIds: List<String>) {
        db.withTransaction {
            dao.clearTagsForItem(itemId)
            dao.addItemTags(tagIds.distinct().map { ItemTagCrossRef(itemId = itemId, tagId = it) })
        }
    }

    suspend fun wipeAllData() {
        db.withTransaction {
            dao.clearAllListItems()
            dao.clearAllLists()
            dao.clearAllItemTags()
            dao.clearAllTags()
            dao.clearAllItems()
            dao.clearAllSpots()
            dao.clearAllSpaces()
        }
    }
}

