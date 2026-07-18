package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stories")
data class StoryEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val author: String,
    val coverImageUrl: String,
    val category: String,
    val description: String,
    val content: String,
    val isDownloaded: Boolean = true,
    val isFavorite: Boolean = false,
    val lastReadPage: Int = 0,
    val lastReadTime: Long = 0,
    val publishDate: String = ""
)

@Entity(tableName = "chondos")
data class ChondoEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val author: String,
    val content: String,
    val gradientIndex: Int = 0,
    val publishDate: String = "",
    val isFavorite: Boolean = false,
    val isBookmarked: Boolean = false,
    val isSavedOffline: Boolean = true
)

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val storyId: Int,
    val pageIndex: Int,
    val chapterTitle: String,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "highlights")
data class HighlightEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val storyId: Int,
    val pageIndex: Int,
    val selectedText: String,
    val colorHex: String, // e.g. "Yellow", "Green", "Blue", "Pink"
    val note: String? = null,
    val isUnderline: Boolean = false,
    val isMarker: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)
