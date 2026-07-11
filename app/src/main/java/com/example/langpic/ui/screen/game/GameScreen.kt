@file:OptIn(
    ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.example.langpic.ui.screen.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
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
import com.example.langpic.service.AppPreferences
import com.example.langpic.service.TtsHelper
import com.example.langpic.ui.theme.Blue400
import com.example.langpic.ui.theme.CreamBg
import com.example.langpic.ui.theme.DarkText
import com.example.langpic.ui.theme.ErrorRed
import com.example.langpic.ui.theme.ErrorRedLight
import com.example.langpic.ui.theme.GradientPlayful
import com.example.langpic.ui.theme.GradientWarm
import com.example.langpic.ui.theme.MediumText
import com.example.langpic.ui.theme.Orange100
import com.example.langpic.ui.theme.Orange300
import com.example.langpic.ui.theme.Orange500
import com.example.langpic.ui.theme.Pink400
import com.example.langpic.ui.theme.Purple400
import com.example.langpic.ui.theme.SuccessGreen
import com.example.langpic.ui.theme.SuccessGreenLight
import com.example.langpic.ui.theme.SurfaceWhite
import com.example.langpic.ui.theme.Teal200
import com.example.langpic.ui.theme.Teal400
import com.example.langpic.ui.theme.Teal50
import com.example.langpic.ui.theme.WarningAmber
import kotlinx.coroutines.delay
import org.json.JSONArray
import java.io.File

private const val HINT_DELAY_MS = 8000L

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
    val prefs = remember { AppPreferences.getInstance(context) }
    var typewriterEnabled by remember { mutableStateOf(prefs.typewriterEnabled) }
    var showHint by remember { mutableStateOf(false) }
    var hintIndex by remember { mutableStateOf<Int?>(null) }
    var showCelebration by remember { mutableStateOf(false) }

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
                id = entity.id, packId = entity.packId,
                word1 = entity.word1, language1 = entity.language1,
                word2 = entity.word2, language2 = entity.language2,
                images = images,
            )
        }
        viewModel.loadItems(resolved)
    }

    LaunchedEffect(testMode) { viewModel.setTestingMode(testMode) }
    LaunchedEffect(typewriterEnabled) { viewModel.setTypewriterEnabled(typewriterEnabled) }

    // ---- Letter-by-letter reveal ----
    LaunchedEffect(state.currentItem, typewriterEnabled) {
        val item = state.currentItem ?: return@LaunchedEffect
        val word = item.word1
        if (word.isEmpty()) return@LaunchedEffect
        if (typewriterEnabled) {
            viewModel.setRevealedLength(0)
            val delayPerChar = (2000L / word.length).coerceIn(30L, 300L)
            for (i in 1..word.length) {
                delay(delayPerChar)
                viewModel.setRevealedLength(i)
            }
        } else {
            viewModel.setRevealedLength(word.length)
        }
    }

    LaunchedEffect(state.currentItem, state.revealedLength) {
        showHint = false
        val item = state.currentItem ?: return@LaunchedEffect
        if (state.revealedLength >= item.word1.length) {
            ttsHelper.speak(item.word1, item.language1)
        }
    }

    // Hint timer
    LaunchedEffect(state.currentItem, state.feedback, state.selectedIndices, testMode) {
        if (state.feedback != Feedback.NONE || state.currentItem == null) {
            showHint = false; viewModel.setHintActive(false); return@LaunchedEffect
        }
        showHint = false; viewModel.setHintActive(false)
        delay(HINT_DELAY_MS)
        if (state.feedback == Feedback.NONE && state.currentItem != null) {
            showHint = true; viewModel.setHintActive(true)
        }
    }

    LaunchedEffect(state.isFinished) {
        if (state.isFinished) {
            val packIds = repository.getEnabledPackIds()
            for (id in packIds) repository.updateHighScore(id, state.score)
        }
    }

    // Feedback → celebration + auto-advance
    LaunchedEffect(state.feedback) {
        if (state.feedback == Feedback.CORRECT) {
            showCelebration = true; delay(1800); showCelebration = false
        }
        if (state.feedback != Feedback.NONE) {
            delay(2800); viewModel.nextRound()
        }
    }

    // ---- Screen layout ----
    Scaffold(
        containerColor = CreamBg,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⭐ ${"%.1f".format(state.score)}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(" / ${"%.1f".format(state.maxPossibleScore)}", fontSize = 14.sp, color = MediumText)
                        if (bestHighScore > 0f) {
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("🏆 ${"%.1f".format(bestHighScore)}", fontSize = 14.sp, color = Orange500, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Text("←", fontSize = 22.sp, fontWeight = FontWeight.Bold) }
                },
                actions = {
                    if (state.hasContent && !state.isFinished) {
                        Text("${state.remainingCount} left", fontSize = 13.sp, color = MediumText, modifier = Modifier.padding(end = 12.dp))
                    }
                },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceWhite,
                ),
            )
        },
    ) { padding ->
        when {
            !state.hasContent && !state.isFinished -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📚", fontSize = 56.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No lesson items yet.", fontSize = 18.sp, color = MediumText, fontWeight = FontWeight.Medium)
                        Text("Import a lesson pack to start!", fontSize = 15.sp, color = MediumText)
                    }
                }
            }

            state.isFinished -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text("🎉", fontSize = 64.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("All Done!", fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = Orange500)
                    Spacer(modifier = Modifier.height(16.dp))
                    ScoreCard(state.score, state.maxPossibleScore, state.totalAttempts)
                    Spacer(modifier = Modifier.height(28.dp))
                    Button(
                        onClick = { viewModel.reset() },
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Orange500),
                    ) {
                        Text("🔄  Play Again", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            else -> {
                val item = state.currentItem!!
                Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        // ---- Progress bar ----
                        val progress = if (state.totalItems > 0) {
                            1f - (state.remainingCount.toFloat() / state.totalItems)
                        } else 0f
                        LinearProgressIndicator(
                            progress = { progress.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = Orange500,
                            trackColor = Orange100,
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        // ---- Word card ----
                        WordCard(
                            word = item.word1,
                            language = item.language1,
                            revealedLength = if (typewriterEnabled) state.revealedLength else item.word1.length,
                            typewriterEnabled = typewriterEnabled,
                            onSpeakerClick = { ttsHelper.speak(item.word1, item.language1) },
                        )

                        Spacer(modifier = Modifier.height(14.dp))

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

                        Spacer(modifier = Modifier.height(10.dp))

                        // ---- Word2 reveal ----
                        AnimatedVisibility(
                            visible = state.showingAnswer && item.word2.isNotEmpty(),
                            enter = slideInVertically { it / 2 } + fadeIn(),
                        ) {
                            AnswerBubble(word = item.word2, language = item.language2)
                        }

                        // ---- Check / Self-assess ----
                        if (state.feedback == Feedback.NONE) {
                            if (state.showSelfAssessment) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("How well did you remember?", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = DarkText)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    SelfAssessChip("😢", "not really", ErrorRed, 0f, viewModel, Modifier.weight(1f))
                                    SelfAssessChip("😐", "sort of", WarningAmber, 0.2f, viewModel, Modifier.weight(1f))
                                    SelfAssessChip("😊", "yes!", SuccessGreen, if (showHint) 0.4f else 0.8f, viewModel, Modifier.weight(1f))
                                }
                            } else {
                                val canCheck = state.shuffledImages.isEmpty() || state.selectedIndices.isNotEmpty()
                                Button(
                                    onClick = { viewModel.checkAnswer() },
                                    enabled = canCheck,
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Teal400),
                                ) {
                                    Text("✓  Check", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // ---- Feedback badge ----
                        AnimatedVisibility(
                            visible = state.feedback != Feedback.NONE || state.assessmentMessage != null,
                            enter = scaleIn(initialScale = 0.5f, animationSpec = tween(300, easing = FastOutSlowInEasing)) + fadeIn(),
                        ) {
                            val (text, bg) = when {
                                state.assessmentMessage != null -> {
                                    val c = if (state.assessmentMessage!!.startsWith("Try")) ErrorRed else SuccessGreen
                                    state.assessmentMessage!! to c
                                }
                                state.feedback == Feedback.CORRECT -> "⭐  Well done!" to SuccessGreen
                                else -> "💡  Look at the green ones" to ErrorRed
                            }
                            FeedbackPill(text, bg)
                        }

                        Spacer(modifier = Modifier.weight(1f))
                    }

                    // ---- Bottom controls ----
                    Column(
                        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                            .background(Brush.verticalGradient(listOf(Color.Transparent, SurfaceWhite.copy(alpha = 0.95f))))
                            .padding(horizontal = 20.dp).padding(bottom = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        // Toggles
                        ToggleRow(
                            leftLabel = "normal", rightLabel = "shuffle",
                            checked = testMode == 1,
                            onToggle = { testMode = if (it) 1 else 0 },
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        ToggleRow(
                            leftLabel = "instant", rightLabel = "typewriter",
                            checked = typewriterEnabled,
                            onToggle = {
                                typewriterEnabled = it
                                prefs.typewriterEnabled = it
                            },
                        )

                        // Helper character
                        HelperCharacter(
                            showHint = showHint,
                            hintIndex = hintIndex,
                            images = state.shuffledImages,
                            cols = if (state.shuffledImages.size <= 4) 2 else 3,
                            feedback = state.feedback,
                        )
                    }

                    // ---- Celebration overlay ----
                    if (showCelebration) {
                        CelebrationOverlay()
                    }
                }
            }
        }
    }
}

// ==================== Sub-composables ====================

@Composable
private fun WordCard(
    word: String,
    language: String,
    revealedLength: Int,
    typewriterEnabled: Boolean,
    onSpeakerClick: () -> Unit,
) {
    val displayText = if (typewriterEnabled) word.take(revealedLength.coerceAtMost(word.length)) else word
    val langEmoji = when (language) { "ja" -> "🇯🇵"; "en" -> "🇬🇧"; else -> "🗣️" }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                // Language flag
                Box(
                    modifier = Modifier.size(32.dp).clip(CircleShape).background(Orange100),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(langEmoji, fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))

                // Word
                Text(
                    text = displayText,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = DarkText,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Speaker button
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Teal400, Blue400)))
                        .clickable { onSpeakerClick() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("🔊", fontSize = 20.sp)
                }
            }

            // Typewriter cursor
            if (typewriterEnabled && revealedLength < word.length) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .offset(x = (-50).dp)
                        .size(2.dp, 28.dp)
                        .background(Orange500),
                )
            }
        }
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
    val cols = if (images.size <= 4) 2 else 3
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
                        ImageTile(
                            imageChoice = img,
                            isSelected = idx in selectedIndices,
                            isHinted = idx == hintIndex && enabled,
                            feedback = feedback,
                            enabled = enabled,
                            onClick = { onTap(idx) },
                        )
                    }
                }
                repeat(cols - row.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun ImageTile(
    imageChoice: ImageChoice,
    isSelected: Boolean,
    isHinted: Boolean,
    feedback: Feedback,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 0.94f else 1f,
        animationSpec = tween(200),
        label = "tileScale",
    )

    val borderColor: Color
    val borderW: androidx.compose.ui.unit.Dp

    when {
        feedback != Feedback.NONE && imageChoice.isCorrect -> {
            borderColor = SuccessGreen; borderW = 4.dp
        }
        feedback != Feedback.NONE && isSelected && !imageChoice.isCorrect -> {
            borderColor = ErrorRed; borderW = 4.dp
        }
        isHinted && enabled -> {
            borderColor = Teal400; borderW = 4.dp
        }
        isSelected && enabled -> {
            borderColor = Orange500; borderW = 4.dp
        }
        else -> {
            borderColor = Color.Transparent; borderW = 0.dp
        }
    }

    val elev = when {
        isSelected && enabled -> 6.dp
        isHinted && enabled -> 8.dp
        else -> 3.dp
    }

    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .scale(scale)
            .then(if (borderW > 0.dp) Modifier.border(borderW, borderColor, RoundedCornerShape(18.dp)) else Modifier),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = elev),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        onClick = { if (enabled) onClick() },
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(5.dp), contentAlignment = Alignment.Center) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(File(imageChoice.path)).crossfade(true).build(),
                contentDescription = "Image",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)),
            )

            // Hint sparkle badge
            if (isHinted && enabled) {
                Box(
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)
                        .size(30.dp).clip(CircleShape).background(Teal400),
                    contentAlignment = Alignment.Center,
                ) { Text("✨", fontSize = 14.sp) }
            }

            // Selection check
            if (isSelected && enabled) {
                Box(
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)
                        .size(30.dp).clip(CircleShape).background(Orange500),
                    contentAlignment = Alignment.Center,
                ) { Text("✓", fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Bold) }
            }

            // Correct/incorrect overlay icon
            if (feedback != Feedback.NONE) {
                Box(
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)
                        .size(30.dp).clip(CircleShape)
                        .background(if (imageChoice.isCorrect) SuccessGreen else ErrorRed),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(if (imageChoice.isCorrect) "✓" else "✗", fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun AnswerBubble(word: String, language: String) {
    val langEmoji = when (language) { "ja" -> "🇯🇵"; "en" -> "🇬🇧"; else -> "🗣️" }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Orange100.copy(alpha = 0.5f)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(langEmoji, fontSize = 16.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(word, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Orange500, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun FeedbackPill(text: String, color: Color) {
    Box(
        modifier = Modifier
            .padding(top = 8.dp)
            .shadow(4.dp, RoundedCornerShape(20.dp))
            .background(color, RoundedCornerShape(20.dp))
            .padding(horizontal = 22.dp, vertical = 10.dp),
    ) {
        Text(text, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
private fun SelfAssessChip(
    emoji: String, label: String, color: Color, points: Float,
    viewModel: GameViewModel, modifier: Modifier = Modifier,
) {
    Button(
        onClick = { viewModel.selfAssess(points) },
        modifier = modifier.height(68.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 26.sp)
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
private fun ToggleRow(
    leftLabel: String, rightLabel: String, checked: Boolean, onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(leftLabel, fontSize = 11.sp, color = if (!checked) Orange500 else MediumText, fontWeight = if (!checked) FontWeight.Bold else FontWeight.Normal)
        Spacer(modifier = Modifier.width(6.dp))
        FilterChip(
            selected = checked,
            onClick = { onToggle(!checked) },
            label = {},
            modifier = Modifier.height(28.dp),
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = Teal400,
            ),
            leadingIcon = if (checked) {{ Text("✓", fontSize = 12.sp, color = Color.White) }} else null,
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(rightLabel, fontSize = 11.sp, color = if (checked) Teal400 else MediumText, fontWeight = if (checked) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun HelperCharacter(
    showHint: Boolean, hintIndex: Int?, images: List<ImageChoice>, cols: Int, feedback: Feedback,
) {
    val transition = rememberInfiniteTransition(label = "bounce")
    val bounce by transition.animateFloat(
        initialValue = 0f, targetValue = -5f,
        animationSpec = infiniteRepeatable(tween(600), repeatMode = RepeatMode.Reverse),
        label = "bounceY",
    )

    val canHint = showHint && feedback == Feedback.NONE && hintIndex != null && images.isNotEmpty()

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        AnimatedVisibility(visible = canHint, enter = fadeIn() + scaleIn()) {
            Box(
                modifier = Modifier
                    .background(Color.White, RoundedCornerShape(14.dp))
                    .border(2.dp, Teal400, RoundedCornerShape(14.dp))
                    .padding(10.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("I think it's...", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = DarkText)
                    Text(
                        "the ${positionLabel(hintIndex ?: 0, cols)} one! ${positionArrow(hintIndex ?: 0, cols)}",
                        fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Teal400,
                    )
                }
            }
        }
        Text(
            text = when {
                feedback == Feedback.CORRECT -> "🎉"
                feedback == Feedback.INCORRECT -> "😢"
                canHint -> "🤔"
                else -> "😊"
            },
            fontSize = 34.sp,
            modifier = Modifier.offset(y = bounce.dp),
        )
    }
}

@Composable
private fun ScoreCard(score: Float, maxScore: Float, attempts: Int) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Your Score", fontSize = 16.sp, color = MediumText, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "${"%.1f".format(score)} / ${"%.1f".format(maxScore)}",
                fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = Orange500,
            )
            if (attempts > 0) {
                Text("$attempts attempt${if (attempts > 1) "s" else ""}", fontSize = 14.sp, color = MediumText)
            }
        }
    }
}

@Composable
private fun CelebrationOverlay() {
    val particles = remember {
        listOf("⭐", "🌟", "✨", "🎉", "🎊", "💫", "🌈", "🎈")
    }
    Row(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for ((i, emoji) in particles.withIndex()) {
            val yAnim by rememberInfiniteTransition(label = "celebrate$i").animateFloat(
                initialValue = 0f, targetValue = -40f,
                animationSpec = infiniteRepeatable(
                    tween(600 + i * 80, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "float$i",
            )
            Text(emoji, fontSize = (24 + i % 4 * 6).sp, modifier = Modifier.offset(y = yAnim.dp))
        }
    }
}

// ---- Position helpers for hint ----

private fun parseImages(json: String): List<ImageChoice> {
    return try {
        val arr = JSONArray(json)
        (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            ImageChoice(path = obj.getString("path"), isCorrect = obj.getBoolean("correct"))
        }
    } catch (_: Exception) { emptyList() }
}

private fun positionLabel(index: Int, cols: Int): String {
    val row = index / cols; val col = index % cols
    val rowLabel = when (row) { 0 -> "top"; 1 -> "bottom"; else -> "middle" }
    val colLabel = when {
        cols == 2 && col == 0 -> "left"; cols == 2 -> "right"
        col == 0 -> "left"; col == cols - 1 -> "right"; else -> "middle"
    }
    return if (cols == 1) rowLabel else "$rowLabel-$colLabel"
}

private fun positionArrow(index: Int, cols: Int): String {
    val col = index % cols; val row = index / cols
    return when {
        cols == 1 -> "👇"
        row == 0 && col == 0 -> "👆"; row == 0 && col == cols - 1 -> "👆"
        row > 0 && col == 0 -> "👇"; row > 0 && col == cols - 1 -> "👇"
        col < cols / 2 -> "👈"; else -> "👉"
    }
}
