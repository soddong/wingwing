package com.ssafy.shieldroneapp.utils

/**
 * 앱 전역에서 사용되는 상수 값들을 관리하는 객체
 *
 * 사용
 * - 사용자 관련 상수들
 * - 앱 내 화면 간 Navigation 경로 상수
 * - 맵 마커 관련 상수들
 */

object Constants {
    object User {
        const val MAX_GUARDIANS = 3 // 최대 보호자 수
    }

    object Navigation {
        const val ROUTE_LANDING = "landing_screen"
        const val ROUTE_AUTHENTICATION = "authentication"
        const val ROUTE_MAP = "map"
        // const val ROUTE_MAIN = "main_screen"  // 추후 메인 화면 추가 시 사용
    }

    object Marker {
        const val BASE_LAYER = "base_layer"          // 현재 위치 레이어
        const val HIVE_LAYER = "hive_layer"          // 일반 정류장 레이어
        const val SELECTED_LAYER = "selected_layer"  // 선택된 마커 레이어
    }
}