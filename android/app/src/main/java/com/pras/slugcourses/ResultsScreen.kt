package com.pras.slugcourses

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.pras.slugcourses.api.Course
import com.pras.slugcourses.api.Status
import com.pras.slugcourses.api.Type
import com.pras.slugcourses.api.supabaseQuery
import com.pras.slugcourses.ui.elements.BoringNormalTopBar
import com.pras.slugcourses.ui.elements.CourseCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "results"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(
    navController: NavController,
    term: Int,
    query: String,
    status: Status,
    type: List<Type>,
    genEd: List<String>,
    searchType: String
) {
    var response by remember { mutableStateOf(listOf<Course>()) }
    var dataLoaded by remember { mutableStateOf(false) }

    val uriHandler = LocalUriHandler.current

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        rememberTopAppBarState(),
        canScroll = { true }
    )

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            BoringNormalTopBar(
                titleText = if (query.isNotBlank()) "Results for \"${query.trim()}\"" else "Results",
                navController = navController,
            )
        },
        content = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it)
            ) {
                if (dataLoaded) {
                    if (response.isNotEmpty()) {
                        items(response.size) { course ->
                            CourseCard(
                                course = response[course],
                                uriHandler = uriHandler,
                                navController = navController,
                            )
                        }
                    } else {
                        item {
                            Text(
                                text = "No classes found for your search.",
                                fontSize = 24.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.offset(y = (240).dp)
                            )
                        }
                    }

                } else {
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    )

    val department = query.substringBefore(" ")
    val courseNumber = query.substringAfter(" ")

    val context = LocalContext.current

    LaunchedEffect(Unit) {

        val useDepartment = (Regex("^[A-Za-z]{2}[A-Za-z]?[A-Za-z]?\$").matches(department))
        val useCourseNumber = (Regex("\\d{1,3}[a-zA-Z]?").matches(courseNumber))

        if (useDepartment)  Log.d(TAG, "Using department")
        if (useCourseNumber)  Log.d(TAG, "Using course number")

        try {
            withContext(Dispatchers.IO) {
                response = supabaseQuery(
                    term = term,
                    status = status,
                    department = if (useDepartment) department.uppercase() else "",
                    courseNumber = if (useCourseNumber) courseNumber.uppercase() else "",
                    query = if (!useDepartment && !useCourseNumber) query else "",
                    ge = genEd,
                    asynchronous = type.contains(Type.ASYNC_ONLINE),
                    hybrid = type.contains(Type.HYBRID),
                    synchronous = type.contains(Type.SYNC_ONLINE),
                    inPerson = type.contains(Type.IN_PERSON),
                    searchType = searchType,
                )
                dataLoaded = true
            }
        }  catch (e: Exception) {
            Log.d(TAG, e.toString())
            ShortToast("Error: ${e}", context)
        }
    }
}
