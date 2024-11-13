package com.ssafy.shieldroneapp.data.source.local

import com.ssafy.shieldroneapp.data.model.RouteLocation

/**
 * 로컬 저장소에서 출발지 및 도착지 위치 데이터를 관리하기 위한 인터페이스.
 *
 * 출발지 및 도착지 위치 정보를 저장하고 불러오는 기능을 정의
 * 위치 데이터 초기화 기능을 포함하여 필요 시 모든 위치 데이터 제거
 */
interface MapLocalDataSource {

    suspend fun saveStartLocation(location: RouteLocation)
    suspend fun getStartLocation(): RouteLocation?

    suspend fun saveEndLocation(location: RouteLocation)
    suspend fun getEndLocation(): RouteLocation?

    suspend fun clearLocationData()
}