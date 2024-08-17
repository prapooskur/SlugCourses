import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import api.Course
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
import ui.elements.BoringNormalTopBar
import ui.elements.CourseCard


private const val TAG = "favorites"

class FavoritesScreen : Screen {
    
    @Composable
    override fun Content() {
        val screenModel = rememberScreenModel { FavoritesScreenModel() }
        val uiState by screenModel.uiState.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        val navScreenModel = navigator.rememberNavigatorScreenModel { NavigatorScreenModel() }
        val database = navScreenModel.uiState.value.database ?: throw Exception()
        val coroutineScope = rememberCoroutineScope()

        val favoriteFlow = database.favoritesQueries.selectAll().asFlow().mapToList(Dispatchers.IO).collectAsState(initial = emptySet())

        when (LocalScreenSize.current.width) {
            in 0..599 -> {
                OnePane(
                    screenModel = screenModel,
                    favoriteFlow = favoriteFlow,
                    onRefresh = {
                        screenModel.getFavorites(database)
                    },
                    onSelect = { term, id, url ->
                        navigator.push(DetailedResultsScreen(term, id, url))
                    },
                    onFavorite = { course ->
                        screenModel.handleFavorite(course, database)
                    },
                    onToggleDialog = {
                        screenModel.toggleDeleteDialog()
                    }
                )
            }
            else -> TwoPane(
                screenModel = screenModel,
                favoriteFlow = favoriteFlow,
                onRefresh = {
                    screenModel.getFavorites(database)
                },
                onFavorite = { course ->
                    screenModel.handleFavorite(course, database)
                },
                onToggleDialog = {
                    screenModel.toggleDeleteDialog()
                }
            )
        }

        if (screenModel.uiState.value.showDeleteDialog) {
            DeleteAllDialog(
                onDeleteAll = {
                    screenModel.deleteFavorites(database)
                    screenModel.toggleDeleteDialog()
                },
                onDismiss = {
                    screenModel.toggleDeleteDialog()
                }
            )
        }

        LaunchedEffect(key1 = uiState.errorMessage) {
            if (uiState.errorMessage.isNotEmpty()) {
                val result = navScreenModel.uiState.value.snackbarHostState.showSnackbar(
                    screenModel.uiState.value.errorMessage,
                    actionLabel = "Retry",
                )
                when(result) {
                    SnackbarResult.ActionPerformed -> {
                        coroutineScope.launch {
                            screenModel.setListRefresh(true)
                            screenModel.getFavorites(database)
                            delay(100)
                        }
                    }
                    SnackbarResult.Dismissed -> {
                        /* Handle snackbar dismissed */
                    }
                }
                screenModel.clearError()
            }
        }

        LaunchedEffect(Unit) {
            if (!uiState.listPane.listDataLoaded) {
                screenModel.setListRefresh(true)
            }
            screenModel.getFavorites(database)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
    @Composable
    private fun OnePane(
        screenModel: FavoritesScreenModel,
        favoriteFlow: State<Collection<String>>,
        onRefresh: () -> Unit,
        onSelect: (term: String, id: String, url: String) -> Unit,
        onFavorite: (Course) -> Unit,
        onToggleDialog: () -> Unit
    ) {

        val uiState by screenModel.uiState.collectAsState()

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
                    actions = {
                        IconButton(
                            onClick = {
                                onToggleDialog()
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Clear favorites")
                            }
                    }
                )
            },
            content = { paddingValues ->

                val pullRefreshState = rememberPullRefreshState(
                    uiState.listPane.listRefreshing,
                    onRefresh = {
                        onRefresh()
                    },
                )

                Box(Modifier.pullRefresh(pullRefreshState).padding(paddingValues)) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (uiState.listPane.listDataLoaded) {
                            val response = uiState.listPane.favoritesList
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
                                            onClick = { clickTerm, clickId, clickUrl ->
                                                onSelect(clickTerm, clickId, clickUrl)
                                            },
                                            isFavorited = favoriteFlow.value.contains(course.id),
                                            onFavorite = {
                                                onFavorite(course)
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
                        }
                    }

                    PullRefreshIndicator(
                        uiState.listPane.listRefreshing,
                        pullRefreshState,
                        Modifier.align(Alignment.TopCenter),
                        backgroundColor = MaterialTheme.colorScheme.surfaceBright,
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                }
            }
        )


    }

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    private fun TwoPane(
        screenModel: FavoritesScreenModel,
        favoriteFlow: State<Collection<String>>,
        onRefresh: () -> Unit,
        onFavorite: (Course) -> Unit,
        onToggleDialog: () -> Unit
    ) {
        // Two pane layout (large screens)
        val uiState by screenModel.uiState.collectAsState()

        // todo optimize?
        var selectedTerm by rememberSaveable { mutableStateOf("") }
        var selectedId by rememberSaveable { mutableStateOf("") }
        var selectedUrl by rememberSaveable { mutableStateOf("") }

        Row(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxHeight().weight(0.5f)) {
                OnePane(
                    screenModel = screenModel,
                    favoriteFlow = favoriteFlow,
                    onRefresh = {
                        onRefresh()
                    },
                    onSelect = { term, id, url ->
                        selectedTerm = term
                        selectedId = id
                        selectedUrl = url
                        Logger.d("$selectedTerm $selectedId $selectedUrl", tag = TAG)
                    },
                    onFavorite = {
                        onFavorite(it)
                    },
                    onToggleDialog = {
                        onToggleDialog()
                    }
                )
            }
            Column(Modifier.fillMaxHeight().weight(0.5f)) {
                // detail pane
                Scaffold(
                    topBar = {
                        BoringNormalTopBar(
                            titleText = uiState.detailPane.courseInfo.primary_section.title_long,
                            onBack = { /* no back */ },
                            showBack = false
                        )
                    },
                    content = { paddingValues ->
                        CourseDetailPane(
                            courseInfo = uiState.detailPane.courseInfo,
                            courseUrl = selectedUrl,
                            dataLoaded = uiState.detailPane.detailDataLoaded,
                            refreshing = uiState.detailPane.detailRefreshing,
                            onRefresh = {
                                screenModel.getCourseInfo(selectedTerm, selectedId)
                            },
                            modifier = Modifier.padding(paddingValues)
                        )
                    }
                )
            }
        }

        LaunchedEffect(uiState.listPane.listDataLoaded) {
            if (uiState.listPane.favoritesList.isNotEmpty()) {
                selectedTerm = uiState.listPane.favoritesList.first().term.toString()
                selectedId = uiState.listPane.favoritesList.first().id
                selectedUrl = uiState.listPane.favoritesList.first().url
            }
        }

        LaunchedEffect(selectedTerm, selectedId, selectedUrl) {
            if (uiState.listPane.favoritesList.isNotEmpty()) {
                screenModel.getCourseInfo(selectedTerm, selectedId)
            }
        }
    }

    @Composable
    private fun DeleteAllDialog(onDeleteAll: () -> Unit, onDismiss: () -> Unit) {
        AlertDialog(
            onDismissRequest = {
                onDismiss()
            },
            confirmButton = {
                Button(
                    onClick = { onDeleteAll() }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { onDismiss() }
                ) {
                    Text("Dismiss")
                }
            },
            title = { Text("Clear Favorites") },
            text = { Text("Are you sure you want to delete all favorited courses?") },
        )
    }
    
}
