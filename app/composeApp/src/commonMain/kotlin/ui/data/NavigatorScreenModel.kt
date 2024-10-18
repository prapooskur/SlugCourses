package ui.data

import androidx.compose.material3.SnackbarHostState
import api.getSuggestions
import api.getTerms
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import co.touchlab.kermit.Logger
import com.pras.Database
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "NavigatorScreenModel"

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
    
    fun updateTerms() {
        Logger.d("Updating terms...", tag = TAG)
        val database = getDb()
        if (database == null) {
            Logger.d("Database is null, exiting", tag = TAG)
            return
        }

        screenModelScope.launch(Dispatchers.IO) {
            try {
                val newTerms = getTerms()
                if (newTerms.isNotEmpty()) {
                    database.termsQueries.deleteAll()
                    newTerms.forEach {
                        database.termsQueries.insert(it.term_id.toLong(), it.term_name)
                    }
                }
            } catch (e: Exception) {
                Logger.e("Error updating terms: $e", tag = TAG)
            }

        }
    }

    fun updateSuggestions() {
        Logger.d("Updating terms...", tag = TAG)
        val database = getDb()
        if (database == null) {
            Logger.d("Database is null, exiting", tag = TAG)
            return
        }

        screenModelScope.launch(Dispatchers.IO) {
            try {
                val newSuggestions = getSuggestions()
                Logger.d(newSuggestions.toString(), tag=TAG)
                database.suggestionsQueries.deleteAll()
                newSuggestions.forEach {
                    database.suggestionsQueries.insert(it)
                }
            } catch (e: Exception) {
                Logger.e("Error updating suggestions: $e", tag = TAG)
            }
        }
    }


}