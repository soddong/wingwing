package com.ssafy.shieldroneapp.ui.map.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.ssafy.shieldroneapp.R
import kotlinx.coroutines.delay

@Composable
fun DroneAnimation(
    onAnimationEnd: () -> Unit
) {
    // 화면 높이 계산
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp

    // 애니메이션 상태
    var isAnimationStarted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isAnimationStarted = true
        delay(3500) // 3.5초 후에 애니메이션 종료 콜백 호출
        onAnimationEnd()
    }

    // 시작 위치를 화면 높이의 0.2배 정도로 조정 (양수값은 아래로 이동)
    val startOffset = screenHeight.value * 0.2f

    // 드론의 Y 위치 애니메이션
    val yOffset by animateFloatAsState(
        // 시작 위치(startOffset)에서 화면 위로(-screenHeight) 이동
        targetValue = if (isAnimationStarted) -screenHeight.value else startOffset,
        animationSpec = tween(
            durationMillis = 3500,
            easing = EaseInOutQuart, // 5차 곡선으로 가속 후 감속
        ),
        label = "DroneVerticalAnimation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(Float.MAX_VALUE), // 최상단 레이어로 설정
        contentAlignment = Alignment.BottomCenter
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_drone),
            contentDescription = "Drone Animation",
            modifier = Modifier
                .size(600.dp)
                .offset(y = yOffset.dp)
        )
    }
}