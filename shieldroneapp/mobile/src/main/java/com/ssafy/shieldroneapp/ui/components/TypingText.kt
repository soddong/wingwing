package com.ssafy.shieldroneapp.ui.components

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.unit.sp // sp 단위 import 추가
import kotlinx.coroutines.delay // delay import 추가

@Composable
fun TypingText(
    fullText: String, // 전체 텍스트
    typingSpeed: Long = 100L, // 타이핑 속도 (밀리초 단위)
    pauseDuration: Long = 1000L // 전체 텍스트 출력 후 멈추는 시간 (밀리초 단위)
) {
    // 현재 표시될 텍스트 상태
    var displayedText by remember { mutableStateOf("") }

    // 타이핑 효과 반복 구현
    LaunchedEffect(fullText) {
        while (true) { // 무한 반복
            fullText.forEachIndexed { index, _ ->
                displayedText = fullText.substring(0, index + 1)
                delay(typingSpeed) // 글자 추가 간격
            }
            delay(pauseDuration) // 전체 텍스트 출력 후 멈춤
            displayedText = "" // 텍스트 초기화
        }
    }

    // 텍스트 출력
    Text(
        text = displayedText,
        style = MaterialTheme.typography.h1.copy(letterSpacing = 3.sp), // 자간 조절
        color = MaterialTheme.colors.secondary
    )
}