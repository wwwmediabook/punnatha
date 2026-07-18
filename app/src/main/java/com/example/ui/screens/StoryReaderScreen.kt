package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.HighlightEntity
import com.example.ui.viewmodel.PunnotarGolpoViewModel
import com.example.ui.utils.SoundHelper
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.absoluteValue
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun StoryReaderScreen(
    viewModel: PunnotarGolpoViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val story by viewModel.selectedStory.collectAsState()
    val pages by viewModel.paginatedPages.collectAsState()
    val toc by viewModel.tableOfContents.collectAsState()
    val currentPageIndex by viewModel.currentPageIndex.collectAsState()
    
    // UI Settings
    val fontSize by viewModel.fontSize.collectAsState()
    val lineHeightMultiplier by viewModel.lineHeightMultiplier.collectAsState()
    val fontStyleSelected by viewModel.fontStyle.collectAsState()
    val readerThemeSelected by viewModel.readerTheme.collectAsState()
    
    // Notes and Highlights
    val highlights by viewModel.currentStoryHighlights.collectAsState()
    val bookmarks by viewModel.currentStoryBookmarks.collectAsState()

    // TTS state collections
    val isTtsSpeaking by viewModel.isTtsSpeaking.collectAsState()
    val ttsVoiceGender by viewModel.ttsVoiceGender.collectAsState()
    val ttsVolume by viewModel.ttsVolume.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Active bottom sheet toggles
    var showFontSettings by remember { mutableStateOf(false) }
    var showTableOfContents by remember { mutableStateOf(false) }
    var showAddHighlightDialog by remember { mutableStateOf(false) }
    var showJumpDialog by remember { mutableStateOf(false) }

    if (story == null || pages.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFFFFD700))
        }
        return
    }

    // Pager state linked to ViewModel state
    val pagerState = rememberPagerState(
        initialPage = currentPageIndex.coerceIn(pages.indices),
        pageCount = { pages.size }
    )

    // Play page turn sound on page change, skipping initial loading trigger
    var lastPlayedPage by remember { mutableStateOf(pagerState.currentPage) }
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != lastPlayedPage) {
            SoundHelper.playPageFlipSound()
            lastPlayedPage = pagerState.currentPage
        }
        viewModel.updateCurrentPageIndex(pagerState.currentPage)
    }

    // Sync search jump or chapter jump
    LaunchedEffect(currentPageIndex) {
        if (currentPageIndex != pagerState.currentPage && currentPageIndex in pages.indices) {
            pagerState.animateScrollToPage(currentPageIndex)
        }
    }

    // Theme values
    val (backgroundColor, textColor, accentColor) = when (readerThemeSelected) {
        "Sepia" -> Triple(Color(0xFFFFF9E6), Color(0xFF1C1C1C), Color(0xFFD4AF37)) // Warm Yellow Paper
        "Dark" -> Triple(Color(0xFF121212), Color(0xFFE0E0E0), Color(0xFFFFD700))   // Midnight Black
        else -> Triple(Color(0xFFFFFFFF), Color(0xFF000000), Color(0xFF1E3A8A))      // Classic Light
    }

    val composeFontFamily = when (fontStyleSelected) {
        "Serif" -> FontFamily.Serif
        "Monospace" -> FontFamily.Monospace
        else -> FontFamily.SansSerif
    }

    val currentGlobalPage = pagerState.currentPage + 1
    val isPageBookmarked = bookmarks.any { it.pageIndex == currentGlobalPage }

    Scaffold(
        modifier = modifier.testTag("story_reader_root"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = story?.title ?: "",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = textColor
                        )
                    }
                },
                actions = {
                    // Quick page search / jump
                    IconButton(onClick = { showJumpDialog = true }) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Page Search", tint = textColor)
                    }

                    // Table of contents icon
                    IconButton(onClick = { showTableOfContents = true }) {
                        Icon(imageVector = Icons.Default.FormatListNumbered, contentDescription = "Table of Contents", tint = textColor)
                    }

                    // Highlights/Notes icon
                    IconButton(onClick = { showAddHighlightDialog = true }) {
                        Icon(imageVector = Icons.Default.BorderColor, contentDescription = "Add Highlight or Note", tint = textColor)
                    }

                    // Bookmark icon
                    IconButton(onClick = {
                        if (isPageBookmarked) {
                            viewModel.removeBookmarkForPage(story!!.id, currentGlobalPage)
                            Toast.makeText(context, "বুকমার্ক মুছে ফেলা হয়েছে", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.addBookmark(
                                storyId = story!!.id,
                                pageIndex = currentGlobalPage,
                                chapterTitle = pages[pagerState.currentPage].chapterTitle
                            )
                            Toast.makeText(context, "পৃষ্ঠা বুকমার্ক করা হয়েছে!", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(
                            imageVector = if (isPageBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Bookmark Page",
                            tint = if (isPageBookmarked) Color(0xFFFFD700) else textColor
                        )
                    }

                    // Speaker / TTS Button
                    IconButton(onClick = {
                        val currentText = pages[pagerState.currentPage].text
                        if (currentText.isNotBlank()) {
                            viewModel.speakItem(
                                text = currentText,
                                itemId = story!!.id,
                                itemType = "story",
                                title = story!!.title
                            )
                        } else {
                            Toast.makeText(context, "এই পৃষ্ঠায় পড়ার উপযোগী কোনো লেখা নেই", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(
                            imageVector = if (isTtsSpeaking) Icons.Filled.VolumeUp else Icons.Outlined.VolumeUp,
                            contentDescription = "Read Aloud",
                            tint = if (isTtsSpeaking) Color(0xFFFFD700) else textColor
                        )
                    }

                    // Font options button
                    IconButton(onClick = { showFontSettings = true }) {
                        Icon(imageVector = Icons.Default.TextFormat, contentDescription = "Font Settings", tint = textColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundColor,
                    titleContentColor = textColor
                )
            )
        },
        bottomBar = {
            // Immersive footer as requested with Developer Watermark & Page Number
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(backgroundColor)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Centered Developer watermark
                Text(
                    text = "Developer Md Medul",
                    fontSize = 11.sp,
                    color = textColor.copy(alpha = 0.4f),
                    fontStyle = FontStyle.Italic,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center)
                )

                // Right aligned page count
                Text(
                    text = "পৃষ্ঠা: $currentGlobalPage / ${pages.size}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = textColor.copy(alpha = 0.7f),
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }
        },
        containerColor = backgroundColor
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Book Swiping view
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { pageIdx ->
                val page = pages[pageIdx]
                val pageHighlights = highlights.filter { it.pageIndex == pageIdx + 1 }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            // Calculate exact fractional scroll offset for this page
                            val pageOffset = ((pagerState.currentPage - pageIdx) + pagerState.currentPageOffsetFraction)
                            
                            // 3D fold and scale transition
                            val scale = (1f - pageOffset.absoluteValue * 0.12f).coerceIn(0.88f, 1f)
                            val alpha = (1f - pageOffset.absoluteValue * 0.5f).coerceIn(0.5f, 1f)
                            val rotation = pageOffset * -45f
                            
                            this.scaleX = scale
                            this.scaleY = scale
                            this.alpha = alpha
                            this.rotationY = rotation.coerceIn(-90f, 90f)
                            this.cameraDistance = 8 * density
                            
                            // Pivot on the left or right edges depending on fold direction
                            if (pageOffset > 0) {
                                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
                            } else {
                                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 0.5f)
                            }
                        }
                ) {
                    if (page.isCoverPage) {
                        // Render Cover Page
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .border(
                                    width = 2.dp,
                                    color = accentColor.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "গল্পের বই",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = accentColor,
                                    letterSpacing = 2.sp
                                )
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Text(
                                    text = story?.title ?: "",
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 34.sp
                                )
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                Text(
                                    text = "— ${story?.author ?: ""} —",
                                    fontSize = 16.sp,
                                    fontStyle = FontStyle.Italic,
                                    color = textColor.copy(alpha = 0.8f),
                                    textAlign = TextAlign.Center
                                )
                                
                                Spacer(modifier = Modifier.height(24.dp))
                                
                                if (story != null && !story!!.coverImageUrl.isNullOrBlank() && story!!.coverImageUrl.startsWith("http")) {
                                    Card(
                                        modifier = Modifier
                                            .size(width = 140.dp, height = 190.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                                    ) {
                                        coil.compose.AsyncImage(
                                            model = story!!.coverImageUrl,
                                            contentDescription = "Cover Image",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                        )
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(width = 120.dp, height = 170.dp)
                                            .background(
                                                androidx.compose.ui.graphics.Brush.verticalGradient(
                                                    colors = listOf(Color(0xFF1E3A8A), Color(0xFF111827))
                                                ),
                                                shape = RoundedCornerShape(10.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("📖", fontSize = 54.sp)
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(24.dp))
                                
                                Text(
                                    text = "পড়ার জন্য বামে সোয়াইপ করুন 📖",
                                    fontSize = 11.sp,
                                    color = textColor.copy(alpha = 0.5f),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    } else if (page.isTocPage) {
                        // Render TOC Page
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Ornamental Header Badge
                            Box(
                                modifier = Modifier
                                    .padding(bottom = 8.dp)
                                    .background(accentColor.copy(alpha = 0.08f), shape = RoundedCornerShape(12.dp))
                                    .border(1.dp, accentColor.copy(alpha = 0.2f), shape = RoundedCornerShape(12.dp))
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome, 
                                        contentDescription = null, 
                                        tint = accentColor, 
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "অধ্যায় বিন্যাস",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = accentColor,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }

                            Text(
                                text = "সূচিপত্র",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = textColor,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )

                            Divider(
                                color = accentColor.copy(alpha = 0.3f),
                                thickness = 2.dp,
                                modifier = Modifier.width(40.dp)
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth().weight(1f)
                            ) {
                                items(toc) { item ->
                                    Card(
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = textColor.copy(alpha = 0.03f)),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, textColor.copy(alpha = 0.07f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                coroutineScope.launch {
                                                    pagerState.animateScrollToPage(item.startPageNumber - 1)
                                                }
                                            }
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 12.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .background(accentColor.copy(alpha = 0.1f), shape = CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text("📜", fontSize = 14.sp)
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(
                                                    text = item.chapterTitle,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = textColor,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "পৃষ্ঠা ${item.startPageNumber}",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = accentColor
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Icon(
                                                    imageVector = Icons.Default.ChevronRight,
                                                    contentDescription = null,
                                                    tint = accentColor,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Chapter Heading
                            item {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 16.dp, bottom = 8.dp)
                                ) {
                                    Text(
                                        text = page.chapterTitle,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = accentColor.copy(alpha = 0.8f),
                                        textAlign = TextAlign.Center,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Divider(
                                        color = accentColor.copy(alpha = 0.2f),
                                        thickness = 1.dp,
                                        modifier = Modifier.width(60.dp)
                                    )
                                }
                            }

                            // Story Image shown "মাঝে মাঝে" (interspersed on odd page numbers)
                            val showStoryImage = story != null && 
                                    !story!!.coverImageUrl.isNullOrBlank() && 
                                    story!!.coverImageUrl.startsWith("http") && 
                                    (page.relativePageNumber % 2 == 1)

                            if (showStoryImage) {
                                item {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Card(
                                            modifier = Modifier
                                                .size(width = 180.dp, height = 120.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color.White)
                                        ) {
                                            coil.compose.AsyncImage(
                                                model = story!!.coverImageUrl,
                                                contentDescription = "Story illustration",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }

                            // Main Reading Text
                            item {
                                Text(
                                    text = page.text,
                                    fontSize = fontSize.sp,
                                    fontFamily = composeFontFamily,
                                    lineHeight = (fontSize * lineHeightMultiplier).sp,
                                    color = textColor,
                                    textAlign = TextAlign.Justify,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("reader_page_text")
                                )
                            }

                            // Display Highlights / Notes applied to this page visually at the bottom
                            if (pageHighlights.isNotEmpty()) {
                                item {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "এই পৃষ্ঠার হাইলাইট ও নোটসমূহ:",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor.copy(alpha = 0.7f)
                                    )
                                }

                                items(pageHighlights) { highlight ->
                                    val highlightBg = when (highlight.colorHex) {
                                        "Yellow" -> Color(0xFFFEF08A)
                                        "Green" -> Color(0xFFBBF7D0)
                                        "Blue" -> Color(0xFFBFDBFE)
                                        "Pink" -> Color(0xFFFBCFE8)
                                        else -> Color(0xFFFEF08A)
                                    }
                                    
                                    val highlightBorder = when (highlight.colorHex) {
                                        "Yellow" -> Color(0xFFCA8A04)
                                        "Green" -> Color(0xFF16A34A)
                                        "Blue" -> Color(0xFF2563EB)
                                        "Pink" -> Color(0xFFDB2777)
                                        else -> Color(0xFFCA8A04)
                                    }

                                    Card(
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = highlightBg.copy(alpha = 0.15f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(
                                                width = 0.5.dp,
                                                color = highlightBorder.copy(alpha = 0.3f),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(10.dp)
                                                            .background(highlightBg, shape = CircleShape)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = if (highlight.isUnderline) "আন্ডারলাইন" else "হাইলাইটার",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = highlightBorder
                                                    )
                                                }
                                                
                                                IconButton(
                                                    onClick = { viewModel.removeHighlight(highlight.id) },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Delete Highlight",
                                                        tint = Color.Red.copy(alpha = 0.7f),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(4.dp))

                                            Text(
                                                text = "“${highlight.selectedText}”",
                                                fontSize = 13.sp,
                                                fontStyle = FontStyle.Italic,
                                                textDecoration = if (highlight.isUnderline) TextDecoration.Underline else TextDecoration.None,
                                                color = textColor
                                            )

                                            if (!highlight.note.isNullOrBlank()) {
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Divider(color = textColor.copy(alpha = 0.08f), thickness = 0.5.dp)
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Row(verticalAlignment = Alignment.Top) {
                                                    Icon(
                                                        imageVector = Icons.Default.Note,
                                                        contentDescription = null,
                                                        tint = accentColor,
                                                        modifier = Modifier
                                                            .size(14.dp)
                                                            .padding(top = 2.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = highlight.note,
                                                        fontSize = 12.sp,
                                                        color = textColor.copy(alpha = 0.9f),
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

            // --- JUMP TO PAGE DIALOG ---
            if (showJumpDialog) {
                var pageInput by remember { mutableStateOf("") }
                AlertDialog(
                    onDismissRequest = { showJumpDialog = false },
                    title = { Text("পৃষ্ঠায় যান (Jump to Page)") },
                    text = {
                        Column {
                            Text("১ থেকে ${pages.size} এর মধ্যে পৃষ্ঠা নম্বর লিখুন:", fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            TextField(
                                value = pageInput,
                                onValueChange = { pageInput = it },
                                placeholder = { Text("যেমন: ২") },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            val targetPage = pageInput.toIntOrNull()
                            if (targetPage != null && targetPage in 1..pages.size) {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(targetPage - 1)
                                }
                                showJumpDialog = false
                            } else {
                                Toast.makeText(context, "সঠিক পৃষ্ঠা নম্বর দিন", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Text("ওকে")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showJumpDialog = false }) {
                            Text("বাতিল")
                        }
                    }
                )
            }

            // --- FONT SETTINGS BOTTOM SHEET ---
            if (showFontSettings) {
                ModalBottomSheet(
                    onDismissRequest = { showFontSettings = false },
                    containerColor = backgroundColor
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = "পড়া কাস্টমাইজ করুন",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // 1. Text Theme Toggles
                        Text(
                            text = "ব্যাকগ্রাউন্ড থিম",
                            fontSize = 13.sp,
                            color = textColor.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf("Light", "Sepia", "Dark").forEach { t ->
                                val isSelected = readerThemeSelected == t
                                val themeBg = when (t) {
                                    "Sepia" -> Color(0xFFFFF9E6)
                                    "Dark" -> Color(0xFF121212)
                                    else -> Color(0xFFFFFFFF)
                                }
                                val themeBorderColor = if (isSelected) Color(0xFFFFD700) else Color.LightGray

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(themeBg)
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = themeBorderColor,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { viewModel.setReaderTheme(t) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = when (t) {
                                            "Sepia" -> "সেপিয়া (হলুদ)"
                                            "Dark" -> "ডার্ক মোড"
                                            else -> "লাইট মোড"
                                        },
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (t == "Dark") Color.White else Color.Black
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 2. Font Style Picker
                        Text(
                            text = "ফন্ট স্টাইল",
                            fontSize = 13.sp,
                            color = textColor.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf("SansSerif", "Serif", "Monospace").forEach { f ->
                                val isSelected = fontStyleSelected == f
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.setFontStyle(f) },
                                    label = { Text(f) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 3. Font Size Slider
                        Text(
                            text = "ফন্ট সাইজ: ${fontSize.toInt()} sp",
                            fontSize = 13.sp,
                            color = textColor.copy(alpha = 0.7f)
                        )
                        Slider(
                            value = fontSize,
                            onValueChange = { viewModel.setFontSize(it) },
                            valueRange = 14f..32f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFFFD700),
                                activeTrackColor = Color(0xFFFFD700)
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // 4. Line Height Slider
                        Text(
                            text = "লাইনের ব্যবধান (Line Height): ${String.format("%.1f", lineHeightMultiplier)}x",
                            fontSize = 13.sp,
                            color = textColor.copy(alpha = 0.7f)
                        )
                        Slider(
                            value = lineHeightMultiplier,
                            onValueChange = { viewModel.setLineHeight(it) },
                            valueRange = 1.2f..2.2f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFFFD700),
                                activeTrackColor = Color(0xFFFFD700)
                            )
                        )
                        
                        // 5. Text-To-Speech Settings
                        Divider(color = textColor.copy(alpha = 0.1f), thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))
                        Text(
                            text = "অডিও পঠন সেটিংস (Speech Settings)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        // Speaker Play/Stop Button
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        ) {
                            Button(
                                onClick = {
                                    val currentText = pages[pagerState.currentPage].text
                                    if (currentText.isNotBlank()) {
                                        viewModel.speakItem(
                                            text = currentText,
                                            itemId = story!!.id,
                                            itemType = "story",
                                            title = story!!.title
                                        )
                                    } else {
                                        Toast.makeText(context, "এই পৃষ্ঠায় কোনো পড়ার উপযোগী লেখা নেই", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isTtsSpeaking) Color.Red else Color(0xFFFFD700),
                                    contentColor = if (isTtsSpeaking) Color.White else Color.Black
                                ),
                                modifier = Modifier.weight(1.2f)
                            ) {
                                Icon(
                                    imageVector = if (isTtsSpeaking) Icons.Default.Stop else Icons.Default.VolumeUp,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isTtsSpeaking) "পড়া বন্ধ করুন" else "পৃষ্ঠাটি শুনুন",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        // Voice Gender Selection
                        Text(
                            text = "কণ্ঠস্বর (Voice Gender)",
                            fontSize = 13.sp,
                            color = textColor.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        ) {
                            listOf("Female", "Male").forEach { gender ->
                                val isGenderSelected = ttsVoiceGender == gender
                                FilterChip(
                                    selected = isGenderSelected,
                                    onClick = { viewModel.setTtsVoiceGender(gender) },
                                    label = {
                                        Text(if (gender == "Female") "নারী কণ্ঠ (Female)" else "পুরুষ কণ্ঠ (Male)")
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        
                        // Volume control slider
                        Text(
                            text = "ভলিউম (Volume): ${(ttsVolume * 100).toInt()}%",
                            fontSize = 13.sp,
                            color = textColor.copy(alpha = 0.7f)
                        )
                        Slider(
                            value = ttsVolume,
                            onValueChange = { viewModel.setTtsVolume(it) },
                            valueRange = 0.0f..1.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFFFD700),
                                activeTrackColor = Color(0xFFFFD700)
                            )
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }

            // --- TABLE OF CONTENTS BOTTOM SHEET ---
            if (showTableOfContents) {
                ModalBottomSheet(
                    onDismissRequest = { showTableOfContents = false },
                    containerColor = backgroundColor
                ) {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 24.dp, vertical = 20.dp)
                            .fillMaxWidth()
                    ) {
                        // Decorative Header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 18.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(accentColor.copy(alpha = 0.12f), shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "সূচিপত্র",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = textColor
                                )
                                Text(
                                    text = "সরাসরি অধ্যায়ে যেতে স্পর্শ করুন",
                                    fontSize = 11.sp,
                                    color = textColor.copy(alpha = 0.5f)
                                )
                            }
                        }

                        Divider(color = textColor.copy(alpha = 0.08f), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(16.dp))

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(toc) { item ->
                                val isCurrentChapter = pagerState.currentPage == (item.startPageNumber - 1)
                                val itemBg = if (isCurrentChapter) accentColor.copy(alpha = 0.12f) else textColor.copy(alpha = 0.03f)
                                val itemBorderColor = if (isCurrentChapter) accentColor.copy(alpha = 0.4f) else textColor.copy(alpha = 0.07f)
                                val itemTextColor = if (isCurrentChapter) accentColor else textColor

                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = itemBg),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, itemBorderColor),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(item.startPageNumber - 1)
                                            }
                                            showTableOfContents = false
                                        }
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 12.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .background(
                                                        if (isCurrentChapter) accentColor.copy(alpha = 0.2f) else textColor.copy(alpha = 0.06f), 
                                                        shape = CircleShape
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = if (isCurrentChapter) "✨" else "📜", 
                                                    fontSize = 13.sp
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = item.chapterTitle,
                                                fontSize = 15.sp,
                                                fontWeight = if (isCurrentChapter) FontWeight.Bold else FontWeight.Medium,
                                                color = itemTextColor,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "পৃষ্ঠা ${item.startPageNumber}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isCurrentChapter) accentColor else textColor.copy(alpha = 0.6f)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                imageVector = Icons.Default.ChevronRight,
                                                contentDescription = null,
                                                tint = if (isCurrentChapter) accentColor else textColor.copy(alpha = 0.3f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }

            // --- HIGHLIGHTS AND NOTES DIALOG/SHEET ---
            if (showAddHighlightDialog) {
                var highlightText by remember { mutableStateOf("") }
                var noteText by remember { mutableStateOf("") }
                var selectedColor by remember { mutableStateOf("Yellow") }
                var isUnderline by remember { mutableStateOf(false) }

                ModalBottomSheet(
                    onDismissRequest = { showAddHighlightDialog = false },
                    containerColor = backgroundColor
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = "হাইলাইট ও নোট যোগ করুন",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // Input: Phrase to highlight
                        Text(
                            text = "হাইলাইট করার টেক্সট বা বাক্য:",
                            fontSize = 13.sp,
                            color = textColor.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        OutlinedTextField(
                            value = highlightText,
                            onValueChange = { highlightText = it },
                            placeholder = { Text("যেমন: সৎ মনই ছিল তার বাঁশির মিষ্টি সুরের উৎস") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFFFD700),
                                focusedLabelColor = Color(0xFFFFD700)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        )

                        // Input: Custom Personal Note
                        Text(
                            text = "আপনার ব্যক্তিগত নোট / মন্তব্য (ঐচ্ছিক):",
                            fontSize = 13.sp,
                            color = textColor.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        OutlinedTextField(
                            value = noteText,
                            onValueChange = { noteText = it },
                            placeholder = { Text("যেমন: অসাধারণ চিন্তা, আমাদের জীবনের সাথে মিলে যায়।") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFFFD700),
                                focusedLabelColor = Color(0xFFFFD700)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        )

                        // Color selection row
                        Text(
                            text = "হাইলাইট মার্কার রং:",
                            fontSize = 13.sp,
                            color = textColor.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf("Yellow", "Green", "Blue", "Pink").forEach { c ->
                                val colorValue = when (c) {
                                    "Yellow" -> Color(0xFFFEF08A)
                                    "Green" -> Color(0xFFBBF7D0)
                                    "Blue" -> Color(0xFFBFDBFE)
                                    "Pink" -> Color(0xFFFBCFE8)
                                    else -> Color(0xFFFEF08A)
                                }
                                val isColorSelected = selectedColor == c

                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(colorValue)
                                        .border(
                                            width = if (isColorSelected) 3.dp else 0.dp,
                                            color = Color.DarkGray,
                                            shape = CircleShape
                                        )
                                        .clickable { selectedColor = c }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Underline vs Marker toggle
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = isUnderline,
                                onCheckedChange = { isUnderline = it },
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFFFFD700))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "আন্ডারলাইন হিসেবে চিহ্নিত করুন (Underline style)",
                                fontSize = 13.sp,
                                color = textColor
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (highlightText.isNotBlank()) {
                                    viewModel.addHighlight(
                                        storyId = story!!.id,
                                        pageIndex = currentGlobalPage,
                                        text = highlightText.trim(),
                                        color = selectedColor,
                                        note = noteText.trim().ifBlank { null },
                                        isUnderline = isUnderline
                                    )
                                    Toast.makeText(context, "হাইলাইট ও নোট সেভ করা হয়েছে!", Toast.LENGTH_SHORT).show()
                                    showAddHighlightDialog = false
                                } else {
                                    Toast.makeText(context, "টেক্সট খালি রাখা যাবে না", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C3A4B), contentColor = Color(0xFFFFD700)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("সংরক্ষণ করুন")
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
