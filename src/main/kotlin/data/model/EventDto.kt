package data.model

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class EventDto(
    val id: Long,
    val name: String,
    val description: String,
    val authorId: Long,
    val authorUsername: String,
    val eventDateTime: LocalDateTime,
    val patients: List<PatientDto>
) {
    // Convert EventResponse to EventDto
    companion object {
        fun fromEventResponse(response: EventResponse): EventDto {
            return EventDto(
                id = response.id,
                name = response.name,
                description = response.description,
                authorId = response.authorId,
                authorUsername = response.authorUsername,
                eventDateTime = response.eventDateTime,
                patients = response.patients.map { 
                    PatientDto(
                        id = it.id,
                        name = it.name,
                        surname = it.surname,
                        roomNo = it.roomNo,
                        dateOfBirth = it.dateOfBirth,
                        gender = it.gender,
                        address = it.address,
                        email = it.email,
                        phone = it.phone,
                        identifier = it.identifier,
                        organizationId = it.organizationId
                    )
                }
            )
        }
    }

    // Convert to EventData for UI display
    fun toEventData(): EventData {
        return EventData(
            id = id.toInt(),
            theme = name,
            text = description,
            patients = patients.map { "${it.name} ${it.surname}" },
            author = authorUsername,
            time = eventDateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
        )
    }
}
