import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.Surface
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import api.SettingsRepository
import api.Theme
import cafe.adriel.voyager.core.model.rememberNavigatorScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.FadeTransition
import co.touchlab.kermit.Logger
import com.pras.Database
import dev.theolm.rinku.DeepLink
import dev.theolm.rinku.compose.ext.DeepLinkListener
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import slugcourses.composeapp.generated.resources.*
import ui.data.NavigatorScreenModel
import ui.theme.SlugCoursesTheme
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

data class NavigationItem(
    val name: String,
    val route: Screen,
    val icon: Painter,
    val selectedIcon: Painter,
    val iconDescription: String,
    val enabled: Boolean
)

@Composable
expect fun GetScreenSize(): IntSize

val LocalScreenSize = compositionLocalOf<IntSize> { error("No Screen Size Info provided") }

@OptIn(ExperimentalEncodingApi::class)
@Composable
@Preview
fun App(driverFactory: DriverFactory) {
    val database: Database = createDatabase(driverFactory)

    val settingsRepository = SettingsRepository()

    var deepLink by remember { mutableStateOf<DeepLink?>(null) }
    DeepLinkListener { deepLink = it }



    SlugCoursesTheme(
        darkTheme = when(settingsRepository.getThemeFlow().collectAsState(settingsRepository.getTheme()).value) {
            Theme.DARK -> true
            Theme.LIGHT -> false
            Theme.SYSTEM -> isSystemInDarkTheme()
        },
        dynamicColor = true,
    ) {
        CompositionLocalProvider(LocalScreenSize provides GetScreenSize()) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Navigator(HomeScreen()) { navigator ->
                        val screenModel = navigator.rememberNavigatorScreenModel { NavigatorScreenModel() }
                        screenModel.setDb(database)

                        val settingsRepository = screenModel.getSettingsRepository()
                        val showChat = settingsRepository.getShowChatFlow().collectAsState(initial = settingsRepository.getShowChat()).value
                        val showFavorites = settingsRepository.getShowFavoritesFlow().collectAsState(initial = settingsRepository.getShowFavorites()).value

                        val navItemList = listOf(
                            NavigationItem(
                                name = "Home",
                                route = HomeScreen(),
                                selectedIcon = painterResource(Res.drawable.home_filled),
                                icon = painterResource(Res.drawable.home_outlined),
                                iconDescription = "Home",
                                enabled = true
                            ),
                            NavigationItem(
                                name = "Chat",
                                route = ChatScreen(),
                                selectedIcon = painterResource(Res.drawable.chat_filled),
                                icon = painterResource(Res.drawable.chat_outlined),
                                iconDescription = "Chat",
                                enabled = showChat
                            ),
                            NavigationItem(
                                name = "Favorites",
                                route = FavoritesScreen(),
                                selectedIcon = painterResource(Res.drawable.star_filled),
                                icon = painterResource(Res.drawable.star),
                                iconDescription = "Favorite",
                                enabled = showFavorites
                            ),
                            NavigationItem(
                                name = "Settings",
                                route = SettingsScreen(),
                                selectedIcon = painterResource(Res.drawable.settings_fill),
                                icon = painterResource(Res.drawable.settings),
                                iconDescription = "Settings",
                                enabled = true
                            ),
                        )

                        LaunchedEffect(Unit) {
                            screenModel.updateTerms()
                            screenModel.updateSuggestions()
                        }

                        LaunchedEffect(deepLink) {
                            if (deepLink != null) {
                                Logger.d(deepLink.toString(), tag="deeplink")
                                val uriPattern = """slugcourses://detail/(\d+)/(\d+)/(.*)"""
                                val uriRegex = Regex(uriPattern)
                                if (deepLink?.data?.startsWith("https://pisa.ucsc.edu/class_search/index.php?action=detail&class_data=") == true) {
                                    val url = deepLink?.data?.substring(startIndex = 35) ?: ""
                                    val encodedData = deepLink?.data?.substring(startIndex = 70)?.replace("%3D", "=") ?: ""
                                    val courseDataRaw = Base64.decode(encodedData).decodeToString()

                                    val coursePattern = """:STRM";s:\d+:"(\d+)";.*:CLASS_NBR";s:\d+:"(\d+)""""
                                    val regex = Regex(coursePattern)
                                    val courseMatch = regex.find(courseDataRaw)

                                    val courseData = Pair(courseMatch?.groupValues?.get(1) ?: "", courseMatch?.groupValues?.get(2) ?: "")
                                    Logger.d("$encodedData $courseData", tag="deeplink")
                                    navigator.push(
                                        DetailedResultsScreen(
                                            term=courseData.first,
                                            courseNumber = courseData.second,
                                            url = url
                                        )
                                    )
                                } else if (uriRegex.containsMatchIn(deepLink?.data ?: "")) {
                                    val uriMatch = uriRegex.find(deepLink?.data ?: "")
                                    val courseData = Triple(uriMatch?.groupValues?.get(1) ?: "", uriMatch?.groupValues?.get(2) ?: "", uriMatch?.groupValues?.get(3) ?: "")
                                    navigator.push(
                                        DetailedResultsScreen(
                                            term=courseData.first,
                                            courseNumber = courseData.second,
                                            url = courseData.third
                                        )
                                    )
                                }
                            }
                        }

                        Scaffold(
                            // custom insets necessary to render behind nav bar
                            contentWindowInsets = WindowInsets(0.dp),
                            content = { paddingValues ->

                                Row(Modifier.padding(paddingValues)) {
                                    // nav rail always visible on large screen
                                    if (LocalScreenSize.current.width >= 600) {
                                        NavRail(
                                            navigator = navigator,
                                            items = navItemList
                                        )
                                    }

                                    Box {
                                        FadeTransition(navigator) { screen ->
                                            // todo see if there's a better way to do this?
                                            screen.Content()
                                        }
                                    }
                                }
                            },
                            bottomBar = {
                                if (LocalScreenSize.current.width < 600 && (onTopDestination(navigator))) {
                                    BottomNavigationBar(navigator, navItemList)
                                }
                            },
                            snackbarHost = {
                                SnackbarHost(hostState = screenModel.uiState.value.snackbarHostState)
                            }
                        )

                    }
                }
            }
        }
    }
}

