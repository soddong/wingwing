package com.ssafy.shieldroneapp.ui.authentication.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
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
 * 사용자의 이름을 입력받는 화면
 *
 * 이름 입력 필드와 다음 단계로 넘어가는 버튼을 제공하며,
 * 사용자가 입력한 값은 ViewModel에 저장된다.
 *
 * @param initialName ViewModel에서 전달받은 이전에 입력된 이름 값
 * @param onNameSubmit 이름 입력 완료 시 호출될 콜백
 * @param onBackClick 뒤로 가기 버튼 클릭 시 호출될 콜백
 */
@Composable
fun NameInputScreen(
    initialName: String = "",
    onNameSubmit: (String) -> Unit,
    onBackClick: () -> Unit
) {
    val (name, setName) = remember { mutableStateOf(initialName) }
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
            text = "이름을 입력해주세요. (1/3)",
            style = MaterialTheme.typography.subtitle1,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )

        OutlinedTextField(
            value = name,
            onValueChange = { newValue ->
                if (!newValue.contains("\n")) {
                    setName(newValue)
                    validationError =
                        if (newValue.isNotBlank()) ValidationUtils.validateName(newValue) else null
                } else {
                    // 유효성 검사를 통과한 경우에만 submit
                    if (validationError == null && name.isNotBlank()) {
                        onNameSubmit(name)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester), // autofocus
            label = {
                Text(
                    text = "이름",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.secondary,
                )
            },
            singleLine = true,
            isError = validationError != null, // 에러 발생 시 TextField에 에러 표시
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done // Enter 키를 Done으로 표시
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (validationError == null && name.isNotBlank()) {
                        onNameSubmit(name)
                    }
                    keyboardController?.hide()
                }
            ),
            colors = TextFieldDefaults.textFieldColors(
                backgroundColor = MaterialTheme.colors.background,
                focusedIndicatorColor = MaterialTheme.colors.secondary,
                unfocusedIndicatorColor = MaterialTheme.colors.secondary,
                cursorColor = MaterialTheme.colors.secondary,
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
                if (validationError == null && name.isNotBlank()) {
                    onNameSubmit(name)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            enabled = validationError == null && name.isNotBlank(),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.colors.secondary, // secondary 배경
                contentColor = MaterialTheme.colors.onSecondary // 텍스트 색상
            ),
        ) {
            Text(
                text = "다음",
                style = MaterialTheme.typography.h6
            )
        }
    }
}