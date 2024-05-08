package ui

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

private const val TAG = "Helpers"

//fun shortToast(text: String, context: Context) {
//    Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
//}

fun getSupabaseClient(): SupabaseClient {
    return createSupabaseClient(
        // todo fixme
        supabaseUrl = "https://cdmaojsmfcuyscmphhjk.supabase.co",
        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImNkbWFvanNtZmN1eXNjbXBoaGprIiwicm9sZSI6ImFub24iLCJpYXQiOjE3MDU3MTg2MzYsImV4cCI6MjAyMTI5NDYzNn0.q_DWwkjq8F3CG5GjDj0UA21nvfirVZkrGkMkDuKRN7c"
    ) {
        install(Postgrest)
    }
}