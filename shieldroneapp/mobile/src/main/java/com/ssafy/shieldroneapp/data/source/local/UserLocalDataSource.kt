package com.ssafy.shieldroneapp.data.source.local

import com.ssafy.shieldroneapp.data.model.Guardian
import com.ssafy.shieldroneapp.data.model.User
import com.ssafy.shieldroneapp.data.model.request.HomeLocationRequest
import com.ssafy.shieldroneapp.data.model.response.HomeLocationResponse
import com.ssafy.shieldroneapp.data.model.response.TokenResponse

/**
 * 로컬 데이터 저장 및 조회
 *
 * 사용자 정보 및 인증 토큰을 로컬에 저장하고 불러오는 기능을 제공합니다.
 * 기본 도착지 및 보호자 정보를 관리하며, 사용자의 로그인 상태를 확인하거나
 * 모든 로컬 데이터를 초기화하는 기능을 포함합니다.
 */

interface UserLocalDataSource {

    suspend fun saveTokens(tokens: TokenResponse)
    suspend fun getTokens(): TokenResponse?

    suspend fun saveUser(user: User)
    suspend fun getUser(): User?

    // 기본 도착지(집) 로컬 저장 및 조회
    suspend fun saveHomeLocation(location: HomeLocationRequest)
    suspend fun getHomeLocation(): HomeLocationResponse?

    suspend fun addGuardian(guardian: Guardian): Boolean
    suspend fun getGuardians(): List<Guardian>

    suspend fun clearUserData()

    suspend fun isLoggedIn(): Boolean
}
