package com.ssafy.shieldroneapp.di

import com.ssafy.shieldroneapp.BuildConfig
import com.ssafy.shieldroneapp.data.source.local.UserLocalDataSourceImpl
import com.ssafy.shieldroneapp.data.source.remote.ApiService
import com.ssafy.shieldroneapp.data.source.remote.ApiConstants
import com.ssafy.shieldroneapp.data.source.remote.ApiInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Hilt 모듈: Retrofit 및 OkHttpClient 인스턴스 제공
 *
 * Retrofit 인스턴스는 ApiService와 연결되며, HTTP 네트워크 요청을 효율적으로 처리할 수 있습니다.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    /**
     * OkHttpClient 설정
     * 
     * 네트워크 타임 아웃 설정 및 OkHttpClient 빌드
     * 디버깅을 위한 HttpLoggingInterceptor 추가
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(
        userLocalDataSourceImpl: UserLocalDataSourceImpl,
        apiService: ApiService
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()

        // HttpLoggingInterceptor 설정 (디버그 빌드에서만 사용)
        if (BuildConfig.DEBUG) {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY // 네트워크 요청/응답 본문까지 출력
            }
            builder.addInterceptor(loggingInterceptor)
        }

        // ApiInterceptor 추가 (모든 요청에 기본 헤더 설정)
        builder.addInterceptor(ApiInterceptor(userLocalDataSourceImpl, apiService))

        return builder
            .connectTimeout(ApiConstants.TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(ApiConstants.TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(ApiConstants.TIMEOUT, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Retrofit 설정
     *
     * API 기본 URL 및 OkHttpClient, Gson 변환기를 사용하여 Retrofit 빌드
     */
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(ApiConstants.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * ApiService 생성
     *
     * Retrofit을 사용하여 ApiService 인터페이스 구현체 반환
     */
    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }
}