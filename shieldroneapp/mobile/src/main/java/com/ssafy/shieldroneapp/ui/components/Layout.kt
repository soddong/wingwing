package com.ssafy.shieldroneapp.ui.components

/**
 * 앱 전체에서 공통으로 사용하는 레이아웃 컴포넌트.
 *
 * 화면 구성에 필요한 기본 레이아웃을 제공하여 일관된 디자인을 유지한다.
 * 자주 사용되는 패딩, 마진 등을 포함한 구조적인 배치를 관리한다.
 *
 * @param content 레이아웃 내에 배치할 컴포저블 콘텐츠
 */

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.ssafy.shieldroneapp.ui.theme.ShieldroneappTheme

@Composable
fun Layout (
    content: @Composable () -> Unit
) {
    // 테마 전환 상태 관리
//    var isDarkTheme by remember { mutableStateOf(true) }
    ShieldroneappTheme {
//    ShieldroneappTheme(darkTheme = isDarkTheme) {
        // 전체 배경을 설정하는 Box
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                // 테마 전환 토글 버튼 -> 추후 다른 데로 이동
//                Row(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .background(MaterialTheme.colors.secondary),
//                    verticalAlignment = Alignment.CenterVertically,
//                    horizontalArrangement = Arrangement.End
//                ) {
//                    Text(
//                        text = if (isDarkTheme) "다크 모드" else "라이트 모드",
//                        style = MaterialTheme.typography.body1,
//                        color = MaterialTheme.colors.onSecondary
//                    )
//                    Switch(
//                        checked = isDarkTheme,
//                        onCheckedChange = { isDarkTheme = it },
//                        colors = SwitchDefaults.colors(
//                            checkedThumbColor = MaterialTheme.colors.onSecondary,
//                            checkedTrackColor = MaterialTheme.colors.onSecondary,
//                            uncheckedThumbColor = MaterialTheme.colors.onSecondary,
//                            uncheckedTrackColor = MaterialTheme.colors.onSecondary
//                        )
//                    )
//                }

                // 메인 콘텐츠 영역
                content()
            }
        }
    }
}