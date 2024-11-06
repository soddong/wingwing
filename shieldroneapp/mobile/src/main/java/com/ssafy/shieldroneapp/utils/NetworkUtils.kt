package com.ssafy.shieldroneapp.utils

/**
 * 네트워크 상태를 확인하는 유틸리티 클래스.
 *
 * 현재 네트워크 연결 상태를 확인하여 인터넷 연결 여부를 반환하는 메서드를 제공
 * 인터넷 연결이 필요한 작업 전에 네트워크 상태를 확인할 수 있도록 지원
 *
 * 사용: ViewModel이나 Repository에서 API 요청 전에 네트워크가 연결되어 있는지 확인
 *
 * [주의] API 요청 중 발생하는 서버 응답 오류는 ApiService에서 따로 처리
 */

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

object NetworkUtils {
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }
}