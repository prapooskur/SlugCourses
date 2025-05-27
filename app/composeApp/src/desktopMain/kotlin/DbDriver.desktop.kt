import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.pras.Database
import java.io.File

actual class DriverFactory {
    actual suspend fun createDriver(schema: SqlSchema<QueryResult.AsyncValue<Unit>>): SqlDriver {
        val dbPath = File(System.getProperty("java.io.tmpdir"), "slugcourses.db")
        val driver: SqlDriver = JdbcSqliteDriver(url = "jdbc:sqlite:${dbPath.absolutePath}")
            .also { schema.create(it).await() }
//        if (!dbPath.exists()) {
//            schema.create(driver)
//        }
        return driver
    }
}
