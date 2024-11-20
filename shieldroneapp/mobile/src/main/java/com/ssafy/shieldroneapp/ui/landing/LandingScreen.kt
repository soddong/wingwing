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
//            Image(
//                painter = painterResource(id = R.drawable.landing_background),
//                contentDescription = null,
//                modifier = Modifier.fillMaxSize(),
//                contentScale = ContentScale.Crop // 이미지를 화면 크기에 맞게 조정
//            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxSize()
                    .padding(top = 220.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // 프로젝트 제목
                    TypingText(
                        fullText = "WINGWING", // 타이핑 효과 적용
                    )

                    Spacer(modifier = Modifier.height(46.dp)) // 간격 추가

                    Text(
                        text = "안전 귀가를 위한 새로운 동행",
                        style = MaterialTheme.typography.subtitle1.copy(lineHeight = 22.sp),
                        color = MaterialTheme.colors.primary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }


                // 국기와 텍스트를 나란히 배치
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 국기 이미지
                    Image(
                        painter = painterResource(id = R.drawable.korea),
                        contentDescription = "대한민국 국기",
                        modifier = Modifier
                            .size(24.dp) // 이미지 크기 조정
                            .padding(end = 8.dp) // 텍스트와 간격 추가
                    )

                    // 텍스트
                    Text(
                        text = "든든한 동행이 언제나 당신의 곁을 지킵니다.",
                        style = MaterialTheme.typography.body2.copy(lineHeight = 22.sp),
                        color = MaterialTheme.colors.primary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}