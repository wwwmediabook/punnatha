package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.PunnotarGolpoViewModel
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: PunnotarGolpoViewModel = viewModel()
                val isSplashFinished by viewModel.isSplashFinished.collectAsState()

                if (!isSplashFinished) {
                    SplashScreen()
                } else {
                    MainAppContent(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: PunnotarGolpoViewModel) {
    var currentTab by remember { mutableStateOf("stories") }
    val selectedStory by viewModel.selectedStory.collectAsState()
    val allStories by viewModel.allStories.collectAsState()

    // Simulated Push Notification state
    var activeNotificationTitle by remember { mutableStateOf<String?>(null) }
    var activeNotificationBody by remember { mutableStateOf<String?>(null) }
    var activeNotificationIsStory by remember { mutableStateOf(true) }
    var activeNotificationItemId by remember { mutableStateOf(0) }

    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()

    // Dynamic Runtime Notification Permission Launcher
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.setNotificationsEnabled(isGranted)
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        viewModel.newContentNotification.collect { payload ->
            if (notificationsEnabled) {
                activeNotificationTitle = payload.title
                activeNotificationBody = payload.body
                activeNotificationIsStory = payload.isStory
                activeNotificationItemId = payload.itemId
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (selectedStory != null) {
            // Immersive Book Reader overlay
            StoryReaderScreen(
                viewModel = viewModel,
                onBackClick = { viewModel.selectStory(null) },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Main Standard Navigation Layout
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                bottomBar = {
                    NavigationBar(
                        containerColor = Color.White,
                        tonalElevation = 8.dp,
                        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        // Tab 1: Stories
                        NavigationBarItem(
                            selected = currentTab == "stories",
                            onClick = { currentTab = "stories" },
                            icon = { Icon(imageVector = if (currentTab == "stories") Icons.Filled.MenuBook else Icons.Outlined.MenuBook, contentDescription = viewModel.translate("গল্প")) },
                            label = { Text(viewModel.translate("গল্প"), fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF1C3A4B),
                                selectedTextColor = Color(0xFF1C3A4B),
                                indicatorColor = Color(0xFFFFD700).copy(alpha = 0.3f),
                                unselectedIconColor = Color.Gray,
                                unselectedTextColor = Color.Gray
                            ),
                            modifier = Modifier.testTag("nav_item_stories")
                        )

                        // Tab 2: Rhyme (Chondo)
                        NavigationBarItem(
                            selected = currentTab == "chondo",
                            onClick = { currentTab = "chondo" },
                            icon = { Icon(imageVector = if (currentTab == "chondo") Icons.Filled.MusicNote else Icons.Outlined.MusicNote, contentDescription = viewModel.translate("ছন্দ")) },
                            label = { Text(viewModel.translate("ছন্দ"), fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF1C3A4B),
                                selectedTextColor = Color(0xFF1C3A4B),
                                indicatorColor = Color(0xFFFFD700).copy(alpha = 0.3f),
                                unselectedIconColor = Color.Gray,
                                unselectedTextColor = Color.Gray
                            ),
                            modifier = Modifier.testTag("nav_item_chondo")
                        )

                        // Tab 3: Favorites (Library)
                        NavigationBarItem(
                            selected = currentTab == "favorites",
                            onClick = { currentTab = "favorites" },
                            icon = { Icon(imageVector = if (currentTab == "favorites") Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, contentDescription = viewModel.translate("লাইব্রেরি")) },
                            label = { Text(viewModel.translate("লাইব্রেরি"), fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF1C3A4B),
                                selectedTextColor = Color(0xFF1C3A4B),
                                indicatorColor = Color(0xFFFFD700).copy(alpha = 0.3f),
                                unselectedIconColor = Color.Gray,
                                unselectedTextColor = Color.Gray
                            ),
                            modifier = Modifier.testTag("nav_item_favorites")
                        )

                        // Tab 4: Settings
                        NavigationBarItem(
                            selected = currentTab == "settings",
                            onClick = { currentTab = "settings" },
                            icon = { Icon(imageVector = if (currentTab == "settings") Icons.Filled.Settings else Icons.Outlined.Settings, contentDescription = viewModel.translate("সেটিংস")) },
                            label = { Text(viewModel.translate("সেটিংস"), fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF1C3A4B),
                                selectedTextColor = Color(0xFF1C3A4B),
                                indicatorColor = Color(0xFFFFD700).copy(alpha = 0.3f),
                                unselectedIconColor = Color.Gray,
                                unselectedTextColor = Color.Gray
                            ),
                            modifier = Modifier.testTag("nav_item_settings")
                        )
                    }
                }
            ) { innerPadding ->
                val isOnline by viewModel.isOnline.collectAsState()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    ConnectivityStatusBanner(isOnline = isOnline, viewModel = viewModel)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        when (currentTab) {
                            "stories" -> HomeScreen(
                                viewModel = viewModel,
                                onStoryClick = { viewModel.selectStory(it) }
                            )
                            "chondo" -> ChondoScreen(
                                viewModel = viewModel
                            )
                            "favorites" -> FavoritesScreen(
                                viewModel = viewModel,
                                onStoryClick = { viewModel.selectStory(it) }
                            )
                            "settings" -> SettingsScreen(
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }
        }

        // Animated Simulated Floating Push Notification Banner
        AnimatedVisibility(
            visible = activeNotificationTitle != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(start = 16.dp, end = 16.dp, top = 48.dp)
                .zIndex(10f)
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2027)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        // Handle clicking of notification to open content directly
                        if (activeNotificationIsStory) {
                            val storyFound = allStories.find { it.id == activeNotificationItemId }
                            if (storyFound != null) {
                                viewModel.selectStory(storyFound)
                            } else {
                                if (allStories.isNotEmpty()) {
                                    viewModel.selectStory(allStories.last())
                                }
                            }
                        } else {
                            currentTab = "chondo"
                        }
                        // Dismiss notification
                        activeNotificationTitle = null
                        activeNotificationBody = null
                    }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFF1C3A4B), shape = RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (activeNotificationIsStory) "📖" else "✨", fontSize = 20.sp)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = activeNotificationTitle ?: "",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD700)
                        )
                        Text(
                            text = activeNotificationBody ?: "",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.9f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            activeNotificationTitle = null
                            activeNotificationBody = null
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close Notification", tint = Color.White.copy(alpha = 0.6f))
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectivityStatusBanner(isOnline: Boolean, viewModel: PunnotarGolpoViewModel) {
    var showOnlineBanner by remember { mutableStateOf(false) }

    LaunchedEffect(isOnline) {
        if (isOnline) {
            showOnlineBanner = true
            delay(4000) // Show for 4 seconds when coming back online, then hide
            showOnlineBanner = false
        } else {
            showOnlineBanner = false
        }
    }

    AnimatedVisibility(
        visible = !isOnline || showOnlineBanner,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        val bgColor = if (isOnline) Color(0xFFE6F4EA) else Color(0xFFFCE8E6)
        val textColor = if (isOnline) Color(0xFF137333) else Color(0xFFC5221F)
        val icon = if (isOnline) Icons.Default.Wifi else Icons.Default.WifiOff
        val text = if (isOnline) {
            viewModel.translate("অনলাইন (নতুন গল্প ও ছন্দ স্বয়ংক্রিয়ভাবে লোড হচ্ছে)")
        } else {
            viewModel.translate("অফলাইন (সংরক্ষিত গল্প ও ছন্দ পড়তে পারবেন)")
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(bgColor)
                .padding(vertical = 6.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                color = textColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

