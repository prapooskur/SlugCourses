import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ListItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import api.Theme
import cafe.adriel.voyager.core.model.rememberNavigatorScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import ui.data.NavigatorScreenModel
import ui.elements.BoringNormalTopBar

// prefs to implement: theme, ai, favorites
class SettingsScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val navScreenModel = navigator.rememberNavigatorScreenModel { NavigatorScreenModel() }

        val settingsRepository = navScreenModel.getSettingsRepository()

        val currentTheme = settingsRepository.getThemeFlow().collectAsState(initial = settingsRepository.getTheme()).value
        val showChat = settingsRepository.getShowChatFlow().collectAsState(initial = settingsRepository.getShowChat()).value
        val showFavorites = settingsRepository.getShowFavoritesFlow().collectAsState(initial = settingsRepository.getShowFavorites()).value

        Column {
            BoringNormalTopBar(titleText = "Settings", onBack = { navigator.pop() }, showBack = (LocalScreenSize.current.width < 600 ))
            ThemeSwitcher(
                labels = listOf("System", "Light", "Dark"),
                current = currentTheme.name,
                onSwitch = { theme -> settingsRepository.setTheme(Theme.valueOf(theme)) }
            )
            PrefSwitcher(
                label = "Show chat",
                isEnabled = showChat,
                onSwitch = { settingsRepository.setShowChat(!showChat) }
            )
            PrefSwitcher(
                label = "Show favorites",
                isEnabled = showFavorites,
                onSwitch = { settingsRepository.setShowFavorites(!showFavorites) }
            )
        }
    }

    @Composable
    private fun ThemeSwitcher(
        labels: List<String>,
        current: String,
        onSwitch: (String) -> Unit
    ) {
        Column {
            labels.forEach { label ->
                ListItem(
                    modifier = Modifier.clickable(onClick = { onSwitch(label.uppercase()) }),
                    headlineContent = { Text(label) },
                    trailingContent = {
                        RadioButton(
                            selected = current.uppercase() == label.uppercase(),
                            onClick = null
                        )
                    }
                )
            }
        }
    }

    @Composable
    private fun PrefSwitcher(
        label: String,
        isEnabled: Boolean,
        onSwitch: () -> Unit,
    ) {
        ListItem(
            modifier = Modifier.clickable(onClick = onSwitch),
            headlineContent = { Text(label) },
            trailingContent = { Switch(checked = isEnabled, onCheckedChange = null ) }
        )
    }
}