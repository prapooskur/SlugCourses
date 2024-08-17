package ui.data

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import co.touchlab.kermit.Logger
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
enum class Author {
    USER,
    SYSTEM
}

@Serializable
data class ChatMessage(
    val message: String,
    val author: Author
)

@Serializable
data class ChatList(
    val messages: List<ChatMessage>
)

data class ChatScreenState(
    val message: String = "",
    val active: Boolean = true,
    val messageList: SnapshotStateList<ChatMessage> = mutableStateListOf(
        ChatMessage(
            "Hello! I'm SlugBot, your assistant for UCSC courses. How can I help you today?",
            Author.SYSTEM
        )
    ),
    val errorMessage: String = ""
)

private const val TAG = "ChatScreenModel"
private const val CHAT_BASE_URL = "https://gem.arae.me"
private const val CHAT_URL = "$CHAT_BASE_URL/?userInput="
private const val CHAT_URL_POST = "$CHAT_BASE_URL/chat/"

class ChatScreenModel : ScreenModel {
    private val _uiState = MutableStateFlow(ChatScreenState())
    val uiState: StateFlow<ChatScreenState> = _uiState.asStateFlow()

    fun setMessage(message: String) {
        _uiState.value = _uiState.value.copy(message = message)
    }

    private fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = "")
    }

    fun resetMessages() {
        _uiState.value = _uiState.value.copy(
            message = "",
            messageList = mutableStateListOf(
                ChatMessage(
                    "Hello! I'm SlugBot, your personal assistant for all things UCSC. How can I help you today?",
                    Author.SYSTEM
                )
            )
        )
    }

    fun addMessage(chatMessage: ChatMessage) {
        _uiState.value.messageList.add(chatMessage)
    }

    fun sendMessage() {
        try {
            screenModelScope.launch {
                withContext(Dispatchers.IO) {
//                val message = uiState.value.message
                    addMessage(ChatMessage(uiState.value.message, Author.USER))
                    //Log.d(TAG, ChatMessage(uiState.value.message, Author.USER).toString())

                    clearMessage()
                    _uiState.value = _uiState.value.copy(active = true)

                    addMessage(ChatMessage("|||", Author.SYSTEM))

                    //queryChat(message, _uiState.value.messageList)
                    queryChatPost(_uiState.value.messageList)

                }
            }
        } catch (e: Exception) {
            Logger.d("Error: ${e.message}", tag = TAG)
            _uiState.value = _uiState.value.copy(
                errorMessage = e.message ?: "error: no exception message",
                messageList = _uiState.value.messageList.dropLast(1).toMutableStateList()
            )
        } finally {
            _uiState.value = _uiState.value.copy(active = false)
        }
    }

//    suspend fun queryChat(query: String, chatList: SnapshotStateList<ChatMessage>) {
//        val response = mutableStateOf("")
//
//        client.prepareGet(CHAT_URL + query.encodeURLPath()) .execute { httpResponse ->
//            val channel: ByteReadChannel = httpResponse.body()
//            while (!channel.isClosedForRead) {
//                val line = channel.readUTF8Line(99999) ?: continue
//
//                if (line.isBlank()) {
//                    response.value+="\n"
//                } else {
//                    response.value+=line
//                    if (!channel.isClosedForRead) {
//                        response.value+="\n"
//                    }
//                }
//
//                chatList[chatList.lastIndex] = (ChatMessage(
//                    response.value,
//                    Author.SYSTEM
//                ))
//                //Log.d(TAG, line)
//            }
//        }
//    }

    suspend fun queryChatPost(chatList: SnapshotStateList<ChatMessage>) {
        val response = mutableStateOf("")

        client.preparePost(CHAT_URL_POST) {
            contentType(ContentType.Application.Json)
            setBody(ChatList(chatList.toList().dropLast(1)))
        } .execute { httpResponse ->
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

                chatList[chatList.lastIndex] = (ChatMessage(
                    response.value,
                    Author.SYSTEM
                ))
                //Log.d(TAG, line)
            }
        }
    }

    private companion object {
        val client = HttpClient() {
            install(HttpTimeout) {
                connectTimeoutMillis = 60000
                socketTimeoutMillis = 10000
            }
            install(ContentNegotiation) {
                json()
            }
        }
    }
}