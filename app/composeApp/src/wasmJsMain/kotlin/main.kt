import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.configureWebResources
import org.jetbrains.compose.resources.preloadFont
import slugcourses.composeapp.generated.resources.NotoColorEmoji
import slugcourses.composeapp.generated.resources.Res
import slugcourses.composeapp.generated.resources.allFontResources

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
            var fontsFallbackInitialized by remember { mutableStateOf(false) }

            App(driverFactory)

//            if (emojiFont != null && fontsFallbackInitialized) {
//                App(driverFactory)
//            } else {
//                // Displays the progress indicator to address a FOUT or the app being temporarily non-functional during loading
//                Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.8f)).clickable {  }) {
//                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
//                }
//                println("Fonts are not ready yet")
//            }
//
            val fontFamilyResolver = LocalFontFamilyResolver.current
            LaunchedEffect(fontFamilyResolver, emojiFont) {
                if (emojiFont != null) {
                    // Preloads a fallback font with emojis to render missing glyphs that are not supported by the bundled font
                    fontFamilyResolver.preload(FontFamily(listOf(emojiFont)))
                    fontsFallbackInitialized = true
                }
            }

        }
    } catch(ex: Throwable) {
        ex.printStackTrace()
        throw ex
    }

}