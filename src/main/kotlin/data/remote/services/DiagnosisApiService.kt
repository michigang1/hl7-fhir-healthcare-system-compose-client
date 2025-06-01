package data.remote.services

import data.model.DiagnosisRequest // Убедитесь, что этот класс существует
import data.model.DiagnosisResponse // Убедитесь, что этот класс существует
import retrofit2.Response
import retrofit2.http.*

interface DiagnosisApiService {

    @GET("diagnoses")
    suspend fun getAllDiagnoses(): Response<List<DiagnosisResponse>>

    @GET("patients/{patientId}/diagnoses/{diagnosisId}")
    suspend fun getDiagnosisByPatient(
        @Path("patientId") patientId: Long,
        @Path("diagnosisId") diagnosisId: Long
    ): Response<DiagnosisResponse> // Mono<ResponseEntity<DiagnosisResponse>>

    @GET("patients/{patientId}/diagnoses")
    suspend fun getAllDiagnosesByPatient(@Path("patientId") patientId: Long): Response<List<DiagnosisResponse>>

    @POST("diagnoses")
    suspend fun createDiagnosis(
        @Body request: DiagnosisRequest
    ): Response<DiagnosisResponse> // Mono<ResponseEntity<DiagnosisResponse>>

    @PUT("patients/{patientId}/diagnoses/{diagnosisId}")
    suspend fun updateDiagnosis(
        @Path("patientId") patientId: Long,
        @Path("diagnosisId") diagnosisId: Long,
        @Body request: DiagnosisRequest
    ): Response<DiagnosisResponse> // Mono<ResponseEntity<DiagnosisResponse>>

    @DELETE("patients/{patientId}/diagnoses/{diagnosisId}")
    suspend fun deleteDiagnosis(
        @Path("patientId") patientId: Long,
        @Path("diagnosisId") diagnosisId: Long
    ): Response<Boolean> // Mono<ResponseEntity<Boolean>>
}
