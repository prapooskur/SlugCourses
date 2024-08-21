package api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*

private const val TAG = "ChatAPI"

private const val CHAT_BASE_URL = "https://gem.arae.me"
private const val CHAT_URL = "$CHAT_BASE_URL/chat/"
private const val SUGGESTIONS_URL = "$CHAT_BASE_URL/suggestions"

// todo move other chat logic here?
suspend fun getSuggestions(): List<String> {
    val client = HttpClient() {
        install(ContentNegotiation) {
            json()
        }
    }
    val response = client.get(SUGGESTIONS_URL)
    return response.body() ?: emptyList()
}