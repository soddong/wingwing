package com.ssafy.shieldroneapp.data.source.remote

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Named

class KakaoAuthorizationInterceptor @Inject constructor(
    @Named("KAKAO_API_KEY") private val apiKey: String
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val newRequest = originalRequest.newBuilder().apply {
            header("Authorization", "KakaoAK $apiKey")
            header("Accept", "application/json")
            header("KA", buildKAHeader())
        }.build()
        return chain.proceed(newRequest)
    }

    private fun buildKAHeader(): String {
        return "mapSdk/2.12.8" +  // SDK 버전
                " os/android-31" +  // Android API 레벨
                " lang/ko-KR" +  // 언어 설정
                " origin/WPqqvQvN3YOjbnp0DHvIfruixY4=" +  // 고정값 사용
                " device/${android.os.Build.MODEL}" +  // 기기 모델
                " android_pkg/com.ssafy.shieldroneapp"  // 패키지명
    }
}
