package com.pras.slugcourses.ui.data

import android.util.Log
import androidx.lifecycle.ViewModel
import com.pras.slugcourses.api.CourseInfo
import com.pras.slugcourses.api.classAPIResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

private const val TAG = "ResultsViewModel"

data class DetailedResultsUiState(
    val courseInfo: CourseInfo = CourseInfo(),
    val dataLoaded: Boolean = false,
    val errorMessage: String = ""
)


class DetailedResultsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(DetailedResultsUiState())
    val uiState: StateFlow<DetailedResultsUiState> = _uiState.asStateFlow()

    suspend fun getCourseInfo(term: String, courseNum: String) {
        try {
            withContext(Dispatchers.IO) {
                Log.d(TAG, "term: $term, courseNumber: $courseNum")
                val courseInfo = classAPIResponse(term, courseNum)
                _uiState.update { currentState ->
                    currentState.copy(
                        courseInfo = courseInfo,
                        dataLoaded = true
                    )
                }
            }

        }  catch (e: Exception) {
            Log.d(TAG, "An error occurred: ${e.message}")
            _uiState.update { currentState ->
                currentState.copy(
                    errorMessage = "An error occurred: ${e.message}"
                )
            }
        }
    }

    fun resetError() {
        _uiState.update { currentState ->
            currentState.copy(
                errorMessage = ""
            )
        }
    }
}