import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.native.NativeSqliteDriver

import com.pras.Database

actual class DriverFactory {
    actual suspend fun createDriver(schema: SqlSchema<QueryResult.AsyncValue<Unit>>): SqlDriver {
        return NativeSqliteDriver(schema.synchronous(), "courses.db")
    }
}