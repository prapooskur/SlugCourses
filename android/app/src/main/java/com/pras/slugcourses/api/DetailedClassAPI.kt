package com.pras.slugcourses.api

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private const val TAG = "DetailedClassAPI"

@Serializable
data class CourseInfo(
    val primary_section: PrimarySection,
    val meetings: List<Meeting>,
    val secondary_sections: List<SecondarySection>
)

@Serializable
data class PrimarySection(
    val strm: String = "",
    val session_code: String = "",
    val class_nbr: String = "",
    val component: String = "",
    val acad_career: String = "",
    val subject: String = "",
    val class_section: String = "",
    val start_date: String = "",
    val end_date: String = "",
    val capacity: String = "",
    val enrl_total: String = "",
    val waitlist_capacity: String = "",
    val waitlist_total: String = "",
    val enrl_status: String = "",
    val credits: String = "",
    val grading: String = "",
    val title: String = "",
    val title_long: String = "",
    val description: String = "",
    val gened: String = "",
    val requirements: String = "",
    val catalog_nbr: String = "",
)

@Serializable
data class Meeting(
    val days: String,
    val start_time: String,
    val end_time: String,
    val location: String,
    val instructors: List<Instructor>
)

@Serializable
data class Instructor(
    val cruzid: String,
    val name: String
)

@Serializable
data class SecondarySection(
    val class_section: String,
    val component: String,
    val class_nbr: String,
    val enrl_total: String,
    val capacity: String,
    val waitlist_total: String,
    val waitlist_capacity: String,
    val enrl_status: String,
    val meetings: List<Meeting>
)

suspend fun classAPIResponse(term: String, courseNum: String): CourseInfo {
    val client = HttpClient(CIO)
    val url = "https://my.ucsc.edu/PSIGW/RESTListeningConnector/PSFT_CSPRD/SCX_CLASS_DETAIL.v1/${term}/${courseNum}"
    val response = client.get(url).body<String>()

    val courseInfo: CourseInfo = Json.decodeFromString(response)

    Log.d(TAG, courseInfo.toString())
    return courseInfo
}