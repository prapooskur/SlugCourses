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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import api.Type
import cafe.adriel.voyager.core.model.rememberNavigatorScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.delay
import ui.elements.BoringNormalTopBar
import kotlinx.coroutines.launch
import ui.data.NavigatorScreenModel
import ui.data.ResultsScreenModel
import ui.elements.CourseCard

private const val TAG = "results"

data class ResultsScreen(
    val term: Int,
    val query: String,
    val type: List<Type>,
    val genEd: List<String>,
    val searchType: String,
) : Screen {
    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    override fun Content() {
        val screenModel = rememberScreenModel { ResultsScreenModel() }
        val uiState by screenModel.uiState.collectAsState()
        val coroutineScope = rememberCoroutineScope()
        val navigator = LocalNavigator.currentOrThrow
        val navScreenModel = navigator.rememberNavigatorScreenModel { NavigatorScreenModel() }
        val database = navScreenModel.uiState.value.database ?: throw Exception()

        val splitQuery = query.contains(" ")
        val department = if (splitQuery) { query.substringBefore(" ") } else { query }
        val courseNumber = if (splitQuery) { query.substringAfter(" ") } else { query }

        val refreshScope = rememberCoroutineScope()

//        val type = listOf(Type.IN_PERSON, Type.ASYNC_ONLINE, Type.SYNC_ONLINE, Type.HYBRID)

        Scaffold(
            contentWindowInsets = WindowInsets(0.dp),
            topBar = {
                BoringNormalTopBar(
                    titleText = if (query.isNotBlank()) "Results for \"${query.trim()}\"" else "Results",
                    navigator = navigator,
                )
            },
            content = { paddingValues ->

                val pullRefreshState = rememberPullRefreshState(
                    uiState.refreshing,
                    onRefresh = {
                        refreshScope.launch {
                            screenModel.setRefresh(true)
                            screenModel.getCourses(
                                term,
                                department,
                                courseNumber,
                                query,
                                type,
                                genEd,
                                searchType
                            )
                            screenModel.getFavorites(database)
                            delay(250)
                        }
                    },
                )

                Box(Modifier.pullRefresh(pullRefreshState).padding(paddingValues)) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (uiState.dataLoaded) {
                            val response = uiState.resultsList
                            // val favorites: Set<String> = database.favoritesQueries.selectAll().executeAsList().toSet()
                            if (response.isNotEmpty()) {
                                items(response.size) { id ->
                                    val course = response[id]
                                    CourseCard(
                                        course = course,
                                        navigator = navigator,
                                        isFavorited = uiState.favoritesList.contains(course.id),
                                        onFavorite = {
                                            coroutineScope.launch {
                                                screenModel.handleFavorite(course, database)
                                            }
                                        }
                                    )
                                }
                            } else {
                                item {
                                    Text(
                                        text = "No classes found.",
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

                    PullRefreshIndicator(
                        uiState.refreshing,
                        pullRefreshState,
                        Modifier.align(Alignment.TopCenter),
                        backgroundColor = MaterialTheme.colorScheme.surfaceBright,
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                }
            }
        )

        LaunchedEffect(Unit) {
            if (!uiState.dataLoaded) {
                screenModel.setRefresh(true)
            }
            screenModel.getCourses(
                term,
                department,
                courseNumber,
                query,
                type,
                genEd,
                searchType
            )
            screenModel.getFavorites(database)
        }

        LaunchedEffect(screenModel.uiState.value.errorMessage) {
            if (screenModel.uiState.value.errorMessage.isNotBlank()) {
                navScreenModel.uiState.value.snackbarHostState.showSnackbar(screenModel.uiState.value.errorMessage)
            }
        }
    }
}