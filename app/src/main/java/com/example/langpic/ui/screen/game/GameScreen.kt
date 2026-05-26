package com.example.langpic.ui.screen.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.example.langpic.domain.model.ImageChoice
import com.example.langpic.service.TtsHelper
import com.example.langpic.ui.theme.ErrorRed
import com.example.langpic.ui.theme.Orange500
import com.example.langpic.ui.theme.SuccessGreen
import com.example.langpic.ui.theme.Teal400
import kotlinx.coroutines.delay
import org.json.JSONArray
import java.io.File

private const val HINT_DELAY_MS = 8000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    viewModel: GameViewModel,
    ttsHelper: TtsHelper,
    repository: com.example.langpic.data.repository.LessonRepository,
    onBack: () -> Unit,
) {
    var testMode by remember { mutableStateOf(0) }
    var bestHighScore by remember { mutableStateOf(0f) }
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showHint by remember { mutableStateOf(false) }
    var hintIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(showHint, state.shuffledImages) {
        if (showHint && state.shuffledImages.isNotEmpty()) {
            val correct = state.shuffledImages.indices.filter { state.shuffledImages[it].isCorrect }
            hintIndex = if (correct.isNotEmpty()) correct.random() else null
        } else {
            hintIndex = null
        }
    }

    LaunchedEffect(Unit) {
        repository.getAllPacks().collect { list ->
            bestHighScore = list.filter { it.enabled }.maxOfOrNull { it.highScore } ?: 0f
        }
    }

    LaunchedEffect(Unit) {
        val db = AppDatabase.getInstance(context)
        val entities = db.lessonItemDao().getEnabledItems()
        val resolved = entities.map { entity ->
            val pack = db.lessonPackDao().getById(entity.packId)
            val basePath = pack?.extractedPath ?: ""
            val images = parseImages(entity.imagesJson).map { img ->
                img.copy(path = File(basePath, img.path).absolutePath)
            }
            com.example.langpic.domain.model.LessonItem(
                id = entity.id,
                packId = entity.packId,
                word1 = entity.word1,
                language1 = entity.language1,
                word2 = entity.word2,
                language2 = entity.language2,
                images = images,
            )
        }
        viewModel.loadItems(resolved)
    }

    LaunchedEffect(testMode) {
        viewModel.setTestingMode(testMode)
    }

    LaunchedEffect(state.currentItem) {
        showHint = false
        val item = state.currentItem ?: return@LaunchedEffect
        ttsHelper.speak(item.word1, item.language1)
    }

    LaunchedEffect(state.currentItem, state.feedback, state.selectedIndices, testMode) {
        if (state.feedback != Feedback.NONE || state.currentItem == null) {
            showHint = false
            viewModel.setHintActive(false)
            return@LaunchedEffect
        }
        showHint = false
        viewModel.setHintActive(false)
        delay(HINT_DELAY_MS)
        if (state.feedback == Feedback.NONE && state.currentItem != null) {
            showHint = true
            viewModel.setHintActive(true)
        }
    }

    LaunchedEffect(state.isFinished) {
        if (state.isFinished) {
            val packIds = repository.getEnabledPackIds()
            for (id in packIds) {
                repository.updateHighScore(id, state.score)
            }
        }
    }

    LaunchedEffect(state.feedback) {
        if (state.feedback != Feedback.NONE) {
            delay(3000)
            viewModel.nextRound()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⭐ ${"%.1f".format(state.score)} / ${"%.1f".format(state.maxPossibleScore)}", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(12.dp))
                        if (bestHighScore > 0f) {
                            Text("Best: ${"%.1f".format(bestHighScore)}", fontSize = 15.sp, color = Orange500, fontWeight = FontWeight.Bold)
                        }
                    }
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
                    Text("You scored ${"%.1f".format(state.score)}", fontSize = 24.sp)
                    Text("of ${"%.1f".format(state.maxPossibleScore)} max", fontSize = 16.sp, color = Color.Gray)
                    if (state.totalAttempts > 0) {
                        Text("Took ${state.totalAttempts} tries", fontSize = 16.sp, color = Color.Gray)
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
                    modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // ---- Word + speaker ----
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = item.word1,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        )
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                            IconButton(
                                onClick = { ttsHelper.speak(item.word1, item.language1) },
                                modifier = Modifier.size(44.dp).background(Teal400, CircleShape),
                            ) {
                                Icon(Icons.Filled.VolumeUp, contentDescription = "Speak", tint = Color.White, modifier = Modifier.size(26.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // ---- Image grid ----
                    if (state.shuffledImages.isNotEmpty()) {
                        ImageGrid(
                            images = state.shuffledImages,
                            selectedIndices = state.selectedIndices,
                            hintIndex = hintIndex,
                            feedback = state.feedback,
                            enabled = state.feedback == Feedback.NONE,
                            onTap = { viewModel.selectImage(it) },
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // ---- Word2 reveal ----
                    AnimatedVisibility(visible = state.showingAnswer && item.word2.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Orange500.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                                .border(2.dp, Orange500, RoundedCornerShape(16.dp))
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = item.word2,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Orange500,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }

                    // ---- Check button / self-assessment ----
                    if (state.feedback == Feedback.NONE) {
                        if (state.showSelfAssessment) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("How well did you remember?", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                SelfAssessButton(
                                    emoji = "😢", label = "not really",
                                    color = ErrorRed,
                                    onClick = { viewModel.selfAssess(0f) },
                                    modifier = Modifier.weight(1f),
                                )
                                SelfAssessButton(
                                    emoji = "😐", label = "sort of",
                                    color = Color(0xFFFFA726),
                                    onClick = { viewModel.selfAssess(0.2f) },
                                    modifier = Modifier.weight(1f),
                                )
                                SelfAssessButton(
                                    emoji = "😊", label = "yes!",
                                    color = SuccessGreen,
                                    onClick = {
                                        val pts = if (showHint) 0.4f else 0.8f
                                        viewModel.selfAssess(pts)
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        } else {
                            val canCheck = state.shuffledImages.isEmpty() || state.selectedIndices.isNotEmpty()
                            Button(
                                onClick = { viewModel.checkAnswer() },
                                enabled = canCheck,
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            ) {
                                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.size(6.dp))
                                Text("Check", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // ---- Feedback badge ----
                    when {
                        state.assessmentMessage != null -> {
                            val bg = if (state.assessmentMessage!!.startsWith("Try")) ErrorRed else SuccessGreen
                            FeedbackBadge(state.assessmentMessage!!, bg)
                        }
                        state.feedback == Feedback.CORRECT -> FeedbackBadge("Well done!", SuccessGreen)
                        state.feedback == Feedback.INCORRECT -> FeedbackBadge("Oops! Look at the green ones.", ErrorRed)
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // ---- Test mode toggle ----
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text("normal", fontSize = 12.sp, color = if (testMode == 0) MaterialTheme.colorScheme.primary else Color.Gray)
                        Spacer(modifier = Modifier.width(6.dp))
                        androidx.compose.material3.Switch(
                            checked = testMode == 1,
                            onCheckedChange = { testMode = if (it) 1 else 0 },
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("shuffle", fontSize = 12.sp, color = if (testMode == 1) MaterialTheme.colorScheme.primary else Color.Gray)
                    }

                    // ---- Helper character ----
                    HelperCharacter(
                        showHint = showHint,
                        hintIndex = hintIndex,
                        images = state.shuffledImages,
                        cols = if (state.shuffledImages.size <= 4) 2 else 3,
                        feedback = state.feedback,
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

// ---- Helpers ----

private fun parseImages(json: String): List<ImageChoice> {
    return try {
        val arr = JSONArray(json)
        (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            ImageChoice(path = obj.getString("path"), isCorrect = obj.getBoolean("correct"))
        }
    } catch (_: Exception) {
        emptyList()
    }
}

@Composable
private fun ImageGrid(
    images: List<ImageChoice>,
    selectedIndices: Set<Int>,
    hintIndex: Int?,
    feedback: Feedback,
    enabled: Boolean,
    onTap: (Int) -> Unit,
) {
    val cols = when {
        images.size <= 4 -> 2
        else -> 3
    }
    val rows = images.chunked(cols)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        for (row in rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                for (img in row) {
                    val idx = images.indexOf(img)
                    Box(modifier = Modifier.weight(1f)) {
                        ImageCard(
                            imageChoice = img,
                            isSelected = idx in selectedIndices,
                            isHinted = idx == hintIndex,
                            feedback = feedback,
                            enabled = enabled,
                            onClick = { onTap(idx) },
                        )
                    }
                }
                repeat(cols - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ImageCard(
    imageChoice: ImageChoice,
    isSelected: Boolean,
    isHinted: Boolean,
    feedback: Feedback,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = when {
        feedback == Feedback.NONE && isSelected -> Orange500
        feedback == Feedback.NONE && isHinted -> Teal400
        feedback == Feedback.NONE -> MaterialTheme.colorScheme.outline
        feedback == Feedback.CORRECT && imageChoice.isCorrect -> SuccessGreen
        feedback == Feedback.INCORRECT && imageChoice.isCorrect -> SuccessGreen
        else -> Color.Transparent
    }
    val borderWidth = when {
        isSelected && feedback == Feedback.NONE -> 4.dp
        isHinted && feedback == Feedback.NONE -> 4.dp
        feedback != Feedback.NONE && imageChoice.isCorrect -> 4.dp
        else -> 2.dp
    }

    val pulseAlpha by animateFloatAsState(
        targetValue = if (isHinted) 0.6f else 1f,
        animationSpec = tween(800),
        label = "hintPulse",
    )

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .border(borderWidth, borderColor.copy(alpha = pulseAlpha), RoundedCornerShape(16.dp))
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(File(imageChoice.path))
                .crossfade(true)
                .build(),
            contentDescription = "Image",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        // Hint indicator — small ✨ badge
        if (isHinted && feedback == Feedback.NONE) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(28.dp)
                    .background(Teal400, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("✨", fontSize = 14.sp)
            }
        }

        // Selection checkmark overlay
        if (isSelected && feedback == Feedback.NONE) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(28.dp)
                    .background(Orange500, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = "Selected",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun FeedbackBadge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .padding(top = 8.dp)
            .background(color, RoundedCornerShape(16.dp))
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        Text(text, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
private fun SelfAssessButton(
    emoji: String,
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(72.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 28.sp)
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
private fun HelperCharacter(
    showHint: Boolean,
    hintIndex: Int?,
    images: List<ImageChoice>,
    cols: Int,
    feedback: Feedback,
) {
    val transition = rememberInfiniteTransition(label = "bounce")
    val bounce by transition.animateFloat(
        initialValue = 0f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(tween(600), repeatMode = RepeatMode.Reverse),
        label = "bounceY",
    )

    val canHint = showHint && feedback == Feedback.NONE && hintIndex != null && images.isNotEmpty()
    val posText = if (canHint) positionLabel(hintIndex!!, cols) else ""
    val arrow = if (canHint) positionArrow(hintIndex!!, cols) else ""

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Chat bubble
        AnimatedVisibility(visible = canHint) {
            Box(
                modifier = Modifier
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .border(2.dp, Teal400, RoundedCornerShape(16.dp))
                    .padding(12.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "I think it may be...",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        "the $posText one! $arrow",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Teal400,
                    )
                }
            }
        }

        // Character
        Text(
            text = if (canHint) "🤔" else "😊",
            fontSize = 40.sp,
            modifier = Modifier.offset(y = bounce.dp),
        )

        if (feedback == Feedback.CORRECT) {
            Text("🎉", fontSize = 32.sp)
        } else if (feedback == Feedback.INCORRECT) {
            Text("😢", fontSize = 32.sp)
        }
    }
}

private fun positionLabel(index: Int, cols: Int): String {
    val row = index / cols
    val col = index % cols
    val rowLabel = when (row) { 0 -> "top"; 1 -> "bottom"; else -> "middle" }
    val colLabel = when {
        cols == 2 && col == 0 -> "left"
        cols == 2 -> "right"
        col == 0 -> "left"
        col == cols - 1 -> "right"
        else -> "middle"
    }
    return if (cols == 1) rowLabel else "$rowLabel-$colLabel"
}

private fun positionArrow(index: Int, cols: Int): String {
    val col = index % cols
    val row = index / cols
    return when {
        cols == 1 -> "👇"
        row == 0 && col == 0 -> "👆"
        row == 0 && col == cols - 1 -> "👆"
        row > 0 && col == 0 -> "👇"
        row > 0 && col == cols - 1 -> "👇"
        col < cols / 2 -> "👈"
        else -> "👉"
    }
}
