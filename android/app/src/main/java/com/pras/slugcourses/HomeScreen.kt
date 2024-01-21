package com.pras.slugcourses

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.pras.slugcourses.api.Status
import com.pras.slugcourses.api.Type
import com.pras.slugcourses.ui.elements.LargeDropdownMenu
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun HomeScreen(navController: NavController = rememberNavController()) {
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

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .offset(y = -(56).dp)
    ) {
        Text("Slug Courses", fontSize = 42.sp, fontWeight = FontWeight.SemiBold)
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
                val geList = Json.encodeToString(GEList.value)
                navController.navigate(
                    "results/${termMap[termChosen.value]}/${searchText}/${status}/${type}/${geList}"
                )
            }
        ) { /* do nothing */ }
        Row {
            val termList = termMap.keys.toList()
            var selectedIndex by remember { mutableIntStateOf(0) }
            LargeDropdownMenu(
                modifier = Modifier.padding(32.dp),
                label = "Term",
                items = termList,
                selectedIndex = selectedIndex,
                onItemSelected = { index, _ -> selectedIndex = index },
            )
        }
    }

}