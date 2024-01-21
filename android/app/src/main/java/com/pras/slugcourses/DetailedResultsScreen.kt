package com.pras.slugcourses

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.pras.slugcourses.api.ClassAPIResponse
import com.pras.slugcourses.api.CourseInfo
import com.pras.slugcourses.api.PrimarySection
import com.pras.slugcourses.ui.elements.CollapsingLargeTopBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailedResultsScreen(
    navController: NavController,
    term: String,
    courseNumber: String
) {
    var courseInfo: CourseInfo by remember { mutableStateOf(
        CourseInfo(
            primary_section = PrimarySection(),
            meetings = listOf(),
            secondary_sections = listOf()
        )
    ) }
    var dataLoaded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            courseInfo = ClassAPIResponse(term, courseNumber)
            dataLoaded = true
        }
    }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        rememberTopAppBarState(),
        canScroll = { true }
    )



    Scaffold(
        // custom insets necessary to render behind nav bar
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            CollapsingLargeTopBar(
                titleText = courseInfo.primary_section.title,
                navController = rememberNavController(),
                scrollBehavior = scrollBehavior
            )
        },
        content = {paddingValues ->
            LazyColumn(Modifier.padding(paddingValues)) {
                item {
                    CourseDescriptionBox(courseInfo = courseInfo)
                }
            }
        }

    )
}

@Composable
fun CourseDescriptionBox(courseInfo: CourseInfo) {

}