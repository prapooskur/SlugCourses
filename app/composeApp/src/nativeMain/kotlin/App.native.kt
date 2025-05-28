import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

@OptIn(ExperimentalComposeUiApi::class)
@Composable
actual fun GetScreenSize(): IntSize {
//    return LocalWindowInfo.current.containerSize
    return with(LocalDensity.current) {
        val currentContainerSize = LocalWindowInfo.current.containerSize
        IntSize(currentContainerSize.width.toDp().value.toInt(), currentContainerSize.height.toDp().value.toInt())
    }
}

actual val ioDispatcher = Dispatchers.IO