package com.ssafy.shieldroneapp.ui.authentication.screens

/**
 * 서비스 초기 안내 화면.
 *
 * "안심 귀가 서비스 이용을 위해 본인 인증을 진행합니다."와 같은 안내 문구와
 * "인증하기" 버튼을 제공하여 인증 절차를 시작할 수 있도록 한다.
 */

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
    onAuthenticateClick: () -> Unit // 인증하기 버튼 클릭 콜백
) {
    Layout {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(48.dp)
                .padding(top = 78.dp, bottom = 16.dp), // 위아래 추가 패딩
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
                Text(
                    text = "안심 귀가 서비스\n" +
                            "이용을 위해\n" +
                            "본인인증을 진행합니다.",
                    style = MaterialTheme.typography.h3.copy(lineHeight = 48.sp), // 줄 간격 추가,
                    color = MaterialTheme.colors.onBackground
                )

            CustomButton(
                text = "인증하기",
                onClick = {
                    onAuthenticateClick()
                },
                type = ButtonType.LARGE
            )
        }
    }
}