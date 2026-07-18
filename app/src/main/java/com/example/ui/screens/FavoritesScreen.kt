package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.ChondoEntity
import com.example.data.database.StoryEntity
import com.example.ui.viewmodel.PunnotarGolpoViewModel

@Composable
fun FavoritesScreen(
    viewModel: PunnotarGolpoViewModel,
    onStoryClick: (StoryEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val stories by viewModel.allStories.collectAsState()
    val chondos by viewModel.allChondos.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()

    // TTS states
    val isTtsSpeaking by viewModel.isTtsSpeaking.collectAsState()
    val currentTtsItemId by viewModel.currentTtsItemId.collectAsState()
    val currentTtsItemType by viewModel.currentTtsItemType.collectAsState()
    
    val favoriteStories = remember(stories) { stories.filter { it.isFavorite } }
    val favoriteChondos = remember(chondos) { chondos.filter { it.isFavorite || it.isBookmarked } }

    var tabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf(
        "${viewModel.translate("পছন্দের গল্প")} (${favoriteStories.size})",
        "${viewModel.translate("সংরক্ষিত ছন্দ")} (${favoriteChondos.size})"
    )

    // Card gradients list
    val cardGradients = listOf(
        Brush.linearGradient(listOf(Color(0xFF8A2387), Color(0xFFE94057), Color(0xFFF27121))),
        Brush.linearGradient(listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))),
        Brush.linearGradient(listOf(Color(0xFF11998e), Color(0xFF38ef7d))),
        Brush.linearGradient(listOf(Color(0xFF4e54c8), Color(0xFF8f94fb))),
        Brush.linearGradient(listOf(Color(0xFFfc4a1a), Color(0xFFf7b733)))
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF4F6F9))
            .testTag("favorites_screen_root")
    ) {
        // Upper banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF0F2027), Color(0xFF1C3A4B))
                    )
                )
                .padding(top = 24.dp, bottom = 12.dp, start = 16.dp, end = 16.dp)
        ) {
            Column {
                Text(
                    text = viewModel.translate("ব্যক্তিগত লাইব্রেরি"),
                    fontSize = 13.sp,
                    color = Color(0xFFFFD700)
                )
                Text(
                    text = viewModel.translate("আমার প্রিয় তালিকা"),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // Tab selection row
        TabRow(
            selectedTabIndex = tabIndex,
            containerColor = Color.White,
            contentColor = Color(0xFF1C3A4B),
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[tabIndex]),
                    color = Color(0xFFFFD700)
                )
            }
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = tabIndex == index,
                    onClick = { tabIndex = index },
                    text = {
                        Text(
                            text = title,
                            fontSize = 14.sp,
                            fontWeight = if (tabIndex == index) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                )
            }
        }

        if (tabIndex == 0) {
            // Stories Tab
            if (favoriteStories.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("❤️", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = viewModel.translate("পছন্দের তালিকায় কোনো গল্প নেই!"),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Gray
                        )
                        Text(
                            text = viewModel.translate("গল্পের পাশে থাকা হার্ট আইকনে ক্লিক করে যুক্ত করুন।"),
                            fontSize = 12.sp,
                            color = Color.Gray.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(favoriteStories, key = { it.id }) { story ->
                        val isSpeaking = isTtsSpeaking && currentTtsItemId == story.id && currentTtsItemType == "story"
                        StoryCard(
                            story = story,
                            onCardClick = { onStoryClick(story) },
                            onFavoriteToggle = { viewModel.toggleStoryFavorite(story) },
                            onSpeakClick = {
                                if (isSpeaking) {
                                    viewModel.stopSpeaking()
                                } else {
                                    viewModel.speakItem(
                                        text = "${story.title}। লিখেছেন ${story.author}। ${story.description}",
                                        itemId = story.id,
                                        itemType = "story",
                                        title = story.title
                                    )
                                }
                            },
                            isSpeaking = isSpeaking,
                            modifier = Modifier.testTag("fav_story_${story.id}")
                        )
                    }
                }
            }
        } else {
            // Rhymes Tab
            if (favoriteChondos.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🔖", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = viewModel.translate("কোনো ছন্দ সংরক্ষিত নেই!"),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Gray
                        )
                        Text(
                            text = viewModel.translate("ছন্দের নিচে থাকা বুকমার্ক বা হার্ট আইকনে ক্লিক করুন।"),
                            fontSize = 12.sp,
                            color = Color.Gray.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(favoriteChondos, key = { it.id }) { chondo ->
                        val gradient = cardGradients[chondo.gradientIndex % cardGradients.size]
                        val isSpeaking = isTtsSpeaking && currentTtsItemId == chondo.id && currentTtsItemType == "chondo"
                        ChondoCard(
                            chondo = chondo,
                            gradient = gradient,
                            onFavoriteToggle = { viewModel.toggleChondoFavorite(chondo) },
                            onBookmarkToggle = { viewModel.toggleChondoBookmark(chondo) },
                            onSpeakClick = {
                                if (isSpeaking) {
                                    viewModel.stopSpeaking()
                                } else {
                                    viewModel.speakItem(
                                        text = "${chondo.title}। লিখেছেন ${chondo.author}। ${chondo.content}",
                                        itemId = chondo.id,
                                        itemType = "chondo",
                                        title = chondo.title
                                    )
                                }
                            },
                            isSpeaking = isSpeaking,
                            modifier = Modifier.testTag("fav_chondo_${chondo.id}")
                        )
                    }
                }
            }
        }
    }
}
