package com.pras.slugcourses.api

import android.util.Log
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.filter.TextSearchType
import io.github.jan.supabase.supabaseJson
import kotlinx.serialization.Serializable
import java.util.Locale.filter

private const val TAG = "SupabaseAPI"
@Serializable
data class Course(
    val id: Int,
    val term: Int,
    val department: String,
    val course_number: String,
    val section_number: Int,
    val instructor: String,
    val description: String,
    val location: String,
    val time: String,
    val alt_location: String,
    val alt_time: String,
    val enrolled: String,
    val summer_session: String,
    val url: String,
    val status: String,
    val type: String,
    val gen_ed: String,
)

suspend fun SupabaseQuery(
    term: Int = 2240,
    department: String = "",
    courseNumber: String = "",
    description: String = "",
    ge: String = "",
    days: String = "",
    acadCareer: String = "",
    asynchronous: Boolean = true,
    hybrid: Boolean = true,
    synchronous: Boolean = true,
    inPerson: Boolean = true
): List<Course> {
    val supabase = createSupabaseClient(
        supabaseUrl = "https://cdmaojsmfcuyscmphhjk.supabase.co",
        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImNkbWFvanNtZmN1eXNjbXBoaGprIiwicm9sZSI6ImFub24iLCJpYXQiOjE3MDU3MTg2MzYsImV4cCI6MjAyMTI5NDYzNn0.q_DWwkjq8F3CG5GjDj0UA21nvfirVZkrGkMkDuKRN7c"
    ) {
        install(Postgrest)
    }
    val courseList = when {
        department.isNotBlank() && courseNumber.isNotBlank() -> {
            supabase.from("courses").select() {
                filter {
                    eq("term", term)
                    eq("department", department)
                    eq("courseNumber", courseNumber)
                }
            }.decodeList<Course>()

        }
        department.isNotBlank() -> {
            supabase.from("courses").select() {
                filter {
                    eq("term", term)
                    eq("department", department)
                }
            }.decodeList<Course>()
        }
        courseNumber.isNotBlank() -> {
            supabase.from("courses").select() {
                filter {
                    eq("term", term)
                    eq("courseNumber", courseNumber)
                }
            }.decodeList<Course>()
        }
        description.isNotBlank() -> {
            supabase.from("courses").select() {
                filter {
                    textSearch(
                        column = "description",
                        query = description,
                        config = "english",
                        textSearchType = TextSearchType.PLAINTO
                    )
                }
            }.decodeList<Course>()
        }
        else -> {
            supabase.from("courses").select() {
                filter {
                    eq("term", term)
                }
            }.decodeList<Course>()
        }
    }
    Log.d(TAG, courseList.toString())
    return courseList


}