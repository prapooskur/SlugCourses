import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import api.CourseInfo
import api.Grade
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import co.touchlab.kermit.Logger
import io.github.koalaplot.core.ChartLayout
import io.github.koalaplot.core.bar.*
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import io.github.koalaplot.core.util.VerticalRotation
import io.github.koalaplot.core.util.rotateVertically
import io.github.koalaplot.core.util.toString
import io.github.koalaplot.core.xygraph.FloatLinearAxisModel
import io.github.koalaplot.core.xygraph.TickPosition
import io.github.koalaplot.core.xygraph.XYGraph
import io.github.koalaplot.core.xygraph.rememberAxisStyle
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import slugcourses.composeapp.generated.resources.Res
import slugcourses.composeapp.generated.resources.open_in_new
import ui.data.DetailedResultsScreenModel
import ui.elements.BoringNormalTopBar
import kotlin.math.max

private const val TAG = "DetailedResults"

data class DetailedResultsScreen(
    val term: String,
    val courseNumber: String,
    val url: String
) : Screen {
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
        val gradeInfo = uiState.value.gradeInfo

        Scaffold(
            contentWindowInsets = WindowInsets(0.dp),
            topBar = {
                BoringNormalTopBar(
                    titleText = courseInfo.primary_section.title_long,
                    onBack = {
                        navigator.pop()
                    },
                )
            },
            content = { paddingValues ->

                CourseDetailPane(
                    courseInfo = courseInfo,
                    gradesInfo = gradeInfo,
                    courseUrl = url,
                    dataLoaded = uiState.value.dataLoaded,
                    refreshing = uiState.value.refreshing,
                    onRefresh = {
                        Logger.d("updating.", tag = TAG)
                        screenModel.getCourseInfo(term, courseNumber)
                    },
                    modifier = Modifier.padding(paddingValues)
                )

            }
        )
    }
}

private val ELEV_VALUE = 4.dp
private val PADDING_VALUE = 16.dp

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun CourseDetailPane(
    courseInfo: CourseInfo,
    gradesInfo: List<Grade>,
    courseUrl: String,
    dataLoaded: Boolean,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pullRefreshState = rememberPullRefreshState(
        refreshing,
        onRefresh = {
            Logger.d("updating?", tag = TAG)
            onRefresh()
        },
    )

    Box(Modifier.pullRefresh(pullRefreshState).then(modifier)) {
        LazyColumn(
            Modifier.fillMaxSize().widthIn(max = 800.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (dataLoaded) {
                item {
                    CourseDetailBox(courseInfo = courseInfo)
                }
                item {
                    CourseDescriptionBox(courseInfo = courseInfo, url = courseUrl)
                }
                if (courseInfo.primary_section.requirements.isNotEmpty()) {
                    item {
                        CourseRequirementsBox(courseInfo = courseInfo)
                    }
                }
                if (courseInfo.notes.isNotEmpty()) {
                    item {
                        CourseNotesBox(courseInfo = courseInfo)
                    }
                }
                item {
                    CourseMeetingsBox(courseInfo = courseInfo)
                }
                if (courseInfo.secondary_sections.isNotEmpty()) {
                    item {
                        CourseSectionsBox(courseInfo = courseInfo)
                    }
                }
                if (gradesInfo.isNotEmpty()) {
                    item {
                        CourseGradesBox(courseInfo, gradesInfo)
                    }
                }

            }
        }

        PullRefreshIndicator(
            refreshing,
            pullRefreshState,
            Modifier.align(Alignment.TopCenter),
            backgroundColor = MaterialTheme.colorScheme.surfaceBright,
            contentColor = MaterialTheme.colorScheme.primary
        )
    }
}

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
                        if(courseInfo.primary_section.waitlist_capacity != "0" || courseInfo.primary_section.waitlist_total != "0") {
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

@Composable
fun CourseGradesBox(courseInfo: CourseInfo, gradesInfo: List<Grade>) {
    val selectedTerm = remember { mutableIntStateOf(0) }
    val termList = gradesInfo.map { it.term }
    val termMap = mapOf(
        2250 to "Winter 2025",
        2248 to "Fall 2024",
        2244 to "Summer 2024",
        2242 to "Spring 2024",
        2240 to "Winter 2024",
        2238 to "Fall 2023",
        2234 to "Summer 2023",
        2232 to "Spring 2023",
        2230 to "Winter 2023",
        2228 to "Fall 2022",
        2224 to "Summer 2022",
        2222 to "Spring 2022",
        2220 to "Winter 2022",
        2218 to "Fall 2021",
        2214 to "Summer 2021",
        2212 to "Spring 2021",
        2210 to "Winter 2021",
        2208 to "Fall 2020",
        2204 to "Summer 2020",
        2202 to "Spring 2020",
        2200 to "Winter 2020",
        2198 to "Fall 2019",
    )


    Logger.d(termList.toString(), tag="WTF")
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
                        text = "Grade History for ${courseInfo.primary_section.subject} ${courseInfo.primary_section.catalog_nbr} with ${courseInfo.meetings[0].instructors.joinToString(" and ") { it.name.split(",")[0] }}",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 24.sp,
                        modifier = Modifier.padding(start = 4.dp, end = 4.dp)
                    )
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(termList.size) { index ->
                        FilterChip(
                            onClick = {
                                selectedTerm.value = index
                            },
                            label = {
                                Text(termMap[termList[index]] ?: "Unknown: ${termList[index]}")
                            },
                            selected = selectedTerm.value == index,
//                            leadingIcon = if (selectedTerm.value == index) {
//                                {
//                                    Icon(
//                                        imageVector = Icons.Filled.Done,
//                                        contentDescription = "Selected term",
//                                        modifier = Modifier.size(FilterChipDefaults.IconSize)
//                                    )
//                                }
//                            } else {
//                                null
//                            },
                        )
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(top = 4.dp, bottom = 4.dp))
                Row(Modifier.fillMaxWidth().heightIn(max=300.dp)) {
                    BuildGradesChart(courseInfo, gradesInfo[selectedTerm.value])
                }

            }
        }
    }
}

@OptIn(ExperimentalKoalaPlotApi::class)
@Composable
private fun BuildGradesChart(courseInfo: CourseInfo, gradeInfo: Grade) {
    val gradeList = listOf(
        gradeInfo.aPlus,
        gradeInfo.a,
        gradeInfo.aMinus,
        gradeInfo.bPlus,
        gradeInfo.b,
        gradeInfo.bMinus,
        gradeInfo.cPlus,
        gradeInfo.c,
        gradeInfo.cMinus,
        gradeInfo.dPlus,
        gradeInfo.d,
        gradeInfo.dMinus,
        gradeInfo.f,
        gradeInfo.p,
        gradeInfo.np,
//        gradeInfo.s,
//        gradeInfo.u,
        gradeInfo.i,
        gradeInfo.w
    )

    val gradeNameList = listOf(
        "A+",
        "A",
        "A-",
        "B+",
        "B",
        "B-",
        "C+",
        "C",
        "C-",
        "D+",
        "D",
        "D-",
        "F",
        "P",
        "NP",
//    "S",  // Include these if needed
//    "U",
        "I",
        "W",
        "x", // dummy values, should never be shown
        "x",
        "x"
    )

    val barChartEntries = buildList<VerticalBarPlotEntry<Float, Float>> {
        gradeList.forEachIndexed { index, grade ->
            if (index < 5)
                add(DefaultVerticalBarPlotEntry((index + 1).toFloat()-0.3f, DefaultVerticalBarPosition(0f, grade.toFloat())))
        }
    }

    Logger.d("$gradeList ${gradeList.max()}", tag="WTF")

    ChartLayout(
//        modifier = paddingMod,
//        title = { Text("Grade Distribution for ${courseInfo.primary_section.subject}${courseInfo.primary_section.class_nbr}") }
    ) {
        XYGraph(
            xAxisModel = FloatLinearAxisModel(
                range = 0f..gradeList.size.toFloat(),
                minimumMajorTickIncrement = 1f,
                minimumMajorTickSpacing = 10.dp,
                maxViewExtent = 3f,
                minorTickCount = 0
            ),
            yAxisModel = FloatLinearAxisModel(
                0f..max(20,(20 * ((gradeList.max() + 10) / 20))).toFloat(),
                minimumMajorTickIncrement = 1f,
                minorTickCount = 0
            ),
            xAxisStyle = rememberAxisStyle(
                tickPosition = TickPosition.None,
                color = Color.LightGray
            ),
            xAxisLabels = { index ->
                Logger.d("${index.toInt()}", tag="WTF")
                // only display every other label if width is small
                if (index.toInt() > 0 && (index.toInt() % 2 == 0 || LocalScreenSize.current.width.dp > 900.dp)) {
                    Column(Modifier.fillMaxWidth()) {
                        AxisLabel(gradeNameList[index.toInt()-1])
                    }
                }
            },
            xAxisTitle = {},
            yAxisStyle = rememberAxisStyle(tickPosition = TickPosition.Outside),
            yAxisLabels = {
                Text (it.toString(0))
            },
            yAxisTitle = {
                Text(
                    "Students",
                    modifier = Modifier.rotateVertically(VerticalRotation.COUNTER_CLOCKWISE)
                        .padding(bottom = 4.dp)
                )
            },
            verticalMajorGridLineStyle = null
        ) {
            VerticalBarPlot(
                barChartEntries,
                bar = { index ->
                    DefaultVerticalBar(
                        brush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        HoverSurface { Text(gradeNameList[index]+": "+barChartEntries[index].y.yMax.toString(0)) }
                    }
                },
                barWidth = 0.8f
            )
        }
    }
}


@Composable
private fun AxisLabel(label: String, modifier: Modifier = Modifier) {
    Text(
        label,
        color = MaterialTheme.colorScheme.onBackground,
        style = MaterialTheme.typography.bodySmall,
        modifier = modifier,
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun HoverSurface(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        shadowElevation = 2.dp,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.onPrimary,
        modifier = modifier.padding(8.dp)
    ) {
        Box(modifier = Modifier.padding(8.dp)) {
            content()
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