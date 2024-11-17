package com.ssafy.shieldroneapp.data.source.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.google.gson.Gson
import com.ssafy.shieldroneapp.data.model.RouteLocation
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 로컬 저장소에서 출발지 및 도착지 위치 데이터를 관리하는 구현체.
 *
 * EncryptedSharedPreferences를 사용하여 데이터를 암호화하여 안전하게 저장하며,
 * Gson을 이용해 RouteLocation 객체를 JSON 형식으로 직렬화 및 역직렬화하여 저장 및 조회.
 */
@Singleton
class MapLocalDataSourceImpl @Inject constructor(
    context: Context,
    private val gson: Gson
) : MapLocalDataSource {
    /**
     * EncryptedSharedPreferences 설정으로 보안 강화
     *
     * - masterKeyAlias: AES256_GCM 방식의 마스터 키 생성
     * - sharedPreferences: AES256_SIV와 AES256_GCM 암호화 스키마를 사용하여 보안 저장
     */
    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        "map_encrypted_prefs",
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val startLocationKey = "start_location"
    private val endLocationKey = "end_location"

    /**
     * 1. 출발지 위치 저장
     *
     * @param location 저장할 출발지 위치 데이터 (RouteLocation 객체)
     */
    override suspend fun saveStartLocation(location: RouteLocation) {
        val locationJson = gson.toJson(location)
        sharedPreferences.edit().putString(startLocationKey, locationJson).apply()
    }

    /**
     * 2. 저장된 출발지 위치 불러오기
     *
     * @return 저장된 출발지 위치 데이터 (RouteLocation 객체) 또는 null
     */
    override suspend fun getStartLocation(): RouteLocation? {
        val locationJson = sharedPreferences.getString(startLocationKey, null) ?: return null
        return gson.fromJson(locationJson, RouteLocation::class.java)
    }

    /**
     * 3. 저장된 출발지 초기화
     */
    override suspend fun clearStartLocation() {
        sharedPreferences.edit().remove(startLocationKey).apply()
    }

    /**
     * 4. 도착지 위치 저장
     *
     * @param location 저장할 도착지 위치 데이터 (RouteLocation 객체)
     */
    override suspend fun saveEndLocation(location: RouteLocation) {
        val locationJson = gson.toJson(location)
        sharedPreferences.edit().putString(endLocationKey, locationJson).apply()
    }

    /**
     * 5. 저장된 도착지 위치 불러오기
     *
     * @return 저장된 도착지 위치 데이터 (RouteLocation 객체) 또는 null
     */
    override suspend fun getEndLocation(): RouteLocation? {
        val locationJson = sharedPreferences.getString(endLocationKey, null) ?: return null
        return gson.fromJson(locationJson, RouteLocation::class.java)
    }

    /**
     * 6. 저장된 도착지 초기화
     */
    override suspend fun clearEndLocation() {
        sharedPreferences.edit().remove(endLocationKey).apply()
    }
}