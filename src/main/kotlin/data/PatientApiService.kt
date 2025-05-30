package data

import data.model.PatientRequest
import data.model.PatientResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface PatientApiService {

    @GET("patients")
    suspend fun getAllPatients(): Response<List<List<PatientResponse>>> // Бэкенд возвращает Flux<List<PatientResponse>>, что для клиента будет одним списком

    @GET("patients/{id}")
    suspend fun getPatientById(@Path("id") id: Long): Response<PatientResponse>

    @POST("patients")
    suspend fun createPatient(@Body patientRequest: PatientRequest): Response<PatientResponse>

    @PUT("patients/{id}")
    suspend fun updatePatient(
        @Path("id") id: Long,
        @Body patientRequest: PatientRequest
    ): Response<PatientResponse>

    @DELETE("patients/{id}")
    suspend fun deletePatient(@Path("id") id: Long): Response<Boolean> // Контроллер возвращает ResponseEntity<Boolean>
}