package com.ssafy.shieldroneapp.data.source.remote

/**
 * 서버와의 HTTP 통신을 담당하는 Retrofit 인터페이스.
 *
 * 로그인, 회원가입, 출발지 및 도착지 전송, 드론 배정 등의 API 요청을 정의.
 * 서버로부터의 응답 데이터를 관리하며, 성공 및 오류 응답에 대한 처리를 포함.
 *
 * 사용 방식:
 * - Retrofit을 사용하여 API 요청을 전송하고, 서버의 응답을 처리합니다.
 * - 네트워크 상태 확인은 NetworkUtils를 통해 별도로 수행하며,
 *   서버 응답 오류 처리는 ApiService 내에서 집중적으로 처리합니다.
 */

import com.ssafy.shieldroneapp.data.model.Guardian
import com.ssafy.shieldroneapp.data.model.request.CodeVerificationRequest
import com.ssafy.shieldroneapp.data.model.request.PhoneNumberRequest
import com.ssafy.shieldroneapp.data.model.request.TokenRequest
import com.ssafy.shieldroneapp.data.model.request.UserAuthRequest
import com.ssafy.shieldroneapp.data.model.request.VerificationCodeRequest
import com.ssafy.shieldroneapp.data.model.response.TokenResponse
import com.ssafy.shieldroneapp.data.model.response.VerificationResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {
    @POST("users/send")
    suspend fun sendVerificationCode(@Body requestBody: VerificationCodeRequest): Response<Unit>

    @POST("users/verify")
    suspend fun verifyCode(
        @Body requestBody: CodeVerificationRequest
    ): Response<VerificationResponse>

    @POST("users/sign-up")
    suspend fun signUp(@Body requestBody: UserAuthRequest): Response<Unit>

    @POST("users/sign-in")
    suspend fun signIn(@Body requestBody: PhoneNumberRequest): Response<TokenResponse>

    @POST("users/refresh")
    suspend fun refreshToken(@Body requestBody: TokenRequest): Response<TokenResponse>

    @POST("settings/end-pos")
    suspend fun setEndPos(
        @Query("homeAddress") homeAddress: String,
        @Query("lat") lat: Double,
        @Query("lng") lng: Double
    )

    @POST("/settings/guardian")
    suspend fun addGuardian(@Body guardian: Guardian)
}