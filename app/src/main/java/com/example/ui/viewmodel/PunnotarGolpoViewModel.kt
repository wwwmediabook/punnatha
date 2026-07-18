package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.*
import com.example.data.repository.AppRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest

data class BookPage(
    val globalPageNumber: Int,
    val relativePageNumber: Int,
    val totalPagesInChapter: Int,
    val chapterTitle: String,
    val text: String,
    val isCoverPage: Boolean = false,
    val isTocPage: Boolean = false
)

data class TableOfContentItem(
    val chapterTitle: String,
    val startPageNumber: Int
)

data class NotificationPayload(
    val title: String,
    val body: String,
    val isStory: Boolean,
    val itemId: Int
)

class PunnotarGolpoViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = AppRepository(
        database.storyDao(),
        database.chondoDao(),
        database.bookmarkDao(),
        database.highlightDao()
    )

    // Splash screen state
    private val _isSplashFinished = MutableStateFlow(false)
    val isSplashFinished = _isSplashFinished.asStateFlow()

    // Stories and Chondos state from Repository
    val allStories = repository.allStories.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allChondos = repository.allChondos.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allBookmarks = repository.allBookmarks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Filter states
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    // Active reading state
    private val _selectedStory = MutableStateFlow<StoryEntity?>(null)
    val selectedStory = _selectedStory.asStateFlow()

    private val _paginatedPages = MutableStateFlow<List<BookPage>>(emptyList())
    val paginatedPages = _paginatedPages.asStateFlow()

    private val _tableOfContents = MutableStateFlow<List<TableOfContentItem>>(emptyList())
    val tableOfContents = _tableOfContents.asStateFlow()

    private val _currentPageIndex = MutableStateFlow(0) // 0-indexed relative to paginated pages
    val currentPageIndex = _currentPageIndex.asStateFlow()

    // Highlights state for current story
    val currentStoryHighlights = _selectedStory.flatMapLatest { story ->
        if (story != null) repository.getHighlightsForStory(story.id)
        else flowOf(emptyList())
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Current story bookmarks
    val currentStoryBookmarks = _selectedStory.flatMapLatest { story ->
        if (story != null) repository.getBookmarksForStory(story.id)
        else flowOf(emptyList())
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Reading appearance settings
    private val _fontSize = MutableStateFlow(18f) // in sp
    val fontSize = _fontSize.asStateFlow()

    private val _lineHeightMultiplier = MutableStateFlow(1.5f)
    val lineHeightMultiplier = _lineHeightMultiplier.asStateFlow()

    private val _fontStyle = MutableStateFlow("Serif") // "SansSerif", "Serif", "Monospace"
    val fontStyle = _fontStyle.asStateFlow()

    private val _readerTheme = MutableStateFlow("Sepia") // "Light", "Dark", "Sepia"
    val readerTheme = _readerTheme.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(true)
    val notificationsEnabled = _notificationsEnabled.asStateFlow()

    private val _newContentNotification = MutableSharedFlow<NotificationPayload>(extraBufferCapacity = 1)
    val newContentNotification = _newContentNotification.asSharedFlow()

    private val _appLanguage = MutableStateFlow("বাংলা")
    val appLanguage = _appLanguage.asStateFlow()

    private val _isOnline = MutableStateFlow(true)
    val isOnline = _isOnline.asStateFlow()

    // TTS State Flows
    private val _isTtsSpeaking = MutableStateFlow(false)
    val isTtsSpeaking = _isTtsSpeaking.asStateFlow()

    private val _currentTtsItemId = MutableStateFlow<Int?>(null)
    val currentTtsItemId = _currentTtsItemId.asStateFlow()

    private val _currentTtsItemType = MutableStateFlow<String?>(null) // "story" or "chondo"
    val currentTtsItemType = _currentTtsItemType.asStateFlow()

    private val _currentTtsTitle = MutableStateFlow("")
    val currentTtsTitle = _currentTtsTitle.asStateFlow()

    private val _ttsVoiceGender = MutableStateFlow("Female") // "Female" or "Male"
    val ttsVoiceGender = _ttsVoiceGender.asStateFlow()

    private val _ttsVolume = MutableStateFlow(1.0f) // 0.0f to 1.0f
    val ttsVolume = _ttsVolume.asStateFlow()

    private var tts: android.speech.tts.TextToSpeech? = null
    private var lastSpokenText = ""

    fun setAppLanguage(lang: String) {
        if (lang in listOf("বাংলা", "English")) {
            _appLanguage.value = lang
        }
    }

    fun translate(key: String): String {
        val lang = _appLanguage.value
        if (lang == "English") {
            return when (key) {
                // Navigation
                "গল্প" -> "Stories"
                "ছন্দ" -> "Poems"
                "লাইব্রেরি" -> "Library"
                "সেটিংস" -> "Settings"
                "এডমিন" -> "Admin"
                "আজ" -> "Today"
                "সব" -> "All"
                
                // Home Screen additional
                "শুভদিন!" -> "Welcome!"
                "সব গল্প" -> "All Stories"
                "অফলাইন রিডার" -> "Offline Reader"
                "গল্পের বিভাগসমূহ" -> "Story Categories"
                "পড়া চালিয়ে যান (Continue Reading)" -> "Continue Reading"
                "গল্প, লেখক বা বিভাগ খুঁজুন..." -> "Search stories, authors, or categories..."
                "সর্বশেষ গল্পসমূহ" -> "Latest Stories"
                "অনুসন্ধানের ফলাফল" -> "Search Results"
                "কোনো গল্প পাওয়া যায়নি!" -> "No stories found!"
                "অন্য কোনো কিওয়ার্ড দিয়ে চেষ্টা করুন।" -> "Try searching with other keywords."
                
                // Chondo Screen additional
                "সুর ও তাল" -> "Tone & Rhythm"
                "ছন্দের খেলা" -> "Rhyme Play"
                "মনকে দোলা দেবে যে মধুর ছন্দমালা।" -> "Sweet poems that sway the mind."
                "ছন্দ বা কবি খুঁজুন..." -> "Search poems or poets..."
                "সব ছন্দমালা" -> "All Poems"
                
                // Favorites Screen additional
                "ব্যক্তিগত লাইব্রেরি" -> "Personal Library"
                "আমার প্রিয় তালিকা" -> "My Favorites"
                "পছন্দের গল্প" -> "Favorite Stories"
                "সংরক্ষিত ছন্দ" -> "Saved Poems"
                "পছন্দের তালিকায় কোনো গল্প নেই!" -> "No favorite stories in the list!"
                "গল্পের পাশে থাকা হার্ট আইকনে ক্লিক করে যুক্ত করুন।" -> "Click the heart icon next to a story to add."
                "কোনো ছন্দ সংরক্ষিত নেই!" -> "No poems saved yet!"
                "ছন্দের নিচে থাকা বুকমার্ক বা হার্ট আইকনে ক্লিক করুন।" -> "Click the bookmark or heart icon below a poem."
                
                // Home Screen
                "পুণ্যতার গল্প" -> "Stories of Purity"
                "নৈতিক ও শিক্ষামূলক গল্প সংগ্রহ" -> "Moral & Educational Story Collection"
                "আপনার প্রিয় গল্প পড়তে নিচের তালিকা থেকে নির্বাচন করুন" -> "Select from the list below to read your favorite story"
                "শিক্ষা ও নৈতিকতা" -> "Moral & Education"
                "অনুপ্রেরণামূলক" -> "Inspirational"
                "জীবনমুখী" -> "Life-oriented"
                "গল্পের তালিকা খালি" -> "Story list is empty"
                "পছন্দের গল্প তালিকা" -> "Favorite Stories"
                "কোনো প্রিয় গল্প নেই" -> "No favorite stories yet"
                "বুকমার্ককৃত ছন্দ" -> "Bookmarked Poems"
                "কোনো বুকমার্ক করা ছন্দ নেই" -> "No bookmarked poems yet"
                "অনুসন্ধান করুন..." -> "Search stories..."
                "গল্প খুঁজুন..." -> "Search stories..."
                "ছন্দ খুঁজুন..." -> "Search poems..."
                "লেখকের নাম" -> "Author Name"
                "কবির নাম" -> "Poet Name"
                "গল্পের ক্যাটাগরি" -> "Category"
                "গল্পের সম্ভাব্য পৃষ্ঠা" -> "Page Count"
                "সম্পূর্ণ গল্প পড়ুন" -> "Read Full Story"
                "পছন্দ তালিকা" -> "Favorites"
                
                // Settings Screen
                "অ্যাপ্লিকেশন কনফিগারেশন" -> "App Configuration"
                "সেটিংস ও তথ্য" -> "Settings & Info"
                "পঠন শৈলী কনফিগারেশন" -> "Reading Interface Configuration"
                "ফন্ট সাইজ" -> "Font Size"
                "লাইনের দূরত্ব" -> "Line Spacing"
                "ফন্ট ফ্যামিলি" -> "Font Family"
                "সিস্টেম সেটিংস" -> "System Settings"
                "নতুন গল্প নোটিফিকেশন" -> "New Content Notifications"
                "গল্প বা ছন্দ আপলোড হলে সাথে সাথে পাবেন" -> "Get notified instantly on uploads"
                "অ্যাপের ভাষা" -> "App Language"
                "বাংলা অথবা ইংরেজি ভাষা নির্বাচন করুন" -> "Select Bengali or English language"
                "ক্যাশ পরিষ্কার করুন (Clear Cache)" -> "Clear Cache"
                "সকল পড়ার ইতিহাস, পছন্দ তালিকা ও বুকমার্ক রিজেক্ট করুন" -> "Clear all reading progress, favorites, and bookmarks"
                "সকল ক্যাশ ও পড়ার ইতিহাস মুছে ফেলা হয়েছে!" -> "All cache and reading history have been cleared!"
                "এডমিন প্যানেল" -> "Admin Panel"
                "নতুন গল্প ও ছন্দ আপলোড বা ডিলিট করুন" -> "Upload or delete new stories & poems"
                "ডেভেলপার পরিচিতি" -> "Developer Profile"
                "নাম: মো: মেদুল ইসলাম" -> "Name: Md Medul Islam"
                "ইমেইল: mdmedulofficial@gmail.com" -> "Email: mdmedulofficial@gmail.com"
                "ডেভেলপার সম্পর্কে বিস্তারিত" -> "Developer Details"
                "অফলাইন রিডিং" -> "Offline Reading"
                "সেভ করুন" -> "Save"
                "স্লোগান: \"গল্পে পবিত্রতা, ছنده অনুভূতি।\"" -> "Slogan: \"Purity in stories, feeling in rhymes.\""
                "ডেভেলপার (Developer)" -> "Developer"
                "ভার্সন (Version)" -> "Version"
                "v1.0 (অফলাইন প্যাক)" -> "v1.0 (Offline Pack)"
                "ডেভেলপার সম্পর্কে আরও জানুন" -> "Learn more about Developer"
                "ডেভেলপার প্রোফাইল" -> "Developer Profile"
                "মোঃ মেদুল (Md Medul)" -> "Md Medul"
                "একটি প্রিমিয়াম বাংলা বই পড়ার অভিজ্ঞতা তৈরি করাই ছিল এই অ্যাপ্লিকেশনের মূল লক্ষ্য। যেখানে বাস্তব পাতার অনুভূতির সাথে সাথে চমৎকার গল্প ও ছন্দের মেলবন্ধন ঘটেছে।" -> "The main goal of this application was to create a premium Bengali book reading experience where a real page fold feel matches sweet stories and poems."
                "বন্ধ করুন" -> "Close"
                
                // Reader Screen
                "সূচিপত্র" -> "Table of Contents"
                "বুকমার্ক" -> "Bookmark"
                "হাইলাইট করুন" -> "Highlight"
                "পৃষ্ঠা জাম্প" -> "Jump to Page"
                "অনুসন্ধান" -> "Search"
                "সম্পূর্ণ" -> "Completed"
                "পৃষ্ঠা" -> "Page"
                "কোনো অধ্যায় নেই" -> "No chapters available"
                "বুকমার্ক যোগ করা হয়েছে" -> "Bookmark added successfully"
                "বুকমার্ক সরানো হয়েছে" -> "Bookmark removed successfully"
                "হাইলাইট যোগ করা হয়েছে" -> "Highlight added successfully"
                "হাইলাইট সরানো হয়েছে" -> "Highlight removed successfully"
                
                // Admin Screen
                "নতুন গল্প প্রকাশ" -> "Publish New Story"
                "নতুন ছন্দ প্রকাশ" -> "Publish New Poem"
                "প্রকাশ করুন" -> "Publish Now"
                "আপডেট করুন" -> "Update Now"
                "ডিলিট করুন" -> "Delete Item"
                "আপনি কি নিশ্চিত?" -> "Are you sure?"
                "ডিলিট" -> "Delete"
                "বাতিল" -> "Cancel"
                
                else -> key
            }
        }
        return key
    }

    init {
        registerNetworkCallback()
        initializeTTS()
        // Run database check and prepopulation
        viewModelScope.launch {
            repository.checkAndPrepopulate()
            // Initialize Firebase Realtime Database sync
            startFirebaseSync()
            // Splash Screen delay simulation
            delay(2500)
            _isSplashFinished.value = true
        }
    }

    // Search query updater
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Select story to read and paginate dynamically
    fun selectStory(story: StoryEntity?) {
        _selectedStory.value = story
        if (story != null) {
            val pages = paginateStory(story.content)
            _paginatedPages.value = pages
            
            // Build Table of Contents
            val toc = mutableListOf<TableOfContentItem>()
            var lastChapter = ""
            for (page in pages) {
                if (page.isCoverPage || page.isTocPage) continue
                if (page.chapterTitle != lastChapter) {
                    toc.add(TableOfContentItem(page.chapterTitle, page.globalPageNumber))
                    lastChapter = page.chapterTitle
                }
            }
            _tableOfContents.value = toc

            // Load last read progress
            _currentPageIndex.value = if (story.lastReadPage in 1..pages.size) {
                story.lastReadPage - 1
            } else {
                0
            }
        } else {
            _paginatedPages.value = emptyList()
            _tableOfContents.value = emptyList()
            _currentPageIndex.value = 0
        }
    }

    // Update reading progress
    fun updateCurrentPageIndex(index: Int) {
        val pages = _paginatedPages.value
        if (index in pages.indices) {
            _currentPageIndex.value = index
            val story = _selectedStory.value
            if (story != null) {
                viewModelScope.launch {
                    repository.updateStoryReadProgress(story.id, index + 1)
                }
            }
        }
    }

    // Toggle story favorite
    fun toggleStoryFavorite(story: StoryEntity) {
        viewModelScope.launch {
            repository.updateStoryFavorite(story.id, !story.isFavorite)
            // Update selected story reference if currently reading
            if (_selectedStory.value?.id == story.id) {
                _selectedStory.value = story.copy(isFavorite = !story.isFavorite)
            }
        }
    }

    // Toggle chondo favorite
    fun toggleChondoFavorite(chondo: ChondoEntity) {
        viewModelScope.launch {
            repository.updateChondoFavorite(chondo.id, !chondo.isFavorite)
        }
    }

    // Toggle chondo bookmark
    fun toggleChondoBookmark(chondo: ChondoEntity) {
        viewModelScope.launch {
            repository.updateChondoBookmark(chondo.id, !chondo.isBookmarked)
        }
    }

    // Add reading bookmark
    fun addBookmark(storyId: Int, pageIndex: Int, chapterTitle: String, note: String = "") {
        viewModelScope.launch {
            repository.insertBookmark(
                BookmarkEntity(
                    storyId = storyId,
                    pageIndex = pageIndex,
                    chapterTitle = chapterTitle,
                    note = note
                )
            )
        }
    }

    // Delete reading bookmark
    fun removeBookmark(id: Int) {
        viewModelScope.launch {
            repository.deleteBookmark(id)
        }
    }

    fun removeBookmarkForPage(storyId: Int, pageIndex: Int) {
        viewModelScope.launch {
            repository.deleteBookmarkForPage(storyId, pageIndex)
        }
    }

    // Add Highlight/Note
    fun addHighlight(storyId: Int, pageIndex: Int, text: String, color: String, note: String? = null, isUnderline: Boolean = false) {
        viewModelScope.launch {
            repository.insertHighlight(
                HighlightEntity(
                    storyId = storyId,
                    pageIndex = pageIndex,
                    selectedText = text,
                    colorHex = color,
                    note = note,
                    isUnderline = isUnderline,
                    isMarker = !isUnderline
                )
            )
        }
    }

    // Remove Highlight
    fun removeHighlight(id: Int) {
        viewModelScope.launch {
            repository.deleteHighlight(id)
        }
    }

    // Font and UI settings updaters
    fun setFontSize(size: Float) {
        _fontSize.value = size.coerceIn(14f, 32f)
    }

    fun setLineHeight(height: Float) {
        _lineHeightMultiplier.value = height.coerceIn(1.2f, 2.2f)
    }

    fun setFontStyle(style: String) {
        if (style in listOf("SansSerif", "Serif", "Monospace")) {
            _fontStyle.value = style
        }
    }

    fun setReaderTheme(theme: String) {
        if (theme in listOf("Light", "Dark", "Sepia")) {
            _readerTheme.value = theme
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        _notificationsEnabled.value = enabled
    }

    fun clearCache() {
        // Resets favorites/bookmarks to pristine state
        viewModelScope.launch {
            // Delete all bookmarks
            val bookmarks = repository.allBookmarks.first()
            for (b in bookmarks) {
                repository.deleteBookmark(b.id)
            }
            // Reset story states
            val stories = repository.allStories.first()
            for (s in stories) {
                repository.updateStoryFavorite(s.id, false)
                repository.updateStoryReadProgress(s.id, 0)
            }
            // Reset chondo states
            val chondos = repository.allChondos.first()
            for (c in chondos) {
                repository.updateChondoFavorite(c.id, false)
                repository.updateChondoBookmark(c.id, false)
            }
        }
    }

    // Admin Simulation (Simulate upload of Story or Chondo to Firebase Realtime Database)
    fun uploadStoryFromAdmin(title: String, author: String, category: String, desc: String, content: String, coverImageUrl: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        viewModelScope.launch {
            val stories = repository.allStories.first()
            val newId = (stories.maxOfOrNull { it.id } ?: 0) + 1
            val newStory = StoryEntity(
                id = newId,
                title = title,
                author = author,
                category = category,
                coverImageUrl = coverImageUrl,
                description = desc,
                content = content,
                isDownloaded = true,
                publishDate = "আজ"
            )
            // Save locally first to guarantee instant updates in the user app
            repository.insertStory(newStory)
            
            try {
                // Upload to Firebase Realtime Database
                FirebaseDatabase.getInstance().getReference("stories")
                    .child(newId.toString())
                    .setValue(newStory)
                    .addOnSuccessListener {
                        onSuccess()
                    }
                    .addOnFailureListener { e ->
                        onFailure(e.localizedMessage ?: "Permission Denied")
                    }
            } catch (e: Exception) {
                e.printStackTrace()
                onFailure(e.localizedMessage ?: "Connection Error")
            }
        }
    }

    fun uploadChondoFromAdmin(title: String, author: String, text: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        viewModelScope.launch {
            val chondos = repository.allChondos.first()
            val newId = (chondos.maxOfOrNull { it.id } ?: 0) + 1
            val newChondo = ChondoEntity(
                id = newId,
                title = title,
                author = author,
                content = text,
                gradientIndex = (0..4).random(),
                publishDate = "আজ"
            )
            // Save locally first to guarantee instant updates in the user app
            repository.insertChondo(newChondo)
            
            try {
                // Upload to Firebase Realtime Database
                FirebaseDatabase.getInstance().getReference("chondos")
                    .child(newId.toString())
                    .setValue(newChondo)
                    .addOnSuccessListener {
                        onSuccess()
                    }
                    .addOnFailureListener { e ->
                        onFailure(e.localizedMessage ?: "Permission Denied")
                    }
            } catch (e: Exception) {
                e.printStackTrace()
                onFailure(e.localizedMessage ?: "Connection Error")
            }
        }
    }

    // Start Realtime synchronization with Firebase Realtime Database
    private fun startFirebaseSync() {
        val databaseRef = FirebaseDatabase.getInstance()

        // Sync Stories from Firebase to Room local cache
        databaseRef.getReference("stories").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.childrenCount == 0L) {
                    prepopulateFirebaseIfEmpty()
                    return
                }

                viewModelScope.launch {
                    val storiesFromFirebase = mutableListOf<StoryEntity>()
                    for (storySnapshot in snapshot.children) {
                        try {
                            val id = storySnapshot.child("id").getValue(Int::class.java) ?: continue
                            val title = storySnapshot.child("title").getValue(String::class.java) ?: ""
                            val author = storySnapshot.child("author").getValue(String::class.java) ?: ""
                            val coverImageUrl = storySnapshot.child("coverImageUrl").getValue(String::class.java) ?: ""
                            val category = storySnapshot.child("category").getValue(String::class.java) ?: ""
                            val description = storySnapshot.child("description").getValue(String::class.java) ?: ""
                            val content = storySnapshot.child("content").getValue(String::class.java) ?: ""
                            val publishDate = storySnapshot.child("publishDate").getValue(String::class.java) ?: ""

                            // Query local cache to preserve user progress, bookmarks, and favorite states
                            val existingStory = repository.getStoryById(id).first()
                            val isFavorite = existingStory?.isFavorite ?: false
                            val lastReadPage = existingStory?.lastReadPage ?: 0
                            val lastReadTime = existingStory?.lastReadTime ?: 0L

                            storiesFromFirebase.add(
                                StoryEntity(
                                    id = id,
                                    title = title,
                                    author = author,
                                    coverImageUrl = coverImageUrl,
                                    category = category,
                                    description = description,
                                    content = content,
                                    isDownloaded = true,
                                    isFavorite = isFavorite,
                                    lastReadPage = lastReadPage,
                                    lastReadTime = lastReadTime,
                                    publishDate = publishDate
                                )
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    val currentStoryIds = repository.allStories.first().map { it.id }.toSet()
                    var hasNewStory = false
                    var latestStory: StoryEntity? = null

                    for (story in storiesFromFirebase) {
                        if (story.id !in currentStoryIds && currentStoryIds.isNotEmpty()) {
                            hasNewStory = true
                            latestStory = story
                        }
                        repository.insertStory(story)
                    }

                    if (hasNewStory && latestStory != null) {
                        _newContentNotification.tryEmit(
                            NotificationPayload(
                                title = "নতুন গল্প আপলোড হয়েছে!",
                                body = "${latestStory.title} - ${latestStory.author} এর নতুন গল্পটি এখনই পড়ুন।",
                                isStory = true,
                                itemId = latestStory.id
                            )
                        )
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                error.toException().printStackTrace()
            }
        })

        // Sync Chondos from Firebase to Room local cache
        databaseRef.getReference("chondos").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.childrenCount == 0L) {
                    prepopulateFirebaseIfEmpty()
                    return
                }

                viewModelScope.launch {
                    val chondosFromFirebase = mutableListOf<ChondoEntity>()
                    for (chondoSnapshot in snapshot.children) {
                        try {
                            val id = chondoSnapshot.child("id").getValue(Int::class.java) ?: continue
                            val title = chondoSnapshot.child("title").getValue(String::class.java) ?: ""
                            val author = chondoSnapshot.child("author").getValue(String::class.java) ?: ""
                            val content = chondoSnapshot.child("content").getValue(String::class.java) ?: ""
                            val gradientIndex = chondoSnapshot.child("gradientIndex").getValue(Int::class.java) ?: 0
                            val publishDate = chondoSnapshot.child("publishDate").getValue(String::class.java) ?: ""

                            // Query local cache to preserve favorite and bookmark states
                            val existingChondo = repository.getChondoById(id).first()
                            val isFavorite = existingChondo?.isFavorite ?: false
                            val isBookmarked = existingChondo?.isBookmarked ?: false

                            chondosFromFirebase.add(
                                ChondoEntity(
                                    id = id,
                                    title = title,
                                    author = author,
                                    content = content,
                                    gradientIndex = gradientIndex,
                                    publishDate = publishDate,
                                    isFavorite = isFavorite,
                                    isBookmarked = isBookmarked,
                                    isSavedOffline = true
                                )
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    val currentChondoIds = repository.allChondos.first().map { it.id }.toSet()
                    var hasNewChondo = false
                    var latestChondo: ChondoEntity? = null

                    for (chondo in chondosFromFirebase) {
                        if (chondo.id !in currentChondoIds && currentChondoIds.isNotEmpty()) {
                            hasNewChondo = true
                            latestChondo = chondo
                        }
                        repository.insertChondo(chondo)
                    }

                    if (hasNewChondo && latestChondo != null) {
                        _newContentNotification.tryEmit(
                            NotificationPayload(
                                title = "নতুন ছন্দ আপলোড হয়েছে!",
                                body = "${latestChondo.title} - ${latestChondo.author} এর নতুন ছন্দটি এখনই উপভোগ করুন।",
                                isStory = false,
                                itemId = latestChondo.id
                            )
                        )
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                error.toException().printStackTrace()
            }
        })
    }

    // Populate Firebase with sample data if it's completely empty on first initialization
    private fun prepopulateFirebaseIfEmpty() {
        val databaseRef = FirebaseDatabase.getInstance()

        // Check and populate stories
        databaseRef.getReference("stories").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.childrenCount == 0L) {
                    val sampleStories = com.example.data.repository.SampleData.sampleStories
                    for (story in sampleStories) {
                        databaseRef.getReference("stories").child(story.id.toString()).setValue(story)
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        // Check and populate chondos
        databaseRef.getReference("chondos").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.childrenCount == 0L) {
                    val sampleChondos = com.example.data.repository.SampleData.sampleChondos
                    for (chondo in sampleChondos) {
                        databaseRef.getReference("chondos").child(chondo.id.toString()).setValue(chondo)
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // Helper: Dynamic pagination algorithm
    private fun paginateStory(content: String): List<BookPage> {
        val pages = mutableListOf<BookPage>()
        
        // Add Cover Page as Page 1
        pages.add(
            BookPage(
                globalPageNumber = 1,
                relativePageNumber = 1,
                totalPagesInChapter = 1,
                chapterTitle = "প্রচ্ছদ",
                text = "",
                isCoverPage = true
            )
        )
        
        // Add Table of Contents Page as Page 2
        pages.add(
            BookPage(
                globalPageNumber = 2,
                relativePageNumber = 1,
                totalPagesInChapter = 1,
                chapterTitle = "সূচিপত্র",
                text = "",
                isTocPage = true
            )
        )
        
        // Parse page count constraint if any, e.g. [PageCount: 5]
        var targetPageCount: Int? = null
        val pageCountRegex = Regex("\\[PageCount:\\s*(\\d+)\\]")
        val matchResult = pageCountRegex.find(content)
        var cleanContent = content
        if (matchResult != null) {
            targetPageCount = matchResult.groupValues[1].toIntOrNull()
            cleanContent = content.replace(pageCountRegex, "").trim()
        }

        // If content does not have standard chapter markings, wrap it in a default chapter
        val processedContent = if (!cleanContent.contains("[Chapter:")) {
            "[Chapter: সূচিপত্র]\n$cleanContent"
        } else {
            cleanContent
        }
        
        val chapters = processedContent.split("[Chapter:")
        var globalPageNum = 3

        val chapterDataList = mutableListOf<Pair<String, String>>() // Pair(ChapterTitle, ChapterBody)
        for (part in chapters) {
            if (part.isBlank()) continue
            val nameEndIndex = part.indexOf("]")
            if (nameEndIndex == -1) {
                chapterDataList.add(Pair("সূচিপত্র", part.trim()))
                continue
            }
            val chapterTitle = part.substring(0, nameEndIndex).trim()
            val chapterBody = part.substring(nameEndIndex + 1).trim()
            chapterDataList.add(Pair(chapterTitle, chapterBody))
        }

        if (targetPageCount != null && targetPageCount > 0) {
            // Admin explicitly specified P pages.
            // Let's distribute P pages across all chapters proportionally to their character length.
            val count: Int = targetPageCount!!
            val totalBodyLength = chapterDataList.sumOf { it.second.length }.coerceAtLeast(1)
            var remainingPages: Int = count
            val pagesPerChapter = mutableListOf<Int>()
            
            for (i in chapterDataList.indices) {
                if (i == chapterDataList.lastIndex) {
                    pagesPerChapter.add(remainingPages)
                } else {
                    val share = (((chapterDataList[i].second.length.toDouble() / totalBodyLength) * count).toInt()).coerceAtLeast(1)
                    pagesPerChapter.add(share)
                    remainingPages = (remainingPages - share).coerceAtLeast(1)
                }
            }

            for (i in chapterDataList.indices) {
                val (chapterTitle, chapterBody) = chapterDataList[i]
                val chPages = pagesPerChapter[i]
                val chapterPagesText = splitTextIntoExactPages(chapterBody, chPages)
                val totalInChapter = chapterPagesText.size
                for ((idx, pageText) in chapterPagesText.withIndex()) {
                    pages.add(
                        BookPage(
                            globalPageNumber = globalPageNum,
                            relativePageNumber = idx + 1,
                            totalPagesInChapter = totalInChapter,
                            chapterTitle = chapterTitle,
                            text = pageText
                        )
                    )
                    globalPageNum++
                }
            }
        } else {
            // Default dynamic flow with character limit = 550
            for ((chapterTitle, chapterBody) in chapterDataList) {
                val chapterPagesText = splitTextIntoPages(chapterBody, limit = 550)
                val totalInChapter = chapterPagesText.size
                for ((idx, pageText) in chapterPagesText.withIndex()) {
                    pages.add(
                        BookPage(
                            globalPageNumber = globalPageNum,
                            relativePageNumber = idx + 1,
                            totalPagesInChapter = totalInChapter,
                            chapterTitle = chapterTitle,
                            text = pageText
                        )
                    )
                    globalPageNum++
                }
            }
        }

        // Fallback if pages is still empty
        if (pages.isEmpty()) {
            pages.add(
                BookPage(
                    globalPageNumber = 1,
                    relativePageNumber = 1,
                    totalPagesInChapter = 1,
                    chapterTitle = "সূচিপত্র",
                    text = cleanContent
                )
            )
        }
        return pages
    }

    private fun splitTextIntoExactPages(text: String, numPages: Int): List<String> {
        if (text.isBlank()) return List(numPages) { "" }
        if (numPages <= 1) return listOf(text.trim())

        val chunks = mutableListOf<String>()
        val totalLength = text.length
        val approxChunkSize = totalLength / numPages
        var currentIndex = 0

        for (i in 0 until numPages) {
            if (currentIndex >= totalLength) {
                chunks.add("")
                continue
            }
            if (i == numPages - 1) {
                chunks.add(text.substring(currentIndex).trim())
                break
            }

            var targetEnd = currentIndex + approxChunkSize
            if (targetEnd >= totalLength) {
                chunks.add(text.substring(currentIndex).trim())
                break
            }

            // Find natural breaking point near targetEnd
            val windowStart = (targetEnd - approxChunkSize * 0.2).toInt().coerceIn(currentIndex, totalLength)
            val windowEnd = (targetEnd + approxChunkSize * 0.2).toInt().coerceIn(currentIndex, totalLength)
            val window = text.substring(windowStart, windowEnd)

            val newlineIdx = window.lastIndexOf('\n')
            val dandaIdx = window.lastIndexOf('।')
            val spaceIdx = window.lastIndexOf(' ')

            var breakOffset = approxChunkSize
            if (newlineIdx != -1) {
                breakOffset = (windowStart - currentIndex) + newlineIdx
            } else if (dandaIdx != -1) {
                breakOffset = (windowStart - currentIndex) + dandaIdx + 1
            } else if (spaceIdx != -1) {
                breakOffset = (windowStart - currentIndex) + spaceIdx
            }

            val breakIndex = (currentIndex + breakOffset).coerceIn(currentIndex + 1, totalLength)
            chunks.add(text.substring(currentIndex, breakIndex).trim())
            currentIndex = breakIndex
        }

        return chunks
    }

    private fun splitTextIntoPages(text: String, limit: Int): List<String> {
        if (text.isBlank()) return listOf("")
        val chunks = mutableListOf<String>()
        var currentIndex = 0
        val length = text.length

        while (currentIndex < length) {
            var nextIndex = currentIndex + limit
            if (nextIndex >= length) {
                chunks.add(text.substring(currentIndex).trim())
                break
            }

            // Try to find natural breaking points like newlines, Bengali full-stop (।), or spaces
            var breakIndex = -1
            val range = text.substring(currentIndex, nextIndex)

            val newlineIdx = range.lastIndexOf('\n')
            val dandaIdx = range.lastIndexOf('।')
            val spaceIdx = range.lastIndexOf(' ')

            if (newlineIdx > limit * 0.7) {
                breakIndex = currentIndex + newlineIdx
            } else if (dandaIdx > limit * 0.7) {
                breakIndex = currentIndex + dandaIdx + 1
            } else if (spaceIdx > limit * 0.6) {
                breakIndex = currentIndex + spaceIdx
            } else {
                breakIndex = nextIndex
            }

            if (breakIndex <= currentIndex) {
                breakIndex = nextIndex
            }

            chunks.add(text.substring(currentIndex, breakIndex).trim())
            currentIndex = breakIndex
        }
        return chunks
    }

    // TTS Logic
    private fun initializeTTS() {
        tts = android.speech.tts.TextToSpeech(getApplication()) { status ->
            if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                tts?.language = java.util.Locale("bn", "BD")
                applyTtsVoiceGender()
            }
        }
    }

    private fun applyTtsVoiceGender() {
        try {
            val voices = tts?.voices ?: return
            val gender = _ttsVoiceGender.value
            val targetVoice = if (gender == "Male") {
                voices.firstOrNull { it.name.lowercase().contains("male") && it.locale.language == "bn" }
                    ?: voices.firstOrNull { it.locale.language == "bn" }
            } else {
                voices.firstOrNull { it.name.lowercase().contains("female") && it.locale.language == "bn" }
                    ?: voices.firstOrNull { it.locale.language == "bn" }
            }
            if (targetVoice != null) {
                tts?.voice = targetVoice
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setTtsVoiceGender(gender: String) {
        if (gender == "Female" || gender == "Male") {
            _ttsVoiceGender.value = gender
            applyTtsVoiceGender()
            if (_isTtsSpeaking.value) {
                val currentText = lastSpokenText
                val itemId = _currentTtsItemId.value
                val itemType = _currentTtsItemType.value
                val title = _currentTtsTitle.value
                if (currentText.isNotBlank() && itemId != null && itemType != null) {
                    stopSpeaking()
                    speakItem(currentText, itemId, itemType, title)
                }
            }
        }
    }

    fun setTtsVolume(volume: Float) {
        _ttsVolume.value = volume.coerceIn(0.0f, 1.0f)
        if (_isTtsSpeaking.value) {
            // Re-speak to apply volume immediately or rely on next utterance
            val currentText = lastSpokenText
            val itemId = _currentTtsItemId.value
            val itemType = _currentTtsItemType.value
            val title = _currentTtsTitle.value
            if (currentText.isNotBlank() && itemId != null && itemType != null) {
                stopSpeaking()
                speakItem(currentText, itemId, itemType, title)
            }
        }
    }

    fun speakItem(text: String, itemId: Int, itemType: String, title: String) {
        if (_isTtsSpeaking.value && _currentTtsItemId.value == itemId && _currentTtsItemType.value == itemType) {
            stopSpeaking()
            return
        }

        lastSpokenText = text
        _currentTtsItemId.value = itemId
        _currentTtsItemType.value = itemType
        _currentTtsTitle.value = title
        _isTtsSpeaking.value = true

        val cleanText = text.replace(Regex("\\[Chapter:[^\\]]*\\]"), "")
            .replace(Regex("\\[PageCount:[^\\]]*\\]"), "")
            .trim()

        val params = android.os.Bundle().apply {
            putFloat(android.speech.tts.TextToSpeech.Engine.KEY_PARAM_VOLUME, _ttsVolume.value)
        }

        tts?.speak(cleanText, android.speech.tts.TextToSpeech.QUEUE_FLUSH, params, "UtteranceId")
        
        tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                _isTtsSpeaking.value = false
                _currentTtsItemId.value = null
                _currentTtsItemType.value = null
            }
            override fun onError(utteranceId: String?) {
                _isTtsSpeaking.value = false
                _currentTtsItemId.value = null
                _currentTtsItemType.value = null
            }
        })
    }

    fun stopSpeaking() {
        tts?.stop()
        _isTtsSpeaking.value = false
        _currentTtsItemId.value = null
        _currentTtsItemType.value = null
    }

    // Admin Deletion Functions
    fun deleteStoryFromAdmin(id: Int, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        viewModelScope.launch {
            try {
                repository.deleteStory(id)
                FirebaseDatabase.getInstance().getReference("stories")
                    .child(id.toString())
                    .removeValue()
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { e -> onFailure(e.localizedMessage ?: "Permission Denied") }
            } catch (e: Exception) {
                e.printStackTrace()
                onFailure(e.localizedMessage ?: "Error deleting story")
            }
        }
    }

    fun deleteChondoFromAdmin(id: Int, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        viewModelScope.launch {
            try {
                repository.deleteChondo(id)
                FirebaseDatabase.getInstance().getReference("chondos")
                    .child(id.toString())
                    .removeValue()
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { e -> onFailure(e.localizedMessage ?: "Permission Denied") }
            } catch (e: Exception) {
                e.printStackTrace()
                onFailure(e.localizedMessage ?: "Error deleting chondo")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun registerNetworkCallback() {
        try {
            val connectivityManager = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            
            // Initial check
            val activeNetwork = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            _isOnline.value = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

            // Real-time callback
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
                
            connectivityManager.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    _isOnline.value = true
                }

                override fun onLost(network: Network) {
                    _isOnline.value = false
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
            _isOnline.value = true // Fallback to true if connectivity service is restricted
        }
    }
}
