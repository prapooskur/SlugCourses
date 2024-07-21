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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import api.Type
import cafe.adriel.voyager.core.model.rememberNavigatorScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import co.touchlab.kermit.Logger
import com.pras.Database
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ui.data.NavigatorScreenModel
import ui.data.TwoPaneResultsScreenModel
import ui.elements.BoringNormalTopBar
import ui.elements.CourseCard
import ui.elements.TwoPaneCourseCard

private const val TAG = "twopaneresults"

data class TwoPaneResultsScreen(
    val term: Int,
    val query: String,
    val type: List<Type>,
    val genEd: List<String>,
    val searchType: String,
) : Screen {

    @OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val screenModel = rememberScreenModel { TwoPaneResultsScreenModel() }
        val uiState by screenModel.uiState.collectAsState()
        val coroutineScope = rememberCoroutineScope()
        val navigator = LocalNavigator.currentOrThrow
        val navScreenModel = navigator.rememberNavigatorScreenModel { NavigatorScreenModel() }
        val database = navScreenModel.uiState.value.database ?: throw Exception()

        val splitQuery = query.contains(" ")
        val department = if (splitQuery) { query.substringBefore(" ") } else { query }
        val courseNumber = if (splitQuery) { query.substringAfter(" ") } else { query }

        // todo optimize?
        val refreshScope = rememberCoroutineScope()
        var selectedId by rememberSaveable { mutableIntStateOf(0) }
        var selectedUrl by rememberSaveable { mutableStateOf("") }


        Row(Modifier.fillMaxSize()) {

            // List pane
            var searchQuery by rememberSaveable{ mutableStateOf(query) }
            Column(Modifier.fillMaxHeight().weight(0.5f)) {
                Scaffold(
                    contentWindowInsets = WindowInsets(0.dp),
                    topBar = {
                        TopAppBar(
                            title = {
                                SearchBar(
                                    query = searchQuery,
                                    onQueryChange = { newQuery ->
                                        searchQuery = newQuery
                                    },
                                    active = false,
                                    onActiveChange = { /* do nothing */ },
                                    placeholder = { Text("Search for classes...") },
                                    trailingIcon = { Icon(Icons.Default.Search, contentDescription = "Search icon", modifier = Modifier.clickable {
                                        twoPaneSearchHandler(screenModel, searchQuery)
                                    }) },
                                    onSearch = {
                                        twoPaneSearchHandler(screenModel, searchQuery)
                                    }
                                ) { /* do nothing */ }
                            },
                            navigationIcon = {
                                IconButton(
                                    onClick = {
                                        navigator.pop()
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                    )
                                }
                            },
                            modifier = Modifier.padding(bottom=8.dp)
                        )
                    },
                    content = { paddingValues ->

                        val pullRefreshState = rememberPullRefreshState(
                            uiState.listPane.listRefreshing,
                            onRefresh = {
                                refreshScope.launch {
                                    screenModel.clearError()
                                    screenModel.setListRefresh(true)
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
                                    // val favorites: Set<String> = database.favoritesQueries.selectAll().executeAsList().toSet()
                                    if (response.isNotEmpty()) {
                                        items(response.size) { id ->
                                            val course = response[id]
                                            TwoPaneCourseCard(
                                                course = course,
                                                onClick = {
                                                    // todo figure out what the hell i'm doing here
                                                    selectedId = id
                                                    selectedUrl = course.url
                                                },
                                                selected = (id == selectedId),
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

            // Detail pane
            Column(Modifier.fillMaxHeight().weight(0.5f)) {
                val courseInfo = uiState.detailPane.courseInfo
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    text = courseInfo.primary_section.title_long,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                        )
                    },
                    content = { paddingValues ->

                        val pullRefreshState = rememberPullRefreshState(
                            uiState.detailPane.detailRefreshing,
                            onRefresh = {
                                Logger.d("updating?", tag = TAG)
                                screenModel.getCourseInfo(
                                    screenModel.uiState.value.listPane.resultsList[selectedId].term.toString(),
                                    screenModel.uiState.value.listPane.resultsList[selectedId].id.substringAfter("_")
                                )
                            },
                        )

                        Box(Modifier.pullRefresh(pullRefreshState).padding(paddingValues)) {
                            LazyColumn(
                                Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (uiState.detailPane.detailDataLoaded) {
                                    item {
                                        Column(Modifier.widthIn(max=800.dp)) {
                                            CourseDetailBox(courseInfo = courseInfo)
                                            CourseDescriptionBox(courseInfo = courseInfo, url = selectedUrl)
                                            if (courseInfo.primary_section.requirements.isNotEmpty()) {
                                                CourseRequirementsBox(courseInfo = courseInfo)
                                            }
                                            if (courseInfo.notes.isNotEmpty()) {
                                                CourseNotesBox(courseInfo = courseInfo)
                                            }
                                            CourseMeetingsBox(courseInfo = courseInfo)
                                            if (courseInfo.secondary_sections.isNotEmpty()) {
                                                CourseSectionsBox(courseInfo = courseInfo)
                                            }
                                        }
                                    }
                                }
                            }

                            PullRefreshIndicator(
                                uiState.detailPane.detailRefreshing,
                                pullRefreshState,
                                Modifier.align(Alignment.TopCenter),
                                backgroundColor = MaterialTheme.colorScheme.surfaceBright,
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                )
            }

        }

        // get courses on initial launch
        LaunchedEffect(Unit) {
            if (!uiState.listPane.listDataLoaded) {
                screenModel.setListRefresh(true)
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

        // when a course is selected, update data pane
        LaunchedEffect(selectedId) {
            if (screenModel.uiState.value.listPane.resultsList.isNotEmpty()) {
                Logger.d("selected "+screenModel.uiState.value.listPane.resultsList[selectedId].toString(), tag = TAG)
                screenModel.getCourseInfo(
                    screenModel.uiState.value.listPane.resultsList[selectedId].term.toString(),
                    screenModel.uiState.value.listPane.resultsList[selectedId].id.substringAfter("_")
                )
            }
        }

        LaunchedEffect(uiState.errorMessage) {
            if (uiState.errorMessage.isNotBlank()) {
                // todo add snackbar message
                navScreenModel.uiState.value.snackbarHostState.showSnackbar(
                    screenModel.uiState.value.errorMessage,
                )
                screenModel.clearError()
            }
        }
    }

    private fun twoPaneSearchHandler(screenModel: TwoPaneResultsScreenModel, query: String) {

        val splitQuery = query.contains(" ")
        val department = if (splitQuery) { query.substringBefore(" ") } else { query }
        val courseNumber = if (splitQuery) { query.substringAfter(" ") } else { query }

        screenModel.clearError()
        screenModel.setListRefresh(true)
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