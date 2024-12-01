package com.shieldrone.station

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import com.shieldrone.station.controller.RouteController
import com.shieldrone.station.ui.CameraStreamActivity
import com.shieldrone.station.ui.TrackingTargetActivity
import dji.v5.manager.KeyManager
import io.reactivex.rxjava3.disposables.CompositeDisposable
import org.opencv.android.OpenCVLoader

class DJIMainActivity : AppCompatActivity() {

    private lateinit var routeController: RouteController


    companion object {
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

    private val handler: Handler = Handler(Looper.getMainLooper())
    private val disposable = CompositeDisposable()
    private var isOpenCVInitialized = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkAndRequestPermissions()
        if (!isTaskRoot && intent.hasCategory(Intent.CATEGORY_LAUNCHER) && Intent.ACTION_MAIN == intent.action) {

            finish()
            return

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
        routeController.stopReceivingLocation()
        KeyManager.getInstance().cancelListen(this)
        handler.removeCallbacksAndMessages(null)
        disposable.dispose()

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
                    CameraPermissionScreen()
                }
            }
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


    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun CameraPermissionScreen() {
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
                        Button(onClick = {
                            navigateToTrackingControl()
                        }) {
                            Text(text = "Drone Mode")
                        }
                    }
                }
            }
        )
    }

    private fun navigateToTrackingControl() {
        val intent = Intent(this, TrackingTargetActivity::class.java)
        startActivity(intent)
    }

}

