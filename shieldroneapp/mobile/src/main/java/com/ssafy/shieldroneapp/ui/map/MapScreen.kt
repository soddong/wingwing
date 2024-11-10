package com.ssafy.shieldroneapp.ui.map

import android.content.Intent
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapView
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.ssafy.shieldroneapp.utils.await
import com.ssafy.shieldroneapp.viewmodels.HeartRateViewModel
import kotlinx.coroutines.launch

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
fun MapScreen(
    isAppActive: Boolean,
    viewModel: HeartRateViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val heartRateState by viewModel.heartRateData.collectAsState()
    val connectionState by viewModel.watchConnectionState.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var connectedNodes by remember { mutableStateOf<List<Node>>(emptyList()) }
    var isChecking by remember { mutableStateOf(true) }

    // 워치 연결 상태 확인
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val nodes = Wearable.getNodeClient(context).connectedNodes.await(5000)
                connectedNodes = nodes
                isChecking = false
                if (nodes.isEmpty()) {
                    showDialog = true
                } else {
                    // 워치 앱 실행 요청
                    nodes.forEach { node ->
                        Wearable.getMessageClient(context).sendMessage(
                            node.id,
                            "/start/heart_rate_monitor",
                            ByteArray(0)
                        ).await(5000)
                    }
                }
            } catch (e: Exception) {
                isChecking = false
                showDialog = true
            }
        }
    }

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

        // 워치 앱 실행 요청 다이얼로그
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = {
                    Text(
                        "갤럭시 워치 연동 안내",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        if (connectedNodes.isEmpty())
                            "정확한 위험 상황 판단을 위해 갤럭시 워치와 페어링 해주세요."
                        else
                            "심박수 정보를 얻기 위해 워치의 Shield Drone 앱을 실행해주세요."
                    )
                },
                buttons = {
                    if (connectedNodes.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "모바일 앱만 사용하기",
                                style = TextStyle(
                                    textDecoration = TextDecoration.Underline,
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                ),
                                modifier = Modifier.clickable {
                                    showDialog = false
                                    isChecking = false
                                }
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                        }
                    }
                }
            )
        }
    }
}