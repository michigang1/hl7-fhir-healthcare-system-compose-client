package data.local.db

import app.cash.sqldelight.db.SqlDriver
import data.local.db.HealthcareDatabase

/**
 * Manager for the SQLDelight database.
 * This class is responsible for initializing and providing access to the database.
 */
object DatabaseManager {
    private var database: HealthcareDatabase? = null

    /**
     * Initializes the database with the provided driver.
     * This should be called at application startup.
     * 
     * @param driver The SQL driver to use
     */
    fun initialize(driver: SqlDriver) {
        if (database == null) {
            database = HealthcareDatabase(driver)
        }
    }

    /**
     * Gets the database instance.
     * 
     * @return The database instance
     * @throws IllegalStateException if the database has not been initialized
     */
    fun getDatabase(): HealthcareDatabase {
        return database ?: throw IllegalStateException("Database has not been initialized. Call initialize() first.")
    }

    /**
     * Closes the database connection.
     * This should be called when the application is shutting down.
     */
    fun closeDatabase() {
        database = null
    }
}
