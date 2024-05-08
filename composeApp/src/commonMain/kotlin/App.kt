import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Surface
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberNavigatorScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.FadeTransition
import co.touchlab.kermit.Logger
import com.pras.Database
import com.pras.slugcourses.ChatScreen
import com.pras.slugcourses.FavoritesScreen
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import slugcourses.composeapp.generated.resources.*
import ui.data.NavigatorScreenModel
import ui.theme.SlugCoursesTheme

data class BottomNavigationItem(
    val name: String,
    val route: Screen,
    val icon: Painter,
    val selectedIcon: Painter,
    val iconDescription: String
)

@OptIn(ExperimentalResourceApi::class)
@Composable
@Preview
fun App(driverFactory: DriverFactory) {
    val driver = driverFactory.createDriver()
    val database = Database(driver)

    SlugCoursesTheme {
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
                            Box(Modifier.padding(paddingValues)) {
                                FadeTransition(navigator) { screen ->
                                    // todo see if there's a better way to do this?
                                    screen.Content()
                                }
                                //CurrentScreen()
                            }
                        },
                        bottomBar = {
                            val itemList = listOf(
                                BottomNavigationItem(
                                    name = "Home",
                                    route = HomeScreen(),
                                    selectedIcon = painterResource(Res.drawable.home_filled),
                                    icon = painterResource(Res.drawable.home_outlined),
                                    iconDescription = "Home"
                                ),
                                BottomNavigationItem(
                                    name = "Chat",
                                    route = ChatScreen(),
                                    selectedIcon = painterResource(Res.drawable.chat_filled),
                                    icon = painterResource(Res.drawable.chat_outlined),
                                    iconDescription = "Chat"
                                ),
                                BottomNavigationItem(
                                    name = "Favorites",
                                    route = FavoritesScreen(),
                                    selectedIcon = painterResource(Res.drawable.star_filled),
                                    icon = painterResource(Res.drawable.star),
                                    iconDescription = "Favorite"
                                ),
                            )

                            if (navigator.lastItem is HomeScreen || navigator.lastItem is ChatScreen || navigator.lastItem is FavoritesScreen) {
                                BottomNavigationBar(navigator, itemList)
                            }
                        }
                    )
                }
            }

        }
    }
}



@Composable
fun BottomNavigationBar(navigator: Navigator, items: List<BottomNavigationItem>) {
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