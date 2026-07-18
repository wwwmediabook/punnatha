package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StoryDao {
    @Query("SELECT * FROM stories ORDER BY id ASC")
    fun getAllStories(): Flow<List<StoryEntity>>

    @Query("SELECT * FROM stories WHERE id = :id")
    fun getStoryById(id: Int): Flow<StoryEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStories(stories: List<StoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStory(story: StoryEntity)

    @Query("UPDATE stories SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Int, isFavorite: Boolean)

    @Query("UPDATE stories SET lastReadPage = :page, lastReadTime = :time WHERE id = :id")
    suspend fun updateReadProgress(id: Int, page: Int, time: Long)

    @Query("DELETE FROM stories WHERE id = :id")
    suspend fun deleteStory(id: Int)
}

@Dao
interface ChondoDao {
    @Query("SELECT * FROM chondos ORDER BY id ASC")
    fun getAllChondos(): Flow<List<ChondoEntity>>

    @Query("SELECT * FROM chondos WHERE id = :id")
    fun getChondoById(id: Int): Flow<ChondoEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChondos(chondos: List<ChondoEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChondo(chondo: ChondoEntity)

    @Query("UPDATE chondos SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Int, isFavorite: Boolean)

    @Query("UPDATE chondos SET isBookmarked = :isBookmarked WHERE id = :id")
    suspend fun updateBookmark(id: Int, isBookmarked: Boolean)

    @Query("DELETE FROM chondos WHERE id = :id")
    suspend fun deleteChondo(id: Int)
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY timestamp DESC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE storyId = :storyId ORDER BY pageIndex ASC")
    fun getBookmarksForStory(storyId: Int): Flow<List<BookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmark(id: Int)

    @Query("DELETE FROM bookmarks WHERE storyId = :storyId AND pageIndex = :pageIndex")
    suspend fun deleteBookmarkForPage(storyId: Int, pageIndex: Int)
}

@Dao
interface HighlightDao {
    @Query("SELECT * FROM highlights WHERE storyId = :storyId ORDER BY timestamp DESC")
    fun getHighlightsForStory(storyId: Int): Flow<List<HighlightEntity>>

    @Query("SELECT * FROM highlights WHERE storyId = :storyId AND pageIndex = :pageIndex")
    fun getHighlightsForStoryPage(storyId: Int, pageIndex: Int): Flow<List<HighlightEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHighlight(highlight: HighlightEntity)

    @Query("DELETE FROM highlights WHERE id = :id")
    suspend fun deleteHighlight(id: Int)
}
