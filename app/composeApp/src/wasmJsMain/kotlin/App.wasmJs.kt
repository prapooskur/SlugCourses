import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@Composable
actual fun GetScreenSize(): IntSize {
    // return LocalWindowInfo.current.containerSize
    val intSize = LocalWindowInfo.current.containerSize
    return IntSize(intSize.width.toDp(), intSize.height.toDp())
}

actual val ioDispatcher: CoroutineDispatcher = Dispatchers.Default
