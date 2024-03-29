package com.pras.slugcourses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.pras.Database
import com.pras.slugcourses.api.Status
import com.pras.slugcourses.api.Type
import com.pras.slugcourses.ui.data.ResultsViewModel
import com.pras.slugcourses.ui.elements.BoringNormalTopBar
import com.pras.slugcourses.ui.elements.CourseCard
import com.pras.slugcourses.ui.shortToast
import kotlinx.coroutines.launch

private const val TAG = "results"

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ResultsScreen(
    navController: NavController,
    term: Int,
    query: String,
    status: Status,
    type: List<Type>,
    genEd: List<String>,
    searchType: String,
    database: Database
) {

    val viewModel = viewModel<ResultsViewModel>()
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            BoringNormalTopBar(
                titleText = if (query.isNotBlank()) "Results for \"${query.trim()}\"" else "Results",
                navController = navController,
            )
        },
        content = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it)
            ) {
                if (uiState.dataLoaded) {
                    val response = uiState.resultsList
                    val favorites: Set<String> = database.favoritesQueries.selectAll().executeAsList().toSet()
                    if (response.isNotEmpty()) {
                        items(response.size) { id ->
                            val course = response[id]
                            CourseCard(
                                course = course,
                                navController = navController,
                                isFavorited = favorites.contains(course.id),
                                onFavorite = {
                                    coroutineScope.launch {
                                        viewModel.handleFavorite(course, database)
                                    }
                                }
                            )
                        }
                    } else {
                        item {
                            Text(
                                text = "No classes found for your search.",
                                fontSize = 24.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.offset(y = (240).dp)
                            )
                        }
                    }

                } else {
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    )

    val splitQuery = query.contains(" ")

    val department = if (splitQuery) { query.substringBefore(" ") } else { query }
    val courseNumber = if (splitQuery) { query.substringAfter(" ") } else { query }

    LaunchedEffect(key1 = uiState.errorMessage) {
        if (uiState.errorMessage.isNotEmpty()) {
            shortToast("An error occurred: ${uiState.errorMessage}", context)
        }
    }

    LaunchedEffect(key1 = uiState.favoriteMessage) {
        if (uiState.favoriteMessage.isNotEmpty()) {
            shortToast(uiState.favoriteMessage, context)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.getCourses(
            term,
            department,
            courseNumber,
            query,
            status,
            type,
            genEd,
            searchType
        )
    }
}
