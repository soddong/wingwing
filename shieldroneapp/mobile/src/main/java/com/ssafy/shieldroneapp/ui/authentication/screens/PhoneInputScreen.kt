package com.ssafy.shieldroneapp.ui.authentication.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ssafy.shieldroneapp.utils.ValidationUtils

/**
 * 사용자의 휴대폰 번호를 입력받는 화면
 *
 * 휴대폰 번호 입력 필드와 인증 문자 발송 버튼을 제공하며,
 * 유효한 번호 입력 시 인증 문자가 발송된다.
 * 사용자가 입력한 값은 ViewModel에 저장된다.
 *
 * @param initialPhoneNumber ViewModel에서 전달받은 이전에 입력된 휴대폰 번호
 * @param onPhoneSubmit 휴대폰 번호 입력 완료 및 인증 요청 시 호출될 콜백
 * @param onBackClick 뒤로 가기 버튼 클릭 시 호출될 콜백
 */
@Composable
fun PhoneInputScreen(
    initialPhoneNumber: String = "",
    onPhoneSubmit: (String) -> Unit,
    onBackClick: () -> Unit
) {
    val (phoneNumber, setPhoneNumber) = remember { mutableStateOf(initialPhoneNumber) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .background(MaterialTheme.colors.background)
            .fillMaxWidth()
            .padding(16.dp)
            .clickable { keyboardController?.hide() }, // 화면을 클릭하면 키보드 숨기기
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
            text = "휴대폰 번호를 입력해주세요.",
            style = MaterialTheme.typography.h4,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )

        TextField(
            value = phoneNumber,
            onValueChange = { newValue ->
                setPhoneNumber(newValue)
                validationError =
                    if (newValue.isNotBlank()) ValidationUtils.validatePhone(newValue) else null
            },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester), // autofocus
            label = { Text("휴대폰 번호 (숫자만 입력)") },
            singleLine = true,
            isError = validationError != null,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (validationError == null && phoneNumber.isNotBlank()) {
                        onPhoneSubmit(phoneNumber)
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

        Button(
            onClick = {
                if (validationError == null && phoneNumber.isNotBlank()) {
                    onPhoneSubmit(phoneNumber) // 인증 번호 요청 이벤트 전달
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            enabled = validationError == null && phoneNumber.isNotBlank()
        ) {
            Text(
                text = "인증 문자 보내기",
                style = MaterialTheme.typography.h5
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
