import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import cafe.adriel.voyager.core.model.rememberNavigatorScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ui.data.FavoritesScreenModel
import ui.data.NavigatorScreenModel
import ui.elements.CourseCard


private const val TAG = "favorites"

class FavoritesScreen : Screen {
    
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
    @Composable
    override fun Content() {
        val screenModel = rememberScreenModel { FavoritesScreenModel() }
        val uiState by screenModel.uiState.collectAsState()
        val coroutineScope = rememberCoroutineScope()
        val navigator = LocalNavigator.currentOrThrow
        val navScreenModel = navigator.rememberNavigatorScreenModel { NavigatorScreenModel() }
        val database = navScreenModel.uiState.value.database ?: throw Exception()

        val favoriteFlow = database.favoritesQueries.selectAll().asFlow().mapToList(Dispatchers.IO).collectAsState(initial = emptySet())

        val refreshScope = rememberCoroutineScope()

        val pullRefreshState = rememberPullRefreshState(
            uiState.refreshing,
            onRefresh = {
                refreshScope.launch {
                    Logger.d("updating?", tag = TAG)
                    screenModel.getFavorites(database)
                    delay(250)
                }
            },
            refreshingOffset = 128.dp
        )

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
                Box(Modifier.pullRefresh(pullRefreshState)) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(it),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (uiState.dataLoaded) {
                            val response = uiState.favoritesList
                            if (response.isNotEmpty()) {
                                items(response.size) { id ->
                                    val course = response[id]
                                    AnimatedVisibility(
                                        favoriteFlow.value.contains(course.id),
                                        enter = fadeIn(),
                                        exit = fadeOut() + slideOutVertically() + shrinkVertically()
                                    ) {
                                        CourseCard(
                                            course = course,
                                            navigator = navigator,
                                            isFavorited = favoriteFlow.value.contains(course.id),
                                            onFavorite = {
                                                coroutineScope.launch {
                                                    screenModel.handleFavorite(course, database)
                                                }
                                            }
                                        )
                                    }
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
//                            item {
//                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
//                                    CircularProgressIndicator()
//                                }
//                            }
                        }
                    }

                    PullRefreshIndicator(uiState.refreshing, pullRefreshState, Modifier.align(Alignment.TopCenter))
                }
            }
        )

        LaunchedEffect(key1 = uiState.errorMessage) {
            if (uiState.errorMessage.isNotEmpty()) {
                navScreenModel.uiState.value.snackbarHostState.showSnackbar(screenModel.uiState.value.errorMessage)
            }
        }

        LaunchedEffect(Unit) {
            screenModel.getFavorites(database)
        }
    }
    
}
