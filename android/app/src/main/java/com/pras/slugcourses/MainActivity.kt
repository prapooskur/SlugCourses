package com.pras.slugcourses

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pras.slugcourses.ui.theme.SlugCoursesTheme

private const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val darkTheme = isSystemInDarkTheme()
            DisposableEffect(isSystemInDarkTheme()) {
                val lightScrim = android.graphics.Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
                val darkScrim = android.graphics.Color.argb(0x80, 0x1b, 0x1b, 0x1b)
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT,
                    ) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(
                        lightScrim,
                        darkScrim,
                    ) { darkTheme },
                )
                onDispose {}
            }

            SlugCoursesTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Init("home")
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SlugCoursesTheme {
        //Greeting("Android")
        Init("home")
    }
}

data class BottomNavigationItem(
    val name: String,
    val route: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val iconDescription: String
)

const val DELAYTIME: Float = 350F
const val FADETIME: Float = 200F
@Composable
fun Init(startDestination: String) {
    val navController = rememberNavController()
    val index = remember { mutableIntStateOf(0) }
    val itemList = listOf(
        BottomNavigationItem(
            name = "Home",
            route = "home",
            selectedIcon =  Icons.Filled.Home,
            icon = Icons.Outlined.Home,
            iconDescription = "Home"
        ),
        BottomNavigationItem(
            name = "Chat",
            route = "chat",
            selectedIcon = ImageVector.vectorResource(R.drawable.chat_filled),
            icon = ImageVector.vectorResource(R.drawable.chat_outlined),
            iconDescription = "Favorite"
        )
    )


    Scaffold(
        modifier = Modifier.fillMaxSize(),
        content = { paddingValues ->
            Box(Modifier.padding(paddingValues)) {
                NavHost(
                    navController = navController,
                    startDestination = startDestination,
                    enterTransition = { EnterTransition.None },
                    exitTransition = { ExitTransition.None }
                ) {
                    composable(
                        "home",
                        enterTransition = { fadeIn() },
                        exitTransition = { fadeOut() }
                    ) {
                        HomeScreen(navController = navController)
                    }
                    composable(
                        "chat",
                        enterTransition = { fadeIn() },
                        exitTransition = { fadeOut() }
                    ) {
                        ChatScreen(navController = navController)
                    }
                }
            }
        },
        bottomBar = {
            BottomNavigationBar(navController, itemList, index)
        }
    )
}

@Composable
fun BottomNavigationBar(navController: NavController, items: List<BottomNavigationItem>, selectedItem: MutableIntState) {
    NavigationBar {
        items.forEachIndexed { index, item ->
            NavigationBarItem(
                icon = {
                    if (selectedItem.intValue == index) {
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
                selected = selectedItem.intValue == index,
                onClick = {
                    selectedItem.intValue = index

                    Log.d(TAG, index.toString())
                    Log.d(TAG, selectedItem.intValue.toString())
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}