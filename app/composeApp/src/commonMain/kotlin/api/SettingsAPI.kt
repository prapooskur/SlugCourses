package api

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.Settings
import com.russhwolf.settings.coroutines.getBooleanFlow
import com.russhwolf.settings.coroutines.getIntFlow
import com.russhwolf.settings.observable.makeObservable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class Theme {
    SYSTEM,
    LIGHT,
    DARK
}

class SettingsRepository() {
    private val settings: Settings by lazy { Settings() }
//    private val observableSettings: ObservableSettings by lazy { settings as ObservableSettings }
    @OptIn(ExperimentalSettingsApi::class)
    private val observableSettings: ObservableSettings = settings.makeObservable()

    private companion object {
        const val THEME = "theme"
        const val SHOW_CHAT = "show_chat"
        const val SHOW_CHAT_DEFAULT = true
        const val SHOW_FAVORITES = "show_favorites"
        const val SHOW_FAVORITES_DEFAULT = true

    }

    fun setTheme(theme: Theme) {
        observableSettings.putInt(THEME, theme.ordinal)
    }

    fun getTheme(): Theme {
        return Theme.entries[observableSettings.getInt(THEME, defaultValue = Theme.SYSTEM.ordinal)]
    }

    @OptIn(ExperimentalSettingsApi::class)
    fun getThemeFlow(): Flow<Theme> {
        return observableSettings.getIntFlow(THEME, defaultValue = Theme.SYSTEM.ordinal)
            .map { intValue -> Theme.entries[intValue] }
    }

    fun setShowChat(showChat: Boolean) {
        observableSettings.putBoolean(SHOW_CHAT, showChat)
    }

    fun getShowChat(): Boolean {
        return observableSettings.getBoolean(SHOW_CHAT, SHOW_CHAT_DEFAULT)
    }

    @OptIn(ExperimentalSettingsApi::class)
    fun getShowChatFlow(): Flow<Boolean> {
        return observableSettings.getBooleanFlow(SHOW_CHAT, SHOW_CHAT_DEFAULT)
    }


    fun setShowFavorites(showChat: Boolean) {
        observableSettings.putBoolean(SHOW_FAVORITES, showChat)
    }

    fun getShowFavorites(): Boolean {
        return observableSettings.getBoolean(SHOW_FAVORITES, SHOW_FAVORITES_DEFAULT)
    }

    @OptIn(ExperimentalSettingsApi::class)
    fun getShowFavoritesFlow(): Flow<Boolean> {
        return observableSettings.getBooleanFlow(SHOW_FAVORITES, SHOW_FAVORITES_DEFAULT)
    }

}