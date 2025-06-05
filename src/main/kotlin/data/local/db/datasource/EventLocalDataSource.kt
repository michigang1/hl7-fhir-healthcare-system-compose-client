package data.local.db.datasource

import data.local.db.DatabaseManager
import data.model.EventDto
import data.model.PatientDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Local data source for Event entities.
 * This class provides methods for accessing and manipulating event data in the local database.
 */
class EventLocalDataSource {
    private val database = DatabaseManager.getDatabase()
    private val eventQueries = database.eventQueries
    private val dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    /**
     * Gets all events from the local database.
     *
     * @return List of EventDto objects
     */
    suspend fun getAllEvents(): List<EventDto> = withContext(Dispatchers.IO) {
        eventQueries.getAllEvents().executeAsList().map { event ->
            val patients = getPatientsByEvent(event.id)
            event.toEventDto(patients)
        }
    }

    /**
     * Gets an event by ID from the local database.
     *
     * @param id The ID of the event to get
     * @return The EventDto object, or null if not found
     */
    suspend fun getEventById(id: Long): EventDto? = withContext(Dispatchers.IO) {
        val event = eventQueries.getEventById(id).executeAsOneOrNull() ?: return@withContext null
        val patients = getPatientsByEvent(id)
        return@withContext event.toEventDto(patients)
    }

    /**
     * Gets events for a patient from the local database.
     *
     * @param patientId The ID of the patient
     * @return List of EventDto objects
     */
    suspend fun getEventsByPatient(patientId: Long): List<EventDto> = withContext(Dispatchers.IO) {
        eventQueries.getEventsByPatient(patientId).executeAsList().map { event ->
            val patients = getPatientsByEvent(event.id)
            event.toEventDto(patients)
        }
    }

    /**
     * Inserts a new event into the local database.
     *
     * @param event The event to insert
     * @param syncStatus The synchronization status (default: PENDING_CREATE)
     */
    suspend fun insertEvent(event: EventDto, syncStatus: String = "PENDING_CREATE") = withContext(Dispatchers.IO) {
        // Insert the event
        eventQueries.insertEvent(
            id = event.id,
            name = event.name,
            description = event.description,
            authorId = event.authorId,
            authorUsername = event.authorUsername,
            eventDateTime = event.eventDateTime.format(dateTimeFormatter),
            syncStatus = syncStatus
        )

        // Insert the event-patient relationships
        event.patients.forEach { patient ->
            eventQueries.insertEventPatient(
                eventId = event.id,
                patientId = patient.id
            )
        }
    }

    /**
     * Updates an existing event in the local database.
     *
     * @param event The event to update
     * @param syncStatus The synchronization status (default: PENDING_UPDATE)
     */
    suspend fun updateEvent(event: EventDto, syncStatus: String = "PENDING_UPDATE") = withContext(Dispatchers.IO) {
        // Update the event
        eventQueries.updateEvent(
            name = event.name,
            description = event.description,
            authorId = event.authorId,
            authorUsername = event.authorUsername,
            eventDateTime = event.eventDateTime.format(dateTimeFormatter),
            syncStatus = syncStatus,
            id = event.id
        )

        // Update the event-patient relationships
        eventQueries.deleteEventPatients(event.id)
        event.patients.forEach { patient ->
            eventQueries.insertEventPatient(
                eventId = event.id,
                patientId = patient.id
            )
        }
    }

    /**
     * Marks an event for deletion in the local database.
     *
     * @param id The ID of the event to mark for deletion
     */
    suspend fun markEventForDeletion(id: Long) = withContext(Dispatchers.IO) {
        eventQueries.markEventForDeletion(id)
    }

    /**
     * Deletes an event from the local database.
     *
     * @param id The ID of the event to delete
     */
    suspend fun deleteEvent(id: Long) = withContext(Dispatchers.IO) {
        // Delete the event-patient relationships first
        eventQueries.deleteEventPatients(id)
        // Then delete the event
        eventQueries.deleteEvent(id)
    }

    /**
     * Gets all events that need to be synchronized with the server.
     *
     * @return List of EventDto objects that need to be synchronized
     */
    suspend fun getEventsToSync(): List<EventDto> = withContext(Dispatchers.IO) {
        eventQueries.getEventsToSync().executeAsList().map { event ->
            val patients = getPatientsByEvent(event.id)
            event.toEventDto(patients)
        }
    }

    /**
     * Updates the synchronization status of an event.
     *
     * @param id The ID of the event
     * @param syncStatus The new synchronization status
     */
    suspend fun updateSyncStatus(id: Long, syncStatus: String) = withContext(Dispatchers.IO) {
        eventQueries.updateSyncStatus(syncStatus, id)
    }

    /**
     * Inserts an event-patient relationship into the local database.
     *
     * @param eventId The ID of the event
     * @param patientId The ID of the patient
     */
    suspend fun insertEventPatient(eventId: Long, patientId: Long) = withContext(Dispatchers.IO) {
        eventQueries.insertEventPatient(eventId, patientId)
    }

    /**
     * Gets the first patient ID for an event from the local database.
     * This is useful for events created in offline mode, where we need to know which patient the event was created for.
     *
     * @param eventId The ID of the event
     * @return The ID of the first patient associated with the event, or null if none
     */
    suspend fun getFirstPatientIdForEvent(eventId: Long): Long? = withContext(Dispatchers.IO) {
        val patients = getPatientsByEvent(eventId)
        return@withContext patients.firstOrNull()?.id
    }

    /**
     * Gets patients for an event from the local database.
     *
     * @param eventId The ID of the event
     * @return List of PatientDto objects
     */
    private suspend fun getPatientsByEvent(eventId: Long): List<PatientDto> = withContext(Dispatchers.IO) {
        eventQueries.getPatientsByEvent(eventId).executeAsList().map { patient ->
            PatientDto(
                id = patient.id,
                name = patient.name,
                surname = patient.surname,
                roomNo = patient.roomNo,
                dateOfBirth = patient.dateOfBirth,
                gender = patient.gender,
                address = patient.address,
                email = patient.email,
                phone = patient.phone,
                identifier = patient.identifier,
                organizationId = patient.organizationId,
                syncStatus = patient.syncStatus
            )
        }
    }

    /**
     * Converts an Event database entity to an EventDto.
     */
    private fun data.local.db.Event.toEventDto(patients: List<PatientDto>): EventDto {
        return EventDto(
            id = id,
            name = name,
            description = description,
            authorId = authorId,
            authorUsername = authorUsername,
            eventDateTime = LocalDateTime.parse(eventDateTime, dateTimeFormatter),
            patients = patients,
            syncStatus = syncStatus // Include the syncStatus from the database
        )
    }
}
