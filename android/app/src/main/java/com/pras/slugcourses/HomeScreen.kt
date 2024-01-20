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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.pras.slugcourses.api.Course
import com.pras.slugcourses.api.SupabaseQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun HomeScreen(navController: NavController) {
    var response by remember { mutableStateOf(listOf<Course>()) }
    var dataLoaded by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    LazyColumn(
        modifier = Modifier.fillMaxSize()
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
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            response = SupabaseQuery(2240)
            dataLoaded = true
        }
    }
}