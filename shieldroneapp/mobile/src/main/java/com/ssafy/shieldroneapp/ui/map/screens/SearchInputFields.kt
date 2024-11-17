package com.ssafy.shieldroneapp.ui.map.screens

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
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

    Column (
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
            // 출발지 입력 필드
            TextField(
                value = startText,
                onValueChange = onStartTextChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            Log.d("SearchInputFields", "출발지 검색창에 포커스가 들어왔습니다.")
                            onFieldClick(LocationType.START)
                        }
                    },
                label = { Text("출발지 검색") },
                colors = TextFieldDefaults.textFieldColors(backgroundColor = MaterialTheme.colors.background),
                trailingIcon = {
                    if (startText.isNotEmpty()) {
                        TextButton (
                            onClick = { onStartTextChange("") }
                        ) {
                            Text("X")
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
            TextField(
                value = endText,
                onValueChange = onEndTextChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            Log.d("SearchInputFields", "도착지 검색창에 포커스가 들어왔습니다.")
                            onFieldClick(LocationType.END)
                        }
                    },
                label = { Text("도착지 검색") },
                colors = TextFieldDefaults.textFieldColors(backgroundColor = MaterialTheme.colors.background),
                trailingIcon = {
                    if (endText.isNotEmpty()) {
                        TextButton(
                            onClick = { onEndTextChange("") }
                        ) {
                            Text("X")
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