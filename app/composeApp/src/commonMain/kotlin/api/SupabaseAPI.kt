package api

import co.touchlab.kermit.Logger
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.postgrest.query.filter.TextSearchType
import kotlinx.serialization.Serializable
import ui.getSupabaseClient

private const val TAG = "SupabaseAPI"
@Serializable
data class Course(
    val id: String,
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
    term: Int = 2248,
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

    val supabase = getSupabaseClient()

    val courseList = supabase.from("courses").select {

        filter {
            eq("term", term)

            if(status == Status.OPEN) {
                eq("status", "Open")
            }
            if(department.isNotBlank()) {
                ilike("department", department)
            }
            if(courseNumber != -1) {
                eq("course_number", courseNumber)
            }
            if(courseLetter.isNotBlank()) {
                ilike("course_letter", courseLetter)
            }
            if(query.isNotBlank()) {
                or {
                    textSearch(
                        column = "short_name",
                        query = query,
                        config = "english",
                        textSearchType = TextSearchType.WEBSEARCH
                    )
                    textSearch(
                        column = "name",
                        query = query,
                        config = "english",
                        textSearchType = TextSearchType.WEBSEARCH
                    )
                }

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

        order(column = "term", order = Order.DESCENDING)
        order(column = "department", order = Order.ASCENDING)
        order(column = "course_number", order = Order.ASCENDING)
        order(column = "course_letter", order = Order.ASCENDING)
        order(column = "section_number", order = Order.ASCENDING)

    }.decodeList<Course>()

    Logger.d(courseList.toString(), tag = TAG)
    return courseList
}

@Serializable
data class Term(val term_id: Int, val term_name: String)
suspend fun getTerms(): List<Term> {
    val supabase = getSupabaseClient()

    val termList = supabase.from("terms").select {
        order(column = "term_id", order = Order.DESCENDING)
    }.decodeList<Term>()

    Logger.d("Terms: $termList", tag = TAG)
    return termList
}