package com.ssafy.shieldroneapp.ui.authentication.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.ssafy.shieldroneapp.utils.DateUtils
import com.ssafy.shieldroneapp.utils.ValidationUtils

/**
 * 사용자의 생년월일을 입력받는 화면
 *
 * 생년월일 입력 필드와 다음 단계로 넘어가는 버튼을 제공하며,
 * 입력된 생년월일은 자동으로 포맷팅(YYYY-MM-DD)되어 표시된다.
 * 사용자가 입력한 값은 ViewModel에 저장된다.
 *
 * @param initialBirth ViewModel에서 전달받은 이전에 입력된 생년월일 값
 * @param onBirthSubmit 생년월일 입력 완료 시 호출될 콜백
 * @param onBackClick 뒤로 가기 버튼 클릭 시 호출될 콜백
 */
@Composable
fun BirthInputScreen(
    initialBirth: String = "",
    onBirthSubmit: (String) -> Unit,
    onBackClick: () -> Unit
) {
    var birth by remember { mutableStateOf(TextFieldValue(initialBirth)) }
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
            .clickable { keyboardController?.hide() }, // 빈 공간 클릭 시 키보드 숨기기
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
            text = "생년월일을 입력해 주세요.",
            style = MaterialTheme.typography.h4,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )

        TextField(
            value = birth,
            onValueChange = { newValue ->
                val digitsOnly = newValue.text.filter { it.isDigit() }

                // 현재 포맷이 완료된 상태라면 추가 변환을 건너뜀
                if (birth.text.length == 10 && birth.text == DateUtils.formatBirthInput(digitsOnly)) {
                    return@TextField
                }

                val formattedValue = if (digitsOnly.length <= 8) {
                    DateUtils.formatBirthInput(digitsOnly)
                } else {
                    DateUtils.formatBirthInput(digitsOnly.take(8))
                }

                birth = TextFieldValue(
                    text = formattedValue,
                    selection = TextRange(formattedValue.length) // 커서를 텍스트 끝으로 위치
                )

                validationError = if (birth.text.length == 10) {
                    ValidationUtils.validateBirth(birth.text)
                } else {
                    null
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester), // autofocus
            label = { Text("생년월일 (숫자만 입력 / YYYY-MM-DD)") },
            singleLine = true,
            isError = validationError != null,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (validationError == null && birth.text.length == 10) {
                        onBirthSubmit(birth.text)
                    }
                    keyboardController?.hide()
                }
            ),
            colors = TextFieldDefaults.textFieldColors(
                backgroundColor = MaterialTheme.colors.surface
            )
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
                if (validationError == null && birth.text.length == 10) {
                    onBirthSubmit(birth.text)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            enabled = validationError == null && birth.text.length == 10
        ) {
            Text(
                text = "다음",
                style = MaterialTheme.typography.h5
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
