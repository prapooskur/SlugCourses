package com.pras.slugcourses

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.pras.slugcourses.api.CourseInfo
import com.pras.slugcourses.ui.elements.CollapsingLargeTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailedResultsScreen(courseInfo: CourseInfo) {
    val courseName = "${courseInfo.primary_section.subject} ${courseInfo.primary_section.catalog_nbr}"
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        rememberTopAppBarState(),
        canScroll = { true })

    Scaffold(
        topBar = { CollapsingLargeTopBar(titleText = courseName, navController = rememberNavController(), scrollBehavior = scrollBehavior) },
        content = {paddingValues ->
            Box(Modifier.padding(paddingValues)) {

            }
        }

    )
}

@Composable
fun CourseDescriptionBox(courseInfo: CourseInfo) {

}