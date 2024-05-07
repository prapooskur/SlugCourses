import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import java.awt.Dimension

fun main() = application {
    val driverFactory = DriverFactory()
    Window(
        onCloseRequest = ::exitApplication,
        title = "Slug Courses",
    ) {
        window.minimumSize = Dimension(400, 300)
        App(driverFactory)
    }
}