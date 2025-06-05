package data.local.db.datasource

import data.local.db.DatabaseManager
import data.model.MeasureDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Local data source for Measure entities.
 * This class provides methods for accessing and manipulating measure data in the local database.
 */
class MeasureLocalDataSource {
    private val database = DatabaseManager.getDatabase()
    private val measureQueries = database.measureQueries
    private val dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    /**
     * Gets all measures from the local database.
     *
     * @return List of MeasureDto objects
     */
    suspend fun getAllMeasures(): List<MeasureDto> = withContext(Dispatchers.IO) {
        measureQueries.getAllMeasures().executeAsList().map { it.toMeasureDto() }
    }

    /**
     * Gets measures for a goal from the local database.
     *
     * @param goalId The ID of the goal
     * @return List of MeasureDto objects
     */
    suspend fun getMeasuresByGoal(goalId: Long): List<MeasureDto> = withContext(Dispatchers.IO) {
        measureQueries.getMeasuresByGoal(goalId).executeAsList().map { it.toMeasureDto() }
    }

    /**
     * Gets a measure by ID from the local database.
     *
     * @param id The ID of the measure to get
     * @return The MeasureDto object, or null if not found
     */
    suspend fun getMeasureById(id: Long): MeasureDto? = withContext(Dispatchers.IO) {
        measureQueries.getMeasureById(id).executeAsOneOrNull()?.toMeasureDto()
    }

    /**
     * Inserts a new measure into the local database.
     *
     * @param measure The measure to insert
     * @param syncStatus The synchronization status (default: PENDING_CREATE)
     */
    suspend fun insertMeasure(measure: MeasureDto, syncStatus: String = "PENDING_CREATE") = withContext(Dispatchers.IO) {
        measureQueries.insertMeasure(
            id = measure.id ?: System.currentTimeMillis() * -1,
            goalId = measure.goalId,
            name = measure.name,
            description = measure.description,
            scheduledDateTime = measure.scheduledDateTime.format(dateTimeFormatter),
            isCompleted = if (measure.isCompleted) 1L else 0L,
            syncStatus = syncStatus
        )
    }

    /**
     * Updates an existing measure in the local database.
     *
     * @param measure The measure to update
     * @param syncStatus The synchronization status (default: PENDING_UPDATE)
     */
    suspend fun updateMeasure(measure: MeasureDto, syncStatus: String = "PENDING_UPDATE") = withContext(Dispatchers.IO) {
        val measureId = measure.id ?: return@withContext
        measureQueries.updateMeasure(
            goalId = measure.goalId,
            name = measure.name,
            description = measure.description,
            scheduledDateTime = measure.scheduledDateTime.format(dateTimeFormatter),
            isCompleted = if (measure.isCompleted) 1L else 0L,
            syncStatus = syncStatus,
            id = measureId
        )
    }

    /**
     * Marks a measure for deletion in the local database.
     *
     * @param id The ID of the measure to mark for deletion
     */
    suspend fun markMeasureForDeletion(id: Long) = withContext(Dispatchers.IO) {
        measureQueries.markMeasureForDeletion(id)
    }

    /**
     * Deletes a measure from the local database.
     *
     * @param id The ID of the measure to delete
     */
    suspend fun deleteMeasure(id: Long) = withContext(Dispatchers.IO) {
        measureQueries.deleteMeasure(id)
    }

    /**
     * Gets all measures that need to be synchronized with the server.
     *
     * @return List of MeasureDto objects that need to be synchronized
     */
    suspend fun getMeasuresToSync(): List<MeasureDto> = withContext(Dispatchers.IO) {
        measureQueries.getMeasuresToSync().executeAsList().map { it.toMeasureDto() }
    }

    /**
     * Updates the synchronization status of a measure.
     *
     * @param id The ID of the measure
     * @param syncStatus The new synchronization status
     */
    suspend fun updateSyncStatus(id: Long, syncStatus: String) = withContext(Dispatchers.IO) {
        measureQueries.updateSyncStatus(syncStatus, id)
    }

    /**
     * Converts a Measure database entity to a MeasureDto.
     */
    private fun data.local.db.Measure.toMeasureDto(): MeasureDto {
        return MeasureDto(
            id = id,
            goalId = goalId,
            name = name,
            description = description,
            scheduledDateTime = LocalDateTime.parse(scheduledDateTime, dateTimeFormatter),
            isCompleted = isCompleted == 1L
        )
    }
}