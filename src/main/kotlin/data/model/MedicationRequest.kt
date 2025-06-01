package data.model

import kotlinx.serialization.Serializable
import java.time.LocalDate
import data.serializers.LocalDateSerializer

@Serializable
data class MedicationRequest (
    val patientId: Long,
    val medicationName: String,
    val dosage: String,
    val frequency: String,
    @Serializable(with = LocalDateSerializer::class)
    val startDate: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val endDate: LocalDate?,
    val prescribedBy: String
)
