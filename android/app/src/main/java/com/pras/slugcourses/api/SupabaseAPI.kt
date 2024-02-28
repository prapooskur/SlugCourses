package com.pras.slugcourses.api

import android.util.Log
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.postgrest.query.filter.TextSearchType
import kotlinx.serialization.Serializable

private const val TAG = "SupabaseAPI"
@Serializable
data class Course(
    val id: Int,
    val term: Int,
    val department: String,
    val course_number: Int,
    val course_letter: String,
    val section_number: String,
    val instructor: String,
    val short_name: String,
    val name: String,
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


enum class Status {
    OPEN,
    CLOSED,
    WAITLIST,
    ALL
}


@Serializable
enum class Type {
    ASYNC_ONLINE,
    SYNC_ONLINE,
    HYBRID,
    IN_PERSON,
}

suspend fun supabaseQuery(
    term: Int = 2240,
    status: Status = Status.ALL,
    department: String = "",
    courseNumber: Int = -1,
    courseLetter: String = "",
    query: String = "",
    ge: List<String> = listOf(),
    days: String = "",
    acadCareer: String = "",
    asynchronous: Boolean = true,
    hybrid: Boolean = true,
    synchronous: Boolean = true,
    inPerson: Boolean = true,
    searchType: String = "All",
): List<Course> {
    val supabase = createSupabaseClient(
        supabaseUrl = "https://cdmaojsmfcuyscmphhjk.supabase.co",
        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImNkbWFvanNtZmN1eXNjbXBoaGprIiwicm9sZSI6ImFub24iLCJpYXQiOjE3MDU3MTg2MzYsImV4cCI6MjAyMTI5NDYzNn0.q_DWwkjq8F3CG5GjDj0UA21nvfirVZkrGkMkDuKRN7c"
    ) {
        install(Postgrest)
    }


    val courseList = supabase.from("courses").select {

        filter {
            eq("term", term)

            if(status == Status.OPEN) {
                eq("status", "Open")
            }
            if(department.isNotBlank()) {
                //val upperdept = department.uppercase()
                //eq("department", upperdept)

                ilike("department", department)
            }
            if(courseNumber != -1) {
                eq("course_number", courseNumber)
            }
            if(courseLetter.isNotBlank()) {
                //val upperLetter = courseLetter.uppercase()
                //eq("course_letter", upperLetter)

                ilike("course_letter", courseLetter)
            }
            if(query.isNotBlank()) {
                textSearch(
                    column = "name",
                    query = query,
                    config = "english",
                    textSearchType = TextSearchType.NONE
                )
            }
            if(ge.isNotEmpty()) {
                isIn("gen_ed", ge)
            }
            if(!asynchronous) {
                filterNot("type", FilterOperator.NEQ, "Asynchronous Online")
            }
            if(!hybrid) {
                filterNot("type", FilterOperator.NEQ, "Hybrid")
            }
            if (!synchronous) {
                filterNot("type", FilterOperator.NEQ, "Synchronous Online")
            }
            if (!inPerson) {
                filterNot("type", FilterOperator.NEQ, "In Person")
            }
            if (searchType == "Open") {
                filter("status", FilterOperator.EQ, "Open")
            }
        }



        if (department.isNotBlank()) {
            order(column = "course_letter", order = Order.ASCENDING)
            order(column = "course_number", order = Order.ASCENDING)
        } else {
            order(column = "department", order = Order.ASCENDING)
        }

        //order(column = "course_number", order = Order.ASCENDING)
        //order(column = "course_letter", order = Order.ASCENDING)

    }.decodeList<Course>()

    Log.d(TAG, courseList.toString())
    return courseList
}