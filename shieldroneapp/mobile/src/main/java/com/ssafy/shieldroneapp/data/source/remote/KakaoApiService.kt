package com.ssafy.shieldroneapp.data.source.remote

import com.ssafy.shieldroneapp.data.model.response.KakaoSearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface KakaoApiService {
    @GET("v2/local/search/keyword.json")
    suspend fun searchKeyword(
        @Query("query") query: String,
        @Query("page") page: Int = 1, // 페이지 번호
        @Query("size") size: Int = 15, // 검색 결과 수
        @Query("sort") sort: String = "accuracy" // accuracy(정확도순) 또는 distance(거리순)
    ): KakaoSearchResponse
}