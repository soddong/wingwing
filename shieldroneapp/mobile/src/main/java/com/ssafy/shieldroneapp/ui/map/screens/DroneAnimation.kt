//package com.ssafy.shieldroneapp.ui.map.screens
//
//import androidx.compose.animation.Animatable
//import androidx.compose.animation.core.tween
//import androidx.compose.foundation.Image
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.offset
//import androidx.compose.foundation.layout.size
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.LaunchedEffect
//import androidx.compose.runtime.remember
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.res.painterResource
//import androidx.compose.ui.unit.dp
//import com.ssafy.shieldroneapp.R
//
//@Composable
//fun DroneAnimation() {
//    val offsetY = remember { Animatable(300f) }
//
//    LaunchedEffect { }(Unit) {
//        offsetY.animateTo(
//            targetValue = -300f,
//            animationSpec = tween(durationMillis = 2000)
//        )
//    }
//
//    Box(
//        modifier = Modifier
//            .fillMaxSize()
//            .offset(y = offsetY.value.dp),
//        contentAlignment = Alignment.Center
//    ) {
//        Image(
//            painter = painterResource(id = R.drawable.drone_image),
//            contentDescription = "드론 이미지",
//            modifier = Modifier.size(100.dp)
//        )
//    }
//}
