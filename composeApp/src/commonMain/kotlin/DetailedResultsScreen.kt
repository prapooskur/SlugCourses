import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import api.CourseInfo
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import co.touchlab.kermit.Logger
import ui.elements.BoringNormalTopBar
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import slugcourses.composeapp.generated.resources.Res
import slugcourses.composeapp.generated.resources.open_in_new
import ui.data.DetailedResultsScreenModel

private const val TAG = "DetailedResults"

data class DetailedResultsScreen(
    val term: String,
    val courseNumber: String,
    val url: String
) : Screen {
    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { DetailedResultsScreenModel() }
        val uiState = screenModel.uiState.collectAsState()


        LaunchedEffect(Unit) {
            Logger.d("updating.", tag = TAG)
            screenModel.getCourseInfo(term, courseNumber)
        }

        LaunchedEffect(uiState.value.errorMessage) {
            if (uiState.value.errorMessage.isNotBlank()) {
//                shortToast(uiState.value.errorMessage, context)
                screenModel.resetError()
            }
        }

        val courseInfo = uiState.value.courseInfo
        Scaffold(
            // custom insets necessary to render behind nav bar
            contentWindowInsets = WindowInsets(0.dp),
            topBar = {
                BoringNormalTopBar(
                    titleText = courseInfo.primary_section.title_long,
                    navigator = navigator
                )
            },
            content = { paddingValues ->

                val pullRefreshState = rememberPullRefreshState(
                    uiState.value.refreshing,
                    onRefresh = {
                        Logger.d("updating?", tag = TAG)
                        screenModel.getCourseInfo(term, courseNumber)
                    },
                )

                Box(Modifier.pullRefresh(pullRefreshState).padding(paddingValues)) {
                    LazyColumn(
                        Modifier
//                            .padding(paddingValues)
                            .fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (uiState.value.dataLoaded) {
                            item {
                                Column(Modifier.widthIn(max=800.dp)) {
                                    CourseDetailBox(courseInfo = courseInfo)
                                    CourseDescriptionBox(courseInfo = courseInfo, url = url)
                                    if (courseInfo.primary_section.requirements.isNotEmpty()) {
                                        CourseRequirementsBox(courseInfo = courseInfo)
                                    }
                                    if (courseInfo.notes.isNotEmpty()) {
                                        CourseNotesBox(courseInfo = courseInfo)
                                    }
                                    CourseMeetingsBox(courseInfo = courseInfo)
                                    if (courseInfo.secondary_sections.isNotEmpty()) {
                                        CourseSectionsBox(courseInfo = courseInfo)
                                    }
                                }
                            }
                        } else {
//                            item {
//                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
//                                    CircularProgressIndicator()
//                                }
//                            }
                        }
                    }

                    PullRefreshIndicator(
                        uiState.value.refreshing,
                        pullRefreshState,
                        Modifier.align(Alignment.TopCenter),
                        backgroundColor = MaterialTheme.colorScheme.surfaceBright,
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                }
            }
        )
    }

}

private val ELEV_VALUE = 4.dp
private val PADDING_VALUE = 16.dp

@Composable
fun CourseDetailBox(
    courseInfo: CourseInfo,
) {
    Box(
        Modifier
            .padding(12.dp)
            .fillMaxWidth()
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceColorAtElevation(ELEV_VALUE),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(PADDING_VALUE - 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Course Details",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 24.sp,
                        modifier = Modifier.padding(start = 4.dp, end = 4.dp),
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(top = 4.dp, bottom = 4.dp))
                Row(Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp, end = 4.dp)) {
                        Text("Status: ${courseInfo.primary_section.enrl_status}")
                        Text("Credits: ${courseInfo.primary_section.credits}")
                        if (courseInfo.primary_section.gened.isNotBlank()) {
                            Text("Gen Ed: ${courseInfo.primary_section.gened}")
                        } else {
                            Text("Gen Ed: None")
                        }

                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Enrolled: ${courseInfo.primary_section.enrl_total}", overflow = TextOverflow.Ellipsis)
                        Text("Capacity: ${courseInfo.primary_section.capacity}")
                        if(courseInfo.primary_section.waitlist_capacity != "0") {
                            Text("Waitlisted: ${courseInfo.primary_section.waitlist_total}")
                        } else {
                            Text("Waitlist: Closed")
                        }
                    }
                }

            }
        }
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
fun CourseDescriptionBox(
    courseInfo: CourseInfo, url: String, uriHandler: UriHandler = LocalUriHandler.current,
) {
    Box(
        Modifier
            .padding(12.dp)
            .fillMaxWidth()
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceColorAtElevation(ELEV_VALUE),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(PADDING_VALUE - 4.dp)
            ) {
                Row {
                    Text(
                        text = "Description",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 24.sp,
                        modifier = Modifier.padding(start = 4.dp, end = 4.dp)
                    )
                    Spacer(Modifier.weight(1f))
                    Icon(
                        painterResource(Res.drawable.open_in_new),
                        contentDescription = "Open in new tab",
                        modifier = Modifier.clickable {
                            Logger.d(url, tag = TAG)
//                            val decoded = URLDecoder.decode(url, "UTF-8")
                            val combinedUrl = "https://pisa.ucsc.edu/class_search/$url"
                            if (combinedUrl.isNotEmpty() /* && URLUtil.isValidUrl(url)*/) {
                                uriHandler.openUri(combinedUrl)
                            } else {
                                Logger.e("Invalid URL: $combinedUrl", tag = TAG)
                            }
                        }
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(top = 4.dp, bottom = 4.dp))
                Row(Modifier.fillMaxWidth()) {
                    Text(
                        courseInfo.primary_section.description.trimEnd(),
                        modifier = Modifier.padding(start = 4.dp, end = 4.dp)
                    )
                }

            }
        }
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
fun CourseRequirementsBox(
    courseInfo: CourseInfo
) {
    Box(
        Modifier
            .padding(12.dp)
            .fillMaxWidth()
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceColorAtElevation(ELEV_VALUE),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(PADDING_VALUE - 4.dp)
            ) {
                Row {
                    Text(
                        text = "Requirements",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 24.sp,
                        modifier = Modifier.padding(start = 4.dp, end = 4.dp)
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(top = 4.dp, bottom = 4.dp))
                Row(Modifier.fillMaxWidth()) {
                    Text(
                        courseInfo.primary_section.requirements.trimEnd(),
                        modifier = Modifier.padding(start = 4.dp, end = 4.dp)
                    )
                }

            }
        }
    }
}

@Composable
fun CourseNotesBox(
    courseInfo: CourseInfo
) {
    Box(
        Modifier
            .padding(12.dp)
            .fillMaxWidth()
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceColorAtElevation(ELEV_VALUE),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(PADDING_VALUE - 4.dp)
            ) {
                Row {
                    Text(
                        text = "Class Notes",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 24.sp,
                        modifier = Modifier.padding(start = 4.dp, end = 4.dp)
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(top = 4.dp, bottom = 4.dp))
                for (note in courseInfo.notes) {
                    Row(Modifier.fillMaxWidth()) {
                        Text(
                            note.trimEnd(),
                            modifier = Modifier.padding(start = 4.dp, end = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CourseMeetingsBox(courseInfo: CourseInfo) {
    Box(
        Modifier
            .padding(12.dp)
            .fillMaxWidth()
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceColorAtElevation(ELEV_VALUE),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(PADDING_VALUE - 4.dp)
            ) {
                Row {
                    Text(
                        text = "Meetings",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 24.sp,
                        modifier = Modifier.padding(start = 4.dp, end = 4.dp)
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(top = 4.dp, bottom = 4.dp))
                courseInfo.meetings.forEachIndexed { index, meeting ->
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.fillMaxWidth()) {
                        Text(
                            "Days/Times: ${meeting.days} ${meeting.start_time} - ${meeting.end_time}",
                            modifier = Modifier.padding(start = 4.dp, end = 4.dp)
                        )
                    }
                    Row(Modifier.fillMaxWidth()) {
                        Text(
                            "Location: ${meeting.location}",
                            modifier = Modifier.padding(start = 4.dp, end = 4.dp)
                        )
                    }
                    Row(Modifier.fillMaxWidth()) {
                        Text(
                            if (meeting.instructors.size == 1) {
                                "Instructor: ${meeting.instructors[0].name}"
                            } else {
                                "Instructors: ${meeting.instructors.joinToString(", ") { it.name }}"
                            },
                            modifier = Modifier.padding(start = 4.dp, end = 4.dp)
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    if (index != courseInfo.meetings.lastIndex) {
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
fun CourseSectionsBox(courseInfo: CourseInfo) {
    Box(
        Modifier
            .padding(12.dp)
            .fillMaxWidth()
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceColorAtElevation(ELEV_VALUE),
            shape = MaterialTheme.shapes.medium
        ) {

            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(PADDING_VALUE - 4.dp)
            ) {
                Row {
                    Text(
                        text = "Sections",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 24.sp,
                        modifier = Modifier.padding(start = 4.dp, end = 4.dp)
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(top = 4.dp, bottom = 4.dp))
                courseInfo.secondary_sections.forEachIndexed { index, section ->
                    val emoji = when (section.enrl_status) {
                        "Open" -> "\uD83D\uDFE2"
                        "Waitlisted" -> "\uD83D\uDFE1"
                        else -> "\uD83D\uDFE6"
                    }
                    section.meetings.forEachIndexed { _, meeting ->
                        Spacer(Modifier.height(4.dp))
                        Row(Modifier.fillMaxWidth()) {
                            Text(
                                "Days/Times: ${meeting.days} ${meeting.start_time} - ${meeting.end_time}",
                                modifier = Modifier.padding(start = 4.dp, end = 4.dp)
                            )
                        }
                        Row(Modifier.fillMaxWidth()) {
                            Text(
                                "Location: ${meeting.location}",
                                modifier = Modifier.padding(start = 4.dp, end = 4.dp)
                            )
                        }
                        Row(Modifier.fillMaxWidth()) {
                            Text(
                                if (meeting.instructors.size == 1) {
                                    "Instructor: ${meeting.instructors[0].name}"
                                } else {
                                    "Instructors: ${meeting.instructors.joinToString(", ") { it.name }}"
                                },
                                modifier = Modifier.padding(start = 4.dp, end = 4.dp)
                            )
                        }
                        Row(Modifier.fillMaxWidth()) {
                            Text(
                                "$emoji Enrolled: ${section.enrl_total}/${section.capacity}",
                                modifier = Modifier.padding(start = 4.dp, end = 4.dp)
                            )
                        }

                        Spacer(Modifier.height(4.dp))

                    }
                    if (section.meetings.isEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Row(Modifier.fillMaxWidth()) {
                            Text(
                                "Days/Times: Asynchronous Online",
                                modifier = Modifier.padding(start = 4.dp, end = 4.dp)
                            )
                        }
                        Row(Modifier.fillMaxWidth()) {
                            Text(
                                "Location: Asynchronous Online",
                                modifier = Modifier.padding(start = 4.dp, end = 4.dp)
                            )
                        }
                        Row(Modifier.fillMaxWidth()) {
                            Text(
                                "Instructor: Staff",
                                modifier = Modifier.padding(start = 4.dp, end = 4.dp)
                            )
                        }
                        Row(Modifier.fillMaxWidth()) {
                            Text(
                                "$emoji Enrolled: ${section.enrl_total}/${section.capacity}",
                                modifier = Modifier.padding(start = 4.dp, end = 4.dp)
                            )
                        }

                        Spacer(Modifier.height(4.dp))
                    }
                    if (index != courseInfo.secondary_sections.lastIndex) {
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}


//@Preview
//@Composable
//fun DetailedResultsScreenPreview() {
//    DetailedResultsScreen(
//        term = "2240",
//        courseNumber = "30169",
//        url = ""
//    )
//}