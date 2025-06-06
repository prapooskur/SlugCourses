package ui.data

import api.*
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import co.touchlab.kermit.Logger
import com.pras.Database
import io.github.jan.supabase.exceptions.BadRequestRestException
import io.github.jan.supabase.exceptions.HttpRequestException
import io.ktor.client.network.sockets.*
import io.ktor.util.network.*
import ioDispatcher
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import ui.departmentList

private const val TAG = "ResultsScreenModel"

data class ResultsUiState(
    val listPane: ResultListUiState = ResultListUiState(),
    val detailPane: ResultDetailUiState = ResultDetailUiState(),
    val errorMessage: String = "",
)

data class ResultListUiState(
    // list
    val resultsQuery: String = "",
    val useInitQuery: Boolean = true,
    val resultsList: List<Course> = emptyList(),
    val favoritesList: List<String> = emptyList(),
    val listDataLoaded: Boolean = false,
    val favoriteMessage: String = "",
    val listRefreshing: Boolean = false,
)

data class ResultDetailUiState(
    val courseInfo: CourseInfo = CourseInfo(),
    val gradeInfo: List<Grade> = emptyList(),
    val detailDataLoaded: Boolean = false,
    val detailRefreshing: Boolean = false,
    val selectedId: Int = 0,
    val selectedUrl: String = ""
)


class ResultsScreenModel : ScreenModel {
    private val _uiState = MutableStateFlow(ResultsUiState())
    val uiState: StateFlow<ResultsUiState> = _uiState.asStateFlow()

    // list stuff
    private fun setListRefresh(isRefreshing: Boolean) {
        _uiState.update { currentState ->
            currentState.copy(
                listPane = currentState.listPane.copy(
                    listRefreshing = isRefreshing,
                )
            )
        }
    }

    fun updateQuery(newQuery: String) {
        _uiState.update { currentState ->
            currentState.copy(
                listPane = currentState.listPane.copy(
                    resultsQuery = newQuery,
                    useInitQuery = false
                )
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

        setListRefresh(true)

        val useDepartment = (Regex("^[A-Za-z]{2,4}$").matches(department) && departmentList.contains(department.uppercase()))
        val useCourseNumber = (Regex("\\d{1,3}[a-zA-Z]?").matches(courseNumber))

        val genEd = genEd.toMutableList()
        if ("PR" in genEd) {
            genEd.remove("PR")
            genEd.add("PR-C")
            genEd.add("PR-E")
            genEd.add("PR-S")
        }

        if ("PE" in genEd) {
            genEd.remove("PE")
            genEd.add("PE-E")
            genEd.add("PE-H")
            genEd.add("PE-T")
        }

        screenModelScope.launch(ioDispatcher) {
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
//                        resultsList = result,
//                        dataLoaded = true,
                        currentState.listPane.copy(
                            resultsList = result,
                            listDataLoaded = true,
                        )
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
                setListRefresh(false)
            }
        }
    }

    fun getFavorites(database: Database) {
        screenModelScope.launch(ioDispatcher) {
            _uiState.update { currentState ->
                currentState.copy(
//                    favoritesList = database.favoritesQueries.selectAll().executeAsList()
                    currentState.listPane.copy(
                        favoritesList = database.favoritesQueries.selectAll().awaitAsList()
                    )
                )
            }
        }
    }



    suspend fun handleFavorite(course: Course, database: Database) {
        withContext(ioDispatcher) {
            if (database.favoritesQueries.select(course.id).awaitAsOneOrNull().isNullOrEmpty()) {
                database.favoritesQueries.insert(course.id)
                setFavoritesMessage("Favorited ${course.short_name}")
            } else {
                database.favoritesQueries.delete(course.id)
                setFavoritesMessage("Unfavorited ${course.short_name}")
            }

            _uiState.update { currentState ->
                currentState.copy(
//                    favoritesList = database.favoritesQueries.selectAll().executeAsList()
                    currentState.listPane.copy(
                        favoritesList = database.favoritesQueries.selectAll().awaitAsList()
                    )
                )
            }
        }
    }

    private fun setFavoritesMessage(message: String) {
        _uiState.value = _uiState.value.copy(
            listPane = _uiState.value.listPane.copy(
                favoriteMessage = message
            )
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(
            errorMessage = ""
        )
    }

    // Detail stuff
    fun getCourseInfo(term: String, courseNum: String) {
        screenModelScope.launch(ioDispatcher) {
            Logger.d("Getting course info", tag=TAG)
            _uiState.update { currentState ->
                currentState.copy(
                    detailPane = currentState.detailPane.copy(
                        detailRefreshing = true
                    )
                )
            }
            try {
                val courseInfo = classAPIResponse(term, courseNum)
                val gradeInfo = getGradeInfo(courseInfo)
                Logger.d(gradeInfo.toString(), tag=TAG)

                _uiState.update { currentState ->
//                    courseInfo = courseInfo,
//                    dataLoaded = true
                    currentState.copy(
                        detailPane = currentState.detailPane.copy(
                            courseInfo = courseInfo,
                            gradeInfo = gradeInfo,
                            detailDataLoaded = true,
                        )
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
//                        refreshing = false
                        detailPane = currentState.detailPane.copy(
                            detailRefreshing = false
                        )
                    )
                }
            }
        }
    }

    fun setSelectedId(newId: Int) {
        _uiState.value = _uiState.value.copy(
            detailPane = _uiState.value.detailPane.copy(
                selectedId = newId
            )
        )
    }

    fun setSelectedUrl(newUrl: String) {
        _uiState.value = _uiState.value.copy(
            detailPane = _uiState.value.detailPane.copy(
                selectedUrl = newUrl
            )
        )
    }

}

// list of departments imported from ResultsScreenModel