package com.ssafy.shieldroneapp.data.source.remote

/**
 * API 에러를 처리하는 클래스.
 *
 * 서버 응답 상태 코드에 따라 적절한 에러 메시지를 반환.
 */

object ApiErrorHandler {

    fun getErrorMessage(statusCode: Int): String {
        return when (statusCode) {
            ApiConstants.STATUS_BAD_REQUEST -> "잘못된 요청입니다."
            ApiConstants.STATUS_UNAUTHORIZED -> "인증이 필요합니다. 다시 로그인해 주세요."
            ApiConstants.STATUS_FORBIDDEN -> "접근 권한이 없습니다."
            ApiConstants.STATUS_NOT_FOUND -> "요청한 리소스를 찾을 수 없습니다."
            ApiConstants.STATUS_INTERNAL_SERVER_ERROR -> "서버에 문제가 발생했습니다. 잠시 후 다시 시도해 주세요."
            else -> "알 수 없는 오류가 발생했습니다."
        }
    }
}