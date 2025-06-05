package data.model

import kotlinx.serialization.Serializable
import java.time.LocalDate
import data.serializers.LocalDateSerializer

/**
 * Data Transfer Object for Diagnosis.
 */
@Serializable
data class DiagnosisDto(
    val id: Long,
    val patientId: Long,
    val diagnosisCode: String,
    val isPrimary: Boolean,
    val description: String,
    @Serializable(with = LocalDateSerializer::class)
    val date: LocalDate,
    val prescribedBy: String,
    val syncStatus: String = "SYNCED" // SYNCED, PENDING_CREATE, PENDING_UPDATE, PENDING_DELETE
)
