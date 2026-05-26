package com.example.langpic.ui.screen.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.langpic.data.database.AppDatabase
import com.example.langpic.service.TtsHelper
import com.example.langpic.ui.theme.ErrorRed
import com.example.langpic.ui.theme.SuccessGreen
import com.example.langpic.ui.theme.Teal400
import kotlinx.coroutines.delay
import java.io.File

private const val HINT_DELAY_MS = 5000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    viewModel: GameViewModel,
    ttsHelper: TtsHelper,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showHint by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val db = AppDatabase.getInstance(context)
        val items = db.lessonItemDao().getEnabledItems().map {
            com.example.langpic.domain.model.LessonItem(
                id = it.id,
                packId = it.packId,
                word = it.word,
                language = it.language,
                correctImagePath = it.correctImagePath,
                wrongImagePath = it.wrongImagePath,
            )
        }
        val resolved = items.map { item ->
            val pack = db.lessonPackDao().getById(item.packId)
            val basePath = pack?.extractedPath ?: ""
            item.copy(
                correctImagePath = File(basePath, item.correctImagePath).absolutePath,
                wrongImagePath = File(basePath, item.wrongImagePath).absolutePath,
            )
        }
        viewModel.loadItems(resolved)
    }

    LaunchedEffect(state.currentItem) {
        showHint = false
        val item = state.currentItem ?: return@LaunchedEffect
        ttsHelper.speak(item.word, item.language)
    }

    LaunchedEffect(state.currentItem, state.feedback) {
        if (state.feedback != Feedback.NONE || state.currentItem == null) {
            showHint = false
            return@LaunchedEffect
        }
        showHint = false
        delay(HINT_DELAY_MS)
        if (state.feedback == Feedback.NONE && state.currentItem != null) {
            showHint = true
        }
    }

    LaunchedEffect(state.feedback) {
        if (state.feedback != Feedback.NONE) {
            delay(2000)
            viewModel.nextRound()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("⭐ ${state.score} / ${state.totalItems}", fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.hasContent && !state.isFinished) {
                        Text(
                            "Left: ${state.remainingCount}",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.Filled.Home,
                                contentDescription = "Stop and return home",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        when {
            !state.hasContent && !state.isFinished -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No lesson items available. Import a lesson pack first.", fontSize = 18.sp)
                }
            }

            state.isFinished -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text("All Done!", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("You got ${state.score} out of ${state.totalItems}!", fontSize = 24.sp)
                    if (state.totalAttempts > state.score) {
                        Text("Took ${state.totalAttempts} tries", fontSize = 16.sp, color = Color.Gray)
                    }
                    if (state.skippedCount > 0) {
                        Text("Skipped: ${state.skippedCount}", fontSize = 16.sp, color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = { viewModel.reset() },
                        modifier = Modifier.fillMaxWidth().height(64.dp),
                        shape = RoundedCornerShape(20.dp),
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Play Again", fontSize = 22.sp)
                    }
                }
            }

            else -> {
                val item = state.currentItem!!
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.word,
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.size(12.dp))
                        IconButton(
                            onClick = { ttsHelper.speak(item.word, item.language) },
                            modifier = Modifier.size(48.dp).background(Teal400, CircleShape),
                        ) {
                            Icon(
                                Icons.Filled.VolumeUp,
                                contentDescription = "Speak word",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        val (firstPath, secondPath) = state.shuffledImagePaths
                        ImageCard(
                            imagePath = firstPath, index = 0,
                            feedback = state.feedback, correctIndex = state.correctIndex,
                            onClick = { viewModel.selectImage(0) },
                            modifier = Modifier.weight(1f),
                        )
                        ImageCard(
                            imagePath = secondPath, index = 1,
                            feedback = state.feedback, correctIndex = state.correctIndex,
                            onClick = { viewModel.selectImage(1) },
                            modifier = Modifier.weight(1f),
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (state.feedback == Feedback.NONE) {
                        OutlinedButton(
                            onClick = { viewModel.skipQuestion() },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Icon(Icons.Filled.SkipNext, contentDescription = null, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("Skip", fontSize = 18.sp)
                        }
                    }

                    when (state.feedback) {
                        Feedback.CORRECT -> FeedbackBadge("Well done!", SuccessGreen)
                        Feedback.INCORRECT -> FeedbackBadge("Wrong! Look at the green one.", ErrorRed)
                        Feedback.SKIPPED -> FeedbackBadge("Skipped — will come back later!", Color.Gray)
                        Feedback.NONE -> {}
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    HelperCharacter(
                        showHint = showHint,
                        correctIndex = state.correctIndex,
                        feedback = state.feedback,
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun FeedbackBadge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .padding(top = 12.dp)
            .background(color, RoundedCornerShape(16.dp))
            .padding(horizontal = 24.dp, vertical = 10.dp),
    ) {
        Text(text, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
private fun HelperCharacter(
    showHint: Boolean,
    correctIndex: Int,
    feedback: Feedback,
) {
    val transition = rememberInfiniteTransition(label = "bounce")
    val bounce by transition.animateFloat(
        initialValue = 0f,
        targetValue = -8f,
        animationSpec = infiniteRepeatable(tween(600), repeatMode = RepeatMode.Reverse),
        label = "bounceY",
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        AnimatedVisibility(visible = showHint && feedback == Feedback.NONE) {
            Box(
                modifier = Modifier
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                    .padding(12.dp),
            ) {
                val direction = if (correctIndex == 0) "left" else "right"
                val arrow = if (correctIndex == 0) "👈" else "👉"
                Text(
                    "Want some help?\nI think it's the $direction one! $arrow",
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Text(
            text = if (showHint && feedback == Feedback.NONE) "🤔" else "😊",
            fontSize = 48.sp,
            modifier = Modifier.offset(y = bounce.dp),
        )

        if (feedback == Feedback.CORRECT) {
            Text("🎉", fontSize = 36.sp)
        } else if (feedback == Feedback.INCORRECT) {
            Text("😢", fontSize = 36.sp)
        }
    }
}

@Composable
private fun ImageCard(
    imagePath: String,
    index: Int,
    feedback: Feedback,
    correctIndex: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isCorrect = index == correctIndex
    val borderColor = when (feedback) {
        Feedback.CORRECT -> if (isCorrect) SuccessGreen else Color.Transparent
        Feedback.INCORRECT -> if (isCorrect) SuccessGreen else ErrorRed
        Feedback.SKIPPED -> if (isCorrect) SuccessGreen else Color.Transparent
        Feedback.NONE -> MaterialTheme.colorScheme.primary
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(20.dp))
            .border(4.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable(enabled = feedback == Feedback.NONE) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(File(imagePath))
                .crossfade(true)
                .build(),
            contentDescription = "Image choice",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
