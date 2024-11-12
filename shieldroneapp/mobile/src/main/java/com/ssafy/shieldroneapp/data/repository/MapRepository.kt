package com.ssafy.shieldroneapp.data.repository

import com.ssafy.shieldroneapp.data.model.LatLng
import com.ssafy.shieldroneapp.data.model.RouteLocation
import com.ssafy.shieldroneapp.data.model.response.HiveResponse

/**
 * 지도 및 위치 정보를 관리하는 리포지토리 인터페이스.
 *
 * - 현재 위치를 GPS로부터 가져오는 기능
 * - 서버와의 API 통신을 통해, 출발지(드론 정류장) 목록 최초 조회 및 검색 조회
 * - 출발지와 도착지를 로컬에 저장하고 불러오는 기능
 */
interface MapRepository {
    // 현재 GPS 위치
    suspend fun getCurrentLocation(): LatLng

    // 출발지 (드론 정류장) 최초 조회 및 검색 조회 API 요청
    suspend fun getNearbyHives(lat: Double, lng: Double): Result<List<HiveResponse>>
    suspend fun searchHivesByKeyword(keyword: String): Result<List<HiveResponse>>

    // 로컬 저장소에 저장/조회
    suspend fun saveStartLocation(location: RouteLocation) // 출발지
    suspend fun getStartLocation(): RouteLocation?
    suspend fun saveEndLocation(location: RouteLocation) // 도착지
    suspend fun getEndLocation(): RouteLocation?
    suspend fun clearLocationData()
}