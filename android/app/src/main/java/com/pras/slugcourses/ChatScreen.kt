package com.pras.slugcourses

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.prepareGet
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.isEmpty
import io.ktor.utils.io.core.readBytes

//todo pls fix
private const val CHAT_URL = "http://localhost:8000/?userinput="
private const val TAG = "ChatScreen"
@Composable
fun ChatScreen(navController: NavController) {
    Log.d(TAG, "ChatScreen")
    //todo pls fix
    LaunchedEffect(Unit) {
        
    }
    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.Center) {
        //todo pls fix
        
    }
}

suspend fun queryChat(query: String) {
    //todo pls fix
    val client = HttpClient(CIO)

    client.prepareGet(CHAT_URL + query) .execute { httpResponse ->
        val channel: ByteReadChannel = httpResponse.body()
        var response: String = ""
        while (!channel.isClosedForRead) {
            val packet = channel.readRemaining(DEFAULT_BUFFER_SIZE.toLong())
            while (!packet.isEmpty) {
                val bytes = packet.readBytes()
                response+=String(bytes)
                Log.d(TAG, packet.readBytes().toString())
            }
        }
        Log.d(TAG, response)
    }
}