import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import api.Type
import cafe.adriel.voyager.core.model.rememberNavigatorScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import co.touchlab.kermit.Logger
import com.pras.Database
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ui.data.ResultsScreenModel
import ui.data.NavigatorScreenModel
import ui.elements.CourseCard


private const val TAG = "AdaptiveResultsScreen"

data class ResultsScreen(
    val term: Int,
    val query: String,
    val type: List<Type>,
    val genEd: List<String>,
    val searchType: String,
) : Screen {

    private val splitQuery = query.contains(" ")
    private val department = if (splitQuery) { query.substringBefore(" ") } else { query }
    private val courseNumber = if (splitQuery) { query.substringAfter(" ") } else { query }

    @Composable
    override fun Content() {
        val screenModel = rememberScreenModel { ResultsScreenModel() }


        val navigator = LocalNavigator.currentOrThrow
        val navScreenModel = navigator.rememberNavigatorScreenModel { NavigatorScreenModel() }
        val database = navScreenModel.uiState.value.database ?: throw Exception()
        when (LocalScreenSize.current.width) {
            in 0..599 -> OnePane(screenModel, database, navigator)
            else -> TwoPane(screenModel, database, navigator)
        }

        LaunchedEffect(Unit) {
            // todo figure out why i put an if here earlier
            /*if (!screenModel.uiState.value.listPane.listDataLoaded) {
                screenModel.clearError()
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
            }*/

            screenModel.clearError()
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
                navScreenModel.uiState.value.snackbarHostState.showSnackbar(
                    screenModel.uiState.value.errorMessage,
                )
                screenModel.clearError()
            }
        }

    }

    @OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
    @Composable
    private fun OnePane(screenModel: ResultsScreenModel, database: Database, navigator: Navigator) {
        // One pane layout (small screens)
        // Two pane layout (large screens)
        val uiState by screenModel.uiState.collectAsState()
        val coroutineScope = rememberCoroutineScope()

        var searchQuery by rememberSaveable{ mutableStateOf(query) }

        Scaffold(
            contentWindowInsets = WindowInsets(0.dp),
            topBar = {
                SearchTopBar(
                    searchQuery,
                    onQueryChange = { newQuery ->
                        searchQuery = newQuery
                    },
                    onSearch = {
                        searchHandler(screenModel, searchQuery)
                    },
                    onBack = {
                        navigator.pop()
                    },
                )
            },
            content = { paddingValues ->

                val pullRefreshState = rememberPullRefreshState(
                    uiState.listPane.listRefreshing,
                    onRefresh = {
                        coroutineScope.launch {
                            screenModel.clearError()
                            screenModel.getCourses(
                                term,
                                department,
                                courseNumber,
                                searchQuery,
                                type,
                                genEd,
                                searchType
                            )
                            screenModel.getFavorites(database)
                            delay(100)
                        }
                    },
                )

                Box(Modifier.pullRefresh(pullRefreshState).padding(paddingValues)) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (uiState.listPane.listDataLoaded) {
                            val response = uiState.listPane.resultsList
                            if (response.isNotEmpty()) {
                                items(response.size) { id ->
                                    val course = response[id]
                                    CourseCard(
                                        course = course,
                                        onClick = { clickTerm, clickId, clickUrl ->
                                            navigator.push(DetailedResultsScreen(clickTerm, clickId, clickUrl))
                                        },
                                        isFavorited = uiState.listPane.favoritesList.contains(course.id),
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

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
    @Composable
    private fun TwoPane(screenModel: ResultsScreenModel, database: Database, navigator: Navigator) {
        // Two pane layout (large screens)
        val uiState by screenModel.uiState.collectAsState()
        val coroutineScope = rememberCoroutineScope()

        var searchTopBarHeight by remember { mutableStateOf(0.dp) }
        val density = LocalDensity.current  // Get density in composable context

        Row(Modifier.fillMaxSize()) {
            // List pane
            var searchQuery by rememberSaveable{ mutableStateOf(query) }
            Column(Modifier.fillMaxHeight().weight(0.5f)) {
                Scaffold(
                    topBar = {
                        SearchTopBar(
                            searchQuery,
                            onQueryChange = { newQuery ->
                                searchQuery = newQuery
                            },
                            onSearch = {
                                searchHandler(screenModel, searchQuery)
                            },
                            onBack = {
                                navigator.pop()
                            },
                            modifier = Modifier.padding(bottom=8.dp).onGloballyPositioned { coordinates ->
                                // Convert pixels to dp
                                searchTopBarHeight = with(density) {
                                    coordinates.size.height.toDp()
                                }
                            }
                        )
                    },
                    content = { paddingValues ->

                        val pullRefreshState = rememberPullRefreshState(
                            uiState.listPane.listRefreshing,
                            onRefresh = {
                                coroutineScope.launch {
                                    screenModel.clearError()
                                    screenModel.getCourses(
                                        term,
                                        department,
                                        courseNumber,
                                        searchQuery,
                                        type,
                                        genEd,
                                        searchType
                                    )
                                    screenModel.getFavorites(database)
                                    delay(100)
                                }
                            },
                        )

                        Box(Modifier.pullRefresh(pullRefreshState).padding(top = paddingValues.calculateTopPadding())) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (uiState.listPane.listDataLoaded) {
                                    val response = uiState.listPane.resultsList
                                    // val favorites: Set<String> = database.favoritesQueries.selectAll().executeAsList().toSet()
                                    if (response.isNotEmpty()) {
                                        items(response.size) { id ->
                                            val course = response[id]
                                            CourseCard(
                                                course = course,
                                                // no need for parameters here
                                                onClick = { _, _, _ ->
                                                    screenModel.setSelectedId(id)
                                                    screenModel.setSelectedUrl(course.url)
                                                },
                                                isFavorited = uiState.listPane.favoritesList.contains(course.id),
                                                onFavorite = {
                                                    coroutineScope.launch {
                                                        screenModel.handleFavorite(course, database)
                                                    }
                                                },
                                                selected = (id == uiState.detailPane.selectedId)
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

            // Detail pane
            Column(Modifier.fillMaxHeight().weight(0.5f)) {
                val courseInfo = uiState.detailPane.courseInfo
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Column(Modifier.fillMaxSize(), Arrangement.Center) {
                                    Text(
                                        text = courseInfo.primary_section.title_long,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Start,
                                    )
                                }
                            },
                            modifier = Modifier.height(searchTopBarHeight)
                        )
                    },
                    content = { paddingValues ->
                        Box(Modifier.fillMaxSize().padding(top = paddingValues.calculateTopPadding()+8.dp)) {
                            CourseDetailPane(
                                courseInfo = courseInfo,
                                gradesInfo = uiState.detailPane.gradeInfo,
                                courseUrl = uiState.detailPane.selectedUrl,
                                dataLoaded = uiState.detailPane.detailDataLoaded,
                                refreshing = uiState.detailPane.detailRefreshing,
                                onRefresh = {
                                    if (uiState.listPane.resultsList.isNotEmpty()) {
                                        Logger.d("updating detail", tag = TAG)
                                        screenModel.getCourseInfo(
                                            uiState.listPane.resultsList[uiState.detailPane.selectedId].term.toString(),
                                            uiState.listPane.resultsList[uiState.detailPane.selectedId].id.substringAfter("_")
                                        )
                                    } else {
                                        Logger.d("not updating, results empty", tag = TAG)
                                    }
                                },
                            )
                        }
                    }
                )
            }

        }

        fun updateDetailData() {
            if (uiState.listPane.resultsList.isNotEmpty() && uiState.listPane.listDataLoaded) {
                Logger.d("selected "+uiState.listPane.resultsList[uiState.detailPane.selectedId].toString(), tag = TAG)
                screenModel.getCourseInfo(
                    uiState.listPane.resultsList[uiState.detailPane.selectedId].term.toString(),
                    uiState.listPane.resultsList[uiState.detailPane.selectedId].id.substringAfter("_")
                )
                screenModel.setSelectedUrl(uiState.listPane.resultsList[uiState.detailPane.selectedId].url)
            }
        }

        // when a course is selected, update data pane
        LaunchedEffect(uiState.detailPane.selectedId) {
            updateDetailData()
        }

        // when the results list updates, update data pane
        LaunchedEffect(uiState.listPane.resultsList) {
            updateDetailData()
        }
        
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun SearchTopBar(
        searchQuery: String,
        onQueryChange: (String) -> Unit,
        onSearch: (String) -> Unit,
        onBack: () -> Unit,
        modifier: Modifier = Modifier.padding(bottom = 8.dp),
        scrollBehavior: TopAppBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(),
    ) {
        TopAppBar(
            title = {
                SearchBar(
                    inputField = {
                        SearchBarDefaults.InputField(
                            query = searchQuery,
                            onQueryChange = { newQuery ->
                                onQueryChange(newQuery)
                            },
                            onSearch = {
                                onSearch(searchQuery)
                            },
                            expanded = false,
                            onExpandedChange = { /* do nothing */ },
                            enabled = true,
                            placeholder = { Text("Search for classes...") },
                            trailingIcon = { Icon(Icons.Default.Search, contentDescription = "Search icon", modifier = Modifier.clickable {
                                onSearch(searchQuery)
                            }) },
                        )
                    },
                    expanded = false,
                    onExpandedChange = { /* do nothing */ },
                    modifier = Modifier.fillMaxHeight(0.91f).fillMaxWidth()) {}
            },
            navigationIcon = {
                IconButton(
                    onClick = {
                        onBack()
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                    )
                }

            },
            modifier = modifier,
            scrollBehavior = scrollBehavior
        )
    }

    private fun searchHandler(screenModel: ResultsScreenModel, query: String) {
        val splitQuery = query.contains(" ")
        val department = if (splitQuery) { query.substringBefore(" ") } else { query }
        val courseNumber = if (splitQuery) { query.substringAfter(" ") } else { query }

        screenModel.clearError()
        screenModel.getCourses(
            term,
            department,
            courseNumber,
            query,
            type,
            genEd,
            searchType
        )
    }
}

/*
@Preview
@Composable
fun ResultsPreview() {
    ResultsScreen(
        term=2248,
        query="",
        type = listOf(Type.IN_PERSON,Type.HYBRID,Type.SYNC_ONLINE,Type.ASYNC_ONLINE),
        genEd = emptyList(),
        searchType = ""
    )
}
*/
