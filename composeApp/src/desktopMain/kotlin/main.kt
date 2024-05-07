import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    val driverFactory = DriverFactory()
    Window(
        onCloseRequest = ::exitApplication,
        title = "Slug Courses",
    ) {
        App(driverFactory)
    }
}