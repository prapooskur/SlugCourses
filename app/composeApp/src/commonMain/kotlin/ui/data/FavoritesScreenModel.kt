package ui.data

import api.Course
import api.CourseInfo
import api.classAPIResponse
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import co.touchlab.kermit.Logger
import com.pras.Database
import ui.getSupabaseClient
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.exceptions.BadRequestRestException
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.ktor.client.network.sockets.*
import io.ktor.util.network.*
import ioDispatcher
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

private const val TAG = "FavoritesViewModel"

data class FavoritesUiState(
    val listPane: FavoritesListPaneUiState = FavoritesListPaneUiState(),
    val detailPane: FavoritesDetailPaneUiState = FavoritesDetailPaneUiState(),
    val showDeleteDialog: Boolean = false,
    val errorMessage: String = "",
    val favoriteMessage: String = "",
)

data class FavoritesListPaneUiState(
    val favoritesList: List<Course> = emptyList(),
    val listDataLoaded: Boolean = false,
    val listRefreshing: Boolean = false,
)

data class FavoritesDetailPaneUiState(
    val courseInfo: CourseInfo = CourseInfo(),
    val detailDataLoaded: Boolean = false,
    val detailRefreshing: Boolean = false,
)


class FavoritesScreenModel : ScreenModel {

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    // list pane functions
    fun setListRefresh(isRefreshing: Boolean) {
        _uiState.update { currentState ->
            currentState.copy(
                listPane = currentState.listPane.copy(
                    listRefreshing = isRefreshing,
                )
            )
        }
    }

    fun getFavorites(database: Database) {
        screenModelScope.launch(ioDispatcher) {
            setListRefresh(true)
            try {
                val idList = database.favoritesQueries.selectAll().executeAsList()
                val result = favoritesQuery(supabase, idList)
                _uiState.update { currentState ->
                    currentState.copy(
                        listPane = currentState.listPane.copy(
                            favoritesList = result,
                            listDataLoaded = true,
                            listRefreshing = false
                        )
                    )
                }
            }  catch (e: Exception) {
                Logger.d("Exception in favorites: $e", tag = TAG)
                if (e !is CancellationException) {

                    val errorMessage = when(e) {
                        is HttpRequestException -> "Failed to fetch favorites"
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
            }
        }
    }

    fun handleFavorite(course: Course, database: Database) {
        screenModelScope.launch(ioDispatcher) {
            if (database.favoritesQueries.select(course.id).executeAsOneOrNull().isNullOrEmpty()) {
                database.favoritesQueries.insert(course.id)
                setFavoritesMessage("Favorited ${course.short_name}")
            } else {
                database.favoritesQueries.delete(course.id)
                setFavoritesMessage("Unfavorited ${course.short_name}")
            }
        }
    }

    private fun setFavoritesMessage(message: String) {
        _uiState.value = _uiState.value.copy(
            favoriteMessage = message
        )
    }

    private suspend fun favoritesQuery(supabase: SupabaseClient, idList: List<String>): List<Course> {
        val courseList = supabase.from("courses").select {

            filter {
                isIn("id", idList)
            }

            order(column = "term", order = Order.DESCENDING)
            order(column = "department", order = Order.ASCENDING)
            order(column = "course_number", order = Order.ASCENDING)
            order(column = "course_letter", order = Order.ASCENDING)
            order(column = "section_number", order = Order.ASCENDING)

        }.decodeList<Course>()

        Logger.d(courseList.toString(), tag = TAG)
        return courseList
    }

    private fun setDetailRefresh(isRefreshing: Boolean) {
        _uiState.update { currentState ->
            currentState.copy(
                detailPane = currentState.detailPane.copy(
                    detailRefreshing = isRefreshing
                )
            )
        }
    }

    fun getCourseInfo(term: String, id: String) {
        screenModelScope.launch(ioDispatcher) {
            setDetailRefresh(true)
            try {
                val courseInfo = classAPIResponse(term, id.substringAfter("_"))
                _uiState.update { currentState ->
                    currentState.copy(
                        detailPane = currentState.detailPane.copy(
                            courseInfo = courseInfo,
                            detailDataLoaded = true,
                        )
                    )
                }
            } catch (e: Exception) {
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
                setDetailRefresh(false)
            }
        }
    }

    fun deleteFavorites(database: Database) {
        screenModelScope.launch(ioDispatcher) {
            database.favoritesQueries.deleteAll()
            getFavorites(database)
        }
    }

    fun toggleDeleteDialog() {
        _uiState.update { currentState ->
            currentState.copy(
                showDeleteDialog = !currentState.showDeleteDialog
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(
            errorMessage = ""
        )
    }

    private companion object {
        val supabase = getSupabaseClient()
    }
}