package com.ssafy.shieldroneapp.data.repository

import com.ssafy.shieldroneapp.data.model.Guardian
import com.ssafy.shieldroneapp.data.model.Tokens
import com.ssafy.shieldroneapp.data.model.User
import com.ssafy.shieldroneapp.data.model.UserAuthData

/**
 * 사용자 정보 및 설정을 관리하는 리포지토리 인터페이스
 *
 * [주요 역할]
 * API 서비스와 로컬 데이터 소스 간의 데이터 흐름 조정 및 동기화
 * 사용자 인증, 회원가입, 로그인 및 인증 토큰 관리
 * 기본 도착지(집) 설정 및 보호자 등록 관리
 *
 * [데이터 처리 흐름]
 * ViewModel의 요청을 받아 API 호출 또는 로컬 데이터 접근
 * 데이터 가공 및 에러 처리 후 결과 반환
 *
 * [에러 처리]
 * 모든 작업을 Result 타입으로 래핑하여 반환
 * 네트워크 및 유효성 검증 오류 처리
 *
 * @property apiService 서버 API 서비스 인터페이스
 * @property userLocalDataSource 로컬 데이터 소스 인터페이스
 */

interface UserRepository {
    suspend fun requestVerification(phoneNumber: String): Result<Unit>
    suspend fun verifyCode(phoneNumber: String, code: String): Result<Pair<Boolean, Boolean>>

    suspend fun registerUser(userData: UserAuthData): Result<User>
    suspend fun loginUser(phoneNumber: String): Result<Tokens>

    suspend fun saveTokens(tokens: Tokens)
    suspend fun refreshAccessToken(refreshToken: String): Result<Tokens>

    suspend fun setEndPos(homeAddress: String, lat: Double, lng: Double): Result<Unit>
    suspend fun addGuardian(newGuardian: Guardian): Result<Unit>
}
