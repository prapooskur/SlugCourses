import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberNavigatorScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import co.touchlab.kermit.Logger
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import slugcourses.composeapp.generated.resources.Res
import slugcourses.composeapp.generated.resources.stop
import org.jetbrains.compose.resources.vectorResource
import ui.data.Author
import ui.data.ChatScreenModel
import ui.data.NavigatorScreenModel


private const val TAG = "ChatScreen"
private const val ALPHA_FULL = 1f
private const val ALPHA_DISABLED = 0.38f
private val ChatMessageShape = RoundedCornerShape(20.dp, 4.dp, 20.dp, 20.dp)
private val ChatResponseShape = RoundedCornerShape(4.dp, 20.dp, 20.dp, 20.dp)

class ChatScreen : Screen {

    @Composable
    override fun Content() {
        val sendMessage = remember { mutableStateOf(false) }

        val screenModel = rememberScreenModel { ChatScreenModel() }
        val uiState = screenModel.uiState.collectAsState()

        val navigator = LocalNavigator.currentOrThrow
        val navScreenModel = navigator.rememberNavigatorScreenModel { NavigatorScreenModel() }
        val haptics = LocalHapticFeedback.current
        val listState = rememberLazyListState()

        LaunchedEffect(Unit) {
            val database = navScreenModel.getDb()
            if (database == null) {
                Logger.d("Database is null, exiting", tag = TAG)
                return@LaunchedEffect
            }
            screenModel.updateSuggestions(database)
        }

        LaunchedEffect(sendMessage.value) {
            if (sendMessage.value) {
                try {
                    screenModel.sendMessage()
                    listState.scrollToItem(uiState.value.messageList.lastIndex, 9999)
                } catch (e: Exception) {
                    Logger.d(e.toString(), tag=TAG)
//                    shortToast(e.message ?: "error: no exception message", context)
                } finally {
                    sendMessage.value = false
                }
            }
        }

        LaunchedEffect(uiState.value.messageList[uiState.value.messageList.lastIndex]) {
            Logger.d("scroll offset: "+listState.firstVisibleItemScrollOffset.toString(), tag=TAG)
            // force scroll to bottom
            if (
                (listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) == listState.layoutInfo.totalItemsCount - 1 &&
                !listState.isScrollInProgress
//                listState.firstVisibleItemScrollOffset == 0
            ) {
                listState.animateScrollToItem(uiState.value.messageList.lastIndex, 9999)
            }
        }

        LaunchedEffect(uiState.value.errorMessage) {
            if (uiState.value.errorMessage.isNotBlank()) {
                navScreenModel.uiState.value.snackbarHostState.showSnackbar(uiState.value.errorMessage)
            }
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Scaffold(
                modifier = Modifier.widthIn(max = 1200.dp),
                content = { paddingValues ->
                    LazyColumn(
                        Modifier
                            .padding(paddingValues)
                            .padding(horizontal = 8.dp)
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.Bottom,
                        state = listState
                    ) {
                        items(uiState.value.messageList) { chat ->
                            if (chat.author == Author.SYSTEM || chat.author == Author.USER) {
                                Row(Modifier.padding(top = 4.dp, bottom = 4.dp)) {
                                    when (chat.author) {
                                        Author.USER -> ChatMessageBubble(chat.message.trimEnd())
                                        Author.SYSTEM -> ChatResponseBubble(chat.message.trimEnd(), incomplete = (chat.message == "|||"))
                                        else -> {} /* author.function should be hidden */
                                    }
                                }
                            }
                        }
                    }
                },
                bottomBar = {
                    Column {
                        Spacer(Modifier.padding(top = 2.dp))
                        SuggestionBar(
                            suggestions = screenModel.uiState.value.messageSuggestions,
                            visible = (screenModel.uiState.value.messageList.size == 1),
                            onSelect = { screenModel.setMessage(it); screenModel.sendMessage() }
                        )
                        Spacer(Modifier.padding(top = 4.dp))
                        ChatMessageBar(
                            message = uiState.value.message,
                            active = uiState.value.active,
                            sendMessage = sendMessage,
                            onValueChange = { screenModel.setMessage(it) },
                            onSend = { sendMessage.value = true },
                            onCancel = { screenModel.cancelMessage() },
                            onReset = {
                                screenModel.resetMessages()
                                navScreenModel.getDb()?.let { screenModel.updateSuggestions(database = it) }
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        )
                    }
                }
            )
        }
    }

    @Composable
    private fun SuggestionBar(suggestions: List<String>, visible: Boolean, onSelect: (String) -> Unit) {

        AnimatedVisibility(
            visible = (visible && suggestions.isNotEmpty()),
            enter = fadeIn() + slideInVertically( initialOffsetY = { it/2 } ) + expandVertically(),
            exit = fadeOut() + slideOutVertically( targetOffsetY = { it/2 } ) + shrinkVertically(),
            label = "suggestions"
        ) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(horizontal = 6.dp)) {
                items(suggestions.size) { index ->
                    SuggestionChip(
                        onClick = {
                            if (visible) onSelect(suggestions[index])
                        },
                        label = { Text(suggestions[index]) },
                    )
                }
            }
        }
    }

    @Composable
    private fun ChatMessageBar(
        message: String,
        active: Boolean,
        sendMessage: MutableState<Boolean>,
        onValueChange: (String) -> Unit,
        onSend: () -> Unit,
        onCancel: () -> Unit,
        onReset: () -> Unit
    ) {

        Row(
            Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                modifier = Modifier.weight(1f),
                value = message,
                onValueChange = { onValueChange(it) },
                singleLine = false,
                maxLines = 3,
                shape = RoundedCornerShape(16.dp),
                placeholder = { Text("Type a message...") },
            )
            //external circle
            val sendActive = active || message.isNotBlank()
            val canSend = !active && message.isNotBlank()

            CircularIconButton(
                onClick = {
                    if (canSend) {
                        onSend()
                    } else if (active) {
                        onCancel()
                    }
                },
                onLongClick = {
                    if (!sendMessage.value) {
                        onReset()
                    }
                },
                icon = if (!active) Icons.AutoMirrored.Default.Send else vectorResource(Res.drawable.stop),
                contentDescription = "Click to send or cancel, hold to clear messages",
                containerColor = if (sendActive) {
                    ButtonDefaults.filledTonalButtonColors().containerColor
                } else {
                    ButtonDefaults.filledTonalButtonColors().disabledContainerColor
                },
                iconColor = if (sendActive) {
                    ButtonDefaults.filledTonalButtonColors().contentColor
                } else {
                    ButtonDefaults.filledTonalButtonColors().disabledContentColor
                },
                alpha = if (sendActive) {
                    ALPHA_FULL
                } else {
                    ALPHA_DISABLED
                }
            )
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun CircularIconButton(
        onClick: () -> Unit,
        onLongClick: () -> Unit,
        icon: ImageVector,
        contentDescription: String? = null,
        containerColor: Color = ButtonDefaults.filledTonalButtonColors().containerColor,
        iconColor: Color = ButtonDefaults.filledTonalButtonColors().contentColor,
        alpha: Float = ALPHA_FULL,
        modifier: Modifier = Modifier
    ) {
        Box(
            contentAlignment= Alignment.Center,
            modifier = Modifier
                .size(56.dp)
                .aspectRatio(1f)
                .padding(4.dp)
                .clip(CircleShape)
                .background(containerColor)
                .alpha(alpha)
                .combinedClickable (
                    onClick = {
                        onClick()
                    },
                    onLongClick = {
                        onLongClick()
                    }
                )
                .then(modifier)
        ) {
            //internal circle with icon
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier
                    .size(ButtonDefaults.IconSize),
                tint = iconColor
            )
        }
    }

    @Composable
    private fun ChatMessageBubble(message: String){
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxWidth(0.8f),
                horizontalArrangement = Arrangement.End
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = ChatMessageShape
                ) {
                    SelectionContainer {
//                    Text(
//                        text = message,
//                        style = MaterialTheme.typography.bodyLarge.copy(color = LocalContentColor.current),
//                        modifier = Modifier.padding(16.dp)
//                    )
                        Markdown(
                            content = message,
                            modifier = Modifier.padding(16.dp),
                            colors = markdownColor(LocalContentColor.current)
                            //typography = MaterialTheme.typography.bodyLarge.copy(color = LocalContentColor.current),
                            //modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun ChatResponseBubble(response: String, incomplete: Boolean){
        Row(horizontalArrangement = Arrangement.Start, modifier = Modifier.fillMaxWidth(0.8f)) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = ChatResponseShape
            ) {
                if (!incomplete) {
                    SelectionContainer {
                        Markdown(
                            content = response,
                            modifier = Modifier.padding(16.dp),
                            colors = markdownColor(LocalContentColor.current)
                            //typography = MaterialTheme.typography.bodyLarge.copy(color = LocalContentColor.current),
                            //modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), Arrangement.Center, Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.padding(16.dp))
                    }
                }
            }
        }
    }
}