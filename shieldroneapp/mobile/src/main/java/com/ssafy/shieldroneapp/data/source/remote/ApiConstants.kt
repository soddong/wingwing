package com.ssafy.shieldroneapp.data.source.remote

/**
 * API 관련 상수를 관리하는 파일.
 *
 * API 엔드포인트 및 HTTP 상태 코드를 정의.
 */

object ApiConstants {
    const val BASE_URL = "https://k11a307.p.ssafy.io/api/" // API 기본 URL
    const val TIMEOUT = 30L // 네트워크 요청 타임아웃 (초 단위)

    // HTTP 상태 코드
    const val STATUS_OK = 200
    const val STATUS_UNAUTHORIZED = 401
    const val STATUS_FORBIDDEN = 403
    const val STATUS_NOT_FOUND = 404
    const val STATUS_INTERNAL_SERVER_ERROR = 500

    // [네트워크] 인증 헤더가 필요 없는 API 엔드포인트들
    val NO_AUTH_URLS = listOf(
        "/users/send",
        "/users/verify",
        "/users/sign-in",
        "/users/sign-up"
    )
}