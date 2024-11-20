package com.ssafy.shieldroneapp.ui.theme

/**
 * 전체 앱의 테마를 정의하는 파일.
 *
 * 색상, 텍스트 스타일, 쉐이프 등을 결합하여
 * 일관된 앱 테마를 구성하고 적용한다.
 */

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable

// 라이트 테마 색상 팔레트
private val LightColors = lightColors(
    primary = Blue900, // 앱 주요 색상 (툴 바, 버튼 등)
    onPrimary = Gray50, // primary 위 텍스트, 아이콘 등
//    primaryVariant = Blue700, // 강조

    secondary = Blue900, // 보조 색상 (추가 강조 요소)
    onSecondary = Gray50,

    background = Gray50, // 앱 전체 배경 색
    onBackground = Gray900,

//    surface = Gray50, // 모달 같은 컴포넌트 배경 색
//    onSurface = Gray900,
)

// 다크 테마 색상 팔레트
private val DarkColors = darkColors(
    primary = Blue50,
    onPrimary = Gray900,
//    primaryVariant = Blue200,

    secondary = Orange100,
    onSecondary = Gray900,

    background = Gray900,
    onBackground = Gray50,

//    surface = Gray700,
//    onSurface = Gray50,
)

@Composable
fun ShieldroneappTheme(
//    darkTheme: Boolean = isSystemInDarkTheme(), // 시스템 설정에 따라 다크 모드 여부 결정
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colors = colors,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}