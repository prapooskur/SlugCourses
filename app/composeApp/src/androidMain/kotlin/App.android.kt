import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
actual fun GetScreenSize(): IntSize {
    return IntSize(LocalConfiguration.current.screenWidthDp, LocalConfiguration.current.screenHeightDp)
}

actual val ioDispatcher = Dispatchers.IO