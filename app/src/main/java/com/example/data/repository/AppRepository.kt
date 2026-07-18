package com.example.data.repository

import com.example.data.database.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class AppRepository(
    private val storyDao: StoryDao,
    private val chondoDao: ChondoDao,
    private val bookmarkDao: BookmarkDao,
    private val highlightDao: HighlightDao
) {
    val allStories: Flow<List<StoryEntity>> = storyDao.getAllStories()
    val allChondos: Flow<List<ChondoEntity>> = chondoDao.getAllChondos()
    val allBookmarks: Flow<List<BookmarkEntity>> = bookmarkDao.getAllBookmarks()

    fun getStoryById(id: Int): Flow<StoryEntity?> = storyDao.getStoryById(id)
    fun getChondoById(id: Int): Flow<ChondoEntity?> = chondoDao.getChondoById(id)

    fun getBookmarksForStory(storyId: Int): Flow<List<BookmarkEntity>> = bookmarkDao.getBookmarksForStory(storyId)
    fun getHighlightsForStory(storyId: Int): Flow<List<HighlightEntity>> = highlightDao.getHighlightsForStory(storyId)
    fun getHighlightsForStoryPage(storyId: Int, pageIndex: Int): Flow<List<HighlightEntity>> = highlightDao.getHighlightsForStoryPage(storyId, pageIndex)

    suspend fun updateStoryFavorite(id: Int, isFavorite: Boolean) {
        storyDao.updateFavorite(id, isFavorite)
    }

    suspend fun updateStoryReadProgress(id: Int, page: Int) {
        storyDao.updateReadProgress(id, page, System.currentTimeMillis())
    }

    suspend fun updateChondoFavorite(id: Int, isFavorite: Boolean) {
        chondoDao.updateFavorite(id, isFavorite)
    }

    suspend fun updateChondoBookmark(id: Int, isBookmarked: Boolean) {
        chondoDao.updateBookmark(id, isBookmarked)
    }

    suspend fun insertBookmark(bookmark: BookmarkEntity) {
        bookmarkDao.insertBookmark(bookmark)
    }

    suspend fun deleteBookmark(id: Int) {
        bookmarkDao.deleteBookmark(id)
    }

    suspend fun deleteBookmarkForPage(storyId: Int, pageIndex: Int) {
        bookmarkDao.deleteBookmarkForPage(storyId, pageIndex)
    }

    suspend fun insertHighlight(highlight: HighlightEntity) {
        highlightDao.insertHighlight(highlight)
    }

    suspend fun deleteHighlight(id: Int) {
        highlightDao.deleteHighlight(id)
    }

    suspend fun insertStory(story: StoryEntity) {
        storyDao.insertStory(story)
    }

    suspend fun insertChondo(chondo: ChondoEntity) {
        chondoDao.insertChondo(chondo)
    }

    suspend fun deleteStory(id: Int) {
        storyDao.deleteStory(id)
    }

    suspend fun deleteChondo(id: Int) {
        chondoDao.deleteChondo(id)
    }

    suspend fun checkAndPrepopulate() {
        try {
            val currentStories = storyDao.getAllStories().first()
            if (currentStories.isEmpty()) {
                storyDao.insertStories(SampleData.sampleStories)
            }
            val currentChondos = chondoDao.getAllChondos().first()
            if (currentChondos.isEmpty()) {
                chondoDao.insertChondos(SampleData.sampleChondos)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
