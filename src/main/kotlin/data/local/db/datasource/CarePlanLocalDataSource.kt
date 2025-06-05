package data.local.db.datasource

import data.local.db.DatabaseManager
import data.model.CarePlanDto
import data.model.CarePlanGoalDto
import data.model.CarePlanMeasureDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Local data source for CarePlan entities.
 * This class provides methods for accessing and manipulating care plan data in the local database.
 */
class CarePlanLocalDataSource {
    private val database = DatabaseManager.getDatabase()
    private val carePlanQueries = database.carePlanQueries
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    /**
     * Gets all care plans from the local database.
     *
     * @return List of CarePlanDto objects
     */
    suspend fun getAllCarePlans(): List<CarePlanDto> = withContext(Dispatchers.IO) {
        carePlanQueries.getAllCarePlans().executeAsList().map { it.toCarePlanDto() }
    }

    /**
     * Gets care plans for a patient from the local database.
     *
     * @param patientId The ID of the patient
     * @return List of CarePlanDto objects
     */
    suspend fun getCarePlansByPatient(patientId: Long): List<CarePlanDto> = withContext(Dispatchers.IO) {
        carePlanQueries.getCarePlansByPatient(patientId).executeAsList().map { it.toCarePlanDto() }
    }

    /**
     * Gets a care plan by ID from the local database.
     *
     * @param id The ID of the care plan to get
     * @return The CarePlanDto object, or null if not found
     */
    suspend fun getCarePlanById(id: Long): CarePlanDto? = withContext(Dispatchers.IO) {
        carePlanQueries.getCarePlanById(id).executeAsOneOrNull()?.toCarePlanDto()
    }

    /**
     * Inserts a new care plan into the local database.
     *
     * @param carePlan The care plan to insert
     * @param syncStatus The synchronization status (default: PENDING_CREATE)
     */
    suspend fun insertCarePlan(carePlan: CarePlanDto, syncStatus: String = "PENDING_CREATE") = withContext(Dispatchers.IO) {
        carePlanQueries.insertCarePlan(
            id = carePlan.id,
            patientId = carePlan.patientId,
            title = carePlan.title,
            description = carePlan.description,
            startDate = carePlan.startDate.format(dateFormatter),
            endDate = carePlan.endDate.format(dateFormatter),
            syncStatus = syncStatus
        )
    }

    /**
     * Updates an existing care plan in the local database.
     *
     * @param carePlan The care plan to update
     * @param syncStatus The synchronization status (default: PENDING_UPDATE)
     */
    suspend fun updateCarePlan(carePlan: CarePlanDto, syncStatus: String = "PENDING_UPDATE") = withContext(Dispatchers.IO) {
        carePlanQueries.updateCarePlan(
            patientId = carePlan.patientId,
            title = carePlan.title,
            description = carePlan.description,
            startDate = carePlan.startDate.format(dateFormatter),
            endDate = carePlan.endDate.format(dateFormatter),
            syncStatus = syncStatus,
            id = carePlan.id
        )
    }

    /**
     * Marks a care plan for deletion in the local database.
     *
     * @param id The ID of the care plan to mark for deletion
     */
    suspend fun markCarePlanForDeletion(id: Long) = withContext(Dispatchers.IO) {
        carePlanQueries.markCarePlanForDeletion(id)
    }

    /**
     * Deletes a care plan from the local database.
     *
     * @param id The ID of the care plan to delete
     */
    suspend fun deleteCarePlan(id: Long) = withContext(Dispatchers.IO) {
        carePlanQueries.deleteCarePlan(id)
    }

    /**
     * Gets all care plans that need to be synchronized with the server.
     *
     * @return List of CarePlanDto objects that need to be synchronized
     */
    suspend fun getCarePlansToSync(): List<CarePlanDto> = withContext(Dispatchers.IO) {
        carePlanQueries.getCarePlansToSync().executeAsList().map { it.toCarePlanDto() }
    }

    /**
     * Updates the synchronization status of a care plan.
     *
     * @param id The ID of the care plan
     * @param syncStatus The new synchronization status
     */
    suspend fun updateSyncStatus(id: Long, syncStatus: String) = withContext(Dispatchers.IO) {
        carePlanQueries.updateSyncStatus(syncStatus, id)
    }

    /**
     * Converts a CarePlan database entity to a CarePlanDto.
     * Note: This is a simplified conversion that doesn't include goals and measures.
     * In a real implementation, you would need to fetch goals and measures separately.
     */
    private fun data.local.db.CarePlan.toCarePlanDto(): CarePlanDto {
        // Create a dummy goal and measures for now
        // In a real implementation, you would fetch these from the database
        val dummyGoal = CarePlanGoalDto(
            id = 0,
            name = "Dummy Goal",
            description = "This is a dummy goal",
            frequency = "Daily",
            duration = "1 week"
        )
        
        return CarePlanDto(
            id = id,
            patientId = patientId,
            title = title,
            description = description,
            startDate = LocalDate.parse(startDate, dateFormatter),
            endDate = LocalDate.parse(endDate, dateFormatter),
            goal = dummyGoal,
            measures = emptyList()
        )
    }
}