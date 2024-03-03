package com.pras.slugcourses.ui.data

import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.pras.slugcourses.api.Course
import com.pras.slugcourses.api.Status
import com.pras.slugcourses.api.Type
import com.pras.slugcourses.api.supabaseQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

private const val TAG = "ResultsViewModel"

data class ResultsUiState(
    val resultsList: List<Course> = emptyList(),
    val dataLoaded: Boolean = false,
    val errorMessage: String = ""
)


class ResultsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ResultsUiState())
    val uiState: StateFlow<ResultsUiState> = _uiState.asStateFlow()

    private val _dataLoaded: MutableState<Boolean> = mutableStateOf(false)
    val dataLoaded: Boolean
        get() = _dataLoaded.value

    // private var _courseList: List<Course> = emptyList()

    suspend fun getCourses(
        term: Int,
        department: String,
        courseNumber: String,
        query: String,
        status: Status,
        type: List<Type>,
        genEd: List<String>,
        searchType: String
    ) {
        val useDepartment = (Regex("^[A-Za-z]{2,4}$").matches(department))
        val useCourseNumber = (Regex("\\d{1,3}[a-zA-Z]?").matches(courseNumber))

        if (useDepartment)  Log.d(TAG, "Using department")
        if (useCourseNumber)  Log.d(TAG, "Using course number")

        try {
            withContext(Dispatchers.IO) {
                val result = supabaseQuery(
                    term = term,
                    status = status,
                    department = if (useDepartment) department.uppercase() else "",
                    courseNumber = if (useCourseNumber) courseNumber.filter{it.isDigit()}.toInt() else -1,
                    courseLetter = if (useCourseNumber) courseNumber.filter{it.isLetter()} else "",
                    query = if (!useDepartment && !useCourseNumber) query else "",
                    ge = genEd,
                    asynchronous = type.contains(Type.ASYNC_ONLINE),
                    hybrid = type.contains(Type.HYBRID),
                    synchronous = type.contains(Type.SYNC_ONLINE),
                    inPerson = type.contains(Type.IN_PERSON),
                    searchType = searchType,
                )
                _uiState.update { currentState ->
                    currentState.copy(
                        resultsList = result,
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