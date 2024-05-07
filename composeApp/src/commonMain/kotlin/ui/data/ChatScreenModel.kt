package ui.data

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import cafe.adriel.voyager.core.model.ScreenModel
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
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

data class ChatScreenState(
    val message: String = "",
    val active: Boolean = true,
    val messageList: SnapshotStateList<ChatMessage> = mutableStateListOf(
        ChatMessage(
            "Hello! I'm SlugBot, your personal assistant for all things UCSC. How can I help you today?",
            Author.SYSTEM
        )
    ),
)

private const val TAG = "ChatScreenModel"
private const val CHAT_BASE_URL = "https://gem.arae.me"
private const val CHAT_URL = "$CHAT_BASE_URL/?userInput="

class ChatScreenModel : ScreenModel {
    private val _uiState = MutableStateFlow(ChatScreenState())
    val uiState: StateFlow<ChatScreenState> = _uiState.asStateFlow()

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
                val message = uiState.value.message
                addMessage(ChatMessage(uiState.value.message, Author.USER))
                //Log.d(TAG, ChatMessage(uiState.value.message, Author.USER).toString())

                clearMessage()
                _uiState.value = _uiState.value.copy(active = true)

                addMessage(ChatMessage("|||", Author.SYSTEM))

                queryChat(message, _uiState.value.messageList)

            }
        } catch (e: Exception) {
            //Log.d(TAG, "Error: ${e.message}")
        } finally {
            _uiState.value = _uiState.value.copy(active = false)
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
                //Log.d(TAG, line)
            }
        }
    }


}