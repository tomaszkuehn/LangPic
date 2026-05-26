package com.example.langpic.ui.screen.importzip

import androidx.lifecycle.ViewModel
import com.example.langpic.data.repository.LessonRepository
import com.example.langpic.service.ImportResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ImportUiState(
    val isLoading: Boolean = false,
    val result: ImportResult? = null,
    val error: String? = null,
    val warningTitle: String? = null,
)

class ImportViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ImportUiState())
    val uiState: StateFlow<ImportUiState> = _uiState.asStateFlow()

    private var pendingResult: ImportResult? = null
    private var repository: LessonRepository? = null

    fun init(repo: LessonRepository) {
        repository = repo
    }

    fun clearResult() {
        _uiState.value = ImportUiState()
    }

    fun setLoading() {
        _uiState.value = ImportUiState(isLoading = true)
    }

    fun setResult(result: ImportResult) {
        _uiState.value = ImportUiState(result = result)
    }

    fun setError(message: String) {
        _uiState.value = ImportUiState(error = message)
    }

    /** Show duplicate warning — stash result for later confirmation. */
    fun showDuplicateWarning(title: String, result: ImportResult) {
        pendingResult = result
        _uiState.value = ImportUiState(warningTitle = title)
    }

    /** User cancelled the duplicate warning — reset to initial state. */
    fun cancelDuplicate() {
        pendingResult = null
        _uiState.value = ImportUiState()
    }

    /** User confirmed — return the stashed result for insertion. */
    fun consumePendingResult(): ImportResult? {
        val r = pendingResult
        pendingResult = null
        return r
    }
}
