package data.repository.impl

import data.local.db.datasource.EventLocalDataSource
import data.model.EventDto
import data.model.EventRequest
import data.model.EventResponse
import data.remote.services.EventApiService
import data.repository.EventRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import utils.NetworkUtils
import utils.UserManager
import java.io.IOException

/**
 * Implementation of the EventRepository interface.
 * This class uses both the remote API service and the local data source,
 * prioritizing local data when offline.
 */
class EventRepositoryImpl(
    private val eventApiService: EventApiService,
    private val eventLocalDataSource: EventLocalDataSource
) : EventRepository {

    /**
     * Gets all events, prioritizing local data when offline.
     *
     * @return List of EventDto objects
     */
    override suspend fun getAllEvents(): List<EventDto> = withContext(Dispatchers.IO) {
        try {
            if (NetworkUtils.isNetworkAvailable()) {
                // If online, fetch from API and update local database
                val response = eventApiService.getAllEvents()
                if (response.isSuccessful) {
                    val events = response.body() ?: emptyList()
                    // Update local database with fetched data
                    events.forEach { eventResponse ->
                        val eventDto = mapResponseToDto(eventResponse)
                        eventLocalDataSource.insertEvent(eventDto, "SYNCED")
                    }
                    return@withContext events.map { mapResponseToDto(it) }
                }
            }
            // If offline or API call failed, return local data
            return@withContext eventLocalDataSource.getAllEvents()
        } catch (e: IOException) {
            // Network error, return local data
            return@withContext eventLocalDataSource.getAllEvents()
        } catch (e: Exception) {
            // Other error, return local data
            return@withContext eventLocalDataSource.getAllEvents()
        }
    }

    /**
     * Gets events for a patient, prioritizing local data when offline.
     *
     * @param patientId The ID of the patient
     * @return List of EventDto objects
     */
    override suspend fun getEventsByPatient(patientId: Long): List<EventDto> = withContext(Dispatchers.IO) {
        try {
            if (NetworkUtils.isNetworkAvailable()) {
                // If online, fetch from API and update local database
                val response = eventApiService.getEventsByPatient(patientId)
                if (response.isSuccessful) {
                    val events = response.body() ?: emptyList()
                    // Update local database with fetched data
                    events.forEach { eventResponse ->
                        val eventDto = mapResponseToDto(eventResponse)
                        eventLocalDataSource.insertEvent(eventDto, "SYNCED")
                    }
                    return@withContext events.map { mapResponseToDto(it) }
                }
            }
            // If offline or API call failed, return local data
            return@withContext eventLocalDataSource.getEventsByPatient(patientId)
        } catch (e: IOException) {
            // Network error, return local data
            return@withContext eventLocalDataSource.getEventsByPatient(patientId)
        } catch (e: Exception) {
            // Other error, return local data
            return@withContext eventLocalDataSource.getEventsByPatient(patientId)
        }
    }

    /**
     * Gets an event by ID, prioritizing local data when offline.
     *
     * @param eventId The ID of the event to get
     * @return The EventDto object, or null if not found
     */
    override suspend fun getEventById(eventId: Long): EventDto? = withContext(Dispatchers.IO) {
        try {
            if (NetworkUtils.isNetworkAvailable()) {
                // If online, fetch from API and update local database
                val response = eventApiService.getEventById(eventId)
                if (response.isSuccessful) {
                    val eventResponse = response.body()
                    if (eventResponse != null) {
                        val eventDto = mapResponseToDto(eventResponse)
                        eventLocalDataSource.insertEvent(eventDto, "SYNCED")
                        return@withContext eventDto
                    }
                }
            }
            // If offline or API call failed, return local data
            return@withContext eventLocalDataSource.getEventById(eventId)
        } catch (e: IOException) {
            // Network error, return local data
            return@withContext eventLocalDataSource.getEventById(eventId)
        } catch (e: Exception) {
            // Other error, return local data
            return@withContext eventLocalDataSource.getEventById(eventId)
        }
    }

    /**
     * Creates a new event, storing locally when offline.
     *
     * @param patientId The ID of the patient
     * @param eventRequest The event data to create
     * @return The created EventDto object
     */
    override suspend fun createEvent(patientId: Long, eventRequest: EventRequest): EventDto = withContext(Dispatchers.IO) {
        try {
            if (NetworkUtils.isNetworkAvailable()) {
                // If online, create on API and store locally
                val response = eventApiService.createEvent(patientId, eventRequest)
                if (response.isSuccessful) {
                    val eventResponse = response.body()
                    if (eventResponse != null) {
                        val eventDto = mapResponseToDto(eventResponse)
                        eventLocalDataSource.insertEvent(eventDto, "SYNCED")
                        return@withContext eventDto
                    }
                }
                throw Exception("Failed to create event on API")
            } else {
                // If offline, store locally with PENDING_CREATE status
                // Generate a temporary negative ID to avoid conflicts with server-generated IDs
                val tempId = System.currentTimeMillis() * -1

                // Create a minimal EventDto with the available information
                // Note: We don't have patient information here, so we'll use an empty list
                val eventDto = EventDto(
                    id = tempId,
                    name = eventRequest.name,
                    description = eventRequest.description,
                    authorId = eventRequest.authorId,
                    authorUsername = UserManager.getUsername(), // Get username from UserManager
                    eventDateTime = eventRequest.eventDateTime,
                    patients = emptyList() // We'll update this when we sync
                )

                // Insert the event with PENDING_CREATE status
                eventLocalDataSource.insertEvent(eventDto, "PENDING_CREATE")

                // Also insert the event-patient relationship to remember which patient this event is for
                eventLocalDataSource.insertEventPatient(tempId, patientId)

                return@withContext eventDto
            }
        } catch (e: IOException) {
            // Network error, store locally with PENDING_CREATE status
            val tempId = System.currentTimeMillis() * -1
            val eventDto = EventDto(
                id = tempId,
                name = eventRequest.name,
                description = eventRequest.description,
                authorId = eventRequest.authorId,
                authorUsername = UserManager.getUsername(), // Get username from UserManager
                eventDateTime = eventRequest.eventDateTime,
                patients = emptyList() // We'll update this when we sync
            )
            eventLocalDataSource.insertEvent(eventDto, "PENDING_CREATE")

            // Also insert the event-patient relationship to remember which patient this event is for
            eventLocalDataSource.insertEventPatient(tempId, patientId)

            return@withContext eventDto
        } catch (e: Exception) {
            // Other error, store locally with PENDING_CREATE status
            val tempId = System.currentTimeMillis() * -1
            val eventDto = EventDto(
                id = tempId,
                name = eventRequest.name,
                description = eventRequest.description,
                authorId = eventRequest.authorId,
                authorUsername = UserManager.getUsername(), // Get username from UserManager
                eventDateTime = eventRequest.eventDateTime,
                patients = emptyList() // We'll update this when we sync
            )
            eventLocalDataSource.insertEvent(eventDto, "PENDING_CREATE")

            // Also insert the event-patient relationship to remember which patient this event is for
            eventLocalDataSource.insertEventPatient(tempId, patientId)

            return@withContext eventDto
        }
    }

    /**
     * Updates an existing event, storing changes locally when offline.
     *
     * @param eventId The ID of the event to update
     * @param eventRequest The updated event data
     * @return The updated EventDto object
     */
    override suspend fun updateEvent(eventId: Long, eventRequest: EventRequest): EventDto = withContext(Dispatchers.IO) {
        try {
            if (NetworkUtils.isNetworkAvailable()) {
                // If online, update on API and store locally
                val response = eventApiService.updateEvent(eventId, eventRequest)
                if (response.isSuccessful) {
                    val eventResponse = response.body()
                    if (eventResponse != null) {
                        val eventDto = mapResponseToDto(eventResponse)
                        eventLocalDataSource.updateEvent(eventDto, "SYNCED")
                        return@withContext eventDto
                    }
                }
                throw Exception("Failed to update event on API")
            } else {
                // If offline, store locally with PENDING_UPDATE status
                // First, get the existing event to preserve data that's not in the request
                val existingEvent = eventLocalDataSource.getEventById(eventId)
                if (existingEvent != null) {
                    // Determine the appropriate sync status
                    val syncStatus = if (existingEvent.syncStatus == "PENDING_CREATE") "PENDING_CREATE" else "PENDING_UPDATE"

                    val updatedEvent = existingEvent.copy(
                        name = eventRequest.name,
                        description = eventRequest.description,
                        authorId = eventRequest.authorId,
                        eventDateTime = eventRequest.eventDateTime,
                        syncStatus = syncStatus
                        // We don't update patients here as we don't have that information in the request
                    )
                    eventLocalDataSource.updateEvent(updatedEvent, syncStatus)
                    return@withContext updatedEvent
                } else {
                    throw Exception("Event not found in local database")
                }
            }
        } catch (e: IOException) {
            // Network error, store locally with appropriate status
            val existingEvent = eventLocalDataSource.getEventById(eventId)
            if (existingEvent != null) {
                // Determine the appropriate sync status
                val syncStatus = if (existingEvent.syncStatus == "PENDING_CREATE") "PENDING_CREATE" else "PENDING_UPDATE"

                val updatedEvent = existingEvent.copy(
                    name = eventRequest.name,
                    description = eventRequest.description,
                    authorId = eventRequest.authorId,
                    eventDateTime = eventRequest.eventDateTime,
                    syncStatus = syncStatus
                )
                eventLocalDataSource.updateEvent(updatedEvent, syncStatus)
                return@withContext updatedEvent
            } else {
                throw Exception("Event not found in local database")
            }
        } catch (e: Exception) {
            // Other error, store locally with appropriate status
            val existingEvent = eventLocalDataSource.getEventById(eventId)
            if (existingEvent != null) {
                // Determine the appropriate sync status
                val syncStatus = if (existingEvent.syncStatus == "PENDING_CREATE") "PENDING_CREATE" else "PENDING_UPDATE"

                val updatedEvent = existingEvent.copy(
                    name = eventRequest.name,
                    description = eventRequest.description,
                    authorId = eventRequest.authorId,
                    eventDateTime = eventRequest.eventDateTime,
                    syncStatus = syncStatus
                )
                eventLocalDataSource.updateEvent(updatedEvent, syncStatus)
                return@withContext updatedEvent
            } else {
                throw Exception("Event not found in local database")
            }
        }
    }

    /**
     * Deletes an event, marking for deletion locally when offline.
     *
     * @param eventId The ID of the event to delete
     * @return True if the event was deleted, false otherwise
     */
    override suspend fun deleteEvent(eventId: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            // Check if the event is not yet synchronized with the server
            val event = eventLocalDataSource.getEventById(eventId)
            if (event != null && event.syncStatus != "SYNCED") {
                // If not synchronized, delete directly from local database
                eventLocalDataSource.deleteEvent(eventId)
                return@withContext true
            }

            if (NetworkUtils.isNetworkAvailable()) {
                // If online, delete on API and locally
                val response = eventApiService.deleteEvent(eventId)
                if (response.isSuccessful) {
                    eventLocalDataSource.deleteEvent(eventId)
                    return@withContext true
                }
                return@withContext false
            } else {
                // If offline, mark for deletion locally
                eventLocalDataSource.markEventForDeletion(eventId)
                return@withContext true
            }
        } catch (e: IOException) {
            // Network error, mark for deletion locally
            eventLocalDataSource.markEventForDeletion(eventId)
            return@withContext true
        } catch (e: Exception) {
            // Other error, mark for deletion locally
            eventLocalDataSource.markEventForDeletion(eventId)
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
            // Get all events that need to be synchronized
            val eventsToSync = eventLocalDataSource.getEventsToSync()
            println("[DEBUG] Found ${eventsToSync.size} events to sync")

            // Process each event based on its sync status
            eventsToSync.forEach { event ->
                println("[DEBUG] Processing event ${event.id} with sync status ${event.syncStatus}")
                when (event.syncStatus) {
                    "PENDING_CREATE" -> {
                        println("[DEBUG] Handling PENDING_CREATE for event ${event.id}")
                        // Create on server
                        // Get the patient ID from the EventPatient table
                        // This is the patient ID that was stored when the event was created offline
                        val patientId = eventLocalDataSource.getFirstPatientIdForEvent(event.id) ?: 1L
                        println("[DEBUG] Found patientId $patientId for event ${event.id}")

                        // Create a request with the patient ID included in patientIds
                        val request = if (event.patients.isEmpty()) {
                            // If patients list is empty (which is the case for offline-created events),
                            // create a request with the patient ID from the EventPatient table
                            println("[DEBUG] Event has no patients, creating request with patientId $patientId")
                            EventRequest(
                                name = event.name,
                                description = event.description,
                                authorId = event.authorId,
                                eventDateTime = event.eventDateTime,
                                patientIds = setOf(patientId) // Include the patient ID
                            )
                        } else {
                            // Otherwise, use the standard mapping
                            println("[DEBUG] Event has ${event.patients.size} patients, using standard mapping")
                            mapDtoToRequest(event)
                        }

                        println("[DEBUG] Sending createEvent request to API for patientId $patientId")
                        try {
                            val response = eventApiService.createEvent(patientId, request)
                            println("[DEBUG] API response: ${response.code()} ${response.message()}")
                            if (response.isSuccessful) {
                                val serverEvent = response.body()
                                if (serverEvent != null) {
                                    println("[DEBUG] Received server event with ID ${serverEvent.id}")
                                    // Delete the local temporary event and insert the server-generated one
                                    eventLocalDataSource.deleteEvent(event.id)
                                    eventLocalDataSource.insertEvent(mapResponseToDto(serverEvent), "SYNCED")
                                    println("[DEBUG] Updated local database with server event")
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
                        println("[DEBUG] Handling PENDING_UPDATE for event ${event.id}")
                        // Update on server
                        val request = mapDtoToRequest(event)
                        try {
                            val response = eventApiService.updateEvent(event.id, request)
                            println("[DEBUG] API response: ${response.code()} ${response.message()}")
                            if (response.isSuccessful) {
                                // Update sync status to SYNCED
                                eventLocalDataSource.updateSyncStatus(event.id, "SYNCED")
                                println("[DEBUG] Updated sync status to SYNCED for event ${event.id}")
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
                        println("[DEBUG] Handling PENDING_DELETE for event ${event.id}")
                        // Delete on server
                        try {
                            val response = eventApiService.deleteEvent(event.id)
                            println("[DEBUG] API response: ${response.code()} ${response.message()}")
                            if (response.isSuccessful) {
                                // Delete locally
                                eventLocalDataSource.deleteEvent(event.id)
                                println("[DEBUG] Deleted event ${event.id} from local database")
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

            // Fetch all events from server to ensure local database is up-to-date
            println("[DEBUG] Fetching all events from server")
            try {
                val response = eventApiService.getAllEvents()
                println("[DEBUG] API response: ${response.code()} ${response.message()}")
                if (response.isSuccessful) {
                    val serverEvents = response.body() ?: emptyList()
                    println("[DEBUG] Received ${serverEvents.size} events from server")

                    // Get all local events
                    val localEvents = eventLocalDataSource.getAllEvents()
                    println("[DEBUG] Found ${localEvents.size} events in local database")

                    // Create a map of local events by ID for quick lookup
                    val localEventsMap = localEvents.associateBy { it.id }

                    // Process server events
                    serverEvents.forEach { serverEvent ->
                        val serverEventDto = mapResponseToDto(serverEvent)
                        println("[DEBUG] Processing server event ${serverEventDto.id}")
                        val localEvent = localEventsMap[serverEventDto.id]

                        if (localEvent == null) {
                            // Event exists on server but not locally, insert it
                            println("[DEBUG] Event ${serverEventDto.id} exists on server but not locally, inserting")
                            eventLocalDataSource.insertEvent(serverEventDto, "SYNCED")
                        } else if (localEvent.syncStatus == "SYNCED") {
                            // Event exists both locally and on server, and local is synced, update it
                            println("[DEBUG] Event ${serverEventDto.id} exists both locally and on server, updating")
                            eventLocalDataSource.updateEvent(serverEventDto, "SYNCED")
                        } else {
                            // If local event has pending changes, don't overwrite them
                            println("[DEBUG] Event ${serverEventDto.id} has pending changes, not overwriting")
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
     * Converts an EventResponse to an EventDto.
     *
     * @param response The EventResponse to convert
     * @return The converted EventDto
     */
    override fun mapResponseToDto(response: EventResponse): EventDto {
        return EventDto.fromEventResponse(response)
    }

    /**
     * Converts an EventDto to an EventRequest.
     *
     * @param dto The EventDto to convert
     * @return The converted EventRequest
     */
    override fun mapDtoToRequest(dto: EventDto): EventRequest {
        return EventRequest(
            name = dto.name,
            description = dto.description,
            authorId = dto.authorId,
            eventDateTime = dto.eventDateTime,
            patientIds = dto.patients.map { it.id }.toSet()
        )
    }

    /**
     * Gets events that need to be synchronized with the server.
     *
     * @return List of EventDto objects that need to be synchronized
     */
    override suspend fun getEventsToSync(): List<EventDto> = withContext(Dispatchers.IO) {
        return@withContext eventLocalDataSource.getEventsToSync()
    }

    /**
     * Deletes all unsynchronized events from the local database.
     * This includes events with PENDING_CREATE, PENDING_UPDATE, or PENDING_DELETE status.
     *
     * @return The number of events deleted
     */
    override suspend fun deleteUnsynchronizedEvents(): Int = withContext(Dispatchers.IO) {
        try {
            // Get all events that need to be synchronized
            val eventsToDelete = getEventsToSync()

            // Delete each unsynchronized event individually
            eventsToDelete.forEach { event ->
                eventLocalDataSource.deleteEvent(event.id)
            }

            println("[DEBUG] EventRepositoryImpl.deleteUnsynchronizedEvents() deleted ${eventsToDelete.size} events")
            return@withContext eventsToDelete.size
        } catch (e: Exception) {
            println("[DEBUG] Exception during deleteUnsynchronizedEvents: ${e.message}")
            e.printStackTrace()
            return@withContext 0
        }
    }
}
