package com.ssafy.shieldroneapp.ui.landing

/**
 * 앱의 최초 시작 화면을 구성하는 Composable 함수.
 *
 * 앱의 이름과 설명 문구를 화면에 표시하고, 시작하기 버튼을 통해 인증 과정으로 전환할 수 있다.
 * 사용자가 시작하기 버튼을 누르면 `onStartClick` 콜백을 호출하여
 * authentication의 IntroScreen으로 화면이 전환된다.
 *
 * @param onStartClick 시작하기 버튼 클릭 시 호출되는 콜백 함수로, IntroScreen으로 전환된다.
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
fun LandingScreen(
    onStartClick: () -> Unit // 버튼 클릭 콜백
) {
    Layout {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(48.dp)
                .padding(top = 78.dp, bottom = 64.dp), // 위아래 추가 패딩
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
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
                    style = MaterialTheme.typography.h5.copy(lineHeight = 32.sp), // 줄 간격 추가
                    color = MaterialTheme.colors.onBackground
                )
            }

            // 시작하기 버튼
            CustomButton(
                text = "시작하기",
                onClick = onStartClick,
                type = ButtonType.LARGE
            )
        }
    }
}
