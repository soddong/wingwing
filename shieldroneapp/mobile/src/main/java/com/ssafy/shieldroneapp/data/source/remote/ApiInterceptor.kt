package com.ssafy.shieldroneapp.data.source.remote

/**
 * 공통으로 사용할 API 요청 및 응답 인터셉터 정의
 *
 * API 요청 전
 * - 기본 헤더(Content-Type) 추가
 * - 토큰이 필요 없는 요청과 필요한 요청을 분리
 * - 토큰이 필요한 요청의 경우 액세스 토큰을 Authorization 헤더에 추가
 *
 * API 응답 후
 * - 상태 코드에 따라 적절한 에러 처리
 * - 401 응답 시 토큰 갱신 및 요청 재시도
 */

import com.google.gson.Gson
import com.ssafy.shieldroneapp.data.model.response.ErrorResponse
import com.ssafy.shieldroneapp.data.source.local.UserLocalDataSourceImpl
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject

class ApiInterceptor @Inject constructor(
    private val userLocalDataSource: UserLocalDataSourceImpl, // 토큰을 가져오기 위해
    private val tokenRefresher: TokenRefresher,
    private val gson: Gson
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        // [공통] 기본 헤더(Content-Type) 추가
        val requestBuilder = request.newBuilder()
            .addHeader("Content-Type", "application/json")

        // Authorization 헤더를 추가하지 않는 예외 URL
        // ex) 본인 인증 문자 전송, 인증 코드 검증, 로그인, 회원가입
        val url = request.url.toString()
        val isNoAuthUrl = ApiConstants.NO_AUTH_URLS.any { url.contains(it) }

        // [선택] 토큰이 필요한 요청에만 Authorization 헤더 추가
        if (!isNoAuthUrl) {
            // **동기적으로(OKHttp 특성 때문)** 저장된 인증 토큰 가져오기
            val accessToken = userLocalDataSource.getTokensSync()?.accessToken

            // 토큰이 있는 경우 Authorization 헤더 추가
            if (!accessToken.isNullOrEmpty()) {
                requestBuilder.addHeader("Authorization", "Bearer $accessToken")
            }
        }

        val modifiedRequest = requestBuilder.build()
        val response = chain.proceed(modifiedRequest)

        // 400 응답 처리
        if (response.code == 400) {
            // response.peekBody를 사용하여 okhttp3.Response에서 응답 본문을 읽기
            val errorResponse = response.peekBody(Long.MAX_VALUE).string().let { responseBody ->
                gson.fromJson(responseBody, ErrorResponse::class.java)
            }

            val errorMessage = when (errorResponse?.code) {
                "001" -> "인증 코드가 만료되었습니다."
                "002" -> "유효하지 않은 인증 코드입니다."
                "004" -> "번호 인증이 완료되지 않았습니다."
                "005" -> "이미 가입된 회원입니다."

                // 보호자
                "009" -> "보호자 등록 한도를 초과했습니다."

                // 드론 관련 에러 코드 추가
                "012" -> "이용 가능한 드론이 없습니다."
                "013" -> "유효하지 않은 드론입니다."
                "014" -> "이미 매칭된 유저입니다."
                "015" -> "출발지와 도착지가 동일합니다."
                "016" -> "이미 매칭 중인 드론 정류장입니다."
                "017" -> "이미 매칭된 드론입니다."
                "019" -> "입력된 인증코드가 일치하지 않습니다."

                else -> "알 수 없는 오류가 발생했습니다."
            }

            throw IOException(errorMessage)
        }

        // 인증 실패(401 응답) 처리: 토큰 갱신 및 요청 재시도
        if (response.code == 401) {
            val refreshToken = userLocalDataSource.getTokensSync()?.refreshToken

            // 리프레시 토큰이 없을 경우 예외 처리
            // => TODO: 로그아웃 처리 OR 재인증 요청 로직 필요
            if (refreshToken.isNullOrEmpty()) {
                throw IOException("로그인이 만료되었습니다. 다시 로그인해 주세요.")
            }

            // 리프레시 토큰 있으면, 새로운 액세스 토큰 요청
            try {
                // **OkHttp 인터셉터의 동기적 특성**으로 suspend 함수인 refreshToken을 호출할 수 없어
                // runBlocking을 임시로 사용
                // [주의] runBlocking 사용 시 네트워크 지연이 발생하면 애플리케이션 성능에 영향을 줄 수 있음
                // => TODO: 추후 개선 필요
                val newTokens = runBlocking {
                    tokenRefresher.refreshToken(refreshToken)
                }.body() ?: throw IOException("토큰 갱신 실패")
                userLocalDataSource.saveTokensSync(newTokens) // **동기적으로(OKHttp 특성 때문)** 저장

                // 새로운 액세스 토큰을 사용하여 원래 요청 재시도
                val newRequest = requestBuilder
                    .removeHeader("Authorization")
                    .addHeader("Authorization", "Bearer ${newTokens.accessToken}")
                    .build()
                return chain.proceed(newRequest)
            } catch (e: Exception) {
                throw IOException("토큰 갱신 실패: ${e.message}")
            }
        }

        // 응답이 성공적이지 않은 경우, 적절한 에러 메시지를 반환
        if (!response.isSuccessful) {
            throw IOException(ApiErrorHandler.getErrorMessage(response.code))
        }
        return response
    }
}