package com.ssafy.shieldroneapp.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun HeartRateDisplay(
    heartRate: Double,
) {
    var lastValidHeartRate by rememberSaveable { mutableStateOf(heartRate) }

    LaunchedEffect(heartRate) {
        if (heartRate > 0) {
            lastValidHeartRate = heartRate
        }
    }

    // bpm에 따른 애니메이션 속도와 크기 계산
    val animationDuration = if (heartRate > 0) {
        (30000 / heartRate).toInt().coerceIn(200, 800)
    } else {
        500
    }

    // 심박수에 따른 크기 변화 계산
    val scaleRange = if (heartRate > 0) {
        val baseScale = 0.15f
        val additionalScale = (heartRate / 100f) * 0.1f
        baseScale + additionalScale
    } else {
        0.2f
    }

    // 심장 박동 애니메이션
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1f + scaleRange.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = animationDuration,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        )
    )

    Card(
        modifier = Modifier
            .wrapContentWidth(),
        elevation = 0.dp,
        backgroundColor = Color(0xB3FFFFFF),
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier
                .width(146.dp)
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = "Heart Icon",
                tint = if (heartRate >= 100) Color.Red else Color.Red.copy(alpha = 0.8f),
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "${if (heartRate > 0) heartRate.toInt() else lastValidHeartRate.toInt()} bpm",
                style = MaterialTheme.typography.h5.copy(
                    fontWeight = FontWeight.Bold
                ),
            )
        }
    }
}