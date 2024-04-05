package com.pras.slugcourses.ui.elements

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.pras.slugcourses.R
import com.pras.slugcourses.api.Course
import java.net.URLEncoder


enum class Status {
    OPEN,
    CLOSED,
    WAITLIST,
}

@Composable
fun CourseCard(course: Course, navController: NavController, isFavorited: Boolean, onFavorite: () -> Unit, showFavorite: Boolean = true) {
    val uri = URLEncoder.encode(course.url, "UTF-8")
    //Log.d("CourseCard", "CourseCard: $uri")
    val status = when(course.status) {
        "Open" -> Status.OPEN
        "Closed" -> Status.CLOSED
        "Wait List" -> Status.WAITLIST
        else -> Status.CLOSED
    }
    Card(onClick = {
            navController.navigate("detailed/${course.term}/${course.id.substringAfter("_").toInt()}/${uri}")
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
                    SectionSubtitle(painterResource(R.drawable.videocam), course.location)
                } else {
                    SectionSubtitle(painterResource(R.drawable.location_on), course.location)
                }
                if (course.time.isNotBlank()) {
                    SectionSubtitle(painterResource(R.drawable.clock), course.time)
                }
                if (course.alt_location != "None") {
                    if (course.alt_location.contains("Online") || course.alt_location.contains("Remote Instruction")) {
                        SectionSubtitle(painterResource(R.drawable.videocam), course.alt_location)
                    } else {
                        SectionSubtitle(painterResource(R.drawable.location_on), course.alt_location)
                    }
                }
                if (course.alt_time != "None") {
                    SectionSubtitle(painterResource(R.drawable.clock), course.alt_time)
                }
                SectionSubtitle(painterResource(R.drawable.group), course.enrolled)
                if (course.gen_ed.isNotBlank()) {
                    SectionSubtitle(painterResource(R.drawable.books), course.gen_ed)
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
                        imageVector = if (isFavorited) {
                            ImageVector.vectorResource(R.drawable.star_filled)
                        } else {
                            ImageVector.vectorResource(R.drawable.star)
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