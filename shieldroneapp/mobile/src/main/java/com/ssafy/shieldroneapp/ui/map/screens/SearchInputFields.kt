package com.ssafy.shieldroneapp.ui.map.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ssafy.shieldroneapp.utils.rememberKeyboardController

@Composable
fun SearchInputFields(
    startText: String,
    endText: String,
    onStartTextChange: (String) -> Unit,
    onEndTextChange: (String) -> Unit
) {
    // 키보드 매니저 생성 (맵 클릭 시, 키보드 숨기기 위해)
    val keyboardController = rememberKeyboardController()

    Column (
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TextField(
            value = startText,
            onValueChange = onStartTextChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("출발지 검색") },
            colors = TextFieldDefaults.textFieldColors(backgroundColor = MaterialTheme.colors.background),
            singleLine = true,  // 한 줄로 설정
            keyboardActions = KeyboardActions(
                onDone = {
                    onStartTextChange(startText)  // Enter 키 누를 때 검색 호출
                    keyboardController.hideKeyboard()    // Enter 후 키보드 숨기기
                }
            )
        )
        TextField(
            value = endText,
            onValueChange = onEndTextChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("도착지 검색") },
            colors = TextFieldDefaults.textFieldColors(backgroundColor = MaterialTheme.colors.background),
            singleLine = true  // 한 줄로 설정
        )
    }
}