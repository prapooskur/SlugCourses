package ui.data

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.toMutableStateList
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import co.touchlab.kermit.Logger
import com.pras.Database
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.coroutines.cancellation.CancellationException

@Serializable
enum class Author {
    USER,
    SYSTEM,
    FUNCTION
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

const val initMessage = "Hello! I'm SlugBot, your assistant for UCSC courses. How can I help you today?"
data class ChatScreenState(
    val message: String = "",
    val active: Boolean = false,
    val messageList: List<ChatMessage> = listOf(
        ChatMessage(
            initMessage,
            Author.SYSTEM
        )
    ),
    val messageSuggestions: List<String> = listOf(),
    val errorMessage: String = ""
)

private const val TAG = "ChatScreenModel"
private const val CHAT_BASE_URL = "https://gem.arae.me"
private const val CHAT_URL = "$CHAT_BASE_URL/chat/"

class ChatScreenModel : ScreenModel {
    private val _uiState = MutableStateFlow(ChatScreenState())
    val uiState: StateFlow<ChatScreenState> = _uiState.asStateFlow()

    private var currentJob: Job? = null

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
                    initMessage,
                    Author.SYSTEM
                )
            )
        )
    }

    fun sendMessage() {
        currentJob = screenModelScope.launch(Dispatchers.IO) {
            try {
                addMessage(ChatMessage(uiState.value.message, Author.USER))
                clearMessage()
                _uiState.value = _uiState.value.copy(active = true)
                addMessage(ChatMessage("|||", Author.SYSTEM))
                queryChat()
            } catch (e: CancellationException) {
                Logger.d("Generation cancelled", tag = TAG)
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
    }

    private fun addMessage(chatMessage: ChatMessage) {
        _uiState.value = _uiState.value.copy(
            messageList = _uiState.value.messageList.toMutableStateList().apply {
                add(chatMessage)
            }
        )
    }

    fun cancelMessage() {
        // kill ktor request
        currentJob?.cancel()
        currentJob = null
        _uiState.value = _uiState.value.copy(active = false)
    }

    private suspend fun queryChat() {
        val response = mutableStateOf("")

        fun isValidJson(jsonString: String) = runCatching {
            Json.parseToJsonElement(jsonString)
        }.isSuccess

        client.preparePost(CHAT_URL) {
            contentType(ContentType.Application.Json)
            setBody(ChatList(_uiState.value.messageList.toList().dropLast(1)))
        } .execute { httpResponse ->
            val channel: ByteReadChannel = httpResponse.body()
            while (!channel.isClosedForRead) {
                val line = channel.readUTF8Line(99999) ?: continue
                Logger.d("$line\n", tag=TAG)

                if (isValidJson(line)) {
                    _uiState.value = _uiState.value.copy(
                        messageList = _uiState.value.messageList.toMutableStateList().apply {
                            add(_uiState.value.messageList.size-1, ChatMessage(line, Author.FUNCTION))
                        }
                    )
                    print(uiState.value.messageList.toList())
                    continue
                }



                if (line.isBlank()) {
                    response.value+="\n"
                } else {
                    response.value+=line
                    if (!channel.isClosedForRead) {
                        response.value+="\n"
                    }
                }

                _uiState.value = _uiState.value.copy(
                    messageList = _uiState.value.messageList.toMutableStateList().apply {
                        if (_uiState.value.messageList.last().author == Author.SYSTEM) { removeLast() }
                        add(ChatMessage(response.value, Author.SYSTEM))
                    }
                )
            }
            println("message list: "+uiState.value.messageList.toList())
        }
    }

    fun updateSuggestions(database: Database) {
        try {
            val suggestions = database.suggestionsQueries.selectAll().executeAsList()
            // get a random 4 suggestions
            _uiState.value = _uiState.value.copy(messageSuggestions = suggestions.shuffled().take(4))
        } catch (e: Exception) {
            Logger.d("Error getting suggestions: ${e.message}", tag = TAG)
        }
    }

    private companion object {
        val client = HttpClient {
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