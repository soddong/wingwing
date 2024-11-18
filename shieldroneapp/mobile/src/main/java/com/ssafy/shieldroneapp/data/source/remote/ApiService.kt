package com.ssafy.shieldroneapp.data.source.remote

import com.ssafy.shieldroneapp.data.model.request.CodeVerificationRequest
import com.ssafy.shieldroneapp.data.model.request.DroneCancelRequest
import com.ssafy.shieldroneapp.data.model.request.DroneMatchRequest
import com.ssafy.shieldroneapp.data.model.request.DroneRouteRequest
import com.ssafy.shieldroneapp.data.model.request.HomeLocationRequest
import com.ssafy.shieldroneapp.data.model.request.PhoneNumberRequest
import com.ssafy.shieldroneapp.data.model.request.TokenRequest
import com.ssafy.shieldroneapp.data.model.request.UserAuthRequest
import com.ssafy.shieldroneapp.data.model.request.VerificationCodeRequest
import com.ssafy.shieldroneapp.data.model.response.DroneMatchResponse
import com.ssafy.shieldroneapp.data.model.response.DroneRouteResponse
import com.ssafy.shieldroneapp.data.model.response.HiveResponse
import com.ssafy.shieldroneapp.data.model.response.HomeLocationResponse
import com.ssafy.shieldroneapp.data.model.response.TokenResponse
import com.ssafy.shieldroneapp.data.model.response.VerificationResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query
import com.ssafy.shieldroneapp.data.model.Guardian
import com.ssafy.shieldroneapp.data.model.LatLng
import com.ssafy.shieldroneapp.data.model.request.EmergencyRequest
import com.ssafy.shieldroneapp.data.model.request.HiveSearchRequest


/**
 * 서버와의 HTTP 통신을 담당하는 Retrofit 인터페이스.
 *
 * 로그인, 회원가입, 출발지 및 도착지 전송, 드론 배정 등의 API 요청을 정의.
 * 서버로부터의 응답 데이터를 관리하며, 성공 및 오류 응답에 대한 처리를 포함.
 *
 * 사용 방식:
 * - Retrofit을 사용하여 API 요청을 전송하고, 서버의 응답을 처리합니다.
 * - 네트워크 상태 확인은 NetworkUtils를 통해 별도로 수행하며,
 *   서버 응답 오류 처리는 ApiService 내에서 집중적으로 처리합니다.
 */
interface ApiService {

    // 1. 사용자 인증 관련 API

    /**
     * 1) 인증 코드 전송
     * 사용자가 입력한 전화번호로 인증 코드를 요청하는 API
     *
     * @param requestBody 인증 요청 데이터
     * @return 서버로부터 성공/실패 응답
     */
    @POST("users/send")
    suspend fun sendVerificationCode(@Body requestBody: VerificationCodeRequest): Response<Unit>

    /**
     * 2) 인증 코드 검증
     * 서버로부터 받은 인증 코드를 검증하여 인증 여부를 확인하는 API
     *
     * @param requestBody 인증 코드 검증 요청 데이터
     * @return 인증 결과를 포함한 서버 응답
     */
    @POST("users/verify")
    suspend fun verifyCode(@Body requestBody: CodeVerificationRequest): Response<VerificationResponse>

    /**
     * 3) 회원가입 요청
     * 서버에 사용자의 회원가입 정보를 전달하여 회원가입을 처리하는 API
     *
     * @param requestBody 사용자 회원가입 요청 데이터
     * @return 성공 시 유닛(Unit), 실패 시 오류 응답
     */
    @POST("users/sign-up")
    suspend fun signUp(@Body requestBody: UserAuthRequest): Response<Unit>

    /**
     * 4) 로그인 요청
     * 서버에 전화번호를 통해 사용자의 로그인을 요청하는 API
     *
     * @param requestBody 전화번호 로그인 요청 데이터
     * @return 성공 시 토큰을 포함한 서버 응답
     */
    @POST("users/sign-in")
    suspend fun signIn(@Body requestBody: PhoneNumberRequest): Response<TokenResponse>

    /**
     * 5) 액세스 토큰 갱신
     * 만료된 액세스 토큰을 리프레시 토큰으로 갱신하는 API
     *
     * @param requestBody 리프레시 토큰 요청 데이터
     * @return 새로 발급된 토큰 응답
     */
    @POST("users/refresh")
    suspend fun refreshToken(@Body requestBody: TokenRequest): Response<TokenResponse>



    // 2. 마이페이지 관련 API

    /**
     * 1) 기본 도착지 설정
     * 서버에 사용자가 지정한 도착지를 기본 도착지로 설정하는 API
     *
     * @param requestBody 도착지 설정 데이터
     * @return 성공 시 유닛(Unit), 실패 시 오류 응답
     */
    @PATCH("settings/end-pos")
    suspend fun setHomeLocation(@Body requestBody: HomeLocationRequest): Response<Unit>

    /**
     * 2) 기본 도착지 조회
     * 서버에 저장된 사용자의 기본 도착지를 조회하는 API
     *
     * @param lat 기본 도착지의 위도 값
     * @param lng 기본 도착지의 경도 값
     * @return 기본 도착지 정보
     */
    @GET("settings/end-pos")
    suspend fun getHomeLocation(@Query("lat") lat: Double, @Query("lng") lng: Double): Response<HomeLocationResponse>

    /**
     * 3) 보호자 등록
     * */
    @POST("settings/guardian")
    suspend fun addGuardian(@Body guardian: Guardian)



    // 3. 지도 관련 API

    /**
     * 1) 현재 위치 기반 근처 드론 정류장 조회
     * 현재 GPS 위치를 기반으로 근처 드론 정류장 목록을 서버에서 조회
     * 
     * @param requestBody 현재 위치의 위도, 경도 값
     * @return 서버에서 받은 근처 정류장 목록
     */
    @POST("hives")
    suspend fun getNearbyHives(@Body requestBody: LatLng): Response<List<HiveResponse>>

    /**
     * 2) 출발지 검색
     * 사용자가 입력한 키워드를 기반으로 드론 정류장을 검색하는 API
     *
     * @param requestBody 검색 키워드
     * @return 검색 결과 정류장 리스트
     */
    @POST("hives/search")
    suspend fun searchHivesByKeyword(@Body requestBody: HiveSearchRequest): Response<List<HiveResponse>>



    // 4. 드론 관련 API

    /**
     * 1) 드론 배정 요청
     * 사용자가 설정한 출발지와 도착지를 서버에 전송하여 안내 가능 여부를 확인하는 API
     *
     * @param requestBody 드론 경로 요청 데이터
     * @return 드론 배정 가능 여부와 예상 시간, 거리
     */
    @POST("drones/routes")
    suspend fun requestDrone(@Body requestBody: DroneRouteRequest): Response<DroneRouteResponse>

    /**
     * 2) 드론 배정 취소
     * 배정된 드론의 배정을 취소하는 API
     *
     * @param droneId 취소할 드론의 ID
     * @return 성공 시 유닛(Unit), 실패 시 오류 응답
     */
    @POST("drones/cancel")
    suspend fun cancelDrone(@Body requestBody: DroneCancelRequest): Response<Unit>

    /**
     * 3) 드론 매칭 요청
     * 사용자가 배정된 드론의 QR을 통해 매칭을 요청하는 API
     *
     * @param requestBody 드론 매칭 요청 데이터
     * @return 매칭 성공 시 드론 정보 응답
     */
    @POST("drones/match")
    suspend fun matchDrone(@Body requestBody: DroneMatchRequest): Response<DroneMatchResponse>

    /**
     * 4) 서비스 종료
     *
     * @param droneId 서비스 종료할 드론의 ID
     * @return 성공 시 유닛(Unit), 실패 시 오류 응답
     */
    @POST("drones/end")
    suspend fun serviceEnd(@Body requestBody: DroneCancelRequest): Response<Unit>



    // 5. 위험 상황 관련 API
    @POST("settings/emergency")
    suspend fun setEmergency(@Body request: EmergencyRequest): Response<Unit>
}