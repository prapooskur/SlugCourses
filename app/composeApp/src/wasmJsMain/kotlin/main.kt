import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    try {
        val driverFactory = DriverFactory()
        ComposeViewport(document.body!!) {
            App(driverFactory)
        }
    } catch(ex: Throwable) {
        ex.printStackTrace()
        throw ex
    }

}