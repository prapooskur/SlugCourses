package com.pras.slugcourses

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.pras.slugcourses.api.Course
import com.pras.slugcourses.api.Status
import com.pras.slugcourses.api.Type
import com.pras.slugcourses.api.supabaseQuery
import com.pras.slugcourses.ui.elements.CollapsingLargeTopBar
import com.pras.slugcourses.ui.elements.CourseCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(
    navController: NavController,
    term: Int,
    query: String,
    status: Status,
    type: List<Type>,
    genEd: List<String>
) {
    var response by remember { mutableStateOf(listOf<Course>()) }
    var dataLoaded by remember { mutableStateOf(false) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        rememberTopAppBarState(),
        canScroll = { true })

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            CollapsingLargeTopBar(
                titleText = if (query.isNotBlank()) "Results for \"$query\"" else "Results",
                navController = navController,
                scrollBehavior = scrollBehavior,
            )
        },
        content = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it)
            ) {
                if (dataLoaded) {
                    items(response.size) { course ->
                        CourseCard(
                            course = response[course],
                            uriHandler = LocalUriHandler.current,
                        )
                    }
                } else {
                    item {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    )

    val department = query.substringBefore(" ")
    val courseNumber = query.substringAfter(" ")


    val useDepartment = (Regex("^[A-Za-z]{3}[A-Za-z]?\$").matches(department))
    val useCourseNumber = (Regex("\\d{1,3}[a-zA-Z]?").matches(courseNumber))

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            response = supabaseQuery(
                term = term,
                status = status,
                department = if (useDepartment) department else "",
                courseNumber = if (useCourseNumber) courseNumber else "",
                query = if (!useDepartment && !useCourseNumber) query else "",
                ge = genEd,
            )
            dataLoaded = true
        }
    }
}