fun onTopDestination(navigator: Navigator): Boolean {
    return (
        navigator.lastItem is HomeScreen ||
        navigator.lastItem is ChatScreen ||
        navigator.lastItem is FavoritesScreen ||
        navigator.lastItem is SettingsScreen
    )
}


private fun isSelected(route: Screen, navigator: Navigator): Boolean { return route.key == navigator.lastItem.key }
@Composable
fun BottomNavigationBar(navigator: Navigator, items: List<NavigationItem>) {
    NavigationBar {
        items.forEach { item ->
            if (item.enabled) {
                Logger.d(item.route.toString(), tag = "BottomBar item")
                Logger.d(navigator.lastItem.toString(), tag = "BottomBar home")
                NavigationBarItem(
                    icon = {
                        if (isSelected(item.route, navigator)) {
                            Icon(
                                painter = item.selectedIcon,
                                contentDescription = item.iconDescription
                            )
                        } else {
                            Icon(
                                painter = item.icon,
                                contentDescription = item.iconDescription
                            )
                        }
                    },
                    label = { Text(item.name) },
                    selected = isSelected(item.route, navigator),
                    onClick = {
                        if (!isSelected(item.route, navigator)) {
                            navigator.popUntilRoot()
                            navigator.push(item.route)
                        }
                    }
                )
            } else {
                Logger.d("skipping ${item.route}", tag="BottomBar home")
            }
        }
    }
}

@Composable
fun NavRail(navigator: Navigator, items: List<NavigationItem>) {
    // dense ui, so cut down max width 10%
    // within spec
    NavigationRail(modifier = Modifier.widthIn(max=72.dp)) {
        Column(verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxHeight()) {
            items.forEachIndexed { index, item ->
                if (item.enabled) {
                    Logger.d(item.route.toString(), tag = "BottomBar item")
                    Logger.d(navigator.lastItem.toString(), tag = "BottomBar home")

                    NavigationRailItem(
                        icon = {
                            if (isSelected(item.route, navigator)) {
                                Icon(
                                    painter = item.selectedIcon,
                                    contentDescription = item.iconDescription
                                )
                            } else {
                                Icon(
                                    painter = item.icon,
                                    contentDescription = item.iconDescription
                                )
                            }
                        },
                        label = { Text(item.name) },
                        selected = isSelected(item.route, navigator),
                        onClick = {
                            if (!isSelected(item.route, navigator)) {
                                navigator.popUntilRoot()
                                navigator.push(item.route)
                            }
                        }
                    )
                    Spacer(Modifier.padding(vertical = 4.dp))
                } else {
                    Logger.d("skipping ${item.route}", tag = "BottomBar home")
                }
            }
        }
    }
}