package com.example.langpic.ui.screen.manage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.langpic.data.repository.LessonRepository
import com.example.langpic.domain.model.LessonPack
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ManageViewModel : ViewModel() {

    private val _packs = MutableStateFlow<List<LessonPack>>(emptyList())
    val packs: StateFlow<List<LessonPack>> = _packs.asStateFlow()

    private var repository: LessonRepository? = null
    private var collectionStarted = false

    fun loadPacks(repo: LessonRepository) {
        if (collectionStarted) return
        repository = repo
        collectionStarted = true
        viewModelScope.launch {
            repo.getAllPacks().collect { list ->
                _packs.value = list
            }
        }
    }

    fun togglePack(packId: Int, enabled: Boolean) {
        viewModelScope.launch {
            repository?.updatePackEnabled(packId, enabled)
        }
    }

    fun deletePack(packId: Int) {
        viewModelScope.launch {
            repository?.deletePack(packId)
        }
    }
}
