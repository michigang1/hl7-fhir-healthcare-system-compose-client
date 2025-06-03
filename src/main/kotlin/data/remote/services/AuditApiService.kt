package data.remote.services

import data.model.AuditEvent
import retrofit2.Response
import retrofit2.http.*

interface AuditApiService {
    @GET("actuator/auditevents")
    suspend fun getAuditEvents(): Response<MutableList<AuditEvent>>
}
