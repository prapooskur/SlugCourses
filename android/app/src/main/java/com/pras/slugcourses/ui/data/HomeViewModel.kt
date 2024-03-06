package com.pras.slugcourses.ui.data

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class HomeUiState(
    val searchQuery: String = "",
    val searchPressed: Boolean = false,
    val termChosen: String = "2242",
)


class HomeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun setQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }
}