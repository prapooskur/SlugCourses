package com.pras.slugcourses

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.pras.slugcourses.api.Status
import com.pras.slugcourses.api.Type
import com.pras.slugcourses.ui.data.HomeViewModel
import com.pras.slugcourses.ui.elements.LargeDropdownMenu
import com.pras.slugcourses.ui.elements.LargeDropdownMenuMultiSelect
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun HomeScreen(navController: NavController = rememberNavController()) {

    val viewModel = viewModel<HomeViewModel>()
    val uiState by viewModel.uiState.collectAsState()

    val termMap = mapOf(
        "Spring 2024" to "2242",
        "Winter 2024" to "2240",
        "Fall 2023" to "2238",
        "Summer 2023" to "2234",
        "Spring 2023" to "2232",
        "Winter 2023" to "2230",
        "Fall 2022" to "2228",
        "Summer 2022" to "2224"
    )

    val genEdList = listOf("CC", "ER", "IM", "MF", "SI", "SR", "TA", "PE", "PR", "C")
    val selectedGenEdList = rememberSaveable { mutableStateListOf<String>() }

    val termList = termMap.keys.toList()
    var selectedTermIndex by rememberSaveable { mutableIntStateOf(0) }

    val typeList = listOf("Hybrid", "Async Online", "Sync Online", "In Person")
    val selectedTypeList = rememberSaveable { mutableStateListOf<String>("Async Online", "Hybrid", "Sync Online", "In Person") }

    var selectedStatusIndex by rememberSaveable { mutableIntStateOf(1) }

    fun searchHandler() {


        val term = termMap.values.toList()[selectedTermIndex]

        val status = Json.encodeToString(Status.ALL)
        val classType: List<Type> = selectedTypeList.map { Type.valueOf(it.replace(" ","_").uppercase()) }
        val encodedType = Json.encodeToString(classType)
        val geList = Json.encodeToString(selectedGenEdList.toList())
        val searchType = when (selectedStatusIndex) {
            0 -> "Open"
            else -> "All"
        }

        val navPath = if (uiState.searchQuery.isBlank()) {
            "results/${term}/${status}/${encodedType}/${geList}/${searchType}"
        } else {
            "results/${term}/${uiState.searchQuery}/${status}/${encodedType}/${geList}/${searchType}"
        }


        navController.navigate(navPath)
    }


    LazyColumn(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .offset(y = 15.dp)
    ) {
        item {
            //Text("Slug Courses", fontSize = 42.sp, fontWeight = FontWeight.SemiBold)
            Image(
                painterResource(R.drawable.slug),
                contentDescription = "Slug Courses",
                contentScale = ContentScale.Inside,
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .fillMaxHeight(0.25f)
            )
            SearchBar(
                modifier = Modifier.padding(16.dp),
                query = uiState.searchQuery,
                onQueryChange = { newQuery->
                    viewModel.setQuery(newQuery)
                },
                active = false,
                onActiveChange = { /* do nothing */ },
                placeholder = { Text("Search for classes...") },
                trailingIcon = { Icon(Icons.Default.Search, contentDescription = "Search icon", modifier = Modifier.clickable {
                    searchHandler()
                }) },
                onSearch = {
                    searchHandler()
                }
            ) { /* do nothing */ }

            Row(Modifier.padding(horizontal = 32.dp, vertical = 16.dp)) {
                LargeDropdownMenu(
                    modifier = Modifier
                        .weight(.5f)
                        .padding(end = 8.dp),
                    label = "Term",
                    items = termList,
                    selectedIndex = selectedTermIndex,
                    onItemSelected = { index, _ -> selectedTermIndex = index },
                )
                LargeDropdownMenuMultiSelect(
                    modifier = Modifier
                        .weight(.35f)
                        .padding(start = 8.dp),
                    label = "GE",
                    items = genEdList,
                    displayLabel = when (selectedGenEdList.size) {
                        0 -> ""
                        1 -> selectedGenEdList[0]
                        genEdList.size -> "All"
                        else -> "Multi"
                    },
                    selectedItems = selectedGenEdList,
                    onItemSelected = { index, _ ->
                        selectedGenEdList.add(genEdList[index])
                    },
                    onItemRemoved = { _, itemName ->
                        if (itemName in selectedGenEdList) {
                            selectedGenEdList.remove(itemName)
                        }
                    }
                )
            }

            Row(Modifier.padding(horizontal = 32.dp, vertical = 4.dp)) {
                LargeDropdownMenuMultiSelect(
                    modifier = Modifier
                        .weight(.5f)
                        .padding(end = 8.dp),
                    label = "Type",
                    items = typeList,
                    displayLabel = when (selectedTypeList.size) {
                        0 -> ""
                        // without cutting out "Online", async online is too long
                        1 -> selectedTypeList[0].replace(" Online", "")
                        typeList.size -> "All"
                        else -> "Multiple"
                    },
                    selectedItems = selectedTypeList,
                    onItemSelected = { index, _ ->
                        selectedTypeList.add(typeList[index])
                    },
                    onItemRemoved = { _, itemName ->
                        if (itemName in selectedTypeList) {
                            selectedTypeList.remove(itemName)
                        }
                    }
                )

                LargeDropdownMenu(
                    modifier = Modifier
                        .weight(.35f)
                        .padding(start = 8.dp),
                    label = "Status",
                    items = listOf("Open", "All"),
                    selectedIndex = selectedStatusIndex,
                    onItemSelected = { index, _ -> selectedStatusIndex = index },
                )
            }
        }



    }
}

