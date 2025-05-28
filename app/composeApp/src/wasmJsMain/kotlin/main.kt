import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.configureWebResources
import org.jetbrains.compose.resources.preloadFont
import slugcourses.composeapp.generated.resources.NotoColorEmoji
import slugcourses.composeapp.generated.resources.Res

@OptIn(ExperimentalComposeUiApi::class, ExperimentalResourceApi::class)
fun main() {
    configureWebResources {
        // Overrides the resource location
        resourcePathMapping { path -> "./$path" }
    }

    try {
        val driverFactory = DriverFactory()
        ComposeViewport(document.body!!) {
            val emojiFont = preloadFont(Res.font.NotoColorEmoji).value

            App(driverFactory)

            val fontFamilyResolver = LocalFontFamilyResolver.current
            LaunchedEffect(fontFamilyResolver, emojiFont) {
                if (emojiFont != null) {
                    // Preloads a fallback font with emojis to render missing glyphs that are not supported by the bundled font
                    fontFamilyResolver.preload(FontFamily(listOf(emojiFont)))
                }
            }

        }
    } catch(ex: Throwable) {
        ex.printStackTrace()
        throw ex
    }

}