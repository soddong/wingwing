package com.ssafy.shieldroneapp.ui.authentication.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.shieldroneapp.ui.components.ButtonType
import com.ssafy.shieldroneapp.ui.components.CustomButton
import com.ssafy.shieldroneapp.ui.components.Layout

@Composable
fun IntroScreen(
    onAuthenticateClick: () -> Unit
) {
    Layout {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(48.dp)
                .padding(top = 78.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "안심 귀가 서비스\n" +
                        "이용을 위해\n" +
                        "본인인증을 진행합니다.",
                style = MaterialTheme.typography.h3.copy(lineHeight = 48.sp),
                color = MaterialTheme.colors.onBackground
            )

            CustomButton(
                text = "인증하기",
                onClick = {
                    // WebSocket 연결은 인증이 완료된 후에 초기화함
                    onAuthenticateClick()
                },
                type = ButtonType.LARGE
            )
        }
    }
}