package data.local.db.datasource

import data.local.db.DatabaseManager
import data.model.AuditEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.format.DateTimeFormatter
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

/**
 * Local data source for AuditEvent entities.
 * This class provides methods for accessing and manipulating audit event data in the local database.
 */
class AuditLocalDataSource {
    private val database = DatabaseManager.getDatabase()
    private val auditEventQueries = database.auditEventQueries
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Gets all audit events from the local database.
     *
     * @return List of AuditEvent objects
     */
    suspend fun getAllAuditEvents(): List<AuditEvent> = withContext(Dispatchers.IO) {
        auditEventQueries.getAllAuditEvents().executeAsList().map { it.toAuditEvent() }
    }

    /**
     * Gets an audit event by ID from the local database.
     *
     * @param id The ID of the audit event to get
     * @return The AuditEvent object, or null if not found
     */
    suspend fun getAuditEventById(id: Long): AuditEvent? = withContext(Dispatchers.IO) {
        auditEventQueries.getAuditEventById(id).executeAsOneOrNull()?.toAuditEvent()
    }

    /**
     * Inserts a new audit event into the local database.
     *
     * @param auditEvent The audit event to insert
     * @param syncStatus The synchronization status (default: PENDING_CREATE)
     */
    suspend fun insertAuditEvent(auditEvent: AuditEvent, syncStatus: String = "PENDING_CREATE") = withContext(Dispatchers.IO) {
        auditEventQueries.insertAuditEvent(
            id = auditEvent.id,
            eventDate = auditEvent.eventDate.toString(),
            principal = auditEvent.principal,
            eventTypeRaw = auditEvent.eventTypeRaw,
            eventData = json.encodeToString(auditEvent.data),
            syncStatus = syncStatus
        )
    }

    /**
     * Updates an existing audit event in the local database.
     *
     * @param auditEvent The audit event to update
     * @param syncStatus The synchronization status (default: PENDING_UPDATE)
     */
    suspend fun updateAuditEvent(auditEvent: AuditEvent, syncStatus: String = "PENDING_UPDATE") = withContext(Dispatchers.IO) {
        auditEventQueries.updateAuditEvent(
            eventDate = auditEvent.eventDate.toString(),
            principal = auditEvent.principal,
            eventTypeRaw = auditEvent.eventTypeRaw,
            eventData = json.encodeToString(auditEvent.data),
            syncStatus = syncStatus,
            id = auditEvent.id
        )
    }

    /**
     * Marks an audit event for deletion in the local database.
     *
     * @param id The ID of the audit event to mark for deletion
     */
    suspend fun markAuditEventForDeletion(id: Long) = withContext(Dispatchers.IO) {
        auditEventQueries.markAuditEventForDeletion(id)
    }

    /**
     * Deletes an audit event from the local database.
     *
     * @param id The ID of the audit event to delete
     */
    suspend fun deleteAuditEvent(id: Long) = withContext(Dispatchers.IO) {
        auditEventQueries.deleteAuditEvent(id)
    }

    /**
     * Gets all audit events that need to be synchronized with the server.
     *
     * @return List of AuditEvent objects that need to be synchronized
     */
    suspend fun getAuditEventsToSync(): List<AuditEvent> = withContext(Dispatchers.IO) {
        auditEventQueries.getAuditEventsToSync().executeAsList().map { it.toAuditEvent() }
    }

    /**
     * Updates the synchronization status of an audit event.
     *
     * @param id The ID of the audit event
     * @param syncStatus The new synchronization status
     */
    suspend fun updateSyncStatus(id: Long, syncStatus: String) = withContext(Dispatchers.IO) {
        auditEventQueries.updateSyncStatus(syncStatus, id)
    }

    /**
     * Converts an AuditEvent database entity to an AuditEvent model.
     */
    @Suppress("UNCHECKED_CAST")
    private fun data.local.db.AuditEvent.toAuditEvent(): AuditEvent {
        val dataMap = try {
            json.decodeFromString<Map<String, String?>>(eventData)
        } catch (e: Exception) {
            mapOf<String, String?>()
        }
        
        return AuditEvent(
            eventDate = Instant.parse(eventDate),
            principal = principal,
            eventTypeRaw = eventTypeRaw,
            data = dataMap
        )
    }
}