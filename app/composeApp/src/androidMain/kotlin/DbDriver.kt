import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

import com.pras.Database

actual class DriverFactory(private val context: Context) {
    actual fun zzcreateDriver(): SqlDriver {
        return AndroidSqliteDriver(Database.Schema, context, "courses.db")
    }
}