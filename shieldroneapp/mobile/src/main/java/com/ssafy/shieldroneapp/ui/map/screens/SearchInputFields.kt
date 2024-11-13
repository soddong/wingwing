package com.ssafy.shieldroneapp.ui.map.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SearchInputFields(
    startText: String,
    endText: String,
    onStartTextChange: (String) -> Unit,
    onEndTextChange: (String) -> Unit
) {
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
            singleLine = true  // 한 줄로 설정
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