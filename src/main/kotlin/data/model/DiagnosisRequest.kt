package data.model

import java.time.LocalDate
import data.serializers.LocalDateSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class DiagnosisRequest(
    @SerialName("id")
    val id: Long?,

    @SerialName("code")
    val code: String?,

    @SerialName("isPrimary")
    val isPrimary: Boolean? = false,

    @SerialName("description")
    val description: String?,

    @SerialName("diagnosedAt")
    @Serializable(with = LocalDateSerializer::class)
    val diagnosedAt: LocalDate?,

    @SerialName("diagnosedBy")
    val diagnosedBy: String?,
)
