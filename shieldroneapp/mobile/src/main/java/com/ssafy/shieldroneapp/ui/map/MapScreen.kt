package com.ssafy.shieldroneapp.ui.map

import android.Manifest
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.MapView
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.KakaoMapReadyCallback
import com.ssafy.shieldroneapp.data.model.WatchConnectionState
import com.ssafy.shieldroneapp.ui.components.AlertModal
import com.ssafy.shieldroneapp.ui.components.AlertType
import com.ssafy.shieldroneapp.ui.components.ConnectionStatusSnackbar
import com.ssafy.shieldroneapp.ui.components.WatchConnectionManager
import com.ssafy.shieldroneapp.ui.map.screens.AlertHandler
import com.ssafy.shieldroneapp.ui.map.screens.SearchInputFields
import com.ssafy.shieldroneapp.utils.setupMap
import com.ssafy.shieldroneapp.utils.updateAllMarkers
import com.ssafy.shieldroneapp.viewmodels.HeartRateViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 사용자의 현재 위치와 드론 경로 안내를 제공하는 메인 Map 화면.
 *
 * 유저의 GPS 위치를 받아 점으로 표시하고, 근처 드론 정류장을 하늘색 마커로 표시.
 * 출발지/도착지 입력, 드론 배정 요청, 경로 안내, 위험 상황 알림 등 주요 기능을 관리한다.
 * 드론 배정 성공 시 경로를 표시하고, 드론 코드 인식 화면으로 이동한다.
 *
 * @property viewModel Map 화면의 상태와 로직을 관리하는 ViewModel
 */

private const val TAG = "모바일: 맵 화면"

@Composable
fun MapScreen(
    isAppActive: Boolean,
    viewModel: HeartRateViewModel = hiltViewModel(),
    alertHandler: AlertHandler,
    mapViewModel: MapViewModel = hiltViewModel(),
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
) {
    val state = mapViewModel.state.collectAsStateWithLifecycle().value
    val context = LocalContext.current
    val kakaoMap = remember { mutableStateOf<KakaoMap?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val isMapInitialized = remember { mutableStateOf(false) }

    // 화면 회전 감지를 위한 Configuration 변경 감지
    val configuration = LocalConfiguration.current
    val screenRotation = remember { mutableStateOf(configuration.orientation) }

    val heartRateState = viewModel.heartRateData.collectAsStateWithLifecycle().value
    val connectionState = viewModel.watchConnectionState.collectAsStateWithLifecycle().value
    val alertState = mapViewModel.alertState.collectAsStateWithLifecycle().value

    // 워치 연결 관리
    WatchConnectionManager (
        onConnectionStatusDetermined = { isConnected ->
            viewModel.updateWatchConnectionState(
                if (isConnected) WatchConnectionState.Connected
                else WatchConnectionState.Disconnected
            )
        }
    )

    // 화면 회전 시 마커 재생성을 위한 Effect
    LaunchedEffect(configuration.orientation) {
        if (screenRotation.value != configuration.orientation) {
            screenRotation.value = configuration.orientation
            kakaoMap.value?.let { map ->
                if (state.currentLocation != null) {
                    updateAllMarkers(map, state)
                    Log.d("MapScreen", "화면 회전으로 인한 마커 재생성")
                }
            }
        }
    }

    // 위치 권한을 허용받은 후에만 초기 위치를 로드
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val locationGranted = permissions.entries.all { it.value }
        if (locationGranted) {
            mapViewModel.handleEvent(MapEvent.LoadCurrentLocationAndFetchHives)
        }
    }
    LaunchedEffect(Unit) {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    // 위치 갱신 시, 지도 초기화 및 마커 업데이트
    LaunchedEffect(state.currentLocation, state.isTrackingLocation, state.nearbyHives) {
        kakaoMap.value?.let { map ->
            if (!isMapInitialized.value) {
                setupMap(map, mapViewModel)
                isMapInitialized.value = true
                Log.d("MapScreen", "지도 초기 설정 완료")
            }

            // 현재 위치나 주변 정류장 정보가 있을 때 마커 업데이트
            if (state.currentLocation != null || state.nearbyHives.isNotEmpty()) {
                updateAllMarkers(map, state)
                Log.d("MapScreen", "마커 업데이트 - 현재 위치: ${state.currentLocation}, 정류장 수: ${state.nearbyHives.size}")
            }
        }
    }

     Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            // 지도 영역
            val mapView = remember { MapView(context) } // 기존 MapView

            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_CREATE -> mapView.start(
                            object : MapLifeCycleCallback() {
                                override fun onMapDestroy() {
                                    Log.d("MapScreen", "Map destroyed")
                                    isMapInitialized.value = false
                                }
                                override fun onMapError(error: Exception) {
                                    Log.e("MapScreen", "Map error: ${error.message}", error)
                                }
                            },
                            object : KakaoMapReadyCallback() {
                                override fun onMapReady(map: KakaoMap) {
                                    Log.d("MapScreen", "Map ready")
                                    kakaoMap.value = map
                                    if (state.currentLocation != null) {
                                        setupMap(map, mapViewModel)
                                        isMapInitialized.value = true
                                        updateAllMarkers(map, state)  // 명시적으로 마커 업데이트 호출
                                    }
                                }
                            }
                        )
                        Lifecycle.Event.ON_RESUME -> {
                            // 화면이 다시 활성화될 때 마커 상태 복원
                            kakaoMap.value?.let { map ->
                                if (state.currentLocation != null) {
                                    updateAllMarkers(map, state)
                                    Log.d("MapScreen", "화면 재개로 인한 마커 재생성")
                                }
                            }
                        }
                        else -> {}
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                    kakaoMap.value = null
                    isMapInitialized.value = false
                }
            }

            // AndroidView로 MapView를 전체 화면에 배치
            AndroidView(
                factory = { mapView },
                modifier = Modifier.fillMaxSize(),
            )

            // 상단 검색 필드
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                SearchInputFields (
                    startText = state.startSearchText,
                    endText = state.endSearchText,
                    onStartTextChange = { mapViewModel.updateStartLocationText(it) },
                    onEndTextChange = { mapViewModel.updateEndLocationText(it) }
                )
            }

            // 하단 버튼
            Button (
                onClick = { mapViewModel.handleEvent(MapEvent.RequestDroneAssignment) },
                modifier = Modifier.fillMaxWidth()
                    .height(56.dp)
                    .align(Alignment.BottomCenter),
                enabled = state.isTrackingLocation,
            ) {
                Text(
                    text = "드론 배정 요청",
                    style = MaterialTheme.typography.h5
                )
            }

        AlertModal(
            alertState = alertState,
            onDismiss = mapViewModel::dismissAlert,
            onEmergencyAlert = if (alertState.alertType == AlertType.WARNING) {
                {
                    coroutineScope.launch {
                        val success = mapViewModel.sendEmergencyAlert()
                        if (success) {
                            mapViewModel.showToast()
                        } else {
                            // TODO: API 호출 실패 시 에러 메시지 등의 추가 작업 수행
                        }
                    }
                    true
                }
            } else null,
            alertHandler = alertHandler
        )

        ConnectionStatusSnackbar(
            connectionState = connectionState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
     }
}