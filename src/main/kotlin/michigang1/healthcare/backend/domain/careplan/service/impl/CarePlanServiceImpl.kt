package michigang1.healthcare.backend.domain.careplan.service.impl

import data.remote.services.CarePlanApiService
import michigang1.healthcare.backend.domain.careplan.payload.GoalDto
import michigang1.healthcare.backend.domain.careplan.payload.MeasureDto
import michigang1.healthcare.backend.domain.careplan.service.CarePlanService
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class CarePlanServiceImpl : CarePlanService, KoinComponent {
    private val carePlanApiService: CarePlanApiService by inject()

    override suspend fun getCarePlanByPatientId(patientId: Long): List<MeasureDto> {
        val response = carePlanApiService.getCarePlansByPatient(patientId)
        if (response.isSuccessful) {
            return response.body()?.map { mapToDomainMeasureDto(it) } ?: emptyList()
        }
        throw Exception("Failed to get care plan: ${response.errorBody()?.string()}")
    }

    override suspend fun createGoal(goalDto: GoalDto): GoalDto {
        val response = carePlanApiService.createGoal(goalDto.patientId, mapToDataGoalDto(goalDto))
        if (response.isSuccessful) {
            return mapToDomainGoalDto(response.body()!!)
        }
        throw Exception("Failed to create goal: ${response.errorBody()?.string()}")
    }

    override suspend fun createMeasure(goalId: Long, measureDto: MeasureDto): MeasureDto {
        val response = carePlanApiService.createMeasure(measureDto.goalId, goalId, mapToDataMeasureDto(measureDto))
        if (response.isSuccessful) {
            return mapToDomainMeasureDto(response.body()!!)
        }
        throw Exception("Failed to create measure: ${response.errorBody()?.string()}")
    }

    override suspend fun getGoalById(goalId: Long): GoalDto? {
        val response = carePlanApiService.getGoalById(goalId = goalId)
        if (response.isSuccessful) {
            return response.body()?.let { mapToDomainGoalDto(it) }
        }
        if (response.code() == 404) {
            return null
        }
        throw Exception("Failed to get goal: ${response.errorBody()?.string()}")
    }

    override suspend fun getMeasureById(goalId: Long, measureId: Long): MeasureDto? {
        val response = carePlanApiService.getMeasureById(goalId = goalId, measureId = measureId)
        if (response.isSuccessful) {
            return response.body()?.let { mapToDomainMeasureDto(it) }
        }
        if (response.code() == 404) {
            return null
        }
        throw Exception("Failed to get measure: ${response.errorBody()?.string()}")
    }

    override suspend fun updateGoal(goalId: Long, goalDto: GoalDto): GoalDto {
        val response = carePlanApiService.updateGoal(goalId = goalId, goalDto = mapToDataGoalDto(goalDto))
        if (response.isSuccessful) {
            return mapToDomainGoalDto(response.body()!!)
        }
        throw Exception("Failed to update goal: ${response.errorBody()?.string()}")
    }

    override suspend fun updateMeasure(goalId: Long, measureId: Long, measureDto: MeasureDto): MeasureDto {
        val response = carePlanApiService.updateMeasure(
            goalId = goalId,
            measureId = measureId,
            measureDto = mapToDataMeasureDto(measureDto)
        )
        if (response.isSuccessful) {
            return mapToDomainMeasureDto(response.body()!!)
        }
        throw Exception("Failed to update measure: ${response.errorBody()?.string()}")
    }

    override suspend fun deleteGoal(goalId: Long) {
        val response = carePlanApiService.deleteGoal(goalId = goalId)
        if (!response.isSuccessful) {
            throw Exception("Failed to delete goal: ${response.errorBody()?.string()}")
        }
    }

    override suspend fun deleteMeasure(goalId: Long, measureId: Long) {
        val response = carePlanApiService.deleteMeasure(goalId = goalId, measureId = measureId)
        if (!response.isSuccessful) {
            throw Exception("Failed to delete measure: ${response.errorBody()?.string()}")
        }
    }

    override suspend fun getAllGoalsByPatient(patientId: Long): List<GoalDto> {
        val response = carePlanApiService.getAllGoalsByPatient(patientId)
        if (response.isSuccessful) {
            return response.body()?.map { mapToDomainGoalDto(it) } ?: emptyList()
        }
        throw Exception("Failed to get goals: ${response.errorBody()?.string()}")
    }

    // Mapping functions
    private fun mapToDataGoalDto(goalDto: GoalDto): data.model.GoalDto {
        return data.model.GoalDto(
            id = goalDto.id,
            patientId = goalDto.patientId,
            name = goalDto.name,
            description = goalDto.description,
            frequency = goalDto.frequency,
            duration = goalDto.duration
        )
    }

    private fun mapToDomainGoalDto(goalDto: data.model.GoalDto): GoalDto {
        return GoalDto(
            id = goalDto.id,
            patientId = goalDto.patientId,
            name = goalDto.name,
            description = goalDto.description,
            frequency = goalDto.frequency,
            duration = goalDto.duration
        )
    }

    private fun mapToDataMeasureDto(measureDto: MeasureDto): data.model.MeasureDto {
        return data.model.MeasureDto(
            id = measureDto.id,
            goalId = measureDto.goalId,
            name = measureDto.name,
            description = measureDto.description,
            scheduledDateTime = measureDto.scheduledDateTime,
            isCompleted = measureDto.isCompleted
        )
    }

    private fun mapToDomainMeasureDto(measureDto: data.model.MeasureDto): MeasureDto {
        return MeasureDto(
            id = measureDto.id,
            goalId = measureDto.goalId,
            name = measureDto.name,
            description = measureDto.description,
            scheduledDateTime = measureDto.scheduledDateTime,
            isCompleted = measureDto.isCompleted
        )
    }
}
