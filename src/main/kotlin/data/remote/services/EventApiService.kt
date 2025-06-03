package data.remote.services

import data.model.EventRequest
import data.model.EventResponse
import retrofit2.Response
import retrofit2.http.*

interface EventApiService {
    @GET("events")
    suspend fun getAllEvents(): Response<List<EventResponse>>

    @GET("events/{id}")
    suspend fun getEventById(@Path("id") id: Long): Response<EventResponse>

    @PUT("events/{id}")
    suspend fun updateEvent(
        @Path("id") id: Long,
        @Body eventRequest: EventRequest
    ): Response<EventResponse>

    @DELETE("events/{id}")
    suspend fun deleteEvent(@Path("id") id: Long): Response<Boolean>

    @GET("patients/{patientId}/events")
    suspend fun getEventsByPatient(@Path("patientId") patientId: Long): Response<List<EventResponse>>

    @POST("patients/{patientId}/events")
    suspend fun createEvent(
        @Path("patientId") patientId: Long,
        @Body eventRequest: EventRequest
    ): Response<EventResponse>
}