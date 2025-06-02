package data.remote.services

import data.model.CarePlanDto
import data.model.CarePlanRequest
import data.model.GoalDto
import data.model.MeasureDto
import retrofit2.Response
import retrofit2.http.*

interface CarePlanApiService {

    @GET("patients/{patientId}/careplan")
    suspend fun getCarePlanByPatientId(@Path("patientId") patientId: Long): Response<List<MeasureDto>>

    // Alias for getCarePlanByPatientId to maintain compatibility with existing code
    @GET("patients/{patientId}/careplan")
    suspend fun getCarePlansByPatient(@Path("patientId") patientId: Long): Response<List<MeasureDto>>

    @POST("patients/{patientId}/careplan/goals")
    suspend fun createGoal(
        @Path("patientId") patientId: Long = 1L,
        @Body goalDto: GoalDto
    ): Response<GoalDto>

    @POST("patients/{patientId}/careplan/goals/{goalId}/measures")
    suspend fun createMeasure(
        @Path("patientId") patientId: Long = 1L,
        @Path("goalId") goalId: Long,
        @Body measureDto: MeasureDto
    ): Response<MeasureDto>

    @GET("patients/{patientId}/careplan/goals/{goalId}")
    suspend fun getGoalById(
        @Path("patientId") patientId: Long = 1L,
        @Path("goalId") goalId: Long
    ): Response<GoalDto?>

    @GET("patients/{patientId}/careplan/goals/{goalId}/measures/{measureId}")
    suspend fun getMeasureById(
        @Path("patientId") patientId: Long = 1L,
        @Path("goalId") goalId: Long,
        @Path("measureId") measureId: Long
    ): Response<MeasureDto?>

    @PUT("patients/{patientId}/careplan/goals/{goalId}")
    suspend fun updateGoal(
        @Path("patientId") patientId: Long = 1L,
        @Path("goalId") goalId: Long,
        @Body goalDto: GoalDto
    ): Response<GoalDto>

    @PUT("patients/{patientId}/careplan/goals/{goalId}/measures/{measureId}")
    suspend fun updateMeasure(
        @Path("patientId") patientId: Long = 1L,
        @Path("goalId") goalId: Long,
        @Path("measureId") measureId: Long,
        @Body measureDto: MeasureDto
    ): Response<MeasureDto>

    @DELETE("patients/{patientId}/careplan/goals/{goalId}")
    suspend fun deleteGoal(
        @Path("patientId") patientId: Long = 1L,
        @Path("goalId") goalId: Long
    ): Response<Unit>

    @DELETE("patients/{patientId}/careplan/goals/{goalId}/measures/{measureId}")
    suspend fun deleteMeasure(
        @Path("patientId") patientId: Long = 1L,
        @Path("goalId") goalId: Long,
        @Path("measureId") measureId: Long
    ): Response<Unit>

    @GET("patients/{patientId}/careplan/goals")
    suspend fun getAllGoalsByPatient(@Path("patientId") patientId: Long): Response<List<GoalDto>>

    // Get all measures for a goal
    // Using GET as the server should support GET for this endpoint
    @GET("patients/{patientId}/careplan/goals/{goalId}/measures")
    suspend fun getMeasuresByGoalId(
        @Path("patientId") patientId: Long = 1L,
        @Path("goalId") goalId: Long
    ): Response<List<MeasureDto>> // Server should return a list of MeasureDto objects

    // Legacy functions for backward compatibility with existing code
    // These should be replaced with the new functions in future updates

    @GET("patients/{patientId}/careplan/{carePlanId}")
    suspend fun getCarePlanById(
        @Path("patientId") patientId: Long = 1L,
        @Path("carePlanId") carePlanId: Long
    ): Response<CarePlanDto>

    @POST("patients/{patientId}/careplan")
    suspend fun createCarePlan(
        @Path("patientId") patientId: Long = 1L,
        @Body request: CarePlanRequest
    ): Response<CarePlanDto>

    @PUT("patients/{patientId}/careplan/{carePlanId}")
    suspend fun updateCarePlan(
        @Path("patientId") patientId: Long = 1L,
        @Path("carePlanId") carePlanId: Long,
        @Body request: CarePlanRequest
    ): Response<CarePlanDto>

    @DELETE("patients/{patientId}/careplan/{carePlanId}")
    suspend fun deleteCarePlan(
        @Path("patientId") patientId: Long = 1L,
        @Path("carePlanId") carePlanId: Long
    ): Response<Boolean>
}
