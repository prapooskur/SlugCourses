package com.pras.slugcourses

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pras.slugcourses.ui.data.Author
import com.pras.slugcourses.ui.data.ChatViewModel
import com.pras.slugcourses.ui.shortToast


private const val TAG = "ChatScreen"

@Composable
fun ChatScreen() {
    Log.d(TAG, "ChatScreen")
    val sendMessage = remember { mutableStateOf(false) }
    val context = LocalContext.current

    val viewModel = viewModel<ChatViewModel>()
    val uiState = viewModel.uiState.collectAsState()

    LaunchedEffect(sendMessage.value) {
        if (sendMessage.value) {
            try {
                viewModel.sendMessage()
            } catch (e: Exception) {
                Log.d(TAG, e.toString())
                shortToast(e.message ?: "error: no exception message", context)
            } finally {
                sendMessage.value = false
            }
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
                items(uiState.value.messageList) { chat ->
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
            ChatMessageBar(viewModel, sendMessage)
        }
    )

}

private const val ALPHA_FULL = 1f
private const val ALPHA_DISABLED = 0.38f

@Composable
fun ChatMessageBar(viewModel: ChatViewModel, sendMessage: MutableState<Boolean>) {
    val uiState = viewModel.uiState.collectAsState()

    //todo pls fix
    Row(
        Modifier
            .padding(16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            modifier = Modifier.weight(1f),
            value = uiState.value.message,
            onValueChange = { viewModel.setMessage(it) },
            singleLine = false,
            maxLines = 3,
            shape = RoundedCornerShape(16.dp),
            placeholder = { Text("Type a message...") },
        )
        Button(
            modifier = Modifier
                .height(56.dp)
                .padding(4.dp)
                //.aspectRatio(1f)
                .alpha(
                    if (!sendMessage.value && uiState.value.message.isNotBlank()) {
                        ALPHA_FULL
                    } else {
                        ALPHA_DISABLED
                    }
                ),
            onClick = {
                if (!sendMessage.value && uiState.value.message.isNotBlank()) {
                    sendMessage.value = true
                }
            },
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send message",
                    modifier = Modifier.size(ButtonDefaults.IconSize)
            )
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

