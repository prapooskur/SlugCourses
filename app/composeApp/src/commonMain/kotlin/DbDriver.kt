import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema

import com.pras.Database

expect class DriverFactory {
    suspend fun createDriver(schema: SqlSchema<QueryResult.AsyncValue<Unit>>): SqlDriver
}

suspend fun createDatabase(driverFactory: DriverFactory): Database {
    val driver = driverFactory.createDriver(Database.Schema)
    val database = Database(driver)

    // Do more work with the database (see below).
    return database
}