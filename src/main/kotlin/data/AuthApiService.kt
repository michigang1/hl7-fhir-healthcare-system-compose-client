package data

import data.model.JwtResponse
import data.model.LoginRequest
import data.model.SignUpRequest
import data.model.SignUpResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {
    @POST("auth/signin") // Укажите ваш реальный эндпоинт
    suspend fun login(@Body loginRequest: LoginRequest): JwtResponse

    @POST("auth/signup") // Укажите ваш реальный эндпоинт
    suspend fun register(@Body signupRequest: SignUpRequest): SignUpResponse // SignupRequest нужно будет создать
}
