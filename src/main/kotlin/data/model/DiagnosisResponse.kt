package data.model

import java.time.LocalDate

data class DiagnosisResponse(
    val id: Long,

    val patientId: Long,

    val code: String,

    val isPrimary: Boolean,

    val description: String,

    val diagnosedAt: LocalDate,

    val diagnosedBy: String,
)
