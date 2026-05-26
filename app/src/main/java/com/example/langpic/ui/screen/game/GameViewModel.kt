package com.example.langpic.ui.screen.game

import androidx.lifecycle.ViewModel
import com.example.langpic.domain.model.ImageChoice
import com.example.langpic.domain.model.LessonItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class Feedback { NONE, CORRECT, INCORRECT }

data class GameUiState(
    val currentItem: LessonItem? = null,
    val shuffledImages: List<ImageChoice> = emptyList(),
    val selectedIndices: Set<Int> = emptySet(),
    val score: Float = 0f,
    val totalItems: Int = 0,
    val maxPossibleScore: Float = 0f,
    val totalAttempts: Int = 0,
    val remainingCount: Int = 0,
    val feedback: Feedback = Feedback.NONE,
    val isFinished: Boolean = false,
    val hasContent: Boolean = false,
    val showingAnswer: Boolean = false,
    val hintActive: Boolean = false,
    val showSelfAssessment: Boolean = false,
    val assessmentMessage: String? = null,
)

class GameViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var items: List<LessonItem> = emptyList()
    private val queue = ArrayDeque<Int>()
    private val lateQueue = ArrayDeque<Int>()
    private var currentIdx = -1
    private var requeueCurrent = false
    private var requeueLate = false
    private var testingMode = 0
    private var wasSwapped = false
    private val swapDecisions = mutableMapOf<Int, Boolean>() // per-item swap consistency
    private var itemStartTime = 0L
    private var hintWasUsed = false
    private var maxScore = 0f
    private val awardedItems = mutableSetOf<Int>()

    fun loadItems(itemList: List<LessonItem>) {
        items = itemList
        maxScore = items.sumOf { if (it.images.isEmpty()) 0.8 else 1.0 }.toFloat()
        queue.clear()
        lateQueue.clear()
        queue.addAll(items.indices.shuffled())
        awardedItems.clear()
        swapDecisions.clear()
        if (queue.isEmpty()) {
            _uiState.value = GameUiState(hasContent = false, isFinished = true)
        } else {
            currentIdx = queue.removeFirst()
            publishItem()
        }
    }

    fun selectImage(index: Int) {
        val state = _uiState.value
        if (state.feedback != Feedback.NONE) return

        val sel = state.selectedIndices.toMutableSet()
        if (sel.contains(index)) sel.remove(index) else sel.add(index)
        _uiState.value = state.copy(selectedIndices = sel)
    }

    fun checkAnswer() {
        val state = _uiState.value
        if (state.feedback != Feedback.NONE) return

        val correctIndices = state.shuffledImages.indices
            .filter { state.shuffledImages[it].isCorrect }
            .toSet()

        if (correctIndices.isEmpty()) {
            // No-image item — show answer first, then self-assessment
            _uiState.value = state.copy(showSelfAssessment = true, showingAnswer = true)
            return
        }

        val isCorrect = state.selectedIndices == correctIndices

        requeueCurrent = !isCorrect

        val earned = if (isCorrect && !awardedItems.contains(currentIdx)) {
            val pts = calculatePoints(correctCount = correctIndices.size, hinted = hintWasUsed)
            awardedItems.add(currentIdx)
            pts
        } else {
            0f
        }

        _uiState.value = state.copy(
            feedback = if (isCorrect) Feedback.CORRECT else Feedback.INCORRECT,
            score = state.score + earned,
            totalAttempts = state.totalAttempts + 1,
            showingAnswer = true,
        )
    }

    fun selfAssess(points: Float) {
        val state = _uiState.value
        if (!state.showSelfAssessment) return

        val alreadyAwarded = awardedItems.contains(currentIdx)
        val earned = if (!alreadyAwarded && points > 0f) {
            awardedItems.add(currentIdx)
            points
        } else {
            0f
        }

        if (earned <= 0f) {
            requeueLate = true
        } else {
            requeueCurrent = false
        }

        val msg = when {
            earned <= 0f -> "Try to remember next time!"
            earned <= 0.2f -> "Getting there!"
            else -> "Well done!"
        }

        _uiState.value = state.copy(
            feedback = Feedback.CORRECT,
            score = state.score + earned,
            totalAttempts = state.totalAttempts + 1,
            showingAnswer = true,
            showSelfAssessment = false,
            assessmentMessage = msg,
        )
    }

    fun setHintActive(active: Boolean) {
        if (active) hintWasUsed = true
        _uiState.value = _uiState.value.copy(hintActive = active)
    }

    fun nextRound() {
        if (requeueCurrent) {
            queue.addLast(currentIdx)
        }
        if (requeueLate) {
            lateQueue.addLast(currentIdx)
        }
        if (queue.isNotEmpty()) {
            currentIdx = queue.removeFirst()
            publishItem()
        } else if (lateQueue.isNotEmpty()) {
            currentIdx = lateQueue.removeFirst()
            publishItem()
        } else {
            _uiState.value = _uiState.value.copy(isFinished = true)
        }
    }

    fun setTestingMode(mode: Int) {
        testingMode = mode
    }

    fun reset() {
        awardedItems.clear()
        _uiState.value = GameUiState(totalItems = items.size)
        queue.clear()
        lateQueue.clear()
        queue.addAll(items.indices.shuffled())
        if (queue.isNotEmpty()) {
            currentIdx = queue.removeFirst()
            publishItem()
        }
    }

    private fun calculatePoints(correctCount: Int, hinted: Boolean): Float {
        return when {
            hinted -> if (correctCount == 1) 0.3f else 0.8f
            else -> 1.0f
        }
    }

    private fun publishItem() {
        val rawItem = items[currentIdx]
        val doSwap = testingMode == 1 && rawItem.word2.isNotEmpty() && (
            swapDecisions.getOrPut(currentIdx) { kotlin.random.Random.nextBoolean() }
        )
        wasSwapped = doSwap
        val swapped = if (doSwap) {
            rawItem.copy(
                word1 = rawItem.word2,
                language1 = rawItem.language2.ifEmpty { rawItem.language1 },
                word2 = rawItem.word1,
                language2 = rawItem.language1,
            )
        } else {
            rawItem
        }
        val shuffled = swapped.images.shuffled()
        itemStartTime = System.currentTimeMillis()
        hintWasUsed = false
        requeueCurrent = false
        requeueLate = false
        _uiState.value = _uiState.value.copy(
            currentItem = swapped,
            shuffledImages = shuffled,
            selectedIndices = emptySet(),
            hasContent = true,
            feedback = Feedback.NONE,
            showingAnswer = false,
            hintActive = false,
            showSelfAssessment = false,
            assessmentMessage = null,
            totalItems = items.size,
            maxPossibleScore = maxScore,
            remainingCount = queue.size + lateQueue.size + 1,
        )
    }
}
