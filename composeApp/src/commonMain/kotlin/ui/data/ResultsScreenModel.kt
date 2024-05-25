package ui.data

import api.Course
import api.Type
import api.supabaseQuery
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import co.touchlab.kermit.Logger
import com.pras.Database
import io.github.jan.supabase.exceptions.BadRequestRestException
import io.github.jan.supabase.exceptions.HttpRequestException
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

private const val TAG = "ResultsScreenModel"

data class ResultsUiState(
    val resultsList: List<Course> = emptyList(),
    val favoritesList: List<String> = emptyList(),
    val dataLoaded: Boolean = false,
    val errorMessage: String = "",
    val favoriteMessage: String = "",
    val refreshing: Boolean = false,
)


class ResultsScreenModel : ScreenModel {
    private val _uiState = MutableStateFlow(ResultsUiState())
    val uiState: StateFlow<ResultsUiState> = _uiState.asStateFlow()

    fun setRefresh(isRefreshing: Boolean) {
        _uiState.update { currentState ->
            currentState.copy(
                refreshing = isRefreshing,
            )
        }
    }

    fun getCourses(
        term: Int,
        department: String,
        courseNumber: String,
        query: String,
        type: List<Type>,
        genEd: List<String>,
        searchType: String
    ) {
        val useDepartment = (Regex("^[A-Za-z]{2,4}$").matches(department) && departmentList.contains(department.uppercase()))
        val useCourseNumber = (Regex("\\d{1,3}[a-zA-Z]?").matches(courseNumber))

//        if (useDepartment)  Log.d(TAG, "Using department")
//        if (useCourseNumber)  Log.d(TAG, "Using course number")
        screenModelScope.launch(Dispatchers.IO) {
            try {
//                _uiState.update { currentState ->
//                    currentState.copy(
//                        refreshing = true,
//                    )
//                }
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
                        dataLoaded = true,
                    )
                }
                Logger.d(_uiState.toString(), tag = TAG)
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    val errorMessage = when(e) {
                        is HttpRequestException -> "Failed to fetch results"
                        is BadRequestRestException -> "Bad request"
                        else -> "An error occurred: ${e.message}"
                    }
                    // cancellation exceptions are normal
                    _uiState.update { currentState ->
                        currentState.copy(
                            errorMessage = errorMessage
                        )
                    }
                }
            } finally {
                setRefresh(false)
            }
        }
    }

    fun getFavorites(database: Database) {
        screenModelScope.launch(Dispatchers.IO) {
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

    fun clearError() {
        _uiState.value = _uiState.value.copy(
            errorMessage = ""
        )
    }
}

// list of departments
val departmentList = setOf(
    "APLX",
    "AM",
    "ARBC",
    "ART",
    "ARTG",
    "ASTR",
    "BIOC",
    "BIOL",
    "BIOE",
    "BME",
    "CRSN",
    "CHEM",
    "CHIN",
    "CSP",
    "CLNI",
    "CMMU",
    "CMPM",
    "CSE",
    "COWL",
    "CRES",
    "CRWN",
    "DANM",
    "EART",
    "ECON",
    "EDUC",
    "ECE",
    "ESCI",
    "ENVS",
    "FMST",
    "FILM",
    "FREN",
    "GAME",
    "GERM",
    "GCH",
    "GRAD",
    "GREE",
    "HEBR",
    "HIS",
    "HAVC",
    "HISC",
    "HCI",
    "HUMN",
    "ITAL",
    "JAPN",
    "JRLC",
    "KRSG",
    "LAAD",
    "LATN",
    "LALS",
    "LGST",
    "LING",
    "LIT",
    "MATH",
    "MERR",
    "METX",
    "MUSC",
    "NLP",
    "OAKS",
    "OCEA",
    "PERS",
    "PHIL",
    "PBS",
    "PHYE",
    "PHYS",
    "POLI",
    "PRTR",
    "PORT",
    "PSYC",
    "SCIC",
    "SOCD",
    "SOCY",
    "SPAN",
    "SPHS",
    "STAT",
    "STEV",
    "TIM",
    "THEA",
    "UCDC",
    "VAST",
    "WRIT"
)