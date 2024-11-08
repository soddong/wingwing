package com.ssafy.shieldroneapp.ui.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

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
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "Map 화면입니다.")
    }
}


//@Composable
//fun MapScreen(viewModel: MapViewModel = hiltViewModel()) {
//    val state by viewModel.state.collectAsStateWithLifecycle()
//
//    // 카카오 맵 띄우기
//    KakaoMap(
//        modifier = Modifier.fillMaxSize(),
//        userLocation = state.userLocation,
//        markers = state.markers,
//        onMarkerClick = { viewModel.onMarkerClick(it) },
//    )
//
//    // 출발지, 도착지 입력란
//    Column(
//        Modifier.fillMaxWidth().padding(16.dp)
//    ) {
//        TextField(
//            value = state.startLocation,
//            onValueChange = { viewModel.updateStartLocation(it) },
//            label = { Text("출발지 입력") }
//        )
//        TextField(
//            value = state.destinationLocation,
//            onValueChange = { viewModel.updateDestinationLocation(it) },
//            label = { Text("도착지 입력") }
//        )
//    }
//
//    // 배정 요청 버튼 및 관련 알림들
//    if (state.isDroneAssigned) {
//        DroneStatusAlert(
//            status = state.droneStatus,
//            onDismiss = { viewModel.clearDroneStatus() }
//        )
//    }
//}