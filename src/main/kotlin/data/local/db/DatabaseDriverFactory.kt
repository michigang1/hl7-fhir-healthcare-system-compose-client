package data.local.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File

/**
 * Factory for creating SQLDelight database drivers.
 * This class is responsible for creating and configuring the database driver
 * for the application's local database.
 */
object DatabaseDriverFactory {
    private const val DATABASE_NAME = "healthcare.db"

    /**
     * Creates a SQLite driver for the application's database.
     * The database file is stored in the user's home directory.
     * 
     * @return A configured SqlDriver instance
     */
    fun createDriver(): SqlDriver {
        val databasePath = "${System.getProperty("user.home")}/$DATABASE_NAME"
        val databaseFile = File(databasePath)
        val driver = JdbcSqliteDriver("jdbc:sqlite:$databasePath")

        // Create the database schema if it doesn't exist
        if (!databaseFile.exists()) {
            data.local.db.HealthcareDatabase.Schema.create(driver)
        }

        return driver
    }
}
