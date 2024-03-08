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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.pras.Database
import com.pras.slugcourses.ui.data.FavoritesViewModel
import com.pras.slugcourses.ui.elements.CourseCard
import com.pras.slugcourses.ui.shortToast
import kotlinx.coroutines.launch


private const val TAG = "favorites"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(navController: NavController, database: Database) {

    val viewModel = viewModel<FavoritesViewModel>()
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Favorites",
                        overflow = TextOverflow.Ellipsis
                    )
                },
            )
        },
        content = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it)
            ) {
                if (uiState.dataLoaded) {
                    val response = uiState.favoritesList
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
                                text = "No favorite classes.",
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

    LaunchedEffect(key1 = uiState.errorMessage) {
        if (uiState.errorMessage.isNotEmpty()) {
            shortToast("An error occurred: ${uiState.errorMessage}", context)
        }
    }

    /*
    LaunchedEffect(key1 = uiState.favoriteMessage) {
        if (uiState.favoriteMessage.isNotEmpty()) {
            shortToast(uiState.favoriteMessage, context)
        }
    }
    */

    LaunchedEffect(Unit) {
        viewModel.getCourses(database.favoritesQueries.selectAll().executeAsList())
    }
}
