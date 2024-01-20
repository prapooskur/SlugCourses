package com.pras.slugcourses

import android.os.Bundle
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
import androidx.compose.material.icons.filled.Favorite
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pras.slugcourses.ui.theme.SlugCoursesTheme

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

const val DELAYTIME: Float = 350F
const val FADETIME: Float = 200F
@Composable
fun Init(startDestination: String) {
    val navController = rememberNavController()
    val index = remember { mutableIntStateOf(0) }


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
            BottomNavigationBar(navController, listOf("home", "chat"), index)
        }
    )
}

@Composable
fun BottomNavigationBar(navController: NavController, items: List<String>, selectedItem: MutableIntState) {
    NavigationBar {
        items.forEachIndexed { index, item ->
            NavigationBarItem(
                icon = { Icon(Icons.Filled.Favorite, contentDescription = item) },
                label = { Text(item) },
                selected = selectedItem.intValue == index,
                onClick = {
                    selectedItem.intValue = index
                    navController.navigate(item) {
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}