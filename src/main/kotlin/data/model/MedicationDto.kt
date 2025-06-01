package data.model

import java.time.LocalDate

/**
 * Data Transfer Object for Medication.
 */
data class MedicationDto(
    val id: Long,
    val patientId: Long,
    val medicationName: String,
    val dosage: String,
    val frequency: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val prescribedBy: String
)