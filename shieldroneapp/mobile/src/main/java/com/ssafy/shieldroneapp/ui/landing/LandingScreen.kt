package com.ssafy.shieldroneapp.ui.landing

import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ssafy.shieldroneapp.ui.components.ButtonType
import com.ssafy.shieldroneapp.ui.components.CustomButton
import com.ssafy.shieldroneapp.ui.components.Layout


@Composable
fun LandingScreen(
    onAuthClick: () -> Unit,
    viewModel: LandingViewModel = hiltViewModel()
) {
    Layout {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(48.dp)
                .padding(top = 78.dp, bottom = 16.dp),
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
                    viewModel.startAudioService()
                    onAuthClick()
                },
                type = ButtonType.LARGE
            )
        }
    }
}