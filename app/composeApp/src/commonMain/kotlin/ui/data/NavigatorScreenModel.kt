package ui.data

import androidx.compose.material3.SnackbarHostState
import cafe.adriel.voyager.core.model.ScreenModel
import com.pras.Database
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NavUiState(
    val database: Database?,
    val snackbarHostState: SnackbarHostState = SnackbarHostState(),
)


class NavigatorScreenModel : ScreenModel {
    private val _uiState = MutableStateFlow(NavUiState(null))
    val uiState: StateFlow<NavUiState> = _uiState.asStateFlow()

    fun setDb(database: Database) {
        _uiState.value = _uiState.value.copy(database = database)
    }

    fun getDb(): Database? {
        return uiState.value.database
    }
}