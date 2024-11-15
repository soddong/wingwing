package com.ssafy.shieldroneapp.di

import com.ssafy.shieldroneapp.BuildConfig
import com.ssafy.shieldroneapp.data.source.remote.KakaoApiService
import com.ssafy.shieldroneapp.data.source.remote.KakaoAuthorizationInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object KakaoNetworkModule {

    /**
     * Kakao API Key를 제공하는 메서드
     *
     * @Named("KAKAO_API_KEY")로 주입될 API Key를 BuildConfig에서 가져옴
     * 다른 모듈과의 API Key 혼동을 피하기 위해 Named 어노테이션 사용
     */
    @Provides
    @Singleton
    @Named("KAKAO_API_KEY")
    fun provideKakaoApiKey(): String {
        return BuildConfig.KAKAO_API_KEY
    }

    /**
     * Kakao Authorization Interceptor 제공
     *
     * Kakao API 호출 시, Authorization 헤더에 API Key를 추가하는 인터셉터를 제공
     * @Named("KAKAO_API_KEY")로 주입된 API Key를 사용
     */
    @Provides
    @Singleton
    fun provideKakaoAuthorizationInterceptor(
        @Named("KAKAO_API_KEY") apiKey: String
    ): KakaoAuthorizationInterceptor {
        return KakaoAuthorizationInterceptor(apiKey)
    }

    /**
     * Kakao Map API Service 제공
     *
     * Kakao API의 기본 URL과 OkHttpClient를 사용하여 Retrofit 인스턴스를 생성
     * OkHttpClient에 KakaoAuthorizationInterceptor와 HttpLoggingInterceptor를 추가하여,
     * 모든 요청에 API Key와 로깅을 포함
     */
    @Provides
    @Singleton
    fun provideKakaoMapApiService(
        kakaoAuthorizationInterceptor: KakaoAuthorizationInterceptor,
        loggingInterceptor: HttpLoggingInterceptor,
    ): KakaoApiService {
        val client = OkHttpClient.Builder()
            .addInterceptor(kakaoAuthorizationInterceptor) // Kakao API Key를 요청 헤더에 추가
            .addInterceptor(loggingInterceptor)
            .build()

        return Retrofit.Builder()
            .baseUrl("https://dapi.kakao.com/") // Kakao API의 기본 URL 설정
            .client(client) // API 요청에 사용할 OkHttpClient 설정
            .addConverterFactory(GsonConverterFactory.create()) // JSON 응답을 변환할 Gson Converter 추가
            .build()
            .create(KakaoApiService::class.java) // KakaoApiService 인터페이스 구현체 생성
    }
}
