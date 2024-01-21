package com.pras.slugcourses

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
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
    val response: String,
    //val documentList: List<String>,
)

//todo pls fix
private const val CHAT_URL = "http://169.233.234.234:8000/?userInput="
private const val TAG = "ChatScreen"
@Composable
fun ChatScreen() {
    Log.d(TAG, "ChatScreen")
    //todo pls fix
    val message = remember { mutableStateOf("What classes should i take if i want to learn about machine learning? Respond in a spartan tone with little detail.") }
    val sendMessage = remember { mutableStateOf(false) }
    val response = remember { mutableStateOf("") }
    val chatList = remember { mutableStateListOf<ChatMessage>() }
    LaunchedEffect(sendMessage.value) {
        //todo pls fix
        withContext(Dispatchers.IO) {
            if (sendMessage.value) {
                chatList.add(ChatMessage(message.value, Author.USER))
                Log.d(TAG, message.value)
                chatList.add(ChatMessage("|||", Author.SYSTEM))
                queryChat(message.value, chatList)
                message.value = ""
                sendMessage.value = false
            }
        }
    }
    Scaffold(
        content = { paddingValues ->
            val asdf = paddingValues
            LazyColumn(
                Modifier
                    .statusBarsPadding()
                    .padding(4.dp)
                    .padding(bottom = 96.dp)
                    .fillMaxSize()) {
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

suspend fun queryChat(query: String, chatList: SnapshotStateList<ChatMessage>) {
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
    val response = client.get(CHAT_URL + query.encodeURLPath()).body<String>()
    //chatList.add(ChatMessage(response.replace("\\\\n","\n"), Author.SYSTEM))
    //val oldMessage = chatList.last()
    chatList[chatList.size-1] = ChatMessage(response.replace("\\\\n","\n"), Author.SYSTEM)
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
        verticalAlignment = Alignment.Bottom
    ) {
        SearchBar(
            modifier = Modifier
                .weight(.8f)
                .padding(4.dp),
            query = input.value,
            onQueryChange = { input.value = it },
            onSearch = { if (input.value.isNotBlank()) { sendMessage.value = true } },
            active = false,
            onActiveChange = { /* do nothing */ },
        ) { /* do nothing */ }
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
            Icon(Icons.Filled.Send, contentDescription = "Send message")
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

