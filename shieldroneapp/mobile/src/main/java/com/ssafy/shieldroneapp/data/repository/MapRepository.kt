package com.ssafy.shieldroneapp.data.repository

import com.ssafy.shieldroneapp.data.model.LatLng
import com.ssafy.shieldroneapp.data.model.RouteLocation
import com.ssafy.shieldroneapp.data.model.request.HiveSearchRequest
import com.ssafy.shieldroneapp.data.model.response.HiveResponse
import kotlinx.coroutines.flow.Flow

/**
 * 지도 및 위치 정보를 관리하는 리포지토리 인터페이스.
 *
 * - GPS 현재 위치 가져오기, 실시간 위치 업데이트 기능
 * - 서버와의 API 통신을 통해, 출발지(드론 정류장) 목록 최초 조회/검색 조회
 * - 출발지와 도착지 위치 정보를 로컬에 저장/조회
 */
interface MapRepository {
    // 현재 GPS 위치를 한 번 가져옴
    suspend fun getCurrentLocation(): LatLng

    // 실시간 위치 업데이트를 Flow로 제공
    fun getLocationUpdates(): Flow<LatLng>

    // 출발지(드론 정류장) 목록 조회 및 키워드 검색
    suspend fun getNearbyHives(location: LatLng): Result<List<HiveResponse>>
    suspend fun searchHivesByKeyword(keyword: HiveSearchRequest): Result<List<HiveResponse>>

    // 출발지와 도착지 위치 정보를 로컬에 저장/조회/초기화
    suspend fun saveStartLocation(location: RouteLocation) // 출발지
    suspend fun getStartLocation(): RouteLocation?
    suspend fun saveEndLocation(location: RouteLocation) // 도착지
    suspend fun getEndLocation(): RouteLocation?
    suspend fun clearLocationData()
}