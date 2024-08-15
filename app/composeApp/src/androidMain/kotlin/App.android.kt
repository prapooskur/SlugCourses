import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.IntSize

@Composable
actual fun GetScreenSize(): IntSize {
    return IntSize(LocalConfiguration.current.screenWidthDp, LocalConfiguration.current.screenHeightDp)
}