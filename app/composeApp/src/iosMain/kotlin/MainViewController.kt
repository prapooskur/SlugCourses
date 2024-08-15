import androidx.compose.ui.window.ComposeUIViewController

val driverFactory = DriverFactory()
fun MainViewController() = ComposeUIViewController { App(driverFactory) }