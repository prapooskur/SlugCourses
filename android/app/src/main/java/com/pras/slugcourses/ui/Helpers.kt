package com.pras.slugcourses.ui

import android.content.Context
import android.widget.Toast
import com.pras.slugcourses.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.compose.auth.ComposeAuth
import io.github.jan.supabase.compose.auth.googleNativeLogin
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest

fun shortToast(text: String, context: Context) {
    Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
}

fun getSupabaseClient(): SupabaseClient {
    return createSupabaseClient(
        supabaseUrl = BuildConfig.supabaseUrl,
        supabaseKey = BuildConfig.supabaseKey
    ) {
        install(Postgrest)
        install(Auth)
        install(ComposeAuth) {
            googleNativeLogin(serverClientId = BuildConfig.googleWebClientId)
        }
    }
}