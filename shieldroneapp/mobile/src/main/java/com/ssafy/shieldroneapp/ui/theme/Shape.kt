package com.ssafy.shieldroneapp.ui.theme

/**
 * 앱에서 사용되는 모양(Shape)을 정의하는 파일.
 *
 * 버튼, 모달 등의 컴포넌트에 적용할 모서리 둥글기를 설정하여
 * 일관된 디자인을 제공한다.
 */

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Shapes
import androidx.compose.ui.unit.dp

// 기본 Shapes 설정
val Shapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(30.dp)
)