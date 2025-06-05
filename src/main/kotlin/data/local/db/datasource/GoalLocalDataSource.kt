package data.local.db.datasource

import data.local.db.DatabaseManager
import data.model.GoalDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Local data source for Goal entities.
 * This class provides methods for accessing and manipulating goal data in the local database.
 */
class GoalLocalDataSource {
    private val database = DatabaseManager.getDatabase()
    private val goalQueries = database.goalQueries

    /**
     * Gets all goals from the local database.
     *
     * @return List of GoalDto objects
     */
    suspend fun getAllGoals(): List<GoalDto> = withContext(Dispatchers.IO) {
        goalQueries.getAllGoals().executeAsList().map { it.toGoalDto() }
    }

    /**
     * Gets goals for a patient from the local database.
     *
     * @param patientId The ID of the patient
     * @return List of GoalDto objects
     */
    suspend fun getGoalsByPatient(patientId: Long): List<GoalDto> = withContext(Dispatchers.IO) {
        goalQueries.getGoalsByPatient(patientId).executeAsList().map { it.toGoalDto() }
    }

    /**
     * Gets a goal by ID from the local database.
     *
     * @param id The ID of the goal to get
     * @return The GoalDto object, or null if not found
     */
    suspend fun getGoalById(id: Long): GoalDto? = withContext(Dispatchers.IO) {
        goalQueries.getGoalById(id).executeAsOneOrNull()?.toGoalDto()
    }

    /**
     * Inserts a new goal into the local database.
     *
     * @param goal The goal to insert
     * @param syncStatus The synchronization status (default: PENDING_CREATE)
     */
    suspend fun insertGoal(goal: GoalDto, syncStatus: String = "PENDING_CREATE") = withContext(Dispatchers.IO) {
        goalQueries.insertGoal(
            id = goal.id ?: System.currentTimeMillis() * -1,
            patientId = goal.patientId,
            name = goal.name,
            description = goal.description,
            frequency = goal.frequency,
            duration = goal.duration,
            syncStatus = syncStatus
        )
    }

    /**
     * Updates an existing goal in the local database.
     *
     * @param goal The goal to update
     * @param syncStatus The synchronization status (default: PENDING_UPDATE)
     */
    suspend fun updateGoal(goal: GoalDto, syncStatus: String = "PENDING_UPDATE") = withContext(Dispatchers.IO) {
        val goalId = goal.id ?: return@withContext
        goalQueries.updateGoal(
            patientId = goal.patientId,
            name = goal.name,
            description = goal.description,
            frequency = goal.frequency,
            duration = goal.duration,
            syncStatus = syncStatus,
            id = goalId
        )
    }

    /**
     * Marks a goal for deletion in the local database.
     *
     * @param id The ID of the goal to mark for deletion
     */
    suspend fun markGoalForDeletion(id: Long) = withContext(Dispatchers.IO) {
        goalQueries.markGoalForDeletion(id)
    }

    /**
     * Deletes a goal from the local database.
     *
     * @param id The ID of the goal to delete
     */
    suspend fun deleteGoal(id: Long) = withContext(Dispatchers.IO) {
        goalQueries.deleteGoal(id)
    }

    /**
     * Gets all goals that need to be synchronized with the server.
     *
     * @return List of GoalDto objects that need to be synchronized
     */
    suspend fun getGoalsToSync(): List<GoalDto> = withContext(Dispatchers.IO) {
        goalQueries.getGoalsToSync().executeAsList().map { it.toGoalDto() }
    }

    /**
     * Updates the synchronization status of a goal.
     *
     * @param id The ID of the goal
     * @param syncStatus The new synchronization status
     */
    suspend fun updateSyncStatus(id: Long, syncStatus: String) = withContext(Dispatchers.IO) {
        goalQueries.updateSyncStatus(syncStatus, id)
    }

    /**
     * Converts a Goal database entity to a GoalDto.
     */
    private fun data.local.db.Goal.toGoalDto(): GoalDto {
        return GoalDto(
            id = id,
            patientId = patientId,
            name = name,
            description = description,
            frequency = frequency,
            duration = duration
        )
    }
}