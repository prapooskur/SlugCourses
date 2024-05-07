package ui.data

import api.Course
import api.Type
import api.supabaseQuery
import cafe.adriel.voyager.core.model.ScreenModel
import co.touchlab.kermit.Logger
import com.pras.Database
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

private const val TAG = "ResultsScreenModel"

data class ResultsUiState(
    val resultsList: List<Course> = emptyList(),
    val favoritesList: List<String> = emptyList(),
    val dataLoaded: Boolean = false,
    val errorMessage: String = "",
    val favoriteMessage: String = ""
)


class ResultsScreenModel : ScreenModel {
    private val _uiState = MutableStateFlow(ResultsUiState())
    val uiState: StateFlow<ResultsUiState> = _uiState.asStateFlow()

    suspend fun getCourses(
        term: Int,
        department: String,
        courseNumber: String,
        query: String,
        type: List<Type>,
        genEd: List<String>,
        searchType: String
    ) {
        val useDepartment = (Regex("^[A-Za-z]{2,4}$").matches(department))
        val useCourseNumber = (Regex("\\d{1,3}[a-zA-Z]?").matches(courseNumber))

//        if (useDepartment)  Log.d(TAG, "Using department")
//        if (useCourseNumber)  Log.d(TAG, "Using course number")

        try {
            withContext(Dispatchers.IO) {
                val result = supabaseQuery(
                    term = term,
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
                Logger.d(result.toString(), tag = TAG)
                _uiState.update { currentState ->
                    currentState.copy(
                        resultsList = result,
                        dataLoaded = true
                    )
                }
                Logger.d(_uiState.toString(), tag = TAG)
            }

        }  catch (e: Exception) {
//            Log.d(TAG, "An error occurred: ${e.message}")
            _uiState.update { currentState ->
                currentState.copy(
                    errorMessage = "An error occurred: ${e.message}"
                )
            }
        }
    }

    suspend fun getFavorites(database: Database) {
        withContext(Dispatchers.IO) {
            _uiState.update { currentState ->
                currentState.copy(
                    favoritesList = database.favoritesQueries.selectAll().executeAsList()
                )
            }
        }
    }



    suspend fun handleFavorite(course: Course, database: Database) {
        withContext(Dispatchers.IO) {
            if (database.favoritesQueries.select(course.id).executeAsOneOrNull().isNullOrEmpty()) {
                database.favoritesQueries.insert(course.id)
                setFavoritesMessage("Favorited ${course.short_name}")
            } else {
                database.favoritesQueries.delete(course.id)
                setFavoritesMessage("Unfavorited ${course.short_name}")
            }

            _uiState.update { currentState ->
                currentState.copy(
                    favoritesList = database.favoritesQueries.selectAll().executeAsList()
                )
            }
        }
    }

    private fun setFavoritesMessage(message: String) {
        _uiState.value = _uiState.value.copy(
            favoriteMessage = message
        )
    }
}