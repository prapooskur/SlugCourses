import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import api.Type
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import slugcourses.composeapp.generated.resources.Res
import slugcourses.composeapp.generated.resources.slug
import ui.data.HomeScreenModel
import ui.elements.LargeDropdownMenu
import ui.elements.LargeDropdownMenuMultiSelect


class HomeScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalResourceApi::class)
    @Composable
    override fun Content() {
        val navigator: Navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { HomeScreenModel() }
        val uiState by screenModel.uiState.collectAsState()

        val termMap = mapOf(
            "Fall 2024"   to 2248,
            "Summer 2024" to 2244,
            "Spring 2024" to 2242,
            "Winter 2024" to 2240,
            "Fall 2023"   to 2238,
            "Summer 2023" to 2234,
            "Spring 2023" to 2232,
            "Winter 2023" to 2230,
            "Fall 2022"   to 2228,
            "Summer 2022" to 2224
        )

        val genEdList = listOf("CC", "ER", "IM", "MF", "SI", "SR", "TA", "PE", "PR", "C")
//        val selectedGenEdList = remember { mutableStateListOf<String>() }

        val termList = termMap.keys.toList()
        var selectedTermIndex by rememberSaveable { mutableIntStateOf(0) }

        val typeList = listOf("Hybrid", "Async Online", "Sync Online", "In Person")
//        val selectedTypeList = remember { mutableStateListOf("Async Online", "Hybrid", "Sync Online", "In Person") }

        var selectedStatusIndex by rememberSaveable { mutableIntStateOf(1) }
        
        fun searchHandler() {

            val term = termMap.values.toList()[selectedTermIndex]

//            val status = Json.encodeToString(Status.ALL)
            val classType: List<Type> = uiState.selectedTypeList.map { Type.valueOf(it.replace(" ","_").uppercase()) }
//            val encodedType = Json.encodeToString(classType)

            // dear god why is this necessary
            val geList = mutableListOf<String>()
            geList.addAll(uiState.selectedGenEdList)

            val searchType = when (selectedStatusIndex) {
                0 -> "Open"
                else -> "All"
            }

            navigator.push(ResultsScreen(term, uiState.searchQuery, classType, geList, searchType))

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
                    painterResource(Res.drawable.slug),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .widthIn(max = 250.dp)
                        .fillMaxHeight(0.25f)
                )
                SearchBar(
                    modifier = Modifier.padding(16.dp),
                    query = uiState.searchQuery,
                    onQueryChange = { newQuery->
                        screenModel.setQuery(newQuery)
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

                val widthMax = 380.dp

                Row(Modifier.padding(horizontal = 32.dp, vertical = 16.dp).widthIn(max=widthMax)) {
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
                        displayLabel = when (uiState.selectedGenEdList.size) {
                            0 -> ""
                            1 -> uiState.selectedGenEdList[0]
                            genEdList.size -> "All"
                            else -> "Multi"
                        },
                        selectedItems = uiState.selectedGenEdList,
                        onItemSelected = { index, _ ->
                            uiState.selectedGenEdList.add(genEdList[index])
                        },
                        onItemRemoved = { _, itemName ->
                            if (itemName in uiState.selectedGenEdList) {
                                uiState.selectedGenEdList.remove(itemName)
                            }
                        }
                    )
                }

                Row(Modifier.padding(horizontal = 32.dp, vertical = 4.dp).widthIn(max=widthMax)) {
                    LargeDropdownMenuMultiSelect(
                        modifier = Modifier
                            .weight(.5f)
                            .padding(end = 8.dp),
                        label = "Type",
                        items = typeList,
                        displayLabel = when (uiState.selectedTypeList.size) {
                            0 -> ""
                            // without cutting out "Online", async online is too long
                            1 -> uiState.selectedTypeList[0].replace(" Online", "")
                            typeList.size -> "All"
                            else -> "Multiple"
                        },
                        selectedItems = uiState.selectedTypeList,
                        onItemSelected = { index, _ ->
                            uiState.selectedTypeList.add(typeList[index])
                        },
                        onItemRemoved = { _, itemName ->
                            if (itemName in uiState.selectedTypeList) {
                                uiState.selectedTypeList.remove(itemName)
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
            item {
                Spacer(Modifier.padding(bottom=56.dp))
            }
        }
    }
}