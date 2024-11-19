package com.ssafy.shieldroneapp.ui.map.screens

import android.util.Log
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import com.ssafy.shieldroneapp.data.model.LocationType
import com.ssafy.shieldroneapp.utils.rememberKeyboardController

@Composable
fun SearchInputFields(
    startText: String,
    endText: String,
    onStartTextChange: (String) -> Unit,
    onEndTextChange: (String) -> Unit,
    onFieldClick: (LocationType) -> Unit,
) {
    // 키보드 매니저 생성 (맵 클릭 시, 키보드 숨기기 위해)
    val keyboardController = rememberKeyboardController()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 출발지 입력 필드
        val isStartFocused = remember { mutableStateOf(false) }
        val emptyStartText = startText.isEmpty()
        OutlinedTextField(
            value = startText,
            onValueChange = onStartTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    isStartFocused.value = focusState.isFocused
                    if (focusState.isFocused) {
                        Log.d("SearchInputFields", "출발지 검색창에 포커스가 들어왔습니다.")
                        onFieldClick(LocationType.START)
                    }
                },
            label = {
                val labelStyle = when {
                    emptyStartText && !isStartFocused.value -> MaterialTheme.typography.subtitle2 // 텍스트가 없고, 포커스가 없을 때
                    else -> MaterialTheme.typography.caption // 텍스트가 있거나 포커스가 있을 때
                }
                Text(
                    text = "출발지 검색",
                    style = labelStyle,
                    color = if (isStartFocused.value || !emptyStartText) {
                        MaterialTheme.colors.secondary // 포커스되었거나 텍스트가 입력되었을 때 진한 secondary
                    } else {
                        MaterialTheme.colors.onSurface.copy(alpha = 0.4f) // 최초 회색(투명도 조정)
                    },
                    modifier = Modifier.animateContentSize() // 텍스트 크기 변경 애니메이션
                )
            },
            colors = TextFieldDefaults.textFieldColors(
                textColor = MaterialTheme.colors.onBackground.copy(alpha = 0.8f),
                disabledTextColor = MaterialTheme.colors.secondary.copy(alpha = 0.5f),
                backgroundColor = MaterialTheme.colors.background,
                focusedIndicatorColor = MaterialTheme.colors.secondary,
                unfocusedIndicatorColor = MaterialTheme.colors.secondary.copy(alpha = 0.5f),
                cursorColor = MaterialTheme.colors.secondary,
                trailingIconColor = MaterialTheme.colors.secondary
            ),
            trailingIcon = {
                if (!emptyStartText) {
                    TextButton(
                        onClick = { onStartTextChange("") }
                    ) {
                        Text("x", color = MaterialTheme.colors.secondary)
                    }
                }
            },
            singleLine = true,  // 한 줄로 설정
            keyboardActions = KeyboardActions(
                onDone = {
                    onStartTextChange(startText)  // Enter 키 누를 때 검색 호출
                    keyboardController.hideKeyboard()    // Enter 후 키보드 숨기기
                }
            ),
        )

        // 도착지 입력 필드
        val isEndFocused = remember { mutableStateOf(false) }
        val emptyEndText = endText.isEmpty()
        OutlinedTextField(
            value = endText,
            onValueChange = onEndTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    isEndFocused.value = focusState.isFocused
                    if (focusState.isFocused) {
                        Log.d("SearchInputFields", "도착지 검색창에 포커스가 들어왔습니다.")
                        onFieldClick(LocationType.END)
                    }
                },
            label = {
                val labelStyle = when {
                    emptyEndText && !isEndFocused.value -> MaterialTheme.typography.subtitle2 // 텍스트가 없고, 포커스가 없을 때
                    else -> MaterialTheme.typography.caption // 텍스트가 있거나 포커스가 있을 때
                }
                Text(
                    text = "도착지 검색",
                    style = labelStyle,
                    color = if (isEndFocused.value || !emptyEndText) {
                        MaterialTheme.colors.secondary // 포커스되었거나 텍스트가 입력되었을 때 진한 secondary
                    } else {
                        MaterialTheme.colors.onSurface.copy(alpha = 0.4f) // 최초 회색(투명도 조정)
                    },
                    modifier = Modifier.animateContentSize()
                )
            },
            colors = TextFieldDefaults.textFieldColors(
                textColor = MaterialTheme.colors.onBackground.copy(alpha = 0.8f),
                disabledTextColor = MaterialTheme.colors.secondary.copy(alpha = 0.5f),
                backgroundColor = MaterialTheme.colors.background,
                focusedIndicatorColor = MaterialTheme.colors.secondary,
                unfocusedIndicatorColor = MaterialTheme.colors.secondary.copy(alpha = 0.5f),
                cursorColor = MaterialTheme.colors.secondary,
                trailingIconColor = MaterialTheme.colors.secondary
            ),
            trailingIcon = {
                if (endText.isNotEmpty()) {
                    TextButton(
                        onClick = { onEndTextChange("") }
                    ) {
                        Text("x", color = MaterialTheme.colors.secondary)
                    }
                }
            },
            singleLine = true,  // 한 줄로 설정
            keyboardActions = KeyboardActions(
                onDone = {
                    onEndTextChange(endText)  // Enter 키 누를 때 검색 호출
                    keyboardController.hideKeyboard()    // Enter 후 키보드 숨기기
                }
            ),
        )
    }
}