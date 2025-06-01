package data.model

import java.time.LocalDate

data class DiagnosisRequest(
    val id: Long?,

    val code: String,

    val isPrimary: Boolean,

    val description: String,

    val diagnosedAt: LocalDate,

    val diagnosedBy: String
)
