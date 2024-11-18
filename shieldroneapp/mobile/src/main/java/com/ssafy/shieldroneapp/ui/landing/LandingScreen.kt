package com.ssafy.shieldroneapp.ui.landing

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import com.ssafy.shieldroneapp.R
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ssafy.shieldroneapp.ui.components.Layout
import com.ssafy.shieldroneapp.ui.components.TypingText
import kotlinx.coroutines.delay

@Composable
fun LandingScreen(
    navigateToNextScreen: () -> Unit,
    viewModel: LandingViewModel = hiltViewModel()
) {
    // 3초 후 자동 화면 전환
    LaunchedEffect(Unit) {
        delay(3000L)
        navigateToNextScreen()
    }

    Layout {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // 배경 이미지
            Image(
                painter = painterResource(id = R.drawable.landing_background),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop // 이미지를 화면 크기에 맞게 조정
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxSize()
                    .padding(top = 220.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 프로젝트 제목
                TypingText(
                    fullText = "WINGWING", // 타이핑 효과 적용
                )

                Text(
                    text = "WINGWING과 함께 안전 귀가를 시작하세요.\n" +
                            "든든한 동행이 언제나 당신의 곁을 지킵니다.",
                    style = MaterialTheme.typography.body2.copy(lineHeight = 22.sp),
                    color = MaterialTheme.colors.background,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}