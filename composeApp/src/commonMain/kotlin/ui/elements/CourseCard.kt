package ui.elements

import DetailedResultsScreen
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.Navigator
import api.Course
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import slugcourses.composeapp.generated.resources.*


enum class Status {
    OPEN,
    CLOSED,
    WAITLIST,
}

@OptIn(ExperimentalResourceApi::class)
@Composable
fun CourseCard(course: Course, navigator: Navigator, isFavorited: Boolean, onFavorite: () -> Unit, showFavorite: Boolean = true) {
    //val uri = URLEncoder.encode(course.url, "UTF-8")
    //Log.d("CourseCard", "CourseCard: $uri")
    val status = when(course.status) {
        "Open" -> Status.OPEN
        "Closed" -> Status.CLOSED
        "Wait List" -> Status.WAITLIST
        else -> Status.CLOSED
    }
    Card(onClick = {
            navigator.push(DetailedResultsScreen(course.term.toString(), course.id.substringAfter("_"), url = course.url))
//            navController.navigate("detailed/${course.term}/${course.id.substringAfter("_").toInt()}/${uri}")
        },
        modifier = Modifier
            .padding(6.dp)
            .widthIn(max = 800.dp)
    )

    {
        Box( // Use Box to position elements freely
            modifier = Modifier.fillMaxSize()
        ) {
            Column(modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
            ) {
                SectionTitle("${course.department} ${course.course_number}${course.course_letter} - ${course.section_number}: ${course.short_name}", status)
                SectionSubtitle(Icons.Default.Person, course.instructor)
                if (course.location.contains("Online") || course.location.contains("Remote Instruction")) {
                    SectionSubtitle(painterResource(Res.drawable.videocam), course.location)
                } else {
                    SectionSubtitle(painterResource(Res.drawable.location_on), course.location)
                }
                if (course.time.isNotBlank()) {
                    SectionSubtitle(painterResource(Res.drawable.clock), course.time)
                }
                if (course.alt_location != "None") {
                    if (course.alt_location.contains("Online") || course.alt_location.contains("Remote Instruction")) {
                        SectionSubtitle(painterResource(Res.drawable.videocam), course.alt_location)
                    } else {
                        SectionSubtitle(painterResource(Res.drawable.location_on), course.alt_location)
                    }
                }
                if (course.alt_time != "None") {
                    SectionSubtitle(painterResource(Res.drawable.clock), course.alt_time)
                }
                SectionSubtitle(painterResource(Res.drawable.group), course.enrolled)
                if (course.gen_ed.isNotBlank()) {
                    SectionSubtitle(painterResource(Res.drawable.books), course.gen_ed)
                }
            }
            if (showFavorite) {
                IconButton( // Add star icon with click handling
                    onClick = onFavorite,
                    modifier = Modifier
                        .align(Alignment.BottomEnd) // Position at bottom right
                        .padding(8.dp) // Add some padding
                ) {
                    Icon(
                        if (isFavorited) {
                            painterResource(Res.drawable.star_filled)
                        } else {
                            painterResource(Res.drawable.star)
                        },
                        contentDescription = "Favorite"
                    )
                }
            }
        }
    }
}



@Composable
fun SectionTitle(title: String, status: Status) {
    Row {
        val emoji = when (status) {
            Status.OPEN -> "\uD83D\uDFE2"
            Status.CLOSED -> "\uD83D\uDFE6"
            Status.WAITLIST -> "\uD83D\uDFE1"
        }
        Text(
            text = "$emoji $title",
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp
        )
    }

}

val ROW_PADDING = 4.dp

@Composable
fun SectionSubtitle(icon: ImageVector, subtitle: String) {
    Row {
        Icon(icon,null, modifier = Modifier.padding(end = ROW_PADDING))
        Text(
            text = subtitle,
            fontSize = 16.sp
        )
    }
}

@Composable
fun SectionSubtitle(icon: Painter, subtitle: String) {
    Row {
        Icon(icon,null, modifier = Modifier.padding(end = ROW_PADDING))
        Text(
            text = subtitle,
            fontSize = 16.sp
        )
    }
}