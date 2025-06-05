package data.model

import kotlinx.serialization.Serializable
import java.time.LocalDate
import data.serializers.LocalDateSerializer

/**
 * Data Transfer Object for Medication.
 */
@Serializable
data class MedicationDto(
    val id: Long,
    val patientId: Long,
    val medicationName: String,
    val dosage: String,
    val frequency: String,
    @Serializable(with = LocalDateSerializer::class)
    val startDate: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val endDate: LocalDate,
    val prescribedBy: String,
    val syncStatus: String = "SYNCED" // SYNCED, PENDING_CREATE, PENDING_UPDATE, PENDING_DELETE
)
