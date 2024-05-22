package ui.data

import cafe.adriel.voyager.core.model.ScreenModel
import api.CourseInfo
import api.classAPIResponse
import cafe.adriel.voyager.core.model.screenModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "ResultsViewModel"

data class DetailedResultsUiState(
    val courseInfo: CourseInfo = CourseInfo(),
    val dataLoaded: Boolean = false,
    val errorMessage: String = "",
    val refreshing: Boolean = false,
)


class DetailedResultsScreenModel : ScreenModel {
    private val _uiState = MutableStateFlow(DetailedResultsUiState())
    val uiState: StateFlow<DetailedResultsUiState> = _uiState.asStateFlow()

    fun getCourseInfo(term: String, courseNum: String) {
        screenModelScope.launch(Dispatchers.IO) {
            Logger.d("Getting course info", tag=TAG)
            _uiState.update { currentState ->
                currentState.copy(
                    refreshing = true,
                )
            }
            try {
                //Log.d(TAG, "term: $term, courseNumber: $courseNum")
                val courseInfo = classAPIResponse(term, courseNum)
                _uiState.update { currentState ->
                    currentState.copy(
                        courseInfo = courseInfo,
                        dataLoaded = true
                    )
                }
            }  catch (e: Exception) {
                //Log.d(TAG, "An error occurred: ${e.message}")
                _uiState.update { currentState ->
                    currentState.copy(
                        errorMessage = "An error occurred: ${e.message}"
                    )
                }
            } finally {
                _uiState.update { currentState ->
                    currentState.copy(
                        refreshing = false
                    )
                }
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