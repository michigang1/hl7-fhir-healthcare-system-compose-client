package data.remote.services

import data.model.MedicationRequest
import data.model.MedicationResponse
import retrofit2.Response
import retrofit2.http.*

interface MedicationApiService {

    @GET("medications")
    suspend fun getAllMedications(): Response<List<MedicationResponse>>

    @GET("patients/{patientId}/medications")
    suspend fun getAllByPatient(@Path("patientId") patientId: Long): Response<List<MedicationResponse>>

    @GET("patients/{patientId}/medications/{id}")
    suspend fun getMedicationByPatient(
        @Path("patientId") patientId: Long,
        @Path("id") id: Long
    ): Response<MedicationResponse>

    @POST("medications")
    suspend fun createMedication(
        @Body request: MedicationRequest
    ): Response<MedicationResponse>

    @PUT("patients/{patientId}/medications/{id}")
    suspend fun updateMedication(
        @Path("patientId") patientId: Long,
        @Path("id") id: Long,
        @Body request: MedicationRequest
    ): Response<MedicationResponse>

    @DELETE("patients/{patientId}/medications/{id}")
    suspend fun deleteMedication(
        @Path("patientId") patientId: Long,
        @Path("id") id: Long
    ): Response<Boolean>
}