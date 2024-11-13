package com.ssafy.shieldroneapp.data.source.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.ssafy.shieldroneapp.data.model.Guardian
import com.ssafy.shieldroneapp.data.model.User
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ssafy.shieldroneapp.data.model.request.HomeLocationRequest
import com.ssafy.shieldroneapp.data.model.response.HomeLocationResponse
import com.ssafy.shieldroneapp.data.model.response.TokenResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserLocalDataSourceImpl @Inject constructor(
    context: Context,
    private val gson: Gson
) : UserLocalDataSource {
    /**
     * 0. EncryptedSharedPreferences 설정으로 보안 강화
     *
     * - masterKeyAlias: AES256_GCM 방식의 마스터 키 생성
     * - sharedPreferences: EncryptedSharedPreferences를 사용해
     *   AES256_SIV 및 AES256_GCM 암호화 스키마로 데이터를 안전하게 저장
     *
     * 민감한 정보를 암호화하여 보안을 강화합니다.
     */
    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        "user_encrypted_prefs",
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    /**
     * 1-1. 인증 토큰 저장 (비동기)
     *
     * @param tokens 저장할 인증 토큰 (액세스 토큰과 리프레시 토큰)
     */
    override suspend fun saveTokens(tokens: TokenResponse) {
        sharedPreferences.edit().apply {
            putString("accessToken", tokens.accessToken)
            putString("refreshToken", tokens.refreshToken)
            apply()
        }
    }

    /**
     * 1-2. 인증 토큰 저장 (동기)
     *
     * 특정 목적(ApiInterceptor에서 토큰을 동기적으로 저장하려고)을 위해 생성
     * SharedPreferences에 동기적으로 토큰을 저장
     *
     * @param tokens 저장할 인증 토큰 (액세스 토큰과 리프레시 토큰)
     */
    fun saveTokensSync(tokens: TokenResponse) {
        sharedPreferences.edit().apply {
            putString("accessToken", tokens.accessToken)
            putString("refreshToken", tokens.refreshToken)
            apply() // 동기 저장
        }
    }

    /**
     * 2-1. 저장된 인증 토큰 불러오기 (비동기)
     *
     * @return 저장된 인증 토큰, 없을 경우 null 반환
     */
    override suspend fun getTokens(): TokenResponse? {
        val accessToken = sharedPreferences.getString("accessToken", null)
        val refreshToken = sharedPreferences.getString("refreshToken", null)
        return if (accessToken != null && refreshToken != null) {
            TokenResponse(accessToken, refreshToken)
        } else {
            null
        }
    }

    /**
     * 2-2. 저장된 인증 토큰 불러오기 (동기)
     *
     * 특정 목적(ApiInterceptor에서 토큰을 동기적으로 가져오려고)을 위해 생성
     * SharedPreferences에 동기적으로 접근
     *
     * @return 저장된 인증 토큰, 없을 경우 null 반환
     */
    fun getTokensSync(): TokenResponse? {
        val accessToken = sharedPreferences.getString("accessToken", null)
        val refreshToken = sharedPreferences.getString("refreshToken", null)
        return if (accessToken != null && refreshToken != null) {
            TokenResponse(accessToken, refreshToken)
        } else {
            null
        }
    }

    /**
     * 3. 사용자 정보 저장
     *
     * @param user 저장할 사용자 정보
     */
    override suspend fun saveUser(user: User) {
        val userJson = gson.toJson(user)
        sharedPreferences.edit().putString("user", userJson).apply()
    }

    /**
     * 4. 저장된 사용자 정보 불러오기
     *
     * @return 저장된 사용자 정보, 없을 경우 null 반환
     */
    override suspend fun getUser(): User? {
        val userJson = sharedPreferences.getString("user", null) ?: return null
        return gson.fromJson(userJson, User::class.java)
    }

    /**
     * 5. 기본 도착지 정보 저장
     * @param location 기본 도착지 요청 데이터 (도로명 주소, 위도, 경도 포함)
     */
    override suspend fun saveHomeLocation(location: HomeLocationRequest) {
        sharedPreferences.edit().putString("homeLocation", gson.toJson(location)).apply()
    }

    /**
     * 6. 저장된 기본 도착지 정보 불러오기
     * @return 저장된 기본 도착지 요청 데이터, 없을 경우 null 반환
     */
    override suspend fun getHomeLocation(): HomeLocationResponse? {
        val locationJson = sharedPreferences.getString("homeLocation", null) ?: return null
        return gson.fromJson(locationJson, HomeLocationResponse::class.java)
    }

    /**
     * 7. 보호자 정보 추가
     *
     * @param guardian 추가할 보호자 정보 (유저와의 관계, 핸드폰 번호)
     * @return 성공 여부 (보호자 추가 성공 시 true, 실패 시 false)
     */
    override suspend fun addGuardian(guardian: Guardian): Boolean {
        val guardiansJson = sharedPreferences.getString("guardians", "[]")
        val type = object : TypeToken<MutableList<Guardian>>() {}.type
        val guardians: MutableList<Guardian> = gson.fromJson(guardiansJson, type)
        if (guardians.size >= 3) return false
        guardians.add(guardian)
        sharedPreferences.edit().putString("guardians", gson.toJson(guardians)).apply()
        return true
    }

    /**
     * 8. 저장된 보호자 정보 불러오기
     *
     * @return 저장된 보호자 정보 리스트, 없을 경우 빈 리스트 반환
     */
    override suspend fun getGuardians(): List<Guardian> {
        val guardiansJson = sharedPreferences.getString("guardians", "[]")
        val type = object : TypeToken<List<Guardian>>() {}.type
        return gson.fromJson(guardiansJson, type)
    }

    /**
     * 9. 저장된 모든 사용자 데이터 삭제
     *
     * 사용자가 로그아웃할 때 호출하여 로컬에 저장된 모든 사용자 정보를 삭제합니다.
     */
    override suspend fun clearUserData() {
        sharedPreferences.edit().clear().apply()
    }

    /**
     * 10. 로그인 상태 확인
     *
     * @return 사용자가 로그인되어 있는지 여부 (저장된 액세스 토큰이 있는 경우 true)
     */
    override suspend fun isLoggedIn(): Boolean {
        val accessToken = sharedPreferences.getString("accessToken", null)
        return accessToken != null
    }
}
