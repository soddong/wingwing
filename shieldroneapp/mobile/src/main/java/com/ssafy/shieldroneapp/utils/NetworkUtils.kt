package com.ssafy.shieldroneapp.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * 네트워크 상태와 API 호출 처리를 담당하는 유틸리티 클래스.
 *
 * 현재 네트워크 연결 상태를 확인하고,
 * 네트워크가 연결된 경우에만 API 요청을 실행하여 결과를 반환하는 기능을 제공합니다.
 */
object NetworkUtils {
    /**
     * 1. 네트워크 상태 확인
     *
     * 현재 네트워크 연결 상태를 확인하고,인터넷 연결 여부를 반환하는 메서드
     * 인터넷 연결이 필요한 작업 전에 네트워크 상태를 확인할 수 있도록 지원
     *
     * @param context 앱의 Context 객체
     * @return 네트워크가 연결되어 있다면 true, 아니면 false
     */
    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    /**
     * 2. 네트워크 연결된 경우에만 API 호출을 실행, 결과를 Result로 감싸 반환.
     *
     * UserRepositoryImpl, DroneRepositoryImpl 등에서 API 호출 시 활용될 수 있습니다.
     *
     * [Result.success]와 [Result.failure]를 사용하여 API 호출 결과를
     * Result로 감싸기 때문에 호출하는 측에서는 별도로 Result로 감쌀 필요가 없습니다.
     *
     * @param context 앱의 Context 객체
     * @param block 네트워크가 연결된 경우 실행할 API 요청 블록
     * @return API 요청 결과를 Result로 감싼 값. 네트워크 오류나 예외 발생 시 Result.failure 반환
     */
    suspend fun <T> apiCallAfterNetworkCheck(context: Context, block: suspend () -> T): Result<T> {
        return if (isNetworkAvailable(context)) {
            try {
                Result.success(block())
            } catch (e: Exception) {
                Result.failure(e)
            }
        } else {
            Result.failure(Exception("네트워크 연결이 없습니다.")) // 네트워크 미연결 시 에러 반환
        }
    }

}