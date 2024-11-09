package com.ssafy.shieldroneapp.ui.map

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapView
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.camera.CameraUpdateFactory

/**
 * 사용자의 현재 위치와 드론 경로 안내를 제공하는 메인 Map 화면.
 *
 * 유저의 GPS 위치를 받아 점으로 표시하고, 근처 드론 정류장을 하늘색 마커로 표시.
 * 출발지/도착지 입력, 드론 배정 요청, 경로 안내 등 주요 기능을 관리한다.
 * 드론 배정 성공 시 경로를 표시하고, QR 코드 인식 및 위험 상황 알림을 처리한다.
 *
 * @property viewModel Map 화면의 상태와 로직을 관리하는 ViewModel
 */
@Composable
fun MapScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // MapView 생성 및 관리
    val mapView = remember { MapView(context) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> mapView.start(
                    object : MapLifeCycleCallback() {
                        override fun onMapDestroy() {
                            Log.d("MapScreen", "Map destroyed")
                        }
                        override fun onMapError(error: Exception) {
                            Log.e("MapScreen", "Map error: ${error.message}", error)
                        }
                    },
                    object : KakaoMapReadyCallback() {
                        override fun onMapReady(kakaoMap: KakaoMap) {
                            Log.d("MapScreen", "Map ready")
                            val position = LatLng.from(37.5012743, 127.039585) // 서울 멀티캠퍼스 좌표
                            try {
                                kakaoMap.moveCamera(CameraUpdateFactory.newCenterPosition(position))
                                kakaoMap.moveCamera(CameraUpdateFactory.zoomTo(15))
                                Log.d("MapScreen", "Camera moved")
                            } catch (e: Exception) {
                                Log.e("MapScreen", "Camera move error: ${e.message}", e)
                            }
                        }
                    }
                )
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // AndroidView를 사용하여 MapView 표시
    Box(
        modifier = Modifier
            .background(MaterialTheme.colors.background)
            .fillMaxSize()
    ) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        )
    }
}