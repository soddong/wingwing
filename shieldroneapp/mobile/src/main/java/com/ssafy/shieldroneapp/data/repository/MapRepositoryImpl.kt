package com.ssafy.shieldroneapp.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.ssafy.shieldroneapp.data.model.LatLng
import com.ssafy.shieldroneapp.data.model.RouteLocation
import com.ssafy.shieldroneapp.data.model.response.HiveResponse
import com.ssafy.shieldroneapp.data.source.local.MapLocalDataSource
import com.ssafy.shieldroneapp.data.source.remote.ApiService
import com.ssafy.shieldroneapp.utils.NetworkUtils
import com.ssafy.shieldroneapp.utils.getLastKnownLocation
import com.ssafy.shieldroneapp.utils.getUpdatedLocation
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MapRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val mapLocalDataSource: MapLocalDataSource,
    private val context: Context,
    private val fusedLocationClient: FusedLocationProviderClient // GPS
) : MapRepository {
    /**
     * 1. GPS를 통해 현재 위치를 한 번 가져오기
     *
     * FusedLocationProviderClient를 사용하여 기기의 현재 GPS 위치를 가져옴
     * 캐시된 최근(마지막) 위치를 우선 사용하고, 없거나 최신 위치가 필요한 경우 새 위치를 요청하여 반환
     *
     * @return LatLng 현재 위치의 위도와 경도를 포함한 객체 반환
     * @throws SecurityException 위치 권한이 없는 경우 예외 발생
     * @throws Exception 위치 데이터를 가져올 수 없는 경우 예외 발생
     */
    override suspend fun getCurrentLocation(): LatLng {
        // 1) 최근 기록된 마지막 위치를 먼저 가져오기
        val lastLocation = fusedLocationClient.getLastKnownLocation(context)
        lastLocation?.let {
            return LatLng(it.latitude, it.longitude) // 캐시된 위치가 있다면 반환
        }

        // 2) 만약 최근 위치가 없거나, 최신 위치가 필요하면 실시간 위치 요청
        val updatedLocation = fusedLocationClient.getUpdatedLocation()
        return LatLng(updatedLocation.latitude, updatedLocation.longitude)
    }

    /**
     * 2. GPS를 통해 실시간 위치 업데이트
     *
     * @return Flow<LatLng> 위치 업데이트를 포함하는 Flow 객체
     */
    @SuppressLint("MissingPermission")
    override fun getLocationUpdates(): Flow<LatLng> = callbackFlow {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setMinUpdateDistanceMeters(5f)
            .build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    trySend(LatLng(location.latitude, location.longitude))
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        awaitClose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    /**
     * 3. (현재 위치 기반) 근처 드론 정류장 목록 조회
     *
     * 서버에 사용자의 현재 위치(위도, 경도)를 전송하여 근처 드론 정류장 목록을 요청합니다.
     */
    override suspend fun getNearbyHives(lat: Double, lng: Double): Result<List<HiveResponse>> {
        return NetworkUtils.apiCallAfterNetworkCheck(context) {
            val response = apiService.getNearbyHives(lat, lng)
            if (response.isSuccessful) {
                response.body() ?: throw Exception("드론 정류장 응답이 비어 있습니다.")
            } else {
                val errorMsg = response.errorBody()?.string() ?: "근처 드론 정류장 조회 실패"
                throw Exception(errorMsg)
            }
        }
    }

    /**
     * 4. (키워드 기반) 드론 정류장 검색
     *
     * 사용자가 입력한 키워드를 서버에 전송하여 드론 정류장 검색을 요청하고, 검색 결과를 반환합니다.
     */
    override suspend fun searchHivesByKeyword(keyword: String): Result<List<HiveResponse>> {
        return NetworkUtils.apiCallAfterNetworkCheck(context) {
            val response = apiService.searchHivesByKeyword(keyword)
            if (response.isSuccessful) {
                response.body() ?: throw Exception("검색 결과가 없습니다.")
            } else {
                throw Exception("드론 정류장 검색 실패")
            }
        }
    }

    /**
     * 5. 출발지/도착지 위치를 로컬에 저장/조회/초기화
     */
    override suspend fun saveStartLocation(location: RouteLocation) {
        mapLocalDataSource.saveStartLocation(location)
    }

    override suspend fun getStartLocation(): RouteLocation? {
        return mapLocalDataSource.getStartLocation()
    }

    override suspend fun saveEndLocation(location: RouteLocation) {
        mapLocalDataSource.saveEndLocation(location)
    }

    override suspend fun getEndLocation(): RouteLocation? {
        return mapLocalDataSource.getEndLocation()
    }

    override suspend fun clearLocationData() {
        mapLocalDataSource.clearLocationData()
    }

}
