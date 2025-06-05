package data.repository.impl

import data.local.db.datasource.PatientLocalDataSource
import data.model.PatientDto
import data.model.PatientRequest
import data.model.PatientResponse
import data.remote.services.PatientApiService
import data.repository.PatientRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import utils.NetworkUtils
import java.io.IOException

/**
 * Implementation of the PatientRepository interface.
 * This class uses both the remote API service and the local data source,
 * prioritizing local data when offline.
 */
class PatientRepositoryImpl(
    private val patientApiService: PatientApiService,
    private val patientLocalDataSource: PatientLocalDataSource
) : PatientRepository {

    /**
     * Gets all patients, prioritizing local data when offline.
     *
     * @return List of PatientDto objects
     */
    override suspend fun getAllPatients(): List<PatientDto> = withContext(Dispatchers.IO) {
        try {
            if (NetworkUtils.isNetworkAvailable()) {
                // If online, fetch from API and update local database
                val response = patientApiService.getAllPatients()
                if (response.isSuccessful) {
                    val patients = response.body() ?: emptyList()
                    // Update local database with fetched data
                    patients.forEach { patientResponse ->
                        val patientDto = mapResponseToDto(patientResponse)
                        patientLocalDataSource.insertPatient(patientDto, "SYNCED")
                    }
                    return@withContext patients.map { mapResponseToDto(it) }
                }
            }
            // If offline or API call failed, return local data
            return@withContext patientLocalDataSource.getAllPatients()
        } catch (e: IOException) {
            // Network error, return local data
            return@withContext patientLocalDataSource.getAllPatients()
        } catch (e: Exception) {
            // Other error, return local data
            return@withContext patientLocalDataSource.getAllPatients()
        }
    }

    /**
     * Gets a patient by ID, prioritizing local data when offline.
     *
     * @param id The ID of the patient to get
     * @return The PatientDto object, or null if not found
     */
    override suspend fun getPatientById(id: Long): PatientDto? = withContext(Dispatchers.IO) {
        try {
            if (NetworkUtils.isNetworkAvailable()) {
                // If online, fetch from API and update local database
                val response = patientApiService.getPatientById(id)
                if (response.isSuccessful) {
                    val patientResponse = response.body()
                    if (patientResponse != null) {
                        val patientDto = mapResponseToDto(patientResponse)
                        patientLocalDataSource.insertPatient(patientDto, "SYNCED")
                        return@withContext patientDto
                    }
                }
            }
            // If offline or API call failed, return local data
            return@withContext patientLocalDataSource.getPatientById(id)
        } catch (e: IOException) {
            // Network error, return local data
            return@withContext patientLocalDataSource.getPatientById(id)
        } catch (e: Exception) {
            // Other error, return local data
            return@withContext patientLocalDataSource.getPatientById(id)
        }
    }

    /**
     * Creates a new patient, storing locally when offline.
     *
     * @param patientRequest The patient data to create
     * @return The created PatientDto object
     */
    override suspend fun createPatient(patientRequest: PatientRequest): PatientDto = withContext(Dispatchers.IO) {
        try {
            if (NetworkUtils.isNetworkAvailable()) {
                // If online, create on API and store locally
                val response = patientApiService.createPatient(patientRequest)
                if (response.isSuccessful) {
                    val patientResponse = response.body()
                    if (patientResponse != null) {
                        val patientDto = mapResponseToDto(patientResponse)
                        patientLocalDataSource.insertPatient(patientDto, "SYNCED")
                        return@withContext patientDto
                    }
                }
                throw Exception("Failed to create patient on API")
            } else {
                // If offline, store locally with PENDING_CREATE status
                // Generate a temporary negative ID to avoid conflicts with server-generated IDs
                val tempId = System.currentTimeMillis() * -1
                val patientDto = PatientDto(
                    id = tempId,
                    name = patientRequest.name,
                    surname = patientRequest.surname,
                    roomNo = patientRequest.roomNo,
                    dateOfBirth = patientRequest.dateOfBirth,
                    gender = patientRequest.gender,
                    address = patientRequest.address,
                    email = patientRequest.email,
                    phone = patientRequest.phone,
                    identifier = patientRequest.identifier,
                    organizationId = patientRequest.organizationId
                )
                patientLocalDataSource.insertPatient(patientDto, "PENDING_CREATE")
                return@withContext patientDto
            }
        } catch (e: IOException) {
            // Network error, store locally with PENDING_CREATE status
            val tempId = System.currentTimeMillis() * -1
            val patientDto = PatientDto(
                id = tempId,
                name = patientRequest.name,
                surname = patientRequest.surname,
                roomNo = patientRequest.roomNo,
                dateOfBirth = patientRequest.dateOfBirth,
                gender = patientRequest.gender,
                address = patientRequest.address,
                email = patientRequest.email,
                phone = patientRequest.phone,
                identifier = patientRequest.identifier,
                organizationId = patientRequest.organizationId
            )
            patientLocalDataSource.insertPatient(patientDto, "PENDING_CREATE")
            return@withContext patientDto
        } catch (e: Exception) {
            // Other error, store locally with PENDING_CREATE status
            val tempId = System.currentTimeMillis() * -1
            val patientDto = PatientDto(
                id = tempId,
                name = patientRequest.name,
                surname = patientRequest.surname,
                roomNo = patientRequest.roomNo,
                dateOfBirth = patientRequest.dateOfBirth,
                gender = patientRequest.gender,
                address = patientRequest.address,
                email = patientRequest.email,
                phone = patientRequest.phone,
                identifier = patientRequest.identifier,
                organizationId = patientRequest.organizationId
            )
            patientLocalDataSource.insertPatient(patientDto, "PENDING_CREATE")
            return@withContext patientDto
        }
    }

    /**
     * Updates an existing patient, storing changes locally when offline.
     *
     * @param id The ID of the patient to update
     * @param patientRequest The updated patient data
     * @return The updated PatientDto object
     */
    override suspend fun updatePatient(id: Long, patientRequest: PatientRequest): PatientDto = withContext(Dispatchers.IO) {
        try {
            if (NetworkUtils.isNetworkAvailable()) {
                // If online, update on API and store locally
                val response = patientApiService.updatePatient(id, patientRequest)
                if (response.isSuccessful) {
                    val patientResponse = response.body()
                    if (patientResponse != null) {
                        val patientDto = mapResponseToDto(patientResponse)
                        patientLocalDataSource.updatePatient(patientDto, "SYNCED")
                        return@withContext patientDto
                    }
                }
                throw Exception("Failed to update patient on API")
            } else {
                // Check if the patient is a new record created offline
                val existingPatient = patientLocalDataSource.getPatientById(id)
                val syncStatus = if (existingPatient?.syncStatus == "PENDING_CREATE") "PENDING_CREATE" else "PENDING_UPDATE"

                // If offline, store locally with appropriate status
                val patientDto = PatientDto(
                    id = id,
                    name = patientRequest.name,
                    surname = patientRequest.surname,
                    roomNo = patientRequest.roomNo,
                    dateOfBirth = patientRequest.dateOfBirth,
                    gender = patientRequest.gender,
                    address = patientRequest.address,
                    email = patientRequest.email,
                    phone = patientRequest.phone,
                    identifier = patientRequest.identifier,
                    organizationId = patientRequest.organizationId,
                    syncStatus = syncStatus
                )
                patientLocalDataSource.updatePatient(patientDto, syncStatus)
                return@withContext patientDto
            }
        } catch (e: IOException) {
            // Check if the patient is a new record created offline
            val existingPatient = patientLocalDataSource.getPatientById(id)
            val syncStatus = if (existingPatient?.syncStatus == "PENDING_CREATE") "PENDING_CREATE" else "PENDING_UPDATE"

            // Network error, store locally with appropriate status
            val patientDto = PatientDto(
                id = id,
                name = patientRequest.name,
                surname = patientRequest.surname,
                roomNo = patientRequest.roomNo,
                dateOfBirth = patientRequest.dateOfBirth,
                gender = patientRequest.gender,
                address = patientRequest.address,
                email = patientRequest.email,
                phone = patientRequest.phone,
                identifier = patientRequest.identifier,
                organizationId = patientRequest.organizationId,
                syncStatus = syncStatus
            )
            patientLocalDataSource.updatePatient(patientDto, syncStatus)
            return@withContext patientDto
        } catch (e: Exception) {
            // Check if the patient is a new record created offline
            val existingPatient = patientLocalDataSource.getPatientById(id)
            val syncStatus = if (existingPatient?.syncStatus == "PENDING_CREATE") "PENDING_CREATE" else "PENDING_UPDATE"

            // Other error, store locally with appropriate status
            val patientDto = PatientDto(
                id = id,
                name = patientRequest.name,
                surname = patientRequest.surname,
                roomNo = patientRequest.roomNo,
                dateOfBirth = patientRequest.dateOfBirth,
                gender = patientRequest.gender,
                address = patientRequest.address,
                email = patientRequest.email,
                phone = patientRequest.phone,
                identifier = patientRequest.identifier,
                organizationId = patientRequest.organizationId,
                syncStatus = syncStatus
            )
            patientLocalDataSource.updatePatient(patientDto, syncStatus)
            return@withContext patientDto
        }
    }

    /**
     * Deletes a patient, marking for deletion locally when offline.
     *
     * @param id The ID of the patient to delete
     * @return True if the patient was deleted, false otherwise
     */
    override suspend fun deletePatient(id: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            if (NetworkUtils.isNetworkAvailable()) {
                // If online, delete on API and locally
                val response = patientApiService.deletePatient(id)
                if (response.isSuccessful) {
                    patientLocalDataSource.deletePatient(id)
                    return@withContext true
                }
                return@withContext false
            } else {
                // If offline, mark for deletion locally
                patientLocalDataSource.markPatientForDeletion(id)
                return@withContext true
            }
        } catch (e: IOException) {
            // Network error, mark for deletion locally
            patientLocalDataSource.markPatientForDeletion(id)
            return@withContext true
        } catch (e: Exception) {
            // Other error, mark for deletion locally
            patientLocalDataSource.markPatientForDeletion(id)
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
        if (!NetworkUtils.isNetworkAvailable()) {
            println("[DEBUG] Network not available, skipping synchronization")
            return@withContext false
        }

        try {
            // Get all patients that need to be synchronized
            val patientsToSync = patientLocalDataSource.getPatientsToSync()
            println("[DEBUG] Found ${patientsToSync.size} patients to sync")

            // Process each patient based on its sync status
            patientsToSync.forEach { patient ->
                println("[DEBUG] Processing patient ${patient.id} with sync status ${patient.syncStatus}")
                when (patient.syncStatus) {
                    "PENDING_CREATE" -> {
                        println("[DEBUG] Handling PENDING_CREATE for patient ${patient.id}")
                        // Create on server
                        val request = mapDtoToRequest(patient)
                        println("[DEBUG] Sending createPatient request to API")
                        try {
                            val response = patientApiService.createPatient(request)
                            println("[DEBUG] API response: ${response.code()} ${response.message()}")
                            if (response.isSuccessful) {
                                val serverPatient = response.body()
                                if (serverPatient != null) {
                                    println("[DEBUG] Received server patient with ID ${serverPatient.id}")
                                    // Delete the local temporary patient and insert the server-generated one
                                    patientLocalDataSource.deletePatient(patient.id)
                                    patientLocalDataSource.insertPatient(mapResponseToDto(serverPatient), "SYNCED")
                                    println("[DEBUG] Updated local database with server patient")
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
                        println("[DEBUG] Handling PENDING_UPDATE for patient ${patient.id}")
                        // Update on server
                        val request = mapDtoToRequest(patient)
                        try {
                            val response = patientApiService.updatePatient(patient.id, request)
                            println("[DEBUG] API response: ${response.code()} ${response.message()}")
                            if (response.isSuccessful) {
                                // Update sync status to SYNCED
                                patientLocalDataSource.updateSyncStatus(patient.id, "SYNCED")
                                println("[DEBUG] Updated sync status to SYNCED for patient ${patient.id}")
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
                        println("[DEBUG] Handling PENDING_DELETE for patient ${patient.id}")
                        // Delete on server
                        try {
                            val response = patientApiService.deletePatient(patient.id)
                            println("[DEBUG] API response: ${response.code()} ${response.message()}")
                            if (response.isSuccessful) {
                                // Delete locally
                                patientLocalDataSource.deletePatient(patient.id)
                                println("[DEBUG] Deleted patient ${patient.id} from local database")
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

            // Fetch all patients from server to ensure local database is up-to-date
            println("[DEBUG] Fetching all patients from server")
            try {
                val response = patientApiService.getAllPatients()
                println("[DEBUG] API response: ${response.code()} ${response.message()}")
                if (response.isSuccessful) {
                    val serverPatients = response.body() ?: emptyList()
                    println("[DEBUG] Received ${serverPatients.size} patients from server")

                    // Get all local patients
                    val localPatients = patientLocalDataSource.getAllPatients()
                    println("[DEBUG] Found ${localPatients.size} patients in local database")

                    // Create a map of local patients by ID for quick lookup
                    val localPatientsMap = localPatients.associateBy { it.id }

                    // Process server patients
                    serverPatients.forEach { serverPatient ->
                        val serverPatientDto = mapResponseToDto(serverPatient)
                        println("[DEBUG] Processing server patient ${serverPatientDto.id}")
                        val localPatient = localPatientsMap[serverPatientDto.id]

                        if (localPatient == null) {
                            // Patient exists on server but not locally, insert it
                            println("[DEBUG] Patient ${serverPatientDto.id} exists on server but not locally, inserting")
                            patientLocalDataSource.insertPatient(serverPatientDto, "SYNCED")
                        } else if (localPatient.syncStatus == "SYNCED") {
                            // Patient exists both locally and on server, and local is synced, update it
                            println("[DEBUG] Patient ${serverPatientDto.id} exists both locally and on server, updating")
                            patientLocalDataSource.updatePatient(serverPatientDto, "SYNCED")
                        } else {
                            // If local patient has pending changes, don't overwrite them
                            println("[DEBUG] Patient ${serverPatientDto.id} has pending changes, not overwriting")
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
     * Converts a PatientResponse to a PatientDto.
     *
     * @param response The PatientResponse to convert
     * @return The converted PatientDto
     */
    override fun mapResponseToDto(response: PatientResponse): PatientDto {
        return PatientDto(
            id = response.id,
            name = response.name,
            surname = response.surname,
            roomNo = response.roomNo,
            dateOfBirth = response.dateOfBirth,
            gender = response.gender,
            address = response.address,
            email = response.email,
            phone = response.phone,
            identifier = response.identifier,
            organizationId = response.organizationId,
            syncStatus = "SYNCED"
        )
    }

    /**
     * Converts a PatientDto to a PatientRequest.
     *
     * @param dto The PatientDto to convert
     * @return The converted PatientRequest
     */
    override fun mapDtoToRequest(dto: PatientDto): PatientRequest {
        // Use a valid ID (1) instead of negative local ID when sending to server
        val validId = if (dto.id < 0) 1L else dto.id
        return PatientRequest(
            id = validId,
            name = dto.name,
            surname = dto.surname,
            roomNo = dto.roomNo,
            dateOfBirth = dto.dateOfBirth,
            gender = dto.gender,
            address = dto.address,
            email = dto.email,
            phone = dto.phone,
            identifier = dto.identifier,
            organizationId = dto.organizationId
        )
    }

    /**
     * Gets patients that need to be synchronized with the server.
     *
     * @return List of PatientDto objects that need to be synchronized
     */
    override suspend fun getPatientsToSync(): List<PatientDto> = withContext(Dispatchers.IO) {
        return@withContext patientLocalDataSource.getPatientsToSync()
    }
}
