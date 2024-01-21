package com.pras.slugcourses

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.pras.slugcourses.api.Status
import com.pras.slugcourses.api.Type
import com.pras.slugcourses.ui.elements.LargeDropdownMenu
import com.pras.slugcourses.ui.elements.LargeDropdownMenuMultiSelect
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

    val showAdvanced = remember { mutableStateOf(false) }

    val termChosen = remember { mutableStateOf("Winter 2024") }
    val termMap = mapOf(
        "Winter 2024" to "2240",
        "Fall 2023" to "2238",
        "Spring 2023" to "2232",
        "Winter 2023" to "2230",
        "Fall 2022" to "2228"
    )

    val genEdList = listOf("CC", "ER", "IM", "MF", "SI", "SR", "TA", "PE", "PR", "C")
    val selectedGenEdList = remember { mutableStateListOf<String>() }

    val termList = termMap.keys.toList()
    var selectedTermIndex by remember { mutableIntStateOf(0) }

    val typeList = listOf("Hybrid", "Async Online", "Sync Online", "In Person")
    var selectedTypeIndex by remember { mutableIntStateOf(0) }
    val selectedTimeList = remember { mutableStateListOf<String>("Async Online", "Hybrid", "Sync Online", "In Person") }

    var selectedStatusIndex by remember { mutableIntStateOf(1) }


    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .offset(y = 15.dp)
    ) {
        //Text("Slug Courses", fontSize = 42.sp, fontWeight = FontWeight.SemiBold)
        Image(
            painterResource(R.drawable.slug),
            contentDescription = "Slug Courses",
            contentScale = ContentScale.Crop,
            //modifier = Modifier.fillMaxSize(0.4f)
        )
        SearchBar(
            modifier = Modifier.padding(16.dp),
            query = searchText,
            onQueryChange = { searchText = it },
            active = false,
            onActiveChange = { searchActive = it },
            placeholder = { Text("Search for classes...") },
            trailingIcon = { Icon(Icons.Default.Search, contentDescription = "Search icon", modifier = Modifier.clickable {
                if(searchText.isEmpty()) {
                    searchText = " "
                }

                //val snapshotListSerializer = SnapshotListSerializer(String.serializer())
                val status = Json.encodeToString(Status.ALL)
                val classType: List<Type> = selectedTimeList.map { Type.valueOf(it.replace(" ","_").uppercase()) }
                val encodedType = Json.encodeToString(classType)
                val geList = Json.encodeToString(selectedGenEdList.toList())
                val searchType = when (selectedStatusIndex) {
                    0 -> "Open"
                    else -> "All"
                }
                navController.navigate(
                    "results/${termMap[termChosen.value]}/${searchText}/${status}/${encodedType}/${geList}/${searchType}"
                )
            }) },
            onSearch = {
                if(searchText.isEmpty()) {
                    searchText = " "
                }

                //val snapshotListSerializer = SnapshotListSerializer(String.serializer())
                val status = Json.encodeToString(Status.ALL)
                val classType: List<Type> = selectedTimeList.map { Type.valueOf(it.replace(" ","_").uppercase()) }
                val encodedType = Json.encodeToString(classType)
                val geList = Json.encodeToString(selectedGenEdList.toList())
                val searchType = when (selectedStatusIndex) {
                    0 -> "Open"
                    else -> "All"
                }
                navController.navigate(
                    "results/${termMap[termChosen.value]}/${searchText}/${status}/${encodedType}/${geList}/${searchType}"
                )
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
            //var selectedGenEdIndex by remember { mutableIntStateOf(0) }
            LargeDropdownMenuMultiSelect(
                modifier = Modifier
                    .weight(.35f)
                    .padding(start = 8.dp),
                label = "Gen Ed",
                items = genEdList,
                displayLabel = if (selectedGenEdList.size == 1) selectedGenEdList[0].toString() else "Multi",
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
                displayLabel = if (selectedTimeList.size == 1) selectedTimeList[0] else "Multiple",
                selectedItems = selectedTimeList,
                onItemSelected = { index, _ ->
                    selectedTimeList.add(typeList[index])
                },
                onItemRemoved = { _, itemName ->
                    if (itemName in selectedTimeList) {
                        selectedTimeList.remove(itemName)
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
