package com.shieldrone.station

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.shieldrone.station.controller.StreamController
import com.shieldrone.station.model.MSDKManagerVM
import com.shieldrone.station.service.camera.CameraImageFrameProvider
import com.shieldrone.station.service.camera.DroneImageFrameProvider
import com.shieldrone.station.ui.FlightControlActivity
import com.shieldrone.station.ui.SimulatorActivity
import dji.v5.manager.KeyManager
import io.reactivex.rxjava3.disposables.CompositeDisposable
import org.opencv.android.OpenCVLoader

class DJIMainActivity : AppCompatActivity() {

    private lateinit var streamController: StreamController
    private var cameraPermissionGranted by mutableStateOf(false)
    private var isCameraMode by mutableStateOf(true)
    private var isStreaming by mutableStateOf(false)

    // ViewModel 인스턴스 생성
    private val msdkManagerVM: MSDKManagerVM by viewModels()

    // 카메라 권한 요청 런처
    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            cameraPermissionGranted = isGranted
            if (isGranted) {
                startStreaming()
            }
        }
    private val handler: Handler = Handler(Looper.getMainLooper())
    private val disposable = CompositeDisposable()
    private var isOpenCVInitialized = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!isOpenCVInitialized) {
            OpenCVLoader.initDebug()
            isOpenCVInitialized = true
        }
        if (!isTaskRoot && intent.hasCategory(Intent.CATEGORY_LAUNCHER) && Intent.ACTION_MAIN == intent.action) {

            finish()
            return

        }
        setContent {
            com.shieldrone.station.ui.theme.ShieldronStationTheme {
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isStreaming) {
            streamController.stopLive()
        }
        KeyManager.getInstance().cancelListen(this)
        handler.removeCallbacksAndMessages(null)
        disposable.dispose()
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
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
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


    @OptIn(ExperimentalMaterial3Api::class)
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
                            Spacer(modifier = Modifier.width(10.dp))
                            // 시뮬레이터 모드 버튼 추가
                            Button(onClick = { navigateToSimulator() }) {
                                Text(text = "Simulator Mode")
                            }

                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        // 버추얼 스틱 모드 버튼 추가
                        Button(onClick = { navigateToFlightControl() }) {
                            Text(text = "Virtual Stick Mode")
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

    // SimulatorActivity로 이동하는 함수 추가
    private fun navigateToSimulator() {
        val intent = Intent(this, SimulatorActivity::class.java)
        startActivity(intent)
    }

    // FlightControlActivity로 이동하는 함수 추가
    private fun navigateToFlightControl() {
        val intent = Intent(this, FlightControlActivity::class.java)
        startActivity(intent)
    }
}

