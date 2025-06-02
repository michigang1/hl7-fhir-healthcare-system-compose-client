package michigang1.healthcare.backend.domain.careplan.payload

import java.time.LocalDateTime

data class MeasureDto(
    val id: Long? = null,
    val goalId: Long,
    val name: String,
    val description: String,
    val scheduledDateTime: LocalDateTime,
    val isCompleted: Boolean = false
)