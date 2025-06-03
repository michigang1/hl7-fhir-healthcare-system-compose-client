package presentation.state

import data.model.AuditEvent
import java.time.LocalDate

/**
 * Represents the state of the audit events section.
 */
data class AuditState(
    val auditEvents: List<AuditEvent> = emptyList(),
    val selectedDate: LocalDate = LocalDate.now(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)