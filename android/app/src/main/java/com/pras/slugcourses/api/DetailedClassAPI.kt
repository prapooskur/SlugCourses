package com.pras.slugcourses.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get

data class APIResponse(
    val primarySection: PrimarySection,
    val meetings: Meetings,
    val secondarySections: List<Meetings>,
)

data class PrimarySection(
    val strm: String,
    val class_nbr: String,
    val component: String,
    val acad_career: String,
    val subject: String,
    val class_section: String,
    val capacity: Int,
    val enrl_total: Int,
    val waitlist_capacity: Int,
    val waitlist_total: Int,
    val enrl_status: String,
    val credits: Int,
    val grading: String,
    val title: String,
    val title_long: String,
    val description: String,
    val requirements: String,
    val catalog_nbr: String,
)

data class Meetings (

)

suspend fun APIResponse(term: String, courseNum: String) {
    val client = HttpClient(CIO)
    val url = "https://my.ucsc.edu/PSIGW/RESTListeningConnector/PSFT_CSPRD/SCX_CLASS_DETAIL.v1/${term}/${courseNum}"
    val response = client.get(url)

}