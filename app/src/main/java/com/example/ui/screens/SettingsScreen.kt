package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.PunnotarGolpoViewModel

@Composable
fun SettingsScreen(
    viewModel: PunnotarGolpoViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // ViewModel configurations
    val fontSize by viewModel.fontSize.collectAsState()
    val lineHeightMultiplier by viewModel.lineHeightMultiplier.collectAsState()
    val fontStyleSelected by viewModel.fontStyle.collectAsState()
    val readerThemeSelected by viewModel.readerTheme.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()

    // Local configuration states
    var showDeveloperInfoDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF4F6F9))
            .verticalScroll(scrollState)
            .testTag("settings_screen_root"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Aesthetic upper banner
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
                    text = viewModel.translate("অ্যাপ্লিকেশন কনফিগারেশন"),
                    fontSize = 13.sp,
                    color = Color(0xFFFFD700)
                )
                Text(
                    text = viewModel.translate("সেটিংস ও তথ্য"),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- GLOBAL APPERANCE CONFIGURATIONS CARD ---
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Palette, contentDescription = null, tint = Color(0xFF1C3A4B))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = viewModel.translate("পঠন শৈলী কনফিগারেশন"),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1C3A4B)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                // Font Size Adjuster
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "${viewModel.translate("ফন্ট সাইজ")} (${fontSize.toInt()} sp)", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { viewModel.setFontSize(fontSize - 1f) },
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(0xFFF1F5F9), shape = CircleShape)
                        ) {
                            Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease", tint = Color.Black, modifier = Modifier.size(16.dp))
                        }
                        IconButton(
                            onClick = { viewModel.setFontSize(fontSize + 1f) },
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(0xFFF1F5F9), shape = CircleShape)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Increase", tint = Color.Black, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Line Height Adjuster
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "${viewModel.translate("লাইনের দূরত্ব")} (${String.format("%.1f", lineHeightMultiplier)}x)", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { viewModel.setLineHeight(lineHeightMultiplier - 0.1f) },
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(0xFFF1F5F9), shape = CircleShape)
                        ) {
                            Icon(imageVector = Icons.Default.ArrowDownward, contentDescription = "Decrease", tint = Color.Black, modifier = Modifier.size(14.dp))
                        }
                        IconButton(
                            onClick = { viewModel.setLineHeight(lineHeightMultiplier + 0.1f) },
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(0xFFF1F5F9), shape = CircleShape)
                        ) {
                            Icon(imageVector = Icons.Default.ArrowUpward, contentDescription = "Increase", tint = Color.Black, modifier = Modifier.size(14.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Font Style selection
                Text(text = viewModel.translate("ফন্ট ফ্যামিলি"), fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf("SansSerif", "Serif", "Monospace").forEach { f ->
                        val isSelected = fontStyleSelected == f
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFF1C3A4B) else Color(0xFFF1F5F9))
                                .clickable { viewModel.setFontStyle(f) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = f,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color(0xFFFFD700) else Color.DarkGray
                            )
                        }
                    }
                }
            }
        }

        // --- APP OPTIONS CONFIGURATIONS CARD ---
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Settings, contentDescription = null, tint = Color(0xFF1C3A4B))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = viewModel.translate("সিস্টেম সেটিংস"),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1C3A4B)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Push notifications toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = viewModel.translate("নতুন গল্প নোটিফিকেশন"), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text(text = viewModel.translate("গল্প বা ছন্দ আপলোড হলে সাথে সাথে পাবেন"), fontSize = 11.sp, color = Color.Gray)
                    }
                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = { viewModel.setNotificationsEnabled(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFFD700), checkedTrackColor = Color(0xFF1C3A4B))
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // App Language Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = viewModel.translate("অ্যাপের ভাষা"), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text(text = viewModel.translate("বাংলা অথবা ইংরেজি ভাষা নির্বাচন করুন"), fontSize = 11.sp, color = Color.Gray)
                    }
                    Row(
                        modifier = Modifier
                            .background(Color(0xFFF1F5F9), shape = RoundedCornerShape(8.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        listOf("বাংলা", "English").forEach { lang ->
                            val isSel = appLanguage == lang
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) Color(0xFF1C3A4B) else Color.Transparent)
                                    .clickable { viewModel.setAppLanguage(lang) }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = lang,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) Color(0xFFFFD700) else Color.DarkGray
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Divider(color = Color(0xFFF1F5F9))

                Spacer(modifier = Modifier.height(12.dp))

                // Clear Cache Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.clearCache()
                            Toast.makeText(context, viewModel.translate("সকল ক্যাশ ও পড়ার ইতিহাস মুছে ফেলা হয়েছে!"), Toast.LENGTH_LONG).show()
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Clear Cache", tint = Color.Red.copy(alpha = 0.8f))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = viewModel.translate("ক্যাশ পরিষ্কার করুন (Clear Cache)"), fontSize = 13.sp, color = Color.Red.copy(alpha = 0.8f), fontWeight = FontWeight.SemiBold)
                        Text(text = viewModel.translate("সকল পড়ার ইতিহাস, পছন্দ তালিকা ও বুকমার্ক রিজেক্ট করুন"), fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }
        }

        // --- DEVELOPER & APP DETAILS CARD ---
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Developer card info
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            Brush.linearGradient(listOf(Color(0xFF1E3A8A), Color(0xFF0F2027))),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("📖", fontSize = 28.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = viewModel.translate("পুণ্যতার গল্প"),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1C3A4B)
                )

                Text(
                    text = viewModel.translate("স্লোগান: \"গল্পে পবিত্রতা, ছنده অনুভূতি।\""),
                    fontSize = 12.sp,
                    color = Color.Gray,
                    fontStyle = FontStyle.Italic,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Divider(color = Color(0xFFF1F5F9))

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = viewModel.translate("ডেভেলপার (Developer)"), fontSize = 13.sp, color = Color.Gray)
                    Text(text = "Md Medul", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD700))
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = viewModel.translate("ভার্সন (Version)"), fontSize = 13.sp, color = Color.Gray)
                    Text(text = viewModel.translate("v1.0 (অফলাইন প্যাক)"), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1C3A4B))
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { showDeveloperInfoDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C3A4B), contentColor = Color(0xFFFFD700)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(viewModel.translate("ডেভেলপার সম্পর্কে আরও জানুন"))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Developer Info AlertDialog
        if (showDeveloperInfoDialog) {
            AlertDialog(
                onDismissRequest = { showDeveloperInfoDialog = false },
                title = { Text(viewModel.translate("ডেভেলপার প্রোফাইল"), fontWeight = FontWeight.Bold, color = Color(0xFF1C3A4B)) },
                text = {
                    Column {
                        Text(
                            text = viewModel.translate("মোঃ মেদুল (Md Medul)"),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F2027)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = viewModel.translate("একটি প্রিমিয়াম বাংলা বই পড়ার অভিজ্ঞতা তৈরি করাই ছিল এই অ্যাপ্লিকেশনের মূল লক্ষ্য। যেখানে বাস্তব পাতার অনুভূতির সাথে সাথে চমৎকার গল্প ও ছন্দের মেলবন্ধন ঘটেছে।"),
                            fontSize = 13.sp,
                            color = Color.DarkGray,
                            lineHeight = 18.sp
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showDeveloperInfoDialog = false }) {
                        Text(viewModel.translate("বন্ধ করুন"), color = Color(0xFF1C3A4B))
                    }
                }
            )
        }
    }
}
