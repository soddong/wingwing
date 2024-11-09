package com.ssafy.shieldroneapp.utils

/**
 * 앱 전역에서 사용되는 상수 값들을 관리하는 객체
 *
 * 사용
 * - 사용자 관련 상수들
 * - 앱 내 화면 간 Navigation 경로 상수
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
}