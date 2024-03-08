package com.pras.slugcourses.ui.data

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.messaging.FirebaseMessaging
import com.pras.Database
import com.pras.slugcourses.api.Course
import com.pras.slugcourses.api.Status
import com.pras.slugcourses.api.Type
import com.pras.slugcourses.api.supabaseQuery
import com.pras.slugcourses.ui.UserId
import com.pras.slugcourses.ui.getSupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
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
    val errorMessage: String = "",
    val favoriteMessage: String = ""
)


class ResultsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ResultsUiState())
    val uiState: StateFlow<ResultsUiState> = _uiState.asStateFlow()

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
                    user_id = supabase.auth.retrieveUserForCurrentSession().id,
                    fcm_token =  FirebaseMessaging.getInstance().getToken().toString()
                )
            }

            if (dbUserData != null && dbUserData.user_id.isEmpty()) {
                database.authQueries.setUserData(
                    supabase.auth.retrieveUserForCurrentSession().id,
                    database.authQueries.getUserData().executeAsOne().fcm_token
                )
            }

            if (dbUserData != null && dbUserData.fcm_token.isEmpty()) {
                database.authQueries.setUserData(
                    supabase.auth.retrieveUserForCurrentSession().id,
                    FirebaseMessaging.getInstance().getToken().toString()
                )
            }

            val favoritesList: List<String> = database.favoritesQueries.selectAll().executeAsList()

            val userData = dbUserData ?: database.authQueries.getUserData().executeAsOne()

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