package com.pras.slugcourses

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.pras.slugcourses.api.CourseInfo
import com.pras.slugcourses.api.PrimarySection
import com.pras.slugcourses.api.classAPIResponse
import com.pras.slugcourses.ui.elements.CollapsingLargeTopBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "DetailedResults"

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
            Log.d(TAG, "term: $term, courseNumber: $courseNumber")
            courseInfo = classAPIResponse(term, courseNumber)
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
            LazyColumn(
                Modifier.padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (dataLoaded) {
                    item {
                        CourseDetailBox(courseInfo = courseInfo)
                        CourseDescriptionBox(courseInfo = courseInfo)
                        CourseMeetingsBox(courseInfo = courseInfo)
                    }
                } else {
                    item {
                        CircularProgressIndicator()
                    }
                }
            }
        }

    )
}

@Composable
fun CourseDetailBox(courseInfo: CourseInfo) {
    Box(Modifier.padding(16.dp)) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Text("Course Details")
        }
    }
}

@Composable
fun CourseDescriptionBox(courseInfo: CourseInfo) {
    Box(Modifier.padding(16.dp)) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Text("Description")
            Divider()
            Text(courseInfo.primary_section.description)
        }
    }
}

@Composable
fun CourseMeetingsBox(courseInfo: CourseInfo) {
    Box(Modifier.padding(16.dp)) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp),
            shape = MaterialTheme.shapes.medium
        ) {

        }
    }
}

@Preview
@Composable
fun DetailedResultsScreenPreview() {
    DetailedResultsScreen(
        navController = rememberNavController(),
        term = "2240",
        courseNumber = "30169"
    )
}