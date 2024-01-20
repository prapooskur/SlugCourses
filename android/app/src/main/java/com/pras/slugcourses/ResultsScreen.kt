package com.pras.slugcourses

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.pras.slugcourses.api.Course
import com.pras.slugcourses.api.Status
import com.pras.slugcourses.api.SupabaseQuery
import com.pras.slugcourses.api.Type
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext



@Composable
fun ResultsScreen(
    term: Int,
    query: String,
    status: Status,
    type: List<Type>,
    genEd: List<String>
) {
    var response by remember { mutableStateOf(listOf<Course>()) }
    var dataLoaded by remember { mutableStateOf(false) }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
    ) {
        if (dataLoaded) {
            items(response.size) { course ->
                Text(text = response[course].department + " " + response[course].course_number)
            }
        } else {
            item {
                CircularProgressIndicator()
            }
        }
    }

    val department = query.substringBefore(" ")
    val courseNumber = query.substringAfter(" ")


    val useDepartment = (Regex("^[A-Za-z]{3}[A-Za-z]?\$").matches(department))
    val useCourseNumber = (Regex("\\d{1,3}[a-zA-Z]?").matches(courseNumber))

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            response = SupabaseQuery(
                term = term,
                status = status,
                department = if (useDepartment) department else "",
                courseNumber = if (useCourseNumber) courseNumber else "",
                description = if (!useDepartment && !useCourseNumber) query else "",
                ge = genEd,
            )
            dataLoaded = true
        }
    }
}