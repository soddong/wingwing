package com.ssafy.shieldroneapp.utils

/**
 * 현재 위치 가져오기 위한 유틸리티 클래스
 *
 * - 요청한 위치 데이터가 반환될 때까지 기다리는 비동기 처리
 * - 위치 권한 확인, 최근 위치 요청, 최신 위치 갱신 요청 등 다양한 위치 관련 작업을 지원
 */
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * 1. 위치 데이터를 비동기적으로 처리하는 await 확장 함수
 *
 * FusedLocationProviderClient의 위치 데이터(GPS) 요청 시 반환되는
 * Task<Location> 객체를 suspend 함수로 변환하여, Coroutine에서 사용 가능 하게 함
 *
 * 성공 시 위치 데이터를 반환하고,
 * 실패 시 예외를 throw하여 Coroutine 호출부에서 예외 처리를 할 수 있다.
 *
 * @receiver Task<Location> 비동기 위치 요청 Task
 * @return 현재 위치(Location) 또는 null (위치 데이터를 가져올 수 없는 경우)
 * @throws Exception 위치 데이터 요청이 실패한 경우 발생하는 예외
 */
suspend fun Task<Location>.await(): Location? {
    return suspendCoroutine { continuation ->
        addOnSuccessListener { location ->
            continuation.resumeWith(Result.success(location)) // 성공 시 위치 데이터를 반환
        }.addOnFailureListener { exception ->
            continuation.resumeWithException(exception) // 실패 시 예외 반환
        }
    }
}

/**
 * 2. 기기에 최근 기록된 마지막 위치를 가져오는 함수
 *
 * 위치 권한을 확인한 후,
 * FusedLocationProviderClient의 lastLocation을 사용하여
 * 최근 위치를 요청하고, 결과로 Location 객체를 반환
 *
 * @param context Context 앱의 컨텍스트 객체 (위치 권한 확인 시 사용)
 * @receiver FusedLocationProviderClient 위치 서비스를 제공하는 Google API 클라이언트
 * @return Location? 최근 위치 데이터를 반환하거나, 데이터가 없으면 null 반환
 * @throws SecurityException 위치 권한이 없는 경우 예외 발생
 */
suspend fun FusedLocationProviderClient.getLastKnownLocation(context: Context): Location? {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        throw SecurityException("위치 권한이 필요합니다.")
    }
    return lastLocation.await() // 위의 1. Task<Location>.await() 메서드 호출
}

/**
 * 3. 최신 위치 데이터를 직접 요청하여 가져오는 함수
 *
 * requestLocationUpdates를 통해 기기의 실시간 위치를 단 한 번 요청하고,
 * 성공 시 최신 Location 객체를 반환하며,
 * 실패 시 예외를 발생시켜 호출 측에서 예외를 처리할 수 있도록 지원
 *
 * LocationRequest와 LocationCallback을 사용하여, 비동기적으로 위치 데이터를 처리
 *
 * @receiver FusedLocationProviderClient 위치 서비스를 제공하는 Google API 클라이언트
 * @return Location 최신 위치 데이터를 반환
 * @throws Exception 위치 서비스가 비활성화되거나 위치 데이터를 가져올 수 없는 경우 예외 발생
 */
@SuppressLint("MissingPermission")
suspend fun FusedLocationProviderClient.getUpdatedLocation(): Location {
    return suspendCancellableCoroutine { continuation ->

        // 1) 위치 요청 설정
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY // 높은 정확도 모드 설정
            interval = 1000 // 위치 요청 간격 (1초)
            fastestInterval = 500 // 최소 위치 요청 간격 (0.5초)
            numUpdates = 1 // 단 한 번만 위치 요청
        }

        // 2) 위치 콜백 설정
        // 위치 결과를 수신하거나 위치 서비스 활성화 여부를 확인할 때마다 콜백 호출
        val locationCallback = object : LocationCallback() {

            // 위치 결과 수신 시 호출되는 콜백
            override fun onLocationResult(result: LocationResult) {
                removeLocationUpdates(this) // 콜백 제거하여 업데이트 중단 (리소스 절약)
                result.lastLocation?.let { location ->
                    continuation.resume(location) // 위치 데이터를 성공적으로 받아 반환
                } ?: continuation.resumeWithException(Exception("위치 데이터를 가져올 수 없습니다.")) // 위치 데이터가 없을 경우
            }

            // 위치 서비스 활성화 상태를 확인하는 콜백
            override fun onLocationAvailability(availability: LocationAvailability) {
                if (!availability.isLocationAvailable) {
                    continuation.resumeWithException(Exception("위치 서비스가 비활성화되어 있습니다.")) // 위치 서비스 비활성화 시
                }
            }
        }

        // 3) 위치 업데이트 요청 시작
        // 위치 업데이트 요청을 메인 스레드에서 처리하도록 MainLooper를 사용
        requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())

        // 4. 위치 요청 중단 처리
        // 취소(cancellation) 발생 시, 위치 업데이트 중단
        continuation.invokeOnCancellation { removeLocationUpdates(locationCallback) }
    }
}
