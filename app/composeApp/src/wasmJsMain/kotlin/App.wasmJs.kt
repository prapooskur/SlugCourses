import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@Composable
actual fun GetScreenSize(): IntSize {
    return LocalWindowInfo.current.containerSize
}

actual val ioDispatcher: CoroutineDispatcher = Dispatchers.Default