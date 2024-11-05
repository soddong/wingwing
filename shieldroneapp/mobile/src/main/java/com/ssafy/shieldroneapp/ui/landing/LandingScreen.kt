package com.ssafy.shieldroneapp.ui.landing

import android.Manifest
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
import com.ssafy.shieldroneapp.ui.components.ButtonType
import com.ssafy.shieldroneapp.ui.components.CustomButton
import com.ssafy.shieldroneapp.ui.components.Layout

@Composable
fun LandingScreen(
    permissionViewModel: PermissionViewModel = hiltViewModel(),
    onStartClick: () -> Unit
) {
    val context = LocalContext.current
    val permissionState by permissionViewModel.audioPermissionGranted.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionViewModel.updateAudioPermissionStatus(isGranted)
        if (!isGranted) {
            Toast.makeText(
                context,
                "음성 인식을 위해 마이크 권한이 필요합니다",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    LaunchedEffect(Unit) {
        if (!permissionState) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // 메인 레이아웃
    Layout {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(48.dp)
                .padding(top = 78.dp, bottom = 64.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(60.dp)
            ) {
                // 앱 타이틀
                Text(
                    text = "Shieldrone",
                    style = MaterialTheme.typography.h1,
                    color = MaterialTheme.colors.onBackground
                )

                // 앱 설명
                Text(
                    text = "Shieldrone과 함께\n" +
                            "안전 귀가를 시작하세요.\n" +
                            "든든한 동행이 \n" +
                            "언제나 당신의 곁을 지킵니다.",
                    style = MaterialTheme.typography.h5.copy(lineHeight = 32.sp),
                    color = MaterialTheme.colors.onBackground
                )
            }

            // 권한이 있으면 다음 화면으로 이동, 없으면 권한 요청
            CustomButton(
                text = "시작하기",
                onClick = {
                    if (permissionState) {
                        onStartClick()
                    } else {
                        // 권한이 없는 경우 다시 요청
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                type = ButtonType.LARGE
            )
        }
    }
}