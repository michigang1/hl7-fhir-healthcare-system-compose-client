package data.model

import kotlinx.serialization.Serializable

@Serializable
data class PatientRequest(
    val id: Long,
    val name: String,
    val surname: String,
    val roomNo: String,
    val dateOfBirth: String, // Дату можно также парсить в LocalDate, если настроить адаптер
    val gender: String,
    val address: String,
    val email: String,
    val phone: String,
    val identifier: Long,
    val organizationId: Long

)
