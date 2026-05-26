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
    val score: Int = 0,
    val totalItems: Int = 0,
    val totalAttempts: Int = 0,
    val remainingCount: Int = 0,
    val feedback: Feedback = Feedback.NONE,
    val isFinished: Boolean = false,
    val hasContent: Boolean = false,
    val showingAnswer: Boolean = false,
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

        _uiState.value = state.copy(
            feedback = if (isCorrect) Feedback.CORRECT else Feedback.INCORRECT,
            score = if (isCorrect) state.score + 1 else state.score,
            totalAttempts = state.totalAttempts + 1,
            showingAnswer = true,
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
        val shuffled = item.images.shuffled()
        _uiState.value = _uiState.value.copy(
            currentItem = item,
            shuffledImages = shuffled,
            selectedIndices = emptySet(),
            hasContent = true,
            feedback = Feedback.NONE,
            showingAnswer = false,
            totalItems = items.size,
            remainingCount = queue.size,
        )
    }
}
