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

