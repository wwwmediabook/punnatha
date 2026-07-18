package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SplashScreen(modifier: Modifier = Modifier) {
    // Animation states
    val scale = remember { Animatable(0.5f) }
    val alpha = remember { Animatable(0f) }
    
    // Pulse animation for background glow
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowScale"
    )

    LaunchedEffect(Unit) {
        // Animate logo scale and alpha simultaneously
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(1200, easing = LinearOutSlowInEasing)
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F2027), // Deep space blue
                        Color(0xFF1C3A4B), // Calm ocean blue
                        Color(0xFF0D1B2A)  // Rich dark navy
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Glowing Ambient Background lights (Soft Blue & Golden theme)
        Box(
            modifier = Modifier
                .size(300.dp)
                .scale(glowScale)
                .alpha(0.15f)
                .blur(60.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFD700), // Gold
                            Color(0xFF00B4D8), // Soft Blue
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(24.dp)
                .alpha(alpha.value)
                .scale(scale.value)
        ) {
            // Book & Feather Stylized Logo Container
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(160.dp)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF1E3A8A).copy(alpha = 0.4f), // Royal Blue
                                Color(0xFFD4AF37).copy(alpha = 0.2f)  // Honey Gold
                            )
                        ),
                        shape = CircleShape
                    )
                    .padding(8.dp)
            ) {
                // Glow effect inside logo
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(0.4f)
                        .blur(10.dp)
                        .background(Color(0xFFFFD700), shape = CircleShape)
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Open Book Emoji
                    Text(
                        text = "📖",
                        fontSize = 56.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    // Glowing feather/star
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("✨", fontSize = 24.sp)
                        Text("🪶", fontSize = 28.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // App title in stylish Bengali
            Text(
                text = "পুণ্যতার গল্প",
                fontSize = 38.sp,
                color = Color(0xFFFFD700), // Bright Gold
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Catchy slogan / subtext
            Text(
                text = "গল্পে পবিত্রতা, ছنده অনুভূতি।",
                fontSize = 16.sp,
                color = Color(0xFF90E0EF), // Beautiful soft blue accent
                fontWeight = FontWeight.Normal,
                fontFamily = FontFamily.SansSerif,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Smooth loader
            CircularProgressIndicator(
                color = Color(0xFFFFD700),
                strokeWidth = 3.dp,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.height(100.dp))

            // Developer label
            Text(
                text = "Developed by",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.4f),
                fontWeight = FontWeight.Light,
                letterSpacing = 2.sp
            )
            
            Text(
                text = "Md Medul",
                fontSize = 16.sp,
                color = Color(0xFFFFD700).copy(alpha = 0.8f),
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
