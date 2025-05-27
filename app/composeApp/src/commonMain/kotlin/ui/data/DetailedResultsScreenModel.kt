package ui.data

import api.*
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import co.touchlab.kermit.Logger
import io.ktor.client.network.sockets.*
import io.ktor.util.network.*
import ioDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "ResultsViewModel"

data class DetailedResultsUiState(
    val courseInfo: CourseInfo = CourseInfo(),
    val gradeInfo: List<Grade> = emptyList(),
    val dataLoaded: Boolean = false,
    val errorMessage: String = "",
    val refreshing: Boolean = false,
)


class DetailedResultsScreenModel : ScreenModel {
    private val _uiState = MutableStateFlow(DetailedResultsUiState())
    val uiState: StateFlow<DetailedResultsUiState> = _uiState.asStateFlow()

    fun getCourseInfo(term: String, courseNum: String) {
        screenModelScope.launch(ioDispatcher) {
            Logger.d("Getting course info", tag=TAG)
            _uiState.update { currentState ->
                currentState.copy(
                    refreshing = true,
                )
            }
            try {
                //Log.d(TAG, "term: $term, courseNumber: $courseNum")
                val courseInfo = classAPIResponse(term, courseNum)
                val gradeInfo = getGradeInfo(courseInfo)
                Logger.d(gradeInfo.toString(), tag=TAG)

                _uiState.update { currentState ->
                    currentState.copy(
                        courseInfo = courseInfo,
                        gradeInfo = gradeInfo,
                        dataLoaded = true
                    )
                }
            }  catch (e: Exception) {
                Logger.d("Exception in detailed results: $e", tag = TAG)
                val errorMessage = when (e) {
                    is UnresolvedAddressException -> "No Internet connection"
                    is SocketTimeoutException -> "Connection timed out"
                    else -> "Error: ${e.message}"
                }
                _uiState.update { currentState ->
                    currentState.copy(
                        errorMessage = errorMessage
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