package data.repository

import data.model.EventDto
import data.model.EventRequest
import data.model.EventResponse

/**
 * Repository interface for Event entities.
 * This interface defines methods for accessing and manipulating event data,
 * abstracting the data source (local database or remote API).
 */
interface EventRepository {
    /**
     * Gets all events.
     *
     * @return List of EventDto objects
     */
    suspend fun getAllEvents(): List<EventDto>

    /**
     * Gets events for a patient.
     *
     * @param patientId The ID of the patient
     * @return List of EventDto objects
     */
    suspend fun getEventsByPatient(patientId: Long): List<EventDto>

    /**
     * Gets an event by ID.
     *
     * @param eventId The ID of the event to get
     * @return The EventDto object, or null if not found
     */
    suspend fun getEventById(eventId: Long): EventDto?

    /**
     * Creates a new event.
     *
     * @param patientId The ID of the patient
     * @param eventRequest The event data to create
     * @return The created EventDto object
     */
    suspend fun createEvent(patientId: Long, eventRequest: EventRequest): EventDto

    /**
     * Updates an existing event.
     *
     * @param eventId The ID of the event to update
     * @param eventRequest The updated event data
     * @return The updated EventDto object
     */
    suspend fun updateEvent(eventId: Long, eventRequest: EventRequest): EventDto

    /**
     * Deletes an event.
     *
     * @param eventId The ID of the event to delete
     * @return True if the event was deleted, false otherwise
     */
    suspend fun deleteEvent(eventId: Long): Boolean

    /**
     * Gets events that need to be synchronized with the server.
     *
     * @return List of EventDto objects that need to be synchronized
     */
    suspend fun getEventsToSync(): List<EventDto>

    /**
     * Synchronizes local data with the remote server.
     * This method sends pending changes to the server and fetches the latest data.
     *
     * @return True if synchronization was successful, false otherwise
     */
    suspend fun synchronize(): Boolean

    /**
     * Converts an EventResponse to an EventDto.
     *
     * @param response The EventResponse to convert
     * @return The converted EventDto
     */
    fun mapResponseToDto(response: EventResponse): EventDto

    /**
     * Converts an EventDto to an EventRequest.
     *
     * @param dto The EventDto to convert
     * @return The converted EventRequest
     */
    fun mapDtoToRequest(dto: EventDto): EventRequest

    /**
     * Deletes all unsynchronized events from the local database.
     * This includes events with PENDING_CREATE, PENDING_UPDATE, or PENDING_DELETE status.
     *
     * @return The number of events deleted
     */
    suspend fun deleteUnsynchronizedEvents(): Int
}
