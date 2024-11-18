package com.ssafy.shieldroneapp.ui.components

/**
 * 앱 전반에서 사용하는 공통 버튼 컴포넌트.
 *
 * 다양한 버튼 스타일을 제공하며, 액션 버튼으로 사용된다.
 * 버튼 타입에 따라 크기와 색상이 달라진다.
 *
 * FULL_WIDTH: 화면 너비를 꽉 채우는 고정 높이 버튼. 주요 액션에 사용.
 * LARGE: 고정 높이와 80% 너비를 가진 버튼. 주로 "시작하기", "인증하기" 등의 액션에 사용.
 * SMALL: 고정된 높이의 작은 버튼. "설정" 또는 "등록" 등의 짧은 액션에 사용.
 * MODAL_LARGE: 모달 내부에서 사용되는 큰 버튼. 위험 알림 관련 버튼에 사용.
 * MODAL_MEDIUM: 모달 내부에서 사용되는 중간 크기 버튼. 출발지 선택 등에 사용.
 *
 * 버튼 상태는 기본, 비활성화(Disabled)로 나뉘며, 각각의 색상과 스타일을 정의
 * 클릭 이벤트를 처리하여 원하는 동작을 수행할 수 있다.
 *
 * @param text 버튼에 표시할 텍스트
 * @param onClick 버튼 클릭 시 호출되는 콜백 함수
 * @param type 버튼의 스타일을 결정하는 타입 (FULL_WIDTH, LARGE, SMALL, MODAL_LARGE, MODAL_MEDIUM)
 * @param isEnabled 버튼의 활성화 상태. 비활성화 시 회색으로 표시
 */

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// 버튼 타입 정의
enum class ButtonType {
    FULL_WIDTH, LARGE, MODAL_LARGE, MODAL_MEDIUM, SMALL
}

// Large 버튼만 스타일 적용 완료
@Composable
fun CustomButton(
    text: String,
    onClick: () -> Unit,
    type: ButtonType,
    isEnabled: Boolean = true
) {
    // 버튼 모양
    val modifier = when (type) {
        ButtonType.FULL_WIDTH -> Modifier.fillMaxWidth().height(60.dp)
        ButtonType.LARGE -> Modifier.width(300.dp).height(60.dp)
        ButtonType.MODAL_LARGE -> Modifier.width(240.dp).height(48.dp)
        ButtonType.MODAL_MEDIUM -> Modifier.width(200.dp).height(40.dp)
        ButtonType.SMALL -> Modifier.width(100.dp).height(36.dp)
//            .padding(8.dp)
    }

    // 버튼 색상 (기본, Disabled)
    val backgroundColor = if (!isEnabled) {
        MaterialTheme.colors.background // Disabled 상태
    } else {
        when (type) {
            ButtonType.FULL_WIDTH, ButtonType.LARGE, ButtonType.MODAL_MEDIUM -> MaterialTheme.colors.secondary
            ButtonType.MODAL_LARGE -> MaterialTheme.colors.secondary
            ButtonType.SMALL -> MaterialTheme.colors.onSurface
        }
    }

    // 버튼 모서리 둥글게
    val shape = when (type) {
        ButtonType.FULL_WIDTH, ButtonType.LARGE -> MaterialTheme.shapes.large
        ButtonType.MODAL_LARGE -> MaterialTheme.shapes.medium
        ButtonType.MODAL_MEDIUM, ButtonType.SMALL -> MaterialTheme.shapes.small
    }

    // 텍스트 스타일 설정
    val textStyle = when (type) {
        ButtonType.FULL_WIDTH -> MaterialTheme.typography.h6
        ButtonType.LARGE -> MaterialTheme.typography.h5
        ButtonType.MODAL_LARGE -> MaterialTheme.typography.subtitle1
        ButtonType.MODAL_MEDIUM -> MaterialTheme.typography.body1
        ButtonType.SMALL -> MaterialTheme.typography.body2
    }

    // 텍스트 색상
    val contentColor = if (isEnabled) {
        MaterialTheme.colors.onSecondary
    } else {
        MaterialTheme.colors.onBackground
    }

    Button(
        onClick = onClick,
        enabled = isEnabled,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = backgroundColor,
            contentColor = contentColor
        ),
        modifier = modifier
    ) {
        // 버튼 텍스트 컴포넌트 작성
        Text(
            text = text,
            style = textStyle,
        )
    }
}