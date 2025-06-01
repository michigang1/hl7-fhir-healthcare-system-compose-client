package data.model

import kotlinx.serialization.Serializable
import java.time.LocalDate
import data.serializers.LocalDateSerializer

@Serializable
data class DiagnosisResponse(
    val id: Long,

    val patientId: Long,

    val code: String,

    val isPrimary: Boolean,

    val description: String,

    @Serializable(with = LocalDateSerializer::class)
    val diagnosedAt: LocalDate,

    val diagnosedBy: String,
)
