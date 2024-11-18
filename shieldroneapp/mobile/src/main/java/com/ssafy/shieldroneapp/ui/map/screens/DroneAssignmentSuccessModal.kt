package com.ssafy.shieldroneapp.ui.map.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ssafy.shieldroneapp.data.model.DroneState

@Composable
fun DroneAssignmentSuccessModal(
    droneState: DroneState,
    selectedStart: String, // 시작 경로 텍스트 추가
    selectedEnd: String,   // 도착 경로 텍스트 추가
    onDroneCodeInput: (Int) -> Unit,
    onRequestMatching: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val codeInputs = remember { mutableStateOf(List(6) { "" }) }
    val focusRequesters = remember { List(6) { FocusRequester() } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "${droneState.droneId}번 드론이 배정되었습니다!",
                    style = MaterialTheme.typography.subtitle1,
                )
                Spacer(modifier = Modifier.height(8.dp))
                // 시작 > 도착 경로 표시
                Text(
                    text = "$selectedStart > $selectedEnd",
                    style = MaterialTheme.typography.body1
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "(예상) 이동 시간: ${droneState.estimatedTime ?: "N/A"}분",
                    style = MaterialTheme.typography.body2,
                )
                Text(
                    text = "(예상) 이동 거리: ${droneState.distance ?: "N/A"}m",
                    style = MaterialTheme.typography.body2,
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 6칸의 인증 코드 입력 칸
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (i in 0 until 6) {
                        TextField(
                            value = codeInputs.value[i],
                            onValueChange = { input ->
                                if (input.length <= 1 && input.all { it.isDigit() }) {
                                    val newInputs = codeInputs.value.toMutableList()
                                    newInputs[i] = input
                                    codeInputs.value = newInputs

                                    if (input.isNotEmpty() && i < 5) {
                                        // 다음 칸으로 이동할 때 다음 칸의 값을 초기화
                                        val nextInputs = codeInputs.value.toMutableList()
                                        nextInputs[i + 1] = ""
                                        codeInputs.value = nextInputs
                                        focusRequesters[i + 1].requestFocus()
                                    }
                                }
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions.Default.copy(
                                keyboardType = KeyboardType.Number
                            ),
                            colors = TextFieldDefaults.textFieldColors(
                                backgroundColor = MaterialTheme.colors.background, // 배경색
                                focusedIndicatorColor = MaterialTheme.colors.secondary, // 포커스된 밑줄 색상
                                unfocusedIndicatorColor = MaterialTheme.colors.onBackground, // 비포커스 밑줄 색상
                                cursorColor = MaterialTheme.colors.secondary // 커서 색상
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequesters[i])
                                .onFocusChanged { focusState ->
                                    if (focusState.isFocused) {
                                        // 클릭 시 해당 입력 칸 초기화
                                        val newInputs = codeInputs.value.toMutableList()
                                        newInputs[i] = ""
                                        codeInputs.value = newInputs
                                    }
                                }
                                .onKeyEvent { event ->
                                    if (event.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DEL) {
                                        if (codeInputs.value[i].isEmpty() && i > 0) {
                                            // 이전 칸으로 이동하면서 값 삭제
                                            val newInputs = codeInputs.value.toMutableList()
                                            newInputs[i - 1] = ""
                                            codeInputs.value = newInputs
                                            focusRequesters[i - 1].requestFocus()
                                        }
                                    }
                                    false
                                }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "배정 받은 드론의 고유 코드를 입력해주세요.",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.secondary,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val droneCode = codeInputs.value.joinToString("")
                    if (droneCode.length == 6 && droneCode.all { it.isDigit() }) {
                        onDroneCodeInput(droneCode.toInt())
                        onRequestMatching(droneCode.toInt())
                    } else {
                        Log.e("DroneAssignmentSuccessModal", "Invalid drone code: $droneCode")
                    }
                },
                colors = androidx.compose.material.ButtonDefaults.buttonColors(
                    backgroundColor = MaterialTheme.colors.secondary, // 버튼 배경색
                    contentColor = MaterialTheme.colors.onSecondary   // 버튼 텍스트 색
                ),
            ) {
                Text(
                    text = "드론 매칭 요청",
                    style = MaterialTheme.typography.subtitle2,
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = androidx.compose.material.ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colors.secondary)

            ) {
                Text(
                    text = "닫기",
                    style = MaterialTheme.typography.subtitle2,
                )
            }
        }
    )
}