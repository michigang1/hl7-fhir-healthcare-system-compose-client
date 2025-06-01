package data.model

import java.time.LocalDate

/**
 * Data Transfer Object for Diagnosis.
 */
data class DiagnosisDto(
    val id: Long,
    val patientId: Long,
    val diagnosisCode: String,
    val isPrimary: Boolean,
    val description: String,
    val date: LocalDate,
    val prescribedBy: String
)