package com.pras.slugcourses.ui

import android.content.Context
import android.widget.Toast
import com.pras.slugcourses.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.serialization.Serializable

fun shortToast(text: String, context: Context) {
    Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
}

fun getSupabaseClient(): SupabaseClient {
    return createSupabaseClient(
        supabaseUrl = BuildConfig.supabaseUrl,
        supabaseKey = BuildConfig.supabaseKey
    ) {
        install(Postgrest)
    }
}

@Serializable
data class UserId (
    val user_id: String,
    val fcm_token: String,
    val favorites: List<String>
)