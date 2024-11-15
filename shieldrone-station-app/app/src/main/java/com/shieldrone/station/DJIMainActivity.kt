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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.shieldrone.station.controller.RouteController
import com.shieldrone.station.controller.StreamController
import com.shieldrone.station.model.BatteryViewModel
import com.shieldrone.station.service.camera.CameraImageFrameProvider
import com.shieldrone.station.service.camera.DroneImageFrameProvider
import com.shieldrone.station.service.route.RouteAdapter
import com.shieldrone.station.ui.CameraStreamActivity
import com.shieldrone.station.ui.FlightControlActivity
import com.shieldrone.station.ui.SimulatorActivity
import com.shieldrone.station.ui.TrackingTargetActivity
import dji.v5.manager.KeyManager
import io.reactivex.rxjava3.disposables.CompositeDisposable
import org.opencv.android.OpenCVLoader

class DJIMainActivity : AppCompatActivity() {

    private lateinit var streamController: StreamController
    private lateinit var routeController: RouteController
    private var cameraPermissionGranted by mutableStateOf(false)
    private var isCameraMode by mutableStateOf(true)
    private var isStreaming by mutableStateOf(false)


    companion object {
        private const val PERMISSIONS_REQUEST_CODE = 1001
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.INTERNET,
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.POST_NOTIFICATIONS
        )
    }

    private var allPermissionsGranted by mutableStateOf(false)

    // 다중 권한 요청을 위한 런처
    private val permissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        allPermissionsGranted = permissions.all { it.value }
        if (allPermissionsGranted) {
            // 모든 권한이 허용되었을 때 필요한 초기화
            initializeWithPermissions()
        } else {
            // 권한이 거부되었을 때 처리
            showPermissionContextPopup()
        }
    }

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
    private lateinit var batteryViewModel: BatteryViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkAndRequestPermissions()
        if (!isTaskRoot && intent.hasCategory(Intent.CATEGORY_LAUNCHER) && Intent.ACTION_MAIN == intent.action) {

            finish()
            return

        }
//        val batteryModel = BatteryModel()
//        batteryViewModel = BatteryViewModel(batteryModel)
//        batteryViewModel.startSendingBatteryStatus()
//        setContent {
//            com.shieldrone.station.ui.theme.ShieldronStationTheme {
//                CameraPermissionScreen(
//                    onRequestPermission = { requestCameraPermission() },
//                    onModeSelected = { isCamera -> onModeSelected(isCamera) },
//                    cameraPermissionGranted = cameraPermissionGranted,
//                    isCameraMode = isCameraMode,
//                    isStreaming = isStreaming
//                )
//            }
//        }
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
        routeController.stopReceivingLocation()
        KeyManager.getInstance().cancelListen(this)
        handler.removeCallbacksAndMessages(null)
        disposable.dispose()
//        batteryViewModel.stopSendingBatteryStatus()

    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = REQUIRED_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isEmpty()) {
            allPermissionsGranted = true
            initializeWithPermissions()
        } else {
            permissionsLauncher.launch(permissionsToRequest)
        }
    }

    private fun initializeWithPermissions() {
        if (!isOpenCVInitialized) {
            OpenCVLoader.initDebug()
            isOpenCVInitialized = true
        }
        setContent {
            com.shieldrone.station.ui.theme.ShieldronStationTheme {
                if (!allPermissionsGranted) {
                    PermissionsRequiredScreen()
                } else {
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

    private fun showPermissionContextPopup() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("권한이 필요합니다")
            .setMessage(
                "앱을 사용하기 위해서는 다음 권한이 필요합니다:\n" +
                        "- 카메라: 영상 촬영\n" +
                        "- 위치: 드론 위치 추적\n" +
                        "- 인터넷: 데이터 통신\n" +
                        "- 포그라운드 서비스: 백그라운드 작업"
            )
            .setPositiveButton("다시 요청") { _, _ ->
                checkAndRequestPermissions()
            }
            .setNegativeButton("종료") { _, _ ->
                finish()
            }
            .create()
            .show()
    }

    @Composable
    private fun PermissionsRequiredScreen() {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("필요한 권한이 없습니다")
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { checkAndRequestPermissions() }) {
                Text("권한 요청")
            }
        }
    }

    // 카메라 또는 드론 모드에서 스트리밍 시작
    private fun startStreaming() {
//        Log.i("MainActivity", "Starting stream in ${if (isCameraMode) "Camera" else "Drone"} mode")
//
//        streamController = if (isCameraMode) {
//            StreamController(CameraImageFrameProvider(this))
//        } else {
//            StreamController(DroneImageFrameProvider(this))
//        }
//
//        val routeAdapter = RouteAdapter()
//        routeController = RouteController(routeAdapter)
//
//        streamController.startLive()
//        routeController.startReceivingLocation()
//        isStreaming = true
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
                    Row {
                        Button(onClick = { onModeSelected(true) }) {
                            Text(text = "Camera Mode")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
//                            onModeSelected(true)
//                            navigateToCameraStreamList()
                            onModeSelected(false)
                            navigateToTrackingControl()
                        }) {
                            Text(text = "Drone Mode")
                        }
                        Spacer(modifier = Modifier.width(10.dp))
//                        // 시뮬레이터 모드 버튼 추가
//                        Button(onClick = { navigateToSimulator() }) {
//                            Text(text = "Simulator Mode")
//                        }

                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    // 버추얼 스틱 모드 버튼 추가
                    Button(onClick = { navigateToFlightControl() }) {
                        Text(text = "Virtual Stick Mode")
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

    private fun navigateToTrackingControl() {
        val intent = Intent(this, TrackingTargetActivity::class.java)
        startActivity(intent)
    }
    private fun navigateToCameraStreamList() {
        val intent = Intent(this, CameraStreamActivity::class.java)
        startActivity(intent)
    }
}

