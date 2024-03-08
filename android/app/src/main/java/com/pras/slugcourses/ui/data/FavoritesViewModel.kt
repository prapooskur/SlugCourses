package com.pras.slugcourses.ui.data

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.messaging.FirebaseMessaging
import com.pras.Database
import com.pras.slugcourses.api.Course
import com.pras.slugcourses.fcm.UserId
import com.pras.slugcourses.ui.getSupabaseClient
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.util.UUID

private const val TAG = "FavoritesViewModel"

data class FavoritesUiState(
    val favoritesList: List<Course> = emptyList(),
    val dataLoaded: Boolean = false,
    val errorMessage: String = "",
    val favoriteMessage: String = "",
    val isLoggedIn: Boolean = false
)

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

    Log.d(TAG, courseList.toString())
    return courseList
}

class FavoritesViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    suspend fun getCourses(idList: List<String>) {
        try {
            withContext(Dispatchers.IO) {
                val result = favoritesQuery(supabase, idList)
                _uiState.update { currentState ->
                    currentState.copy(
                        favoritesList = result,
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
    
    suspend fun handleFavorite(course: Course, database: Database) {
        withContext(Dispatchers.IO) {
            if (database.favoritesQueries.select(course.id).executeAsOneOrNull().isNullOrEmpty()) {
                database.favoritesQueries.insert(course.id)
                setFavoritesMessage("Favorited ${course.short_name}")
            } else {
                database.favoritesQueries.delete(course.id)
                setFavoritesMessage("Unfavorited ${course.short_name}")
            }

            val dbUserData = database.authQueries.getUserData().executeAsOneOrNull()
            if (dbUserData == null) {
                database.authQueries.setUserData(
                    user_id = UUID.randomUUID().toString(),
                    fcm_token =  ""
                )
            }

            if (database.authQueries.getUserData().executeAsOne().user_id.isEmpty()) {
                database.authQueries.setUserId(
                    user_id = UUID.randomUUID().toString(),
                )
            }

            // init firebase token if it doesn't exist
            if (dbUserData != null && dbUserData.user_id.isNotEmpty() && dbUserData.fcm_token.isEmpty()) {
                database.authQueries.setUserData(
                    dbUserData.user_id,
                    FirebaseMessaging.getInstance().getToken().toString()
                )
            }

            val favoritesList: List<String> = database.favoritesQueries.selectAll().executeAsList()
            val userData = database.authQueries.getUserData().executeAsOne()

            supabase.from("profiles").upsert(
                UserId (
                    userData.user_id,
                    userData.fcm_token,
                    favoritesList
                )
            )
        }
    }

    private fun setFavoritesMessage(message: String) {
        _uiState.value = _uiState.value.copy(
            favoriteMessage = message
        )
    }

    private companion object {
        val supabase = getSupabaseClient()
    }

}