package com.ssafy.shieldroneapp.data.repository

import com.ssafy.shieldroneapp.data.model.Guardian
import com.ssafy.shieldroneapp.data.model.User
import com.ssafy.shieldroneapp.data.model.request.UserAuthRequest
import com.ssafy.shieldroneapp.data.model.response.TokenResponse
import com.ssafy.shieldroneapp.data.model.response.VerificationResponse

/**
 * 사용자 인증 및 설정을 관리하는 리포지토리 인터페이스
 *
 * [주요 역할]
 * - 네트워크(API) 및 로컬 데이터베이스와의 통신을 처리
 * - 사용자 인증, 회원가입, 로그인 및 인증 토큰 관리
 * - 기본 도착지(집) 설정 및 보호자 등록 관리
 *
 * [데이터 처리 흐름]
 * - ViewModel의 요청을 받아 API 호출 또는 로컬 데이터 접근
 * - 데이터 가공 및 에러 처리 후 결과를 ViewModel로 전달 (하여 UI 상태 업데이트 지원)
 *
 * [에러 처리]
 * - 모든 작업을 `Result` 타입으로 감싸서 반환하여 성공/실패 상태를 명확히 전달
 * - 네트워크 및 유효성 검증 오류를 통합 관리
 */
interface UserRepository {
    suspend fun requestVerification(phoneNumber: String): Result<Unit>
    suspend fun verifyCode(phoneNumber: String, code: String): Result<VerificationResponse>

    suspend fun registerUser(userData: UserAuthRequest): Result<Unit>
    suspend fun loginUser(phoneNumber: String): Result<TokenResponse>

    suspend fun saveTokens(tokens: TokenResponse)
    suspend fun refreshAccessToken(refreshToken: String): Result<TokenResponse>

    suspend fun setEndPos(homeAddress: String, lat: Double, lng: Double): Result<Unit>
    suspend fun addGuardian(newGuardian: Guardian): Result<Unit>
}
