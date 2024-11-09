package com.ssafy.shieldroneapp.data.source.remote

import com.ssafy.shieldroneapp.data.model.response.TokenResponse
import retrofit2.Response

interface TokenRefresher {
    /**
     * 리프레시 토큰을 이용해 새로운 액세스 토큰을 발급받는 함수.
     *
     * @param refreshToken 현재 발급된 리프레시 토큰
     * @return 새로운 액세스 토큰 및 리프레시 토큰을 포함한 [TokenResponse]
     */
    suspend fun refreshToken(refreshToken: String): Response<TokenResponse>
}