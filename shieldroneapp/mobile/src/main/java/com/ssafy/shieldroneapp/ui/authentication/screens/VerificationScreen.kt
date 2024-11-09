package com.ssafy.shieldroneapp.ui.authentication.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

/**
 * 인증 코드를 입력하여 본인 인증을 완료하는 화면
 *
 * 인증 코드 입력 필드와 본인 인증 버튼을 제공하며,
 * 본인 인증 성공 시 다음 단계로 진행한다.
 * 또한, "인증 코드를 받지 못하셨나요?" 문구를 클릭하여 사용자가 인증 코드를 다시 받을 수 있도록 한다.
 *
 * @param onVerificationSubmit 인증 코드 입력 완료 시 호출될 콜백
 * @param onResendCode 인증 코드 재전송 시 호출될 콜백
 * @param onBackClick 뒤로 가기 버튼 클릭 시 호출될 콜백
 */
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.ssafy.shieldroneapp.utils.ValidationUtils

@Composable
fun VerificationScreen(
    onVerificationSubmit: (String) -> Unit,
    onResendCode: () -> Unit,
    onBackClick: () -> Unit
) {
    val (code, setCode) = remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .background(MaterialTheme.colors.background)
            .fillMaxSize()
            .padding(16.dp)
            .clickable { keyboardController?.hide() }, // 화면 빈 곳을 클릭하면 키보드 숨기기
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.ArrowBack,
            contentDescription = "뒤로 가기",
            modifier = Modifier
                .align(Alignment.Start)
                .clickable { onBackClick() }
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "인증 코드를 입력해주세요.",
            style = MaterialTheme.typography.h4,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )

        TextField(
            value = code,
            onValueChange = { newValue ->
                if (newValue.all { it.isDigit() }) { // 숫자만 입력 받기
                    setCode(newValue)
                    validationError = ValidationUtils.validateVerificationCode(newValue)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester), // autofocus
            label = { Text("인증 코드 (6자리)") },
            singleLine = true,
            isError = validationError != null,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (validationError == null && code.isNotBlank()) {
                        onVerificationSubmit(code)
                    }
                    keyboardController?.hide() // 키보드 숨기기
                }
            ),
            colors = TextFieldDefaults.textFieldColors(
                backgroundColor = MaterialTheme.colors.surface
            ),
        )

        // 유효성 에러 메시지 표시
        validationError?.let {
            Text(
                text = it,
                color = MaterialTheme.colors.error,
                style = MaterialTheme.typography.caption,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(start = 8.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "인증 코드를 받지 못하셨나요?",
            color = Color.Gray,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier
                .clickable {
                    onResendCode()
                }
                .padding(bottom = 8.dp)
        )

        Button(
            onClick = {
                if (validationError == null && code.isNotBlank()) {
                    onVerificationSubmit(code)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            enabled = validationError == null && code.isNotBlank()
        ) {
            Text(
                text = "인증",
                style = MaterialTheme.typography.h5
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
