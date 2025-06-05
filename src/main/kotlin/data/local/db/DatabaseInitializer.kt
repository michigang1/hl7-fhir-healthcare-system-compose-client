package data.local.db

import app.cash.sqldelight.db.SqlDriver

/**
 * Initializes the database at application startup.
 * This class is responsible for creating the database driver and initializing the database.
 */
object DatabaseInitializer {
    /**
     * Initializes the database.
     * This method should be called at application startup.
     */
    fun initialize() {
        val driver = DatabaseDriverFactory.createDriver()
        DatabaseManager.initialize(driver)
    }
    
    /**
     * Closes the database connection.
     * This method should be called when the application is shutting down.
     */
    fun closeDatabase() {
        DatabaseManager.closeDatabase()
    }
}