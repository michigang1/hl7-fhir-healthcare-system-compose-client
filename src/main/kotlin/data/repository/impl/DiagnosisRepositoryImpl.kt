package data.repository.impl

import data.local.db.datasource.DiagnosisLocalDataSource
import data.model.DiagnosisDto
import data.model.DiagnosisRequest
import data.model.DiagnosisResponse
import data.remote.services.DiagnosisApiService
import data.repository.DiagnosisRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import utils.NetworkUtils
import java.io.IOException
import java.time.LocalDate

/**
 * Implementation of the DiagnosisRepository interface.
 * This class uses both the remote API service and the local data source,
 * prioritizing local data when offline.
 */
class DiagnosisRepositoryImpl(
    private val diagnosisApiService: DiagnosisApiService,
    private val diagnosisLocalDataSource: DiagnosisLocalDataSource
) : DiagnosisRepository {

    /**
     * Gets all diagnoses, prioritizing local data when offline.
     *
     * @return List of DiagnosisDto objects
     */
    override suspend fun getAllDiagnoses(): List<DiagnosisDto> = withContext(Dispatchers.IO) {
        try {
            if (NetworkUtils.isNetworkAvailable()) {
                // If online, fetch from API and update local database
                val response = diagnosisApiService.getAllDiagnoses()
                if (response.isSuccessful) {
                    val diagnoses = response.body() ?: emptyList()
                    // Update local database with fetched data
                    diagnoses.forEach { diagnosisResponse ->
                        val diagnosisDto = mapResponseToDto(diagnosisResponse)
                        diagnosisLocalDataSource.insertDiagnosis(diagnosisDto, "SYNCED")
                    }
                    return@withContext diagnoses.map { mapResponseToDto(it) }
                }
            }
            // If offline or API call failed, return local data
            return@withContext diagnosisLocalDataSource.getAllDiagnoses()
        } catch (e: IOException) {
            // Network error, return local data
            return@withContext diagnosisLocalDataSource.getAllDiagnoses()
        } catch (e: Exception) {
            // Other error, return local data
            return@withContext diagnosisLocalDataSource.getAllDiagnoses()
        }
    }

    /**
     * Gets diagnoses for a patient, prioritizing local data when offline.
     *
     * @param patientId The ID of the patient
     * @return List of DiagnosisDto objects
     */
    override suspend fun getDiagnosesForPatient(patientId: Long): List<DiagnosisDto> = withContext(Dispatchers.IO) {
        try {
            if (NetworkUtils.isNetworkAvailable()) {
                // If online, fetch from API and update local database
                val response = diagnosisApiService.getAllDiagnosesByPatient(patientId)
                if (response.isSuccessful) {
                    val diagnoses = response.body() ?: emptyList()
                    // Update local database with fetched data
                    diagnoses.forEach { diagnosisResponse ->
                        val diagnosisDto = mapResponseToDto(diagnosisResponse)
                        diagnosisLocalDataSource.insertDiagnosis(diagnosisDto, "SYNCED")
                    }
                    return@withContext diagnoses.map { mapResponseToDto(it) }
                }
            }
            // If offline or API call failed, return local data
            return@withContext diagnosisLocalDataSource.getDiagnosesForPatient(patientId)
        } catch (e: IOException) {
            // Network error, return local data
            return@withContext diagnosisLocalDataSource.getDiagnosesForPatient(patientId)
        } catch (e: Exception) {
            // Other error, return local data
            return@withContext diagnosisLocalDataSource.getDiagnosesForPatient(patientId)
        }
    }

    /**
     * Gets a diagnosis by ID, prioritizing local data when offline.
     *
     * @param patientId The ID of the patient
     * @param id The ID of the diagnosis to get
     * @return The DiagnosisDto object, or null if not found
     */
    override suspend fun getDiagnosisById(patientId: Long, id: Long): DiagnosisDto? = withContext(Dispatchers.IO) {
        try {
            if (NetworkUtils.isNetworkAvailable()) {
                // If online, fetch from API and update local database
                val response = diagnosisApiService.getDiagnosisByPatient(patientId, id)
                if (response.isSuccessful) {
                    val diagnosisResponse = response.body()
                    if (diagnosisResponse != null) {
                        val diagnosisDto = mapResponseToDto(diagnosisResponse)
                        diagnosisLocalDataSource.insertDiagnosis(diagnosisDto, "SYNCED")
                        return@withContext diagnosisDto
                    }
                }
            }
            // If offline or API call failed, return local data
            return@withContext diagnosisLocalDataSource.getDiagnosisById(id)
        } catch (e: IOException) {
            // Network error, return local data
            return@withContext diagnosisLocalDataSource.getDiagnosisById(id)
        } catch (e: Exception) {
            // Other error, return local data
            return@withContext diagnosisLocalDataSource.getDiagnosisById(id)
        }
    }

    /**
     * Creates a new diagnosis, storing locally when offline.
     *
     * @param diagnosisRequest The diagnosis data to create
     * @return The created DiagnosisDto object
     */
    override suspend fun createDiagnosis(diagnosisRequest: DiagnosisRequest): DiagnosisDto = withContext(Dispatchers.IO) {
        try {
            if (NetworkUtils.isNetworkAvailable()) {
                // If online, create on API and store locally
                val response = diagnosisApiService.createDiagnosis(diagnosisRequest.id!!, diagnosisRequest)
                if (response.isSuccessful) {
                    val diagnosisResponse = response.body()
                    if (diagnosisResponse != null) {
                        val diagnosisDto = mapResponseToDto(diagnosisResponse)
                        diagnosisLocalDataSource.insertDiagnosis(diagnosisDto, "SYNCED")
                        return@withContext diagnosisDto
                    }
                }
                throw Exception("Failed to create diagnosis on API")
            } else {
                // If offline, store locally with PENDING_CREATE status
                // Generate a temporary negative ID to avoid conflicts with server-generated IDs
                val tempId = System.currentTimeMillis() * -1

                val diagnosisDto = DiagnosisDto(
                    id = tempId,
                    patientId = diagnosisRequest.id!!,
                    diagnosisCode = diagnosisRequest.code!!,
                    isPrimary = diagnosisRequest.isPrimary!!,
                    description = diagnosisRequest.description!!,
                    date = diagnosisRequest.diagnosedAt!!,
                    prescribedBy = diagnosisRequest.diagnosedBy!!,
                    syncStatus = "PENDING_CREATE"
                )
                diagnosisLocalDataSource.insertDiagnosis(diagnosisDto, "PENDING_CREATE")
                return@withContext diagnosisDto
            }
        } catch (e: IOException) {
            // Network error, store locally with PENDING_CREATE status
            val tempId = System.currentTimeMillis() * -1

            val diagnosisDto = DiagnosisDto(
                id = tempId,
                patientId = diagnosisRequest.id!!,
                diagnosisCode = diagnosisRequest.code!!,
                isPrimary = diagnosisRequest.isPrimary!!,
                description = diagnosisRequest.description!!,
                date = diagnosisRequest.diagnosedAt!!,
                prescribedBy = diagnosisRequest.diagnosedBy!!,
                syncStatus = "PENDING_CREATE"
            )
            diagnosisLocalDataSource.insertDiagnosis(diagnosisDto, "PENDING_CREATE")
            return@withContext diagnosisDto
        } catch (e: Exception) {
            // Other error, store locally with PENDING_CREATE status
            val tempId = System.currentTimeMillis() * -1

            val diagnosisDto = DiagnosisDto(
                id = tempId,
                patientId = diagnosisRequest.id!!,
                diagnosisCode = diagnosisRequest.code!!,
                isPrimary = diagnosisRequest.isPrimary!!,
                description = diagnosisRequest.description!!,
                date = diagnosisRequest.diagnosedAt!!,
                prescribedBy = diagnosisRequest.diagnosedBy!!,
                syncStatus = "PENDING_CREATE"
            )
            diagnosisLocalDataSource.insertDiagnosis(diagnosisDto, "PENDING_CREATE")
            return@withContext diagnosisDto
        }
    }

    /**
     * Updates an existing diagnosis, storing changes locally when offline.
     *
     * @param patientId The ID of the patient
     * @param id The ID of the diagnosis to update
     * @param diagnosisRequest The updated diagnosis data
     * @return The updated DiagnosisDto object
     */
    override suspend fun updateDiagnosis(patientId: Long, id: Long, diagnosisRequest: DiagnosisRequest): DiagnosisDto = withContext(Dispatchers.IO) {
        try {
            if (NetworkUtils.isNetworkAvailable()) {
                // If online, update on API and store locally
                val response = diagnosisApiService.updateDiagnosis(patientId, id, diagnosisRequest)
                if (response.isSuccessful) {
                    val diagnosisResponse = response.body()
                    if (diagnosisResponse != null) {
                        val diagnosisDto = mapResponseToDto(diagnosisResponse)
                        diagnosisLocalDataSource.updateDiagnosis(diagnosisDto, "SYNCED")
                        return@withContext diagnosisDto
                    }
                }
                throw Exception("Failed to update diagnosis on API")
            } else {
                // Check if the diagnosis is a new record created offline
                val existingDiagnosis = diagnosisLocalDataSource.getDiagnosisById(id)
                val syncStatus = if (existingDiagnosis?.syncStatus == "PENDING_CREATE") "PENDING_CREATE" else "PENDING_UPDATE"

                // If offline, store locally with appropriate status
                val diagnosisDto = DiagnosisDto(
                    id = id,
                    patientId = patientId,
                    diagnosisCode = diagnosisRequest.code!!,
                    isPrimary = diagnosisRequest.isPrimary!!,
                    description = diagnosisRequest.description!!,
                    date = diagnosisRequest.diagnosedAt!!,
                    prescribedBy = diagnosisRequest.diagnosedBy!!,
                    syncStatus = syncStatus
                )
                diagnosisLocalDataSource.updateDiagnosis(diagnosisDto, syncStatus)
                return@withContext diagnosisDto
            }
        } catch (e: IOException) {
            // Check if the diagnosis is a new record created offline
            val existingDiagnosis = diagnosisLocalDataSource.getDiagnosisById(id)
            val syncStatus = if (existingDiagnosis?.syncStatus == "PENDING_CREATE") "PENDING_CREATE" else "PENDING_UPDATE"

            // Network error, store locally with appropriate status
            val diagnosisDto = DiagnosisDto(
                id = id,
                patientId = patientId,
                diagnosisCode = diagnosisRequest.code!!,
                isPrimary = diagnosisRequest.isPrimary!!,
                description = diagnosisRequest.description!!,
                date = diagnosisRequest.diagnosedAt!!,
                prescribedBy = diagnosisRequest.diagnosedBy!!,
                syncStatus = syncStatus
            )
            diagnosisLocalDataSource.updateDiagnosis(diagnosisDto, syncStatus)
            return@withContext diagnosisDto
        } catch (e: Exception) {
            // Check if the diagnosis is a new record created offline
            val existingDiagnosis = diagnosisLocalDataSource.getDiagnosisById(id)
            val syncStatus = if (existingDiagnosis?.syncStatus == "PENDING_CREATE") "PENDING_CREATE" else "PENDING_UPDATE"

            // Other error, store locally with appropriate status
            val diagnosisDto = DiagnosisDto(
                id = id,
                patientId = patientId,
                diagnosisCode = diagnosisRequest.code!!,
                isPrimary = diagnosisRequest.isPrimary!!,
                description = diagnosisRequest.description!!,
                date = diagnosisRequest.diagnosedAt!!,
                prescribedBy = diagnosisRequest.diagnosedBy!!,
                syncStatus = syncStatus
            )
            diagnosisLocalDataSource.updateDiagnosis(diagnosisDto, syncStatus)
            return@withContext diagnosisDto
        }
    }

    /**
     * Deletes a diagnosis, marking for deletion locally when offline.
     *
     * @param patientId The ID of the patient
     * @param id The ID of the diagnosis to delete
     * @return True if the diagnosis was deleted, false otherwise
     */
    override suspend fun deleteDiagnosis(patientId: Long, id: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            // Check if the diagnosis is not yet synchronized with the server
            val diagnosis = diagnosisLocalDataSource.getDiagnosisById(id)
            if (diagnosis != null && diagnosis.syncStatus != "SYNCED") {
                diagnosisLocalDataSource.deleteDiagnosis(id)
                return@withContext true
            }

            if (NetworkUtils.isNetworkAvailable()) {
                // If online, delete on API and locally
                val response = diagnosisApiService.deleteDiagnosis(patientId, id)
                if (response.isSuccessful) {
                    diagnosisLocalDataSource.deleteDiagnosis(id)
                    return@withContext true
                }
                return@withContext false
            } else {
                // If offline, mark for deletion locally
                diagnosisLocalDataSource.markDiagnosisForDeletion(id)
                return@withContext true
            }
        } catch (e: IOException) {
            // Network error, mark for deletion locally
            diagnosisLocalDataSource.markDiagnosisForDeletion(id)
            return@withContext true
        } catch (e: Exception) {
            // Other error, mark for deletion locally
            diagnosisLocalDataSource.markDiagnosisForDeletion(id)
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
        println("[DEBUG] DiagnosisRepositoryImpl.synchronize() called")
        if (!NetworkUtils.isNetworkAvailable()) {
            println("[DEBUG] Network not available, skipping synchronization")
            return@withContext false
        }

        try {
            // Get all diagnoses that need to be synchronized
            val diagnosesToSync = diagnosisLocalDataSource.getDiagnosesToSync()
            println("[DEBUG] Found ${diagnosesToSync.size} diagnoses to sync")

            // Process each diagnosis based on its sync status
            diagnosesToSync.forEach { diagnosis ->
                println("[DEBUG] Processing diagnosis ${diagnosis.id} with sync status ${diagnosis.syncStatus}")
                when (diagnosis.syncStatus) {
                    "PENDING_CREATE" -> {
                        println("[DEBUG] Handling PENDING_CREATE for diagnosis ${diagnosis.id}")
                        // Create on server
                        // Use a valid patient ID (1) instead of the potentially negative ID
                        val validPatientId = if (diagnosis.patientId <= 0) 1L else diagnosis.patientId
                        val request = mapDtoToRequest(diagnosis).copy(id = validPatientId)
                        println("[DEBUG] Sending createDiagnosis request to API for patientId $validPatientId")
                        try {
                            val response = diagnosisApiService.createDiagnosis(validPatientId, request)
                            println("[DEBUG] API response: ${response.code()} ${response.message()}")
                            if (response.isSuccessful) {
                                val serverDiagnosis = response.body()
                                if (serverDiagnosis != null) {
                                    println("[DEBUG] Received server diagnosis with ID ${serverDiagnosis.id}")
                                    // Delete the local temporary diagnosis and insert the server-generated one
                                    diagnosisLocalDataSource.deleteDiagnosis(diagnosis.id)
                                    diagnosisLocalDataSource.insertDiagnosis(mapResponseToDto(serverDiagnosis), "SYNCED")
                                    println("[DEBUG] Updated local database with server diagnosis")
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
                        println("[DEBUG] Handling PENDING_UPDATE for diagnosis ${diagnosis.id}")
                        // Update on server
                        val request = mapDtoToRequest(diagnosis)
                        try {
                            val response = diagnosisApiService.updateDiagnosis(diagnosis.patientId, diagnosis.id, request)
                            println("[DEBUG] API response: ${response.code()} ${response.message()}")
                            if (response.isSuccessful) {
                                // Update sync status to SYNCED
                                diagnosisLocalDataSource.updateSyncStatus(diagnosis.id, "SYNCED")
                                println("[DEBUG] Updated sync status to SYNCED for diagnosis ${diagnosis.id}")
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
                        println("[DEBUG] Handling PENDING_DELETE for diagnosis ${diagnosis.id}")
                        // Delete on server
                        try {
                            val response = diagnosisApiService.deleteDiagnosis(diagnosis.patientId, diagnosis.id)
                            println("[DEBUG] API response: ${response.code()} ${response.message()}")
                            if (response.isSuccessful) {
                                // Delete locally
                                diagnosisLocalDataSource.deleteDiagnosis(diagnosis.id)
                                println("[DEBUG] Deleted diagnosis ${diagnosis.id} from local database")
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

            // Fetch all diagnoses from server to ensure local database is up-to-date
            println("[DEBUG] Fetching all diagnoses from server")
            try {
                val response = diagnosisApiService.getAllDiagnoses()
                println("[DEBUG] API response: ${response.code()} ${response.message()}")
                if (response.isSuccessful) {
                    val serverDiagnoses = response.body() ?: emptyList()
                    println("[DEBUG] Received ${serverDiagnoses.size} diagnoses from server")

                    // Get all local diagnoses
                    val localDiagnoses = diagnosisLocalDataSource.getAllDiagnoses()
                    println("[DEBUG] Found ${localDiagnoses.size} diagnoses in local database")

                    // Create a map of local diagnoses by ID for quick lookup
                    val localDiagnosesMap = localDiagnoses.associateBy { it.id }

                    // Process server diagnoses
                    serverDiagnoses.forEach { serverDiagnosis ->
                        val serverDiagnosisDto = mapResponseToDto(serverDiagnosis)
                        println("[DEBUG] Processing server diagnosis ${serverDiagnosisDto.id}")
                        val localDiagnosis = localDiagnosesMap[serverDiagnosisDto.id]

                        if (localDiagnosis == null) {
                            // Diagnosis exists on server but not locally, insert it
                            println("[DEBUG] Diagnosis ${serverDiagnosisDto.id} exists on server but not locally, inserting")
                            diagnosisLocalDataSource.insertDiagnosis(serverDiagnosisDto, "SYNCED")
                        } else if (localDiagnosis.syncStatus == "SYNCED") {
                            // Diagnosis exists both locally and on server, and local is synced, update it
                            println("[DEBUG] Diagnosis ${serverDiagnosisDto.id} exists both locally and on server, updating")
                            diagnosisLocalDataSource.updateDiagnosis(serverDiagnosisDto, "SYNCED")
                        } else {
                            // If local diagnosis has pending changes, don't overwrite them
                            println("[DEBUG] Diagnosis ${serverDiagnosisDto.id} has pending changes, not overwriting")
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
     * Converts a DiagnosisResponse to a DiagnosisDto.
     *
     * @param response The DiagnosisResponse to convert
     * @return The converted DiagnosisDto
     */
    override fun mapResponseToDto(response: DiagnosisResponse): DiagnosisDto {
        return DiagnosisDto(
            id = response.id,
            patientId = response.patientId,
            diagnosisCode = response.code,
            isPrimary = response.isPrimary,
            description = response.description,
            date = response.diagnosedAt,
            prescribedBy = response.diagnosedBy,
            syncStatus = "SYNCED"
        )
    }

    /**
     * Converts a DiagnosisDto to a DiagnosisRequest.
     *
     * @param dto The DiagnosisDto to convert
     * @return The converted DiagnosisRequest
     */
    override fun mapDtoToRequest(dto: DiagnosisDto): DiagnosisRequest {
        return DiagnosisRequest(
            id = dto.patientId,
            code = dto.diagnosisCode,
            isPrimary = dto.isPrimary,
            description = dto.description,
            diagnosedAt = dto.date,
            diagnosedBy = dto.prescribedBy
        )
    }

    /**
     * Gets diagnoses that need to be synchronized with the server.
     *
     * @return List of DiagnosisDto objects that need to be synchronized
     */
    override suspend fun getDiagnosesToSync(): List<DiagnosisDto> = withContext(Dispatchers.IO) {
        println("[DEBUG] DiagnosisRepositoryImpl.getDiagnosesToSync() called")
        val diagnoses = diagnosisLocalDataSource.getDiagnosesToSync()
        println("[DEBUG] DiagnosisRepositoryImpl.getDiagnosesToSync() returning ${diagnoses.size} diagnoses")
        return@withContext diagnoses
    }

    /**
     * Deletes all unsynchronized diagnoses from the local database.
     * This includes diagnoses with PENDING_CREATE, PENDING_UPDATE, or PENDING_DELETE status.
     *
     * @return The number of diagnoses deleted
     */
    override suspend fun deleteUnsynchronizedDiagnoses(): Int = withContext(Dispatchers.IO) {
        try {
            val count = diagnosisLocalDataSource.deleteUnsynchronizedDiagnoses()
            println("[DEBUG] DiagnosisRepositoryImpl.deleteUnsynchronizedDiagnoses() deleted $count diagnoses")
            return@withContext count
        } catch (e: Exception) {
            println("[DEBUG] Exception during deleteUnsynchronizedDiagnoses: ${e.message}")
            e.printStackTrace()
            return@withContext 0
        }
    }
}
