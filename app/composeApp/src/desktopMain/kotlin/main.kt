import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.jetbrains.compose.resources.painterResource
import slugcourses.composeapp.generated.resources.Res
import slugcourses.composeapp.generated.resources.slug
import java.awt.Dimension

fun main() = application {
    val driverFactory = DriverFactory()
    Window(
        onCloseRequest = ::exitApplication,
        title = "Slug Courses",
        icon = painterResource(Res.drawable.slug)
    ) {
        window.minimumSize = Dimension(400, 300)
        App(driverFactory)
    }
}