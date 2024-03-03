package com.pras.slugcourses

import android.util.Log
import android.webkit.URLUtil
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.pras.slugcourses.api.CourseInfo
import com.pras.slugcourses.ui.data.DetailedResultsViewModel
import com.pras.slugcourses.ui.elements.BoringNormalTopBar
import com.pras.slugcourses.ui.shortToast
import java.net.URLDecoder

private const val TAG = "DetailedResults"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailedResultsScreen(
    navController: NavController,
    term: String,
    courseNumber: String,
    url: String
) {

    val viewModel = viewModel<DetailedResultsViewModel>()
    val uiState = viewModel.uiState.collectAsState()

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.getCourseInfo(term, courseNumber)
    }

    LaunchedEffect(uiState.value.errorMessage) {
        if (uiState.value.errorMessage.isNotBlank()) {
            shortToast(uiState.value.errorMessage, context)
            viewModel.resetError()
        }
    }

    val courseInfo = uiState.value.courseInfo
    Scaffold(
        // custom insets necessary to render behind nav bar
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            BoringNormalTopBar(
                titleText = courseInfo.primary_section.title_long,
                navController = navController,
            )
        },
        content = {paddingValues ->
            LazyColumn(
                Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (uiState.value.dataLoaded) {
                    item {
                        CourseDetailBox(courseInfo = courseInfo)
                        CourseDescriptionBox(courseInfo = courseInfo,  url = url)
                        CourseMeetingsBox(courseInfo = courseInfo)
                        if (courseInfo.secondary_sections.isNotEmpty()) {
                            CourseSectionsBox(courseInfo = courseInfo)
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
                        painterResource(id = R.drawable.open_in_new),
                        contentDescription = "Open in new tab",
                        modifier = Modifier.clickable {
                            Log.d(TAG, url)
                            val decoded = URLDecoder.decode(url, "UTF-8")
                            val url = "https://pisa.ucsc.edu/class_search/$decoded"
                            if (url.isNotEmpty() && URLUtil.isValidUrl(url)) {
                                uriHandler.openUri(url)
                            } else {
                                Log.e(TAG, "Invalid URL: $url")
                            }
                        }
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(top = 4.dp, bottom = 4.dp))
                Row(Modifier.fillMaxWidth()) {
                    Text(
                        courseInfo.primary_section.description,
                        modifier = Modifier.padding(start = 4.dp, end = 4.dp)
                    )
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
                    if (index != courseInfo.secondary_sections.lastIndex) {
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}


@Preview
@Composable
fun DetailedResultsScreenPreview() {
    DetailedResultsScreen(
        navController = rememberNavController(),
        term = "2240",
        courseNumber = "30169",
        url = ""
    )
}