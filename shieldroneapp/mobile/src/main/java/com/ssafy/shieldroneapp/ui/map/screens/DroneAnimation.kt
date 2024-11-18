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

    // LaunchedEffect로 컴포넌트가 처음 표시될 때 애니메이션 시작
    LaunchedEffect(Unit) {
        isAnimationStarted = true
        // 3초 후에 애니메이션 종료 콜백 호출
        delay(3000)
        onAnimationEnd()
    }

    // 드론의 Y 위치 애니메이션
    val yOffset by animateFloatAsState(
        targetValue = if (isAnimationStarted) -screenHeight.value else 0f,
        animationSpec = tween(
            durationMillis = 3000,
            easing = FastOutLinearInEasing
        ),
        label = "DroneVerticalAnimation"
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_drone),
            contentDescription = "Drone Animation",
            modifier = Modifier
                .size(300.dp)
                .offset(y = yOffset.dp)
        )
    }
}