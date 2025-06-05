package data.repository.impl

import data.local.db.datasource.MedicationLocalDataSource
import data.model.MedicationDto
import data.model.MedicationRequest
import data.model.MedicationResponse
import data.remote.services.MedicationApiService
import data.repository.MedicationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import utils.NetworkUtils
import java.io.IOException
import java.time.LocalDate

/**
 * Implementation of the MedicationRepository interface.
 * This class uses both the remote API service and the local data source,
 * prioritizing local data when offline.
 */
class MedicationRepositoryImpl(
    private val medicationApiService: MedicationApiService,
    private val medicationLocalDataSource: MedicationLocalDataSource
) : MedicationRepository {

    /**
     * Gets all medications, prioritizing local data when offline.
     *
     * @return List of MedicationDto objects
     */
    override suspend fun getAllMedications(): List<MedicationDto> = withContext(Dispatchers.IO) {
        try {
            if (NetworkUtils.isNetworkAvailable()) {
                // If online, fetch from API and update local database
                val response = medicationApiService.getAllMedications()
                if (response.isSuccessful) {
                    val medications = response.body() ?: emptyList()
                    // Update local database with fetched data
                    medications.forEach { medicationResponse ->
                        val medicationDto = mapResponseToDto(medicationResponse)
                        medicationLocalDataSource.insertMedication(medicationDto, "SYNCED")
                    }
                    return@withContext medications.map { mapResponseToDto(it) }
                }
            }
            // If offline or API call failed, return local data
            return@withContext medicationLocalDataSource.getAllMedications()
        } catch (e: IOException) {
            // Network error, return local data
            return@withContext medicationLocalDataSource.getAllMedications()
        } catch (e: Exception) {
            // Other error, return local data
            return@withContext medicationLocalDataSource.getAllMedications()
        }
    }

    /**
     * Gets medications for a patient, prioritizing local data when offline.
     *
     * @param patientId The ID of the patient
     * @return List of MedicationDto objects
     */
    override suspend fun getMedicationsForPatient(patientId: Long): List<MedicationDto> = withContext(Dispatchers.IO) {
        try {
            if (NetworkUtils.isNetworkAvailable()) {
                // If online, fetch from API and update local database
                val response = medicationApiService.getAllByPatient(patientId)
                if (response.isSuccessful) {
                    val medications = response.body() ?: emptyList()
                    // Update local database with fetched data
                    medications.forEach { medicationResponse ->
                        val medicationDto = mapResponseToDto(medicationResponse)
                        medicationLocalDataSource.insertMedication(medicationDto, "SYNCED")
                    }
                    return@withContext medications.map { mapResponseToDto(it) }
                }
            }
            // If offline or API call failed, return local data
            return@withContext medicationLocalDataSource.getMedicationsForPatient(patientId)
        } catch (e: IOException) {
            // Network error, return local data
            return@withContext medicationLocalDataSource.getMedicationsForPatient(patientId)
        } catch (e: Exception) {
            // Other error, return local data
            return@withContext medicationLocalDataSource.getMedicationsForPatient(patientId)
        }
    }

    /**
     * Gets a medication by ID, prioritizing local data when offline.
     *
     * @param patientId The ID of the patient
     * @param id The ID of the medication to get
     * @return The MedicationDto object, or null if not found
     */
    override suspend fun getMedicationById(patientId: Long, id: Long): MedicationDto? = withContext(Dispatchers.IO) {
        try {
            if (NetworkUtils.isNetworkAvailable()) {
                // If online, fetch from API and update local database
                val response = medicationApiService.getMedicationByPatient(patientId, id)
                if (response.isSuccessful) {
                    val medicationResponse = response.body()
                    if (medicationResponse != null) {
                        val medicationDto = mapResponseToDto(medicationResponse)
                        medicationLocalDataSource.insertMedication(medicationDto, "SYNCED")
                        return@withContext medicationDto
                    }
                }
            }
            // If offline or API call failed, return local data
            return@withContext medicationLocalDataSource.getMedicationById(id)
        } catch (e: IOException) {
            // Network error, return local data
            return@withContext medicationLocalDataSource.getMedicationById(id)
        } catch (e: Exception) {
            // Other error, return local data
            return@withContext medicationLocalDataSource.getMedicationById(id)
        }
    }

    /**
     * Creates a new medication, storing locally when offline.
     *
     * @param medicationRequest The medication data to create
     * @return The created MedicationDto object
     */
    override suspend fun createMedication(medicationRequest: MedicationRequest): MedicationDto = withContext(Dispatchers.IO) {
        try {
            if (NetworkUtils.isNetworkAvailable()) {
                // If online, create on API and store locally
                val response = medicationApiService.createMedication(medicationRequest.patientId, medicationRequest)
                if (response.isSuccessful) {
                    val medicationResponse = response.body()
                    if (medicationResponse != null) {
                        val medicationDto = mapResponseToDto(medicationResponse)
                        medicationLocalDataSource.insertMedication(medicationDto, "SYNCED")
                        return@withContext medicationDto
                    }
                }
                throw Exception("Failed to create medication on API")
            } else {
                // If offline, store locally with PENDING_CREATE status
                // Generate a temporary negative ID to avoid conflicts with server-generated IDs
                val tempId = System.currentTimeMillis() * -1
                val medicationDto = MedicationDto(
                    id = tempId,
                    patientId = medicationRequest.patientId,
                    medicationName = medicationRequest.medicationName,
                    dosage = medicationRequest.dosage,
                    frequency = medicationRequest.frequency,
                    startDate = medicationRequest.startDate,
                    endDate = medicationRequest.endDate ?: LocalDate.now().plusMonths(1),
                    prescribedBy = medicationRequest.prescribedBy
                )
                medicationLocalDataSource.insertMedication(medicationDto, "PENDING_CREATE")
                return@withContext medicationDto
            }
        } catch (e: IOException) {
            // Network error, store locally with PENDING_CREATE status
            val tempId = System.currentTimeMillis() * -1
            val medicationDto = MedicationDto(
                id = tempId,
                patientId = medicationRequest.patientId,
                medicationName = medicationRequest.medicationName,
                dosage = medicationRequest.dosage,
                frequency = medicationRequest.frequency,
                startDate = medicationRequest.startDate,
                endDate = medicationRequest.endDate ?: LocalDate.now().plusMonths(1),
                prescribedBy = medicationRequest.prescribedBy
            )
            medicationLocalDataSource.insertMedication(medicationDto, "PENDING_CREATE")
            return@withContext medicationDto
        } catch (e: Exception) {
            // Other error, store locally with PENDING_CREATE status
            val tempId = System.currentTimeMillis() * -1
            val medicationDto = MedicationDto(
                id = tempId,
                patientId = medicationRequest.patientId,
                medicationName = medicationRequest.medicationName,
                dosage = medicationRequest.dosage,
                frequency = medicationRequest.frequency,
                startDate = medicationRequest.startDate,
                endDate = medicationRequest.endDate ?: LocalDate.now().plusMonths(1),
                prescribedBy = medicationRequest.prescribedBy
            )
            medicationLocalDataSource.insertMedication(medicationDto, "PENDING_CREATE")
            return@withContext medicationDto
        }
    }

    /**
     * Updates an existing medication, storing changes locally when offline.
     *
     * @param patientId The ID of the patient
     * @param id The ID of the medication to update
     * @param medicationRequest The updated medication data
     * @return The updated MedicationDto object
     */
    override suspend fun updateMedication(patientId: Long, id: Long, medicationRequest: MedicationRequest): MedicationDto = withContext(Dispatchers.IO) {
        try {
            if (NetworkUtils.isNetworkAvailable()) {
                // If online, update on API and store locally
                val response = medicationApiService.updateMedication(patientId, id, medicationRequest)
                if (response.isSuccessful) {
                    val medicationResponse = response.body()
                    if (medicationResponse != null) {
                        val medicationDto = mapResponseToDto(medicationResponse)
                        medicationLocalDataSource.updateMedication(medicationDto, "SYNCED")
                        return@withContext medicationDto
                    }
                }
                throw Exception("Failed to update medication on API")
            } else {
                // Check if the medication is a new record created offline
                val existingMedication = medicationLocalDataSource.getMedicationById(id)
                val syncStatus = if (existingMedication?.syncStatus == "PENDING_CREATE") "PENDING_CREATE" else "PENDING_UPDATE"

                // If offline, store locally with appropriate status
                val medicationDto = MedicationDto(
                    id = id,
                    patientId = medicationRequest.patientId,
                    medicationName = medicationRequest.medicationName,
                    dosage = medicationRequest.dosage,
                    frequency = medicationRequest.frequency,
                    startDate = medicationRequest.startDate,
                    endDate = medicationRequest.endDate ?: LocalDate.now().plusMonths(1),
                    prescribedBy = medicationRequest.prescribedBy,
                    syncStatus = syncStatus
                )
                medicationLocalDataSource.updateMedication(medicationDto, syncStatus)
                return@withContext medicationDto
            }
        } catch (e: IOException) {
            // Check if the medication is a new record created offline
            val existingMedication = medicationLocalDataSource.getMedicationById(id)
            val syncStatus = if (existingMedication?.syncStatus == "PENDING_CREATE") "PENDING_CREATE" else "PENDING_UPDATE"

            // Network error, store locally with appropriate status
            val medicationDto = MedicationDto(
                id = id,
                patientId = medicationRequest.patientId,
                medicationName = medicationRequest.medicationName,
                dosage = medicationRequest.dosage,
                frequency = medicationRequest.frequency,
                startDate = medicationRequest.startDate,
                endDate = medicationRequest.endDate ?: LocalDate.now().plusMonths(1),
                prescribedBy = medicationRequest.prescribedBy,
                syncStatus = syncStatus
            )
            medicationLocalDataSource.updateMedication(medicationDto, syncStatus)
            return@withContext medicationDto
        } catch (e: Exception) {
            // Check if the medication is a new record created offline
            val existingMedication = medicationLocalDataSource.getMedicationById(id)
            val syncStatus = if (existingMedication?.syncStatus == "PENDING_CREATE") "PENDING_CREATE" else "PENDING_UPDATE"

            // Other error, store locally with appropriate status
            val medicationDto = MedicationDto(
                id = id,
                patientId = medicationRequest.patientId,
                medicationName = medicationRequest.medicationName,
                dosage = medicationRequest.dosage,
                frequency = medicationRequest.frequency,
                startDate = medicationRequest.startDate,
                endDate = medicationRequest.endDate ?: LocalDate.now().plusMonths(1),
                prescribedBy = medicationRequest.prescribedBy,
                syncStatus = syncStatus
            )
            medicationLocalDataSource.updateMedication(medicationDto, syncStatus)
            return@withContext medicationDto
        }
    }

    /**
     * Deletes a medication, marking for deletion locally when offline.
     *
     * @param patientId The ID of the patient
     * @param id The ID of the medication to delete
     * @return True if the medication was deleted, false otherwise
     */
    override suspend fun deleteMedication(patientId: Long, id: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            // Check if the medication is not yet synchronized with the server
            val medication = medicationLocalDataSource.getMedicationById(id)
            if (medication != null && medication.syncStatus != "SYNCED") {
                // If not synchronized, delete directly from local database
                medicationLocalDataSource.deleteMedication(id)
                return@withContext true
            }

            if (NetworkUtils.isNetworkAvailable()) {
                // If online, delete on API and locally
                val response = medicationApiService.deleteMedication(patientId, id)
                if (response.isSuccessful) {
                    medicationLocalDataSource.deleteMedication(id)
                    return@withContext true
                }
                return@withContext false
            } else {
                // If offline, mark for deletion locally
                medicationLocalDataSource.markMedicationForDeletion(id)
                return@withContext true
            }
        } catch (e: IOException) {
            // Network error, mark for deletion locally
            medicationLocalDataSource.markMedicationForDeletion(id)
            return@withContext true
        } catch (e: Exception) {
            // Other error, mark for deletion locally
            medicationLocalDataSource.markMedicationForDeletion(id)
            return@withContext true
        }
    }

    /**
     * Synchronizes local data with the remote server.
     * This method sends pending changes to the server and fetches the latest data.
     *
     * @return True if synchronization was successful, false otherwise
     */
    override suspend fun synchronize(): Boolean = withContext(Dispatchers.IO) {
        println("[DEBUG] MedicationRepositoryImpl.synchronize() called")
        if (!NetworkUtils.isNetworkAvailable()) {
            println("[DEBUG] Network not available, skipping synchronization")
            return@withContext false
        }

        try {
            // Get all medications that need to be synchronized
            val medicationsToSync = medicationLocalDataSource.getMedicationsToSync()
            println("[DEBUG] Found ${medicationsToSync.size} medications to sync")

            // Process each medication based on its sync status
            medicationsToSync.forEach { medication ->
                println("[DEBUG] Processing medication ${medication.id} with sync status ${medication.syncStatus}")
                when (medication.syncStatus) {
                    "PENDING_CREATE" -> {
                        println("[DEBUG] Handling PENDING_CREATE for medication ${medication.id}")
                        // Create on server
                        // Use a valid patient ID (1) instead of the potentially negative ID
                        val validPatientId = if (medication.patientId <= 0) 1L else medication.patientId
                        val request = mapDtoToRequest(medication).copy(patientId = validPatientId)
                        println("[DEBUG] Sending createMedication request to API for patientId $validPatientId")
                        try {
                            val response = medicationApiService.createMedication(validPatientId, request)
                            println("[DEBUG] API response: ${response.code()} ${response.message()}")
                            if (response.isSuccessful) {
                                val serverMedication = response.body()
                                if (serverMedication != null) {
                                    println("[DEBUG] Received server medication with ID ${serverMedication.id}")
                                    // Delete the local temporary medication and insert the server-generated one
                                    medicationLocalDataSource.deleteMedication(medication.id)
                                    medicationLocalDataSource.insertMedication(mapResponseToDto(serverMedication), "SYNCED")
                                    println("[DEBUG] Updated local database with server medication")
                                } else {
                                    println("[DEBUG] Server response body is null")
                                }
                            } else {
                                println("[DEBUG] API call failed: ${response.code()} ${response.message()}")
                                println("[DEBUG] Error body: ${response.errorBody()?.string()}")
                            }
                        } catch (e: Exception) {
                            println("[DEBUG] Exception during API call: ${e.message}")
                            e.printStackTrace()
                        }
                    }
                    "PENDING_UPDATE" -> {
                        println("[DEBUG] Handling PENDING_UPDATE for medication ${medication.id}")
                        // Update on server
                        val request = mapDtoToRequest(medication)
                        try {
                            val response = medicationApiService.updateMedication(medication.patientId, medication.id, request)
                            println("[DEBUG] API response: ${response.code()} ${response.message()}")
                            if (response.isSuccessful) {
                                // Update sync status to SYNCED
                                medicationLocalDataSource.updateSyncStatus(medication.id, "SYNCED")
                                println("[DEBUG] Updated sync status to SYNCED for medication ${medication.id}")
                            } else {
                                println("[DEBUG] API call failed: ${response.code()} ${response.message()}")
                                println("[DEBUG] Error body: ${response.errorBody()?.string()}")
                            }
                        } catch (e: Exception) {
                            println("[DEBUG] Exception during API call: ${e.message}")
                            e.printStackTrace()
                        }
                    }
                    "PENDING_DELETE" -> {
                        println("[DEBUG] Handling PENDING_DELETE for medication ${medication.id}")
                        // Delete on server
                        try {
                            val response = medicationApiService.deleteMedication(medication.patientId, medication.id)
                            println("[DEBUG] API response: ${response.code()} ${response.message()}")
                            if (response.isSuccessful) {
                                // Delete locally
                                medicationLocalDataSource.deleteMedication(medication.id)
                                println("[DEBUG] Deleted medication ${medication.id} from local database")
                            } else {
                                println("[DEBUG] API call failed: ${response.code()} ${response.message()}")
                                println("[DEBUG] Error body: ${response.errorBody()?.string()}")
                            }
                        } catch (e: Exception) {
                            println("[DEBUG] Exception during API call: ${e.message}")
                            e.printStackTrace()
                        }
                    }
                }
            }

            // Fetch all medications from server to ensure local database is up-to-date
            println("[DEBUG] Fetching all medications from server")
            try {
                val response = medicationApiService.getAllMedications()
                println("[DEBUG] API response: ${response.code()} ${response.message()}")
                if (response.isSuccessful) {
                    val serverMedications = response.body() ?: emptyList()
                    println("[DEBUG] Received ${serverMedications.size} medications from server")

                    // Get all local medications
                    val localMedications = medicationLocalDataSource.getAllMedications()
                    println("[DEBUG] Found ${localMedications.size} medications in local database")

                    // Create a map of local medications by ID for quick lookup
                    val localMedicationsMap = localMedications.associateBy { it.id }

                    // Process server medications
                    serverMedications.forEach { serverMedication ->
                        val serverMedicationDto = mapResponseToDto(serverMedication)
                        println("[DEBUG] Processing server medication ${serverMedicationDto.id}")
                        val localMedication = localMedicationsMap[serverMedicationDto.id]

                        if (localMedication == null) {
                            // Medication exists on server but not locally, insert it
                            println("[DEBUG] Medication ${serverMedicationDto.id} exists on server but not locally, inserting")
                            medicationLocalDataSource.insertMedication(serverMedicationDto, "SYNCED")
                        } else if (localMedication.syncStatus == "SYNCED") {
                            // Medication exists both locally and on server, and local is synced, update it
                            println("[DEBUG] Medication ${serverMedicationDto.id} exists both locally and on server, updating")
                            medicationLocalDataSource.updateMedication(serverMedicationDto, "SYNCED")
                        } else {
                            // If local medication has pending changes, don't overwrite them
                            println("[DEBUG] Medication ${serverMedicationDto.id} has pending changes, not overwriting")
                        }
                    }

                    println("[DEBUG] Synchronization completed successfully")
                    return@withContext true
                } else {
                    println("[DEBUG] API call failed: ${response.code()} ${response.message()}")
                    println("[DEBUG] Error body: ${response.errorBody()?.string()}")
                    return@withContext false
                }
            } catch (e: Exception) {
                println("[DEBUG] Exception during API call: ${e.message}")
                e.printStackTrace()
                return@withContext false
            }
        } catch (e: Exception) {
            println("[DEBUG] Exception during synchronization: ${e.message}")
            e.printStackTrace()
            return@withContext false
        }
    }

    /**
     * Converts a MedicationResponse to a MedicationDto.
     *
     * @param response The MedicationResponse to convert
     * @return The converted MedicationDto
     */
    override fun mapResponseToDto(response: MedicationResponse): MedicationDto {
        return MedicationDto(
            id = response.id,
            patientId = response.patientId,
            medicationName = response.medicationName,
            dosage = response.dosage,
            frequency = response.frequency,
            startDate = response.startDate,
            endDate = response.endDate ?: LocalDate.now().plusMonths(1),
            prescribedBy = response.prescribedBy,
            syncStatus = "SYNCED"
        )
    }

    /**
     * Converts a MedicationDto to a MedicationRequest.
     *
     * @param dto The MedicationDto to convert
     * @return The converted MedicationRequest
     */
    override fun mapDtoToRequest(dto: MedicationDto): MedicationRequest {
        return MedicationRequest(
            patientId = dto.patientId,
            medicationName = dto.medicationName,
            dosage = dto.dosage,
            frequency = dto.frequency,
            startDate = dto.startDate,
            endDate = dto.endDate,
            prescribedBy = dto.prescribedBy
        )
    }

    /**
     * Gets medications that need to be synchronized with the server.
     *
     * @return List of MedicationDto objects that need to be synchronized
     */
    override suspend fun getMedicationsToSync(): List<MedicationDto> = withContext(Dispatchers.IO) {
        println("[DEBUG] MedicationRepositoryImpl.getMedicationsToSync() called")
        val medications = medicationLocalDataSource.getMedicationsToSync()
        println("[DEBUG] MedicationRepositoryImpl.getMedicationsToSync() returning ${medications.size} medications")
        return@withContext medications
    }

    /**
     * Deletes all unsynchronized medications from the local database.
     * This includes medications with PENDING_CREATE, PENDING_UPDATE, or PENDING_DELETE status.
     *
     * @return The number of medications deleted
     */
    override suspend fun deleteUnsynchronizedMedications(): Int = withContext(Dispatchers.IO) {
        try {
            // Get all medications that need to be synchronized
            val medicationsToDelete = getMedicationsToSync()

            // Delete each unsynchronized medication individually
            medicationsToDelete.forEach { medication ->
                medicationLocalDataSource.deleteMedication(medication.id)
            }

            println("[DEBUG] MedicationRepositoryImpl.deleteUnsynchronizedMedications() deleted ${medicationsToDelete.size} medications")
            return@withContext medicationsToDelete.size
        } catch (e: Exception) {
            println("[DEBUG] Exception during deleteUnsynchronizedMedications: ${e.message}")
            e.printStackTrace()
            return@withContext 0
        }
    }
}
