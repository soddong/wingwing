package com.ssafy.shieldroneapp.ui.theme

/**
 * 앱에서 사용되는 텍스트 스타일을 정의하는 파일.
 *
 * 다양한 텍스트 요소의 서체, 크기, 무게 등을 설정하여 일관된 텍스트 스타일을 제공한다.
 */

import com.ssafy.shieldroneapp.R
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

// FontFamily 정의: Pretendard 사용
val Pretendard = FontFamily(
    Font(R.font.pretendard_bold, FontWeight.Bold), // Bold 폰트
    Font(R.font.pretendard_regular, FontWeight.Normal), // Regular 폰트
    Font(R.font.pretendard_light, FontWeight.Light), // Light 폰트 (필요한 경우 사용)
)

val LaundryGothic = FontFamily(
    Font(R.font.laundry_gothic_bold, FontWeight.Bold),
    Font(R.font.laundry_gothic_regular, FontWeight.Normal),
)

val Tenada = FontFamily(
    Font(R.font.tenada, FontWeight.Bold)
)

// 기본 텍스트 스타일 설정
val Typography = Typography(
    h1 = TextStyle(
        fontFamily = Tenada,
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp
    ),
    h2 = TextStyle(
        fontFamily = Tenada,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp
    ),
    h3 = TextStyle(
        fontFamily = Tenada,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp
    ),
    h4 = TextStyle(
        fontFamily = Tenada,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp
    ),
    h5 = TextStyle(
        fontFamily = Tenada,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp
    ),
    h6 = TextStyle(
        fontFamily = Tenada,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp
    ),

    subtitle1 = TextStyle(
        fontFamily = LaundryGothic,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp
    ),
    subtitle2 = TextStyle(
        fontFamily = LaundryGothic,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp
    ),

    body1 = TextStyle(
        fontFamily = LaundryGothic,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    ),
    body2 = TextStyle(
        fontFamily = LaundryGothic,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp
    ),

    caption = TextStyle(
        fontFamily = LaundryGothic,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp
    ),
)