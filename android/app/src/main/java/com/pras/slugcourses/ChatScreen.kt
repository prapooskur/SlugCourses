package com.pras.slugcourses

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.http.encodeURLPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

enum class Author {
    USER,
    SYSTEM
}

data class ChatMessage(
    val message: String,
    val author: Author
)

@Serializable
data class ChatResponse(
    val text: String,
    val document_list: List<HaystackDocument>,
)

@Serializable
data class HaystackDocument(
    val content: String
)

fun ShortToast(text: String, context: Context) {
    Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
}

//todo pls fix
private const val CHAT_URL = "http://169.233.234.234:8000/?userInput="
private const val TAG = "ChatScreen"
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen() {
    Log.d(TAG, "ChatScreen")
    //todo pls fix
    val message = remember { mutableStateOf("What classes should i take if i want to learn about machine learning?") }
    val sendMessage = remember { mutableStateOf(false) }
    val response = remember { mutableStateOf("") }
    // user-facing chat list: contains user input and system responses
    val chatList = remember { mutableStateListOf<ChatMessage>() }
    // internal chat history: contains full history as a single string that can be sent to api for history
    val chatHistory = remember { mutableStateOf("") }
    val context = LocalContext.current
    LaunchedEffect(sendMessage.value) {
        //todo pls fix
        try {
            withContext(Dispatchers.IO) {
                if (sendMessage.value) {
                    addChat(chatList, chatHistory, message.value, Author.USER)
                    //chatList.add(ChatMessage(message.value, Author.USER))
                    Log.d(TAG, message.value)
                    val query = message.value
                    message.value = ""
                    chatList.add(ChatMessage("|||", Author.SYSTEM))
                    queryChat(query, chatList, chatHistory)
                    sendMessage.value = false
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, e.toString())
            ShortToast("Error: ${e.toString()}", context)
        }
    }
    Scaffold(
        content = { paddingValues ->
            LazyColumn(
                Modifier
                    .padding(paddingValues)
                    .padding(horizontal = 8.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Bottom
            ) {
                items(chatList) { chat ->
                    Row(Modifier.padding(top = 4.dp, bottom = 4.dp)) {
                        when (chat.author) {
                            Author.USER -> ChatMessageBubble(chat.message)
                            Author.SYSTEM -> ChatResponseBubble(chat.message, incomplete = (chat.message == "|||"))
                        }
                    }
                }
            }
        },
        bottomBar = {
            ChatMessageBar(message, sendMessage)
        }
    )

}

fun addChat(chatList: SnapshotStateList<ChatMessage>, chatHistory: MutableState<String>, message: String, author: Author) {
    when(author) {
        Author.USER -> {
            chatList.add(ChatMessage(message, Author.USER))
            chatHistory.value += "USER: $message\n"
        }
        Author.SYSTEM -> {
            Log.d(TAG, "ERROR: addChat called with Author.SYSTEM")
        }
    }
}

fun replaceChat(chatList: SnapshotStateList<ChatMessage>, chatHistory: MutableState<String>, documentList: List<HaystackDocument>, message: String, author: Author) {
    when(author) {
        Author.SYSTEM -> {
            chatList[chatList.size-1] = (ChatMessage(message, Author.SYSTEM))
            chatHistory.value += "ASSISTANT: $message\n"
        }
        Author.USER -> {
            Log.d(TAG, "ERROR: addChat called with Author.USER")
        }
    }
}

suspend fun queryChat(query: String, chatList: SnapshotStateList<ChatMessage>, chatHistory: MutableState<String>) {
    //todo pls fix
    val client = HttpClient(CIO) {
        //set timeout to 1 min
        install(HttpTimeout) {
            requestTimeoutMillis = 60000
        }
    }

    var buffer = ""

    /*
    client.prepareGet(CHAT_URL + query.encodeURLPath()) .execute { httpResponse ->
        val channel: ByteReadChannel = httpResponse.body()
        while (!channel.isClosedForRead) {
            val packet = channel.readRemaining(DEFAULT_BUFFER_SIZE.toLong())
            while (!packet.isEmpty) {
                val bytes = packet.readBytes()
                buffer+=String(bytes)
                Log.d(TAG, String(bytes))
            }
        }
        val json = Json {
            ignoreUnknownKeys = true
        }
        val response = json.decodeFromString<ChatResponse>(buffer.replace("\'", "\""))
        chatList.add(ChatMessage(response.response.replace("\\n","\n"), Author.SYSTEM))
        Log.d(TAG, response.response)
    }
    */
    val json = Json {
        ignoreUnknownKeys = true
    }
    val response = client.get(CHAT_URL + chatHistory.value.encodeURLPath())
    val decodedResponse: ChatResponse = json.decodeFromString(response.body())
    replaceChat(chatList, chatHistory, listOf<HaystackDocument>(), decodedResponse.text, Author.SYSTEM)
}


private const val ALPHA_FULL = 1f
private const val ALPHA_DISABLED = 0.38f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatMessageBar(input: MutableState<String>, sendMessage: MutableState<Boolean>) {
    //todo pls fix
    Row(
        Modifier
            .padding(16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            modifier = Modifier
                .weight(.8f),
            value = input.value,
            onValueChange = { input.value = it },
            singleLine = false,
            maxLines = 3,
            shape = RoundedCornerShape(16.dp)
        )
        Button(
            modifier = Modifier
                .weight(0.16f)
                .padding(4.dp)
                .aspectRatio(1f)
                .alpha(
                    if (!sendMessage.value && input.value.isNotBlank()) {
                        ALPHA_FULL
                    } else {
                        ALPHA_DISABLED
                    }
                ),
            onClick = {
                if (!sendMessage.value && input.value.isNotBlank()) {
                    sendMessage.value = true
                }
            },
        ) {
            Icon(Icons.Filled.Send, contentDescription = "Send message", Modifier.fillMaxSize(.8f))
        }
    }
}


private val ChatMessageShape = RoundedCornerShape(20.dp, 4.dp, 20.dp, 20.dp)
private val ChatResponseShape = RoundedCornerShape(4.dp, 20.dp, 20.dp, 20.dp)

@Composable
fun ChatMessageBubble(message: String){
    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxWidth(0.8f),
            horizontalArrangement = Arrangement.End
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = ChatMessageShape
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge.copy(color = LocalContentColor.current),
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun ChatResponseBubble(response: String, incomplete: Boolean){
    Row(horizontalArrangement = Arrangement.Start, modifier = Modifier.fillMaxWidth(0.8f)) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = ChatResponseShape
        ) {
            if (!incomplete) {
                Text(
                    text = response,
                    style = MaterialTheme.typography.bodyLarge.copy(color = LocalContentColor.current),
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                Row(Modifier.fillMaxWidth(), Arrangement.Center, Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.padding(16.dp))
                }

            }

        }
    }
}

