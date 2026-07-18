package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.ChondoEntity
import com.example.ui.viewmodel.PunnotarGolpoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChondoScreen(
    viewModel: PunnotarGolpoViewModel,
    modifier: Modifier = Modifier
) {
    val chondos by viewModel.allChondos.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()
    var searchChondoQuery by remember { mutableStateOf("") }

    // TTS states
    val isTtsSpeaking by viewModel.isTtsSpeaking.collectAsState()
    val currentTtsItemId by viewModel.currentTtsItemId.collectAsState()
    val currentTtsItemType by viewModel.currentTtsItemType.collectAsState()

    val filteredChondos = remember(chondos, searchChondoQuery) {
        if (searchChondoQuery.isBlank()) {
            chondos
        } else {
            chondos.filter {
                it.title.contains(searchChondoQuery, ignoreCase = true) ||
                it.author.contains(searchChondoQuery, ignoreCase = true) ||
                it.content.contains(searchChondoQuery, ignoreCase = true)
            }
        }
    }

    // Gradients list for cards as requested ("Gradient Background")
    val cardGradients = listOf(
        Brush.linearGradient(listOf(Color(0xFF8A2387), Color(0xFFE94057), Color(0xFFF27121))), // Sunset Gold
        Brush.linearGradient(listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))), // Serene Blue
        Brush.linearGradient(listOf(Color(0xFF11998e), Color(0xFF38ef7d))), // Green Mint
        Brush.linearGradient(listOf(Color(0xFF4e54c8), Color(0xFF8f94fb))), // Purple Lavender
        Brush.linearGradient(listOf(Color(0xFFfc4a1a), Color(0xFFf7b733)))  // Warm Tangerine
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFAF9F6)) // Warm off-white background
            .testTag("chondo_screen_lazy_list"),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        // Aesthetic header card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF1E3A8A), Color(0xFF111827))
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 24.dp)
            ) {
                Column {
                    Text(
                        text = viewModel.translate("সুর ও তাল"),
                        fontSize = 14.sp,
                        color = Color(0xFFFFD700)
                    )
                    Text(
                        text = viewModel.translate("ছন্দের খেলা"),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = viewModel.translate("মনকে দোলা দেবে যে মধুর ছন্দমালা।"),
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Chondo Search Box
                    TextField(
                        value = searchChondoQuery,
                        onValueChange = { searchChondoQuery = it },
                        placeholder = { Text(viewModel.translate("ছন্দ বা কবি খুঁজুন..."), color = Color.White.copy(alpha = 0.5f)) },
                        leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = Color(0xFFFFD700)) },
                        trailingIcon = {
                            if (searchChondoQuery.isNotEmpty()) {
                                IconButton(onClick = { searchChondoQuery = "" }) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear", tint = Color.White)
                                }
                            }
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White.copy(alpha = 0.15f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.12f),
                            focusedIndicatorColor = Color(0xFFFFD700),
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("chondo_search_bar")
                    )
                }
            }
        }

        // Section Title
        item {
            val sectionTitle = if (searchChondoQuery.isNotEmpty()) {
                "${viewModel.translate("অনুসন্ধানের ফলাফল")} (${filteredChondos.size})"
            } else {
                viewModel.translate("সব ছন্দমালা")
            }
            Text(
                text = sectionTitle,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E3A8A),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 12.dp)
            )
        }

        // Empty state
        if (filteredChondos.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("✨", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "কোনো ছন্দ পাওয়া যায়নি!",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Gray
                        )
                    }
                }
            }
        }

        // List of Chondos
        items(filteredChondos, key = { it.id }) { chondo ->
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
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("chondo_card_${chondo.id}")
            )
        }
    }
}

@Composable
fun ChondoCard(
    chondo: ChondoEntity,
    gradient: Brush,
    onFavoriteToggle: () -> Unit,
    onBookmarkToggle: () -> Unit,
    onSpeakClick: () -> Unit,
    isSpeaking: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .background(gradient)
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top header details (Title and Author)
                Text(
                    text = "“ ${chondo.title} ”",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = FontFamily.Serif,
                    textAlign = TextAlign.Center
                )
                
                Text(
                    text = "— ${chondo.author}",
                    fontSize = 12.sp,
                    fontStyle = FontStyle.Italic,
                    color = Color(0xFFFFD700), // Gold
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                // Large readable rhyme content
                Text(
                    text = chondo.content,
                    fontSize = 17.sp,
                    lineHeight = 28.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Normal,
                    fontFamily = FontFamily.Serif,
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))

                Divider(color = Color.White.copy(alpha = 0.2f), thickness = 0.8.dp)

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left action indicator: Offline Save / Date Info
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = "Saved Offline",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "সংরক্ষিত",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }

                    // Right action buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Bookmark action
                        IconButton(onClick = onBookmarkToggle) {
                            Icon(
                                imageVector = if (chondo.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = "Bookmark",
                                tint = if (chondo.isBookmarked) Color(0xFFFFD700) else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Favorite action
                        IconButton(onClick = onFavoriteToggle) {
                            Icon(
                                imageVector = if (chondo.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (chondo.isFavorite) Color.Red else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Speaker / TTS Audio action
                        IconButton(onClick = onSpeakClick) {
                            Icon(
                                imageVector = if (isSpeaking) Icons.Filled.VolumeUp else Icons.Outlined.VolumeUp,
                                contentDescription = "Listen Aloud",
                                tint = if (isSpeaking) Color(0xFFFFD700) else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Share action via Android Send Intent
                        IconButton(onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, chondo.title)
                                putExtra(Intent.EXTRA_TEXT, "“${chondo.title}”\n— ${chondo.author}\n\n${chondo.content}\n\nশেয়ার করা হয়েছে 'পুণ্যতার গল্প' অ্যাপ থেকে।")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "শেয়ার করুন"))
                        }) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Copy action (copies whole rhyme with 1 click)
                        IconButton(onClick = {
                            clipboardManager.setText(AnnotatedString("${chondo.title}\n— ${chondo.author}\n\n${chondo.content}"))
                            Toast.makeText(context, "ছন্দ কপি করা হয়েছে!", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy text",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
