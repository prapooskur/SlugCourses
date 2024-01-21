package com.pras.slugcourses.ui.elements

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pras.slugcourses.R
import com.pras.slugcourses.api.Course

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseCard(course: Course, uriHandler: UriHandler) {
    val uri = "https://pisa.ucsc.edu/class_search/"+course.url
    Card(onClick = { uriHandler.openUri(uri) },modifier = Modifier.padding(6.dp).widthIn(max=800.dp)) {
        Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            SectionTitle("${course.department} ${course.course_number} - ${course.section_number}: ${course.short_name}")
            SectionSubtitle(Icons.Default.Person, course.instructor)
            if (course.location.contains("Online") || course.location.contains("Remote Instruction")) {
                SectionSubtitle(painterResource(R.drawable.chat_filled), course.location)
            } else {
                SectionSubtitle(painterResource(R.drawable.chat_filled), course.location)
            }
            SectionSubtitle(painterResource(R.drawable.chat_filled), course.time)
            if (course.alt_location != "Not Found") {
                if (course.alt_location.contains("Online") || course.alt_location.contains("Remote Instruction")) {
                    SectionSubtitle(painterResource(R.drawable.chat_filled), course.alt_location)
                } else {
                    SectionSubtitle(painterResource(R.drawable.chat_filled), course.alt_location)
                }
            }
            if (course.alt_time != "Not Found") {
                SectionSubtitle(painterResource(R.drawable.chat_filled), course.alt_time)
            }
            SectionSubtitle(painterResource(R.drawable.chat_filled), course.enrolled)
        }
    }
}



@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp
    )
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

@Preview
@Composable
fun CoursePreview() {
    CourseCard(
        Course(
            1,
            2240,
            "Patrick Tantalo",
            "Remote Instruction",
            "MWF 10:00AM - 10:50AM",
            "Remote Instruction",
            "MWF 10:00AM - 10:50AM",
            "0",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            ""
        ),
        object : UriHandler {
            override fun openUri(uri: String) {

            }
        }
    )
}