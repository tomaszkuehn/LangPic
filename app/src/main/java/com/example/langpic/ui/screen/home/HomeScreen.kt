package com.example.langpic.ui.screen.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.langpic.ui.theme.Blue200
import com.example.langpic.ui.theme.Blue400
import com.example.langpic.ui.theme.CreamBg
import com.example.langpic.ui.theme.CreamBgDark
import com.example.langpic.ui.theme.DarkText
import com.example.langpic.ui.theme.GradientPlayful
import com.example.langpic.ui.theme.MediumText
import com.example.langpic.ui.theme.Orange100
import com.example.langpic.ui.theme.Orange500
import com.example.langpic.ui.theme.Pink200
import com.example.langpic.ui.theme.Pink400
import com.example.langpic.ui.theme.Purple200
import com.example.langpic.ui.theme.Purple400
import com.example.langpic.ui.theme.StaggeredEntrance
import com.example.langpic.ui.theme.SuccessGreen
import com.example.langpic.ui.theme.SurfaceWhite
import com.example.langpic.ui.theme.Teal200
import com.example.langpic.ui.theme.Teal400
import com.example.langpic.ui.theme.rememberBounceScale

@Composable
fun HomeScreen(
    onPlay: () -> Unit,
    onImport: () -> Unit,
    onManage: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(CreamBg, SurfaceWhite, CreamBgDark.copy(alpha = 0.3f))
                )
            ),
    ) {
        // ---- Decorative background circles ----
        Box(
            modifier = Modifier
                .offset(x = (-60).dp, y = (-40).dp)
                .size(200.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Orange100, Orange500.copy(alpha = 0.15f)))),
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 30.dp, y = 60.dp)
                .size(140.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Teal200, Teal400.copy(alpha = 0.12f)))),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-30).dp, y = 30.dp)
                .size(160.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Purple200, Purple400.copy(alpha = 0.1f)))),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 20.dp, y = (-50).dp)
                .size(100.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Pink200, Pink400.copy(alpha = 0.1f)))),
        )

        // ---- Main content ----
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // ---- Animated logo area ----
            StaggeredEntrance(delayMs = 0) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Decorative gradient icon
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(GradientPlayful)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("🌍", fontSize = 44.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // App title with gradient text effect
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brush.linearGradient(GradientPlayful))
                            .padding(horizontal = 24.dp, vertical = 10.dp),
                    ) {
                        Text(
                            text = "LangPic",
                            fontSize = 44.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            letterSpacing = 1.sp,
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Learn words • See pictures • Have fun!",
                        fontSize = 16.sp,
                        color = MediumText,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // ---- Action cards with staggered entrance ----
            StaggeredEntrance(delayMs = 150) {
                ActionCard(
                    emoji = "🎮",
                    title = "Learn",
                    subtitle = "Start playing with words",
                    gradientColors = listOf(Orange500, Color(0xFFFF7043)),
                    onClick = onPlay,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            StaggeredEntrance(delayMs = 300) {
                ActionCard(
                    emoji = "📦",
                    title = "Import Pack",
                    subtitle = "Add new lesson packs from ZIP",
                    gradientColors = listOf(Teal400, Blue400),
                    onClick = onImport,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            StaggeredEntrance(delayMs = 450) {
                ActionCard(
                    emoji = "⚙️",
                    title = "Manage Packs",
                    subtitle = "Enable, disable or remove packs",
                    gradientColors = listOf(Purple400, Pink400),
                    onClick = onManage,
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // ---- Mascot character ----
            StaggeredEntrance(delayMs = 600) {
                val bounce = rememberBounceScale(scaleTo = 1.06f, durationMs = 1000)
                Text(
                    text = "🐱",
                    fontSize = 42.sp,
                    modifier = Modifier.scale(bounce),
                )
            }
        }
    }
}

// ---- Reusable action card ----

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionCard(
    emoji: String,
    title: String,
    subtitle: String,
    gradientColors: List<Color>,
    onClick: () -> Unit,
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = tween(150, easing = FastOutSlowInEasing),
        label = "pressScale",
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp, pressedElevation = 1.dp),
        onClick = {
            pressed = true
            onClick()
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Gradient emoji circle
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(gradientColors)),
                contentAlignment = Alignment.Center,
            ) {
                Text(emoji, fontSize = 30.sp)
            }

            Spacer(modifier = Modifier.width(18.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkText,
                )
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = MediumText,
                    fontWeight = FontWeight.Normal,
                )
            }

            // Arrow indicator
            Text("→", fontSize = 24.sp, color = MediumText.copy(alpha = 0.4f))
        }
    }
}
