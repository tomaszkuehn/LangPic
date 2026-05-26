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
)

class GameViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var items: List<LessonItem> = emptyList()
    private val queue = ArrayDeque<Int>()
    private var currentIdx = -1
    private var requeueCurrent = false
    private var testingMode = 0
    private var wasSwapped = false
    private var itemStartTime = 0L
    private var hintWasUsed = false
    private var maxScore = 0f
    private val awardedItems = mutableSetOf<Int>()

    fun loadItems(itemList: List<LessonItem>) {
        items = itemList
        maxScore = items.sumOf { if (it.images.isEmpty()) 0.5 else 1.0 }.toFloat()
        queue.clear()
        queue.addAll(items.indices.shuffled())
        awardedItems.clear()
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

        val isCorrect = if (correctIndices.isEmpty()) {
            true
        } else {
            state.selectedIndices == correctIndices
        }

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

    fun setHintActive(active: Boolean) {
        if (active) hintWasUsed = true
        _uiState.value = _uiState.value.copy(hintActive = active)
    }

    fun nextRound() {
        if (requeueCurrent) {
            queue.addLast(currentIdx)
        }
        if (queue.isEmpty()) {
            _uiState.value = _uiState.value.copy(isFinished = true)
        } else {
            currentIdx = queue.removeFirst()
            publishItem()
        }
    }

    fun setTestingMode(mode: Int) {
        testingMode = mode
    }

    fun reset() {
        awardedItems.clear()
        _uiState.value = GameUiState(totalItems = items.size)
        queue.clear()
        queue.addAll(items.indices.shuffled())
        if (queue.isNotEmpty()) {
            currentIdx = queue.removeFirst()
            publishItem()
        }
    }

    private fun calculatePoints(correctCount: Int, hinted: Boolean): Float {
        return when {
            correctCount == 0 -> {
                val elapsed = (System.currentTimeMillis() - itemStartTime) / 1000f
                if (elapsed < 8f) 0.5f else 0.3f
            }
            hinted -> if (correctCount == 1) 0.3f else 0.8f
            else -> 1.0f
        }
    }

    private fun publishItem() {
        val rawItem = items[currentIdx]
        val doSwap = testingMode == 1 && rawItem.word2.isNotEmpty() && kotlin.random.Random.nextBoolean()
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
        _uiState.value = _uiState.value.copy(
            currentItem = swapped,
            shuffledImages = shuffled,
            selectedIndices = emptySet(),
            hasContent = true,
            feedback = Feedback.NONE,
            showingAnswer = false,
            hintActive = false,
            totalItems = items.size,
            maxPossibleScore = maxScore,
            remainingCount = queue.size,
        )
    }
}
