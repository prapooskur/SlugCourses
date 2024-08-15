import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.Surface
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberNavigatorScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.FadeTransition
import co.touchlab.kermit.Logger
import com.pras.Database
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import slugcourses.composeapp.generated.resources.*
import ui.data.NavigatorScreenModel
import ui.theme.SlugCoursesTheme

data class NavigationItem(
    val name: String,
    val route: Screen,
    val icon: Painter,
    val selectedIcon: Painter,
    val iconDescription: String
)

@Composable
expect fun GetScreenSize(): IntSize

val LocalScreenSize = compositionLocalOf<IntSize> { error("No Screen Size Info provided") }

@Composable
@Preview
fun App(driverFactory: DriverFactory) {
    val database: Database = createDatabase(driverFactory)
    SlugCoursesTheme(
        darkTheme = isSystemInDarkTheme(),
        dynamicColor = true,
    ) {
        CompositionLocalProvider(LocalScreenSize provides GetScreenSize()) {

            val navItemList = listOf(
                NavigationItem(
                    name = "Home",
                    route = HomeScreen(),
                    selectedIcon = painterResource(Res.drawable.home_filled),
                    icon = painterResource(Res.drawable.home_outlined),
                    iconDescription = "Home"
                ),
                NavigationItem(
                    name = "Chat",
                    route = ChatScreen(),
                    selectedIcon = painterResource(Res.drawable.chat_filled),
                    icon = painterResource(Res.drawable.chat_outlined),
                    iconDescription = "Chat"
                ),
                NavigationItem(
                    name = "Favorites",
                    route = FavoritesScreen(),
                    selectedIcon = painterResource(Res.drawable.star_filled),
                    icon = painterResource(Res.drawable.star),
                    iconDescription = "Favorite"
                ),
            )

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Navigator(HomeScreen()) { navigator ->
                        val screenModel = navigator.rememberNavigatorScreenModel { NavigatorScreenModel() }
                        screenModel.setDb(database)
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
    return (navigator.lastItem is HomeScreen || navigator.lastItem is ChatScreen || navigator.lastItem is FavoritesScreen)
}

@Composable
fun BottomNavigationBar(navigator: Navigator, items: List<NavigationItem>) {
    fun isSelected(index: Int): Boolean {
        return when(index) {
            0 -> navigator.lastItem is HomeScreen
            1 -> navigator.lastItem is ChatScreen
            else -> navigator.lastItem is FavoritesScreen
        }
    }
    NavigationBar {
        items.forEachIndexed { index, item ->
            Logger.d(item.route.toString(), tag = "BottomBar item")
            Logger.d(navigator.lastItem.toString(), tag = "BottomBar home")
            NavigationBarItem(
                icon = {
                    if (isSelected(index)) {
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
                selected = isSelected(index),
                onClick = {
                    if (!isSelected(index)) {
                        navigator.popUntilRoot()
                        navigator.push(item.route)
                    }
                }
            )
        }
    }
}

@Composable
fun NavRail(navigator: Navigator, items: List<NavigationItem>) {
    fun isSelected(index: Int): Boolean {
        return when(index) {
            0 -> navigator.lastItem is HomeScreen
            1 -> navigator.lastItem is ChatScreen
            else -> navigator.lastItem is FavoritesScreen
        }
    }

    // dense ui, so cut down max width 10%
    // within spec
    NavigationRail(modifier = Modifier.widthIn(max=72.dp)) {
        Column(verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxHeight()) {
            items.forEachIndexed { index, item ->
                Logger.d(item.route.toString(), tag = "BottomBar item")
                Logger.d(navigator.lastItem.toString(), tag = "BottomBar home")
                NavigationRailItem(
                    icon = {
                        if (isSelected(index)) {
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
                    selected = isSelected(index),
                    onClick = {
                        if (!isSelected(index)) {
                            navigator.popUntilRoot()
                            navigator.push(item.route)
                        }
                    }
                )
                Spacer(Modifier.padding(vertical = 4.dp))
            }
        }
    }
}