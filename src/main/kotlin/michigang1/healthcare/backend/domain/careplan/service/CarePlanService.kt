package michigang1.healthcare.backend.domain.careplan.service

import michigang1.healthcare.backend.domain.careplan.payload.GoalDto
import michigang1.healthcare.backend.domain.careplan.payload.MeasureDto

interface CarePlanService {
    suspend fun getCarePlanByPatientId(patientId: Long): List<MeasureDto>

    suspend fun createGoal(goalDto: GoalDto): GoalDto

    suspend fun createMeasure(goalId: Long, measureDto: MeasureDto): MeasureDto

    suspend fun getGoalById(goalId: Long): GoalDto?

    suspend fun getMeasureById(goalId: Long, measureId: Long): MeasureDto?

    suspend fun updateGoal(goalId: Long, goalDto: GoalDto): GoalDto

    suspend fun updateMeasure(goalId: Long, measureId: Long, measureDto: MeasureDto): MeasureDto

    suspend fun deleteGoal(goalId: Long)

    suspend fun deleteMeasure(goalId: Long, measureId: Long)

    suspend fun getAllGoalsByPatient(patientId: Long): List<GoalDto>
}
