package com.sheildron.station

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.sheildron.station.ui.theme.SheildronStationTheme
import com.sheildron.station.src.VideoController

class MainActivity : ComponentActivity() {

    private lateinit var videoController: VideoController
    private var cameraPermissionGranted by mutableStateOf(false)
    private var isStreaming by mutableStateOf(false)

    // 카메라 권한 요청 런처
    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            cameraPermissionGranted = isGranted
            if (isGranted) {
                startVideoStreaming()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i("MainActivity", "onCreate")
        videoController = VideoController(this)

        setContent {
            SheildronStationTheme {
                CameraPermissionScreen(
                    onRequestPermission = { requestCameraPermission() },
                    cameraPermissionGranted = cameraPermissionGranted,
                    isStreaming = isStreaming
                )
            }
        }
    }

    // 카메라 권한 요청
    private fun requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            cameraPermissionGranted = true
            startVideoStreaming()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // 카메라 미리보기 및 영상 전송 시작
    private fun startVideoStreaming() {
        Log.i("MainActivity", "startVideoStreaming")
        videoController.startCameraPreview()
    }
}

@Composable
fun CameraPermissionScreen(
    onRequestPermission: () -> Unit,
    cameraPermissionGranted: Boolean,
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
                if (cameraPermissionGranted && isStreaming) {
                    Text(text = "Camera is streaming...")
                } else if (!cameraPermissionGranted) {
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
