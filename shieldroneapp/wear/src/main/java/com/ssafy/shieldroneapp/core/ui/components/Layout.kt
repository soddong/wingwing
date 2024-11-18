package com.ssafy.shieldroneapp.core.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.wear.compose.material.*
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState

@Composable
fun Layout(
    modifier: Modifier = Modifier,
    hasClock: Boolean = true,
    children: @Composable () -> Unit
) {
    val listState = rememberScalingLazyListState()
    
    Scaffold(
        timeText = {
            if (hasClock) {
                TimeText(
                    modifier = Modifier.scrollAway(listState)
                )
            }
        },
        vignette = {
            Vignette(vignettePosition = VignettePosition.TopAndBottom)
        },
        positionIndicator = {
            PositionIndicator(
                scalingLazyListState = listState
            )
        }
    ) {
        Box(modifier = modifier.fillMaxSize()) {
            children()
        }
    }
}