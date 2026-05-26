package com.example.langpic.ui.screen.game

import androidx.lifecycle.ViewModel
import com.example.langpic.domain.model.LessonItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

enum class Feedback { NONE, CORRECT, INCORRECT, SKIPPED }

data class GameUiState(
    val currentItem: LessonItem? = null,
    val correctIndex: Int = 0,
    val score: Int = 0,
    val totalItems: Int = 0,
    val totalAttempts: Int = 0,
    val skippedCount: Int = 0,
    val remainingCount: Int = 0,
    val feedback: Feedback = Feedback.NONE,
    val isFinished: Boolean = false,
    val hasContent: Boolean = false,
    val shuffledImagePaths: Pair<String, String> = Pair("", ""),
)

class GameViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var items: List<LessonItem> = emptyList()
    private val queue = ArrayDeque<Int>()
    private var currentIdx = -1
    private var requeueCurrent = false

    fun loadItems(itemList: List<LessonItem>) {
        items = itemList
        queue.clear()
        queue.addAll(items.indices.shuffled())
        if (queue.isEmpty()) {
            _uiState.value = GameUiState(hasContent = false, isFinished = true)
        } else {
            currentIdx = queue.removeFirst()
            publishItem()
        }
    }

    fun selectImage(tappedIndex: Int) {
        val state = _uiState.value
        if (state.feedback != Feedback.NONE) return

        val isCorrect = tappedIndex == state.correctIndex
        val newScore = if (isCorrect) state.score + 1 else state.score
        requeueCurrent = !isCorrect

        _uiState.value = state.copy(
            feedback = if (isCorrect) Feedback.CORRECT else Feedback.INCORRECT,
            score = newScore,
            totalAttempts = state.totalAttempts + 1,
        )
    }

    fun skipQuestion() {
        val state = _uiState.value
        if (state.feedback != Feedback.NONE) return

        requeueCurrent = true
        _uiState.value = state.copy(
            feedback = Feedback.SKIPPED,
            skippedCount = state.skippedCount + 1,
            totalAttempts = state.totalAttempts + 1,
        )
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

    fun reset() {
        _uiState.value = GameUiState(totalItems = items.size)
        queue.clear()
        queue.addAll(items.indices.shuffled())
        if (queue.isNotEmpty()) {
            currentIdx = queue.removeFirst()
            publishItem()
        }
    }

    private fun publishItem() {
        val item = items[currentIdx]
        val isCorrectFirst = Random.nextBoolean()
        val shuffled = if (isCorrectFirst) {
            Pair(item.correctImagePath, item.wrongImagePath)
        } else {
            Pair(item.wrongImagePath, item.correctImagePath)
        }
        _uiState.value = _uiState.value.copy(
            currentItem = item,
            correctIndex = if (isCorrectFirst) 0 else 1,
            hasContent = true,
            feedback = Feedback.NONE,
            totalItems = items.size,
            remainingCount = queue.size,
            shuffledImagePaths = shuffled,
        )
    }
}
