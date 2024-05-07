import androidx.compose.foundation.layout.*
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.*
import androidx.compose.material3.NavigationBar
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.CurrentScreen
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.FadeTransition
import co.touchlab.kermit.Logger
import com.pras.Database
import com.pras.slugcourses.ChatScreen
import com.pras.slugcourses.FavoritesScreen
import org.jetbrains.compose.ui.tooling.preview.Preview
import ui.theme.SlugCoursesTheme

data class BottomNavigationItem(
    val name: String,
    val route: Screen,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val iconDescription: String
)

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
                Navigator(HomeScreen(database)) { navigator ->
                    FadeTransition(navigator)
                    Scaffold(
                        // custom insets necessary to render behind nav bar
                        contentWindowInsets = WindowInsets(0.dp),
                        modifier = Modifier.fillMaxSize(),
                        content = { paddingValues ->
                            Box(Modifier.padding(paddingValues)) {
                                CurrentScreen()
                            }
                        },
                        bottomBar = {
                            val index = remember { mutableIntStateOf(0) }
                            val itemList = listOf(
                                BottomNavigationItem(
                                    name = "Home",
                                    route = HomeScreen(database),
                                    selectedIcon = Icons.Filled.Home,
                                    icon = Icons.Outlined.Home,
                                    iconDescription = "Home"
                                ),
                                BottomNavigationItem(
                                    name = "Chat",
                                    route = ChatScreen(),
                                    selectedIcon = Icons.Filled.Home,
                                    icon = Icons.Outlined.Home,
                                    iconDescription = "Chat"
                                ),
                                BottomNavigationItem(
                                    name = "Favorites",
                                    route = FavoritesScreen(database),
                                    selectedIcon = Icons.Filled.Home,
                                    icon = Icons.Outlined.Home,
                                    iconDescription = "Favorite"
                                ),
                            )

                            if (navigator.lastItem is HomeScreen || navigator.lastItem is ChatScreen || navigator.lastItem is FavoritesScreen) {
                                BottomNavigationBar(navigator, itemList, index)
                            }
                        }
                    )
                }
            }

        }
    }
}



@Composable
fun BottomNavigationBar(navigator: Navigator, items: List<BottomNavigationItem>, selectedItem: MutableIntState) {
    NavigationBar {
        items.forEachIndexed { index, item ->
            Logger.d(item.route.toString(), tag = "BottomBar item")
            Logger.d(navigator.lastItem.toString(), tag = "BottomBar home")
            NavigationBarItem(
                icon = {
                    if (index == selectedItem.intValue) {
                        Icon(
                            imageVector = item.selectedIcon,
                            contentDescription = item.iconDescription
                        )
                    } else {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.iconDescription
                        )
                    }
                },
                label = { Text(item.name) },
                selected = index == selectedItem.intValue,
                onClick = {
                    if (selectedItem.intValue != index) {
                        selectedItem.intValue = index
                        navigator.popUntilRoot()
                        navigator.push(item.route)
                    }
                }
            )
        }
    }
}