import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.pras.Database
import java.io.File

actual class DriverFactory {
    actual fun createDriver(): SqlDriver {
        val dbPath = File(System.getProperty("java.io.tmpdir"), "slugcourses.db")
        val driver: SqlDriver = JdbcSqliteDriver(url = "jdbc:sqlite:${dbPath.absolutePath}")
        if (!dbPath.exists()) {
            Database.Schema.create(driver)
        }
        return driver
    }
}

actual suspend fun provideDbDriver(
    schema: SqlSchema<QueryResult.AsyncValue<Unit>>
): SqlDriver {
    return JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        .also { schema.create(it).await() }
}