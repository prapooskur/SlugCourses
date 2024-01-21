package com.pras.slugcourses

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SearchBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.pras.slugcourses.api.Status
import com.pras.slugcourses.api.Type
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    var searchText by rememberSaveable { mutableStateOf("") }
    var searchActive by rememberSaveable { mutableStateOf(false) }

    val searchAsyncOnline = rememberSaveable { mutableStateOf(true) }
    val searchHybrid = rememberSaveable { mutableStateOf(true) }
    val searchSynchOnline = rememberSaveable { mutableStateOf(true) }
    val searchInPerson = rememberSaveable { mutableStateOf(true) }

    val searchOpen = rememberSaveable { mutableStateOf(false) }

    val termChosen = remember { mutableStateOf("Winter 2024") }
    val GEList = remember { mutableStateOf(listOf<String>()) }

    val showAdvanced = remember { mutableStateOf(false) }

    val termMap = mapOf(
        "Winter 2024" to "2240",
        "Fall 2023" to "2238",
        "Spring 2023" to "2232",
        "Winter 2023" to "2230",
        "Fall 2022" to "2228"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {
        SearchBar(
            query = searchText,
            onQueryChange = { searchText = it },
            active = false,
            onActiveChange = { searchActive = it },
            onSearch = {
                if(searchText.isEmpty()) {
                    searchText = " "
                }
                val status = Json.encodeToString(Status.ALL)
                val type = Json.encodeToString(listOf(Type.HYBRID, Type.ASYNC, Type.IN_PERSON, Type.SYNC))
                val gelist = Json.encodeToString(GEList.value)
                navController.navigate(
                    "results/${termMap[termChosen.value]}/${searchText}/${status}/${type}/${gelist}"
                )
            }
        ) { /* do nothing */ }
    }

}