package com.ssafy.shieldroneapp.ui.landing

import android.Manifest
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ssafy.shieldroneapp.permissions.PermissionViewModel
import com.ssafy.shieldroneapp.services.manager.AudioServiceManager
import com.ssafy.shieldroneapp.services.sensor.AudioRecordService
import com.ssafy.shieldroneapp.ui.components.ButtonType
import com.ssafy.shieldroneapp.ui.components.CustomButton
import com.ssafy.shieldroneapp.ui.components.Layout

private const val TAG = "모바일: 랜딩 스크린"

@Composable
fun LandingScreen(
    permissionViewModel: PermissionViewModel = hiltViewModel(),
    onStartClick: () -> Unit
) {
    val context = LocalContext.current
    val permissionState by permissionViewModel.audioPermissionGranted.collectAsState()
    val audioServiceManager: AudioServiceManager = remember { AudioServiceManager(context) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Log.d(TAG, "권한 요청 결과: $isGranted")
        permissionViewModel.updateAudioPermissionStatus(isGranted)
        if (!isGranted) {
            Toast.makeText(
                context,
                "음성 인식을 위해 마이크 권한이 필요합니다",
                Toast.LENGTH_LONG
            ).show()
            Log.d(TAG, "권한 거부 메시지 표시")
        } else {
            // 권한이 허용되면 즉시 오디오 서비스 시작
            audioServiceManager.startAudioService()
            onStartClick()
        }
    }

    LaunchedEffect(permissionState) {
        if (permissionState && !audioServiceManager.isRunning()) {
            audioServiceManager.startAudioService()
        }
    }

    Layout {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(48.dp)
                .padding(top = 78.dp, bottom = 64.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(60.dp)
            ) {
                Text(
                    text = "Shieldrone",
                    style = MaterialTheme.typography.h1,
                    color = MaterialTheme.colors.onBackground
                )

                Text(
                    text = "Shieldrone과 함께\n" +
                            "안전 귀가를 시작하세요.\n" +
                            "든든한 동행이 \n" +
                            "언제나 당신의 곁을 지킵니다.",
                    style = MaterialTheme.typography.h5.copy(lineHeight = 32.sp),
                    color = MaterialTheme.colors.onBackground
                )
            }

            CustomButton(
                text = "시작하기",
                onClick = {
                    if (permissionState) {
                        if (!audioServiceManager.isRunning()) {
                            audioServiceManager.startAudioService()
                        }
                        onStartClick()
                    } else {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                type = ButtonType.LARGE
            )
        }
    }
}