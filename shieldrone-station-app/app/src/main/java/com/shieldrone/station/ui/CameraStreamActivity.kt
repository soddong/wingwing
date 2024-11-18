package com.shieldrone.station.ui

import android.os.Bundle
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import com.shieldrone.station.model.CameraStreamVM
import dji.sdk.keyvalue.value.common.ComponentIndexType
import dji.v5.manager.interfaces.ICameraStreamManager

class CameraStreamActivity : ComponentActivity() {

    private val viewModel: CameraStreamVM by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 카메라 인덱스 설정
        viewModel.setCameraModeAndIndex(ComponentIndexType.LEFT_OR_MAIN)

        setContent {
            CameraStreamScreen(viewModel)
        }
    }
}

@Composable
fun CameraStreamScreen(viewModel: CameraStreamVM) {
    val cameraName by viewModel.cameraName.collectAsState()
    val frameInfo by viewModel.frameInfo.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 카메라 이름 출력
        Text(
            text = "Camera: $cameraName",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(8.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(text = "Frame Info: $frameInfo", style = MaterialTheme.typography.bodyLarge)

        Spacer(modifier = Modifier.height(32.dp))

        // SurfaceView를 Compose 내에서 사용하여 스트림을 표시
        CameraStreamSurfaceView(viewModel)

        Spacer(modifier = Modifier.height(32.dp))

        // 스트림 시작 버튼
        Button(onClick = { viewModel.addFrameListener() }) {
            Text("Start Stream")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 스트림 중지 버튼
        Button(onClick = { viewModel.removeFrameListener() }) {
            Text("Stop Stream")
        }
    }
}

@Composable
fun CameraStreamSurfaceView(viewModel: CameraStreamVM, modifier: Modifier = Modifier) {
    // AndroidView를 사용하여 SurfaceView를 포함
    AndroidView(
        factory = { context ->
            SurfaceView(context).apply {
                holder.addCallback(object : SurfaceHolder.Callback {
                    override fun surfaceCreated(holder: SurfaceHolder) {
                        // Surface가 생성되면 스트림을 연결
                        viewModel.putCameraStreamSurface(
                            surface = holder.surface,
                            width = width,
                            height = height,
                            scaleType = ICameraStreamManager.ScaleType.CENTER_INSIDE // ScaleType 설정
                        )
                    }

                    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                        // Surface 크기 변경 시 스트림 업데이트
                        viewModel.putCameraStreamSurface(
                            surface = holder.surface,
                            width = width,
                            height = height,
                            scaleType = ICameraStreamManager.ScaleType.CENTER_INSIDE // ScaleType 설정
                        )
                    }

                    override fun surfaceDestroyed(holder: SurfaceHolder) {
                        // Surface가 파괴되면 스트림 연결 해제
                        viewModel.removeCameraStreamSurface(holder.surface)
                    }
                })
            }
        },
        modifier = modifier
            .fillMaxSize()
    )
}