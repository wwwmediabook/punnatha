package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.StoryEntity
import com.example.ui.viewmodel.PunnotarGolpoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: PunnotarGolpoViewModel,
    onStoryClick: (StoryEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val stories by viewModel.allStories.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()
    
    // TTS states
    val isTtsSpeaking by viewModel.isTtsSpeaking.collectAsState()
    val currentTtsItemId by viewModel.currentTtsItemId.collectAsState()
    val currentTtsItemType by viewModel.currentTtsItemType.collectAsState()
    
    // Derived states
    val continueReadingStories = remember(stories) {
        stories.filter { it.lastReadPage > 0 }.sortedByDescending { it.lastReadTime }
    }

    val filteredStories = remember(stories, searchQuery) {
        if (searchQuery.isBlank()) {
            stories
        } else {
            stories.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.author.contains(searchQuery, ignoreCase = true) ||
                it.category.contains(searchQuery, ignoreCase = true) ||
                it.description.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    // Categories list for filtering or visual categorization
    val categories = listOf("সব গল্প", "শিক্ষা ও নৈতিকতা", "অনুপ্রেরণামূলক", "জীবনমুখী")
    var selectedCategory by remember { mutableStateOf("সব গল্প") }

    val finalStories = remember(filteredStories, selectedCategory) {
        if (selectedCategory == "সব গল্প") filteredStories
        else filteredStories.filter { it.category == selectedCategory }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF4F6F9)) // Calm soft background
            .testTag("home_screen_lazy_list"),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        // Welcoming Header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B))
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 28.dp)
            ) {
                // Background subtle decorative glows
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = 50.dp, y = (-50).dp)
                        .background(Color(0xFFFFD700).copy(alpha = 0.08f), shape = CircleShape)
                )

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Color(0xFF38BDF8), shape = CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = viewModel.translate("স্বাগতম"),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF38BDF8),
                                    letterSpacing = 1.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = viewModel.translate("পুণ্যতার গল্প"),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFFFD700), // Rich Gold
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = viewModel.translate("গল্পে পবিত্রতা, ছنده অনুভূতি।"),
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        // Professional launcher status badge
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFFFD700).copy(alpha = 0.12f), shape = RoundedCornerShape(16.dp))
                                .border(1.dp, Color(0xFFFFD700).copy(alpha = 0.25f), shape = RoundedCornerShape(16.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.CloudDone,
                                    contentDescription = "Offline Ready",
                                    tint = Color(0xFFFFD700),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = viewModel.translate("অফলাইন"),
                                    fontSize = 11.sp,
                                    color = Color(0xFFFFD700),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Premium Rounded Search Bar
                    TextField(
                        value = searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        placeholder = { 
                            Text(
                                text = viewModel.translate("গল্প, লেখক বা বিভাগ খুঁজুন..."), 
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 13.sp
                            ) 
                        },
                        leadingIcon = { 
                            Icon(
                                imageVector = Icons.Default.Search, 
                                contentDescription = "Search", 
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(20.dp)
                            ) 
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear, 
                                        contentDescription = "Clear", 
                                        tint = Color.White.copy(alpha = 0.7f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White.copy(alpha = 0.08f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                            focusedIndicatorColor = Color(0xFFFFD700),
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(20.dp),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                            .testTag("home_search_bar")
                    )
                }
            }
        }

        // Continue Reading Section (Shows up dynamically if there's history)
        if (continueReadingStories.isNotEmpty() && searchQuery.isEmpty()) {
            item {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Text(
                        text = viewModel.translate("পড়া চালিয়ে যান (Continue Reading)"),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E3A8A),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(continueReadingStories) { story ->
                            Card(
                                onClick = { onStoryClick(story) },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier
                                    .width(280.dp)
                                    .padding(vertical = 4.dp)
                                    .testTag("continue_reading_card_${story.id}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Simulated cover book silhouette
                                    Box(
                                        modifier = Modifier
                                            .size(width = 50.dp, height = 70.dp)
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(Color(0xFF1A365D), Color(0xFF2A4365))
                                                ),
                                                shape = RoundedCornerShape(6.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("📖", fontSize = 24.sp)
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = story.title,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F2027),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "লেখক: ${story.author}",
                                            fontSize = 12.sp,
                                            color = Color.Gray,
                                            maxLines = 1
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        
                                        // Progress estimation based on index
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "পৃষ্ঠা: ${story.lastReadPage}",
                                                fontSize = 11.sp,
                                                color = Color(0xFFD4AF37),
                                                fontWeight = FontWeight.Medium
                                            )
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "Read",
                                                tint = Color(0xFFFFD700),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        LinearProgressIndicator(
                                            progress = { 0.5f }, // Mock progress bar
                                            color = Color(0xFFFFD700),
                                            trackColor = Color(0xFFE2E8F0),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(4.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Category Filter Row
        item {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                Text(
                    text = viewModel.translate("গল্পের বিভাগসমূহ"),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E3A8A),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { category ->
                        val isSelected = selectedCategory == category
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = category },
                            label = { Text(viewModel.translate(category)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF1C3A4B),
                                selectedLabelColor = Color(0xFFFFD700),
                                containerColor = Color.White,
                                labelColor = Color(0xFF1C3A4B)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.testTag("category_chip_$category")
                        )
                    }
                }
            }
        }

        // Stories Section Title
        item {
            val storiesTitle = if (searchQuery.isNotEmpty()) {
                "${viewModel.translate("অনুসন্ধানের ফলাফল")} (${finalStories.size})"
            } else {
                viewModel.translate("সর্বশেষ গল্পসমূহ")
            }
            Text(
                text = storiesTitle,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E3A8A),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp)
            )
        }

        // Empty State
        if (finalStories.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🔍", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = viewModel.translate("কোনো গল্প পাওয়া যায়নি!"),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Gray
                        )
                        Text(
                            text = viewModel.translate("অন্য কোনো কিওয়ার্ড দিয়ে চেষ্টা করুন।"),
                            fontSize = 13.sp,
                            color = Color.Gray.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // List of stories
        items(finalStories, key = { it.id }) { story ->
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
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("story_card_${story.id}")
            )
        }
    }
}

@Composable
fun StoryCard(
    story: StoryEntity,
    onCardClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onSpeakClick: () -> Unit,
    isSpeaking: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
            .clickable(onClick = onCardClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // Highly polished book cover representation
            val coverGradient = when (story.category) {
                "শিক্ষা ও নৈতিকতা" -> Brush.verticalGradient(listOf(Color(0xFF0B3C2A), Color(0xFF1E6F50)))
                "অনুপ্রেরণামূলক" -> Brush.verticalGradient(listOf(Color(0xFF1E1B4B), Color(0xFF4338CA)))
                else -> Brush.verticalGradient(listOf(Color(0xFF4C0519), Color(0xFF9F1239)))
            }

            Box(
                modifier = Modifier
                    .size(width = 85.dp, height = 125.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(coverGradient)
                    .padding(vertical = 8.dp, horizontal = 6.dp)
            ) {
                // Book Spine Visual Line
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(4.dp)
                        .background(Color.White.copy(alpha = 0.15f))
                        .align(Alignment.CenterStart)
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier
                            .size(14.dp)
                            .align(Alignment.End)
                    )
                    
                    Text(
                        text = story.title,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 10.dp, end = 4.dp)
                    )
                    
                    Text(
                        text = "Md Medul",
                        fontSize = 7.sp,
                        color = Color(0xFFFFD700),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Modern Category Badge with dynamic pill colors
                    val badgeBg = when (story.category) {
                        "শিক্ষা ও নৈতিকতা" -> Color(0xFF10B981).copy(alpha = 0.08f)
                        "অনুপ্রেরণামূলক" -> Color(0xFF6366F1).copy(alpha = 0.08f)
                        else -> Color(0xFFF43F5E).copy(alpha = 0.08f)
                    }
                    val badgeText = when (story.category) {
                        "শিক্ষা ও নৈতিকতা" -> Color(0xFF047857)
                        "অনুপ্রেরণামূলক" -> Color(0xFF4338CA)
                        else -> Color(0xFFBE123C)
                    }

                    Box(
                        modifier = Modifier
                            .background(badgeBg, shape = RoundedCornerShape(8.dp))
                            .border(1.dp, badgeText.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = story.category,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeText
                        )
                    }

                    // Action buttons with beautiful circles
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onFavoriteToggle,
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(0xFFF8FAFC), shape = CircleShape)
                        ) {
                            Icon(
                                imageVector = if (story.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (story.isFavorite) Color(0xFFEF4444) else Color(0xFF64748B),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(6.dp))
                        
                        IconButton(
                            onClick = onSpeakClick,
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    if (isSpeaking) Color(0xFFFFD700).copy(alpha = 0.15f) else Color(0xFFF8FAFC), 
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = if (isSpeaking) Icons.Filled.VolumeUp else Icons.Outlined.VolumeUp,
                                contentDescription = "Listen Aloud",
                                tint = if (isSpeaking) Color(0xFFB45309) else Color(0xFF64748B),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = story.title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    letterSpacing = 0.25.sp
                )

                Text(
                    text = "লিখেছেন: ${story.author}",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 1.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = story.description,
                    fontSize = 12.sp,
                    color = Color(0xFF475569),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Small offline badge
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = "Offline Saved",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "অফলাইন",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                        )
                    }

                    // Modern Play Button
                    Button(
                        onClick = onCardClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1E293B),
                            contentColor = Color(0xFFFFD700)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "পড়া শুরু করুন",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
