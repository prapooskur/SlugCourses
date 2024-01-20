package com.pras.slugcourses

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.pras.slugcourses.api.Course
import com.pras.slugcourses.api.SupabaseQuery

@Composable
fun HomeScreen(navController: NavController) {
    var response by remember { mutableStateOf(listOf<Course>()) }
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        items(response.size) { course ->
            Text(text = response[course].department+" "+response[course].course_number)
        }
    }
    LaunchedEffect(Unit) {
        response = SupabaseQuery(2240)
    }
}