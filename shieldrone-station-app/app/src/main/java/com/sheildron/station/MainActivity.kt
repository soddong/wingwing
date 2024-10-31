package com.sheildron.station

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.sheildron.station.ui.theme.SheildronStationTheme
import com.sheildron.station.src.controller.StreamController
import com.sheildron.station.src.service.camera.CameraImageFrameProvider
import com.sheildron.station.src.service.camera.DroneImageFrameProvider

class MainActivity : ComponentActivity() {

    private lateinit var streamController: StreamController
    private var cameraPermissionGranted by mutableStateOf(false)
    private var isCameraMode by mutableStateOf(true)
    private var isStreaming by mutableStateOf(false)

    // 카메라 권한 요청 런처
    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            cameraPermissionGranted = isGranted
            if (isGranted) {
                startStreaming()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i("MainActivity", "onCreate")
        setContent {
            SheildronStationTheme {
                CameraPermissionScreen(
                    onRequestPermission = { requestCameraPermission() },
                    onModeSelected = { isCamera -> onModeSelected(isCamera) },
                    cameraPermissionGranted = cameraPermissionGranted,
                    isCameraMode = isCameraMode,
                    isStreaming = isStreaming
                )
            }
        }
    }

    // 모드 선택 시 호출
    private fun onModeSelected(isCamera: Boolean) {
        isCameraMode = isCamera
        if (isCameraMode) {
            requestCameraPermission()
        } else {
            startStreaming()
        }
    }

    // 카메라 권한 요청
    private fun requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            cameraPermissionGranted = true
            startStreaming()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // 카메라 또는 드론 모드에서 스트리밍 시작
    private fun startStreaming() {
        Log.i("MainActivity", "Starting stream in ${if (isCameraMode) "Camera" else "Drone"} mode")
        streamController = if (isCameraMode) {
            StreamController(CameraImageFrameProvider(this))
        } else {
            StreamController(DroneImageFrameProvider(this))
        }

        streamController.startLive()
        isStreaming = true
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isStreaming) {
            streamController.stopLive()
        }
    }
}

@Composable
fun SheildronStationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

// 다크 테마 색상
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFBB86FC),
    onPrimary = Color.White,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onBackground = Color.White,
    onSurface = Color.White
)

// 라이트 테마 색상
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6200EE),
    onPrimary = Color.White,
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    onBackground = Color.Black,
    onSurface = Color.Black
)

@Composable
fun CameraPermissionScreen(
    onRequestPermission: () -> Unit,
    onModeSelected: (Boolean) -> Unit,
    cameraPermissionGranted: Boolean,
    isCameraMode: Boolean,
    isStreaming: Boolean
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        content = { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = if (isStreaming) "Streaming in ${if (isCameraMode) "Camera" else "Drone"} mode..." else "Select Mode")
                Spacer(modifier = Modifier.height(16.dp))
                if (!isStreaming) {
                    Row {
                        Button(onClick = { onModeSelected(true) }) {
                            Text(text = "Camera Mode")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = { onModeSelected(false) }) {
                            Text(text = "Drone Mode")
                        }
                    }
                } else if (!cameraPermissionGranted && isCameraMode) {
                    PermissionRequestButton(onRequestPermission = onRequestPermission)
                }
            }
        }
    )
}

@Composable
fun PermissionRequestButton(onRequestPermission: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text(text = "Camera permission is required to start streaming.")
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { onRequestPermission() }) {
            Text(text = "Request Camera Permission")
        }
    }
}
