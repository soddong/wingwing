package com.ssafy.shieldroneapp.ui.authentication.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Checkbox
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 서비스 이용 약관 동의 화면
 *
 * 개인정보/GPS 수집 동의와 서비스 이용 서약을 받고,
 * 모든 약관 동의 시 서비스 시작이 가능하다.
 *
 * @param onAccept 약관 동의 완료 시 호출될 콜백
 * @param onBackClick 뒤로 가기 버튼 클릭 시 호출될 콜백
 */
@Composable
fun TermAgreementScreen(
    onAccept: () -> Unit,
    onBackClick: () -> Unit
) {
    val (privacyChecked, setPrivacyChecked) = remember { mutableStateOf(false) }
    val (termsChecked, setTermsChecked) = remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .background(MaterialTheme.colors.background)
            .fillMaxSize()
            .padding(16.dp),
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
            text = "본인 인증이 완료되었습니다!\n\n이용 동의 및 서약 후\n서비스를 이용하실 수 있습니다.",
            style = MaterialTheme.typography.h5.copy(lineHeight = 32.sp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )

        Spacer(modifier = Modifier.weight(1f))

        // 개인정보 동의
        Row (
            modifier = Modifier
                .fillMaxWidth()
                .clickable { setPrivacyChecked(!privacyChecked) }, // 텍스트 클릭 시 체크 상태 변경
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = privacyChecked,
                onCheckedChange = setPrivacyChecked,
            )
            Text(
                text = "개인 정보 및 GPS 수집에 동의합니다.",
                style = MaterialTheme.typography.body1,
            )
        }

        // 이용 서약
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { setTermsChecked(!termsChecked) }, // 텍스트 클릭 시 체크 상태 변경
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = termsChecked,
                onCheckedChange = setTermsChecked,
            )
            Text(
                text = "앱을 본래 목적 외로 사용하거나 불법적으로 이용함이 적발되는 경우, 관련 법에 따라 엄중한 처벌을 받을 수 있음을 확인했습니다.",
                style = MaterialTheme.typography.body1,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (privacyChecked && termsChecked) {
                    onAccept()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            enabled = privacyChecked && termsChecked,
            colors = ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.colors.secondary, // secondary 배경
                contentColor = MaterialTheme.colors.onSecondary // 텍스트 색상
            ),
        ) {
            Text(
                text = "시작하기",
                style = MaterialTheme.typography.h6
            )
        }
    }
}