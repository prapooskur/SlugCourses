package ui.data

import api.Course
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import co.touchlab.kermit.Logger
import com.pras.Database
import ui.getSupabaseClient
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "FavoritesViewModel"

data class FavoritesUiState(
    val favoritesList: List<Course> = emptyList(),
    val dataLoaded: Boolean = false,
    val errorMessage: String = "",
    val favoriteMessage: String = "",
    val isLoggedIn: Boolean = false
)


class FavoritesScreenModel : ScreenModel {

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()
    fun getFavorites(database: Database) {
        try {
            screenModelScope.launch {
                withContext(Dispatchers.IO) {
                    val idList = database.favoritesQueries.selectAll().executeAsList()
                    val result = favoritesQuery(supabase, idList)
                    _uiState.update { currentState ->
                        currentState.copy(
                            favoritesList = result,
                            dataLoaded = true
                        )
                    }
                }
            }
        }  catch (e: Exception) {
            Logger.d("An error occurred: ${e.message}", tag = TAG)
            _uiState.update { currentState ->
                currentState.copy(
                    errorMessage = "An error occurred: ${e.message}"
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
        }
    }

    private fun setFavoritesMessage(message: String) {
        _uiState.value = _uiState.value.copy(
            favoriteMessage = message
        )
    }

    suspend fun favoritesQuery(supabase: SupabaseClient, idList: List<String>): List<Course> {
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

    private companion object {
        val supabase = getSupabaseClient()
    }
}