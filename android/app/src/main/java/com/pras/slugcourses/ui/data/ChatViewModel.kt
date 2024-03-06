package com.pras.slugcourses.ui.data

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import com.pras.slugcourses.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.prepareGet
import io.ktor.http.encodeURLPath
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

enum class Author {
    USER,
    SYSTEM
}

data class ChatMessage(
    val message: String,
    val author: Author
)

data class ChatViewState(
    val message: String = "",
    val active: Boolean = true,
    val messageList: SnapshotStateList<ChatMessage> = mutableStateListOf(
        ChatMessage(
            "Hello! I'm SlugBot, your personal assistant for all things UCSC. How can I help you today?",
            Author.SYSTEM
        )
    ),
    val messageLoading: Boolean = false
)

private const val TAG = "ChatViewModel"
private const val CHAT_BASE_URL = BuildConfig.chatUrl
private const val CHAT_URL = "${CHAT_BASE_URL}/?userInput="

class ChatViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ChatViewState())
    val uiState: StateFlow<ChatViewState> = _uiState.asStateFlow()

    fun setMessage(message: String) {
        _uiState.value = _uiState.value.copy(message = message)
    }

    private fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = "")
    }

    fun addMessage(chatMessage: ChatMessage) {
        _uiState.value.messageList.add(chatMessage)
    }

    suspend fun sendMessage() {
        try {
            withContext(Dispatchers.IO) {
                addMessage(ChatMessage(uiState.value.message, Author.USER))
                Log.d(TAG, ChatMessage(uiState.value.message, Author.USER).toString())


                addMessage(ChatMessage("|||", Author.SYSTEM))

                queryChat(uiState.value.message, _uiState.value.messageList)
                clearMessage()
                _uiState.value = _uiState.value.copy(active = true)
            }
        } catch (e: Exception) {
            Log.d(TAG, "Error: ${e.message}")
        }
    }

    suspend fun queryChat(query: String, chatList: SnapshotStateList<ChatMessage>) {
        val client = HttpClient(CIO) {
            install(HttpTimeout) {
                requestTimeoutMillis = 60000
            }
        }

        val response = mutableStateOf("")

        client.prepareGet(CHAT_URL + query.encodeURLPath()) .execute { httpResponse ->
            val channel: ByteReadChannel = httpResponse.body()
            while (!channel.isClosedForRead) {
                val line = channel.readUTF8Line(99999) ?: continue

                if (line.isBlank()) {
                    response.value+="\n"
                } else {
                    response.value+=line
                    if (!channel.isClosedForRead) {
                        response.value+="\n"
                    }
                }

                chatList[chatList.size-1] = (ChatMessage(
                    response.value,
                    Author.SYSTEM
                ))
                Log.d(TAG, line)
            }
        }
    }


}