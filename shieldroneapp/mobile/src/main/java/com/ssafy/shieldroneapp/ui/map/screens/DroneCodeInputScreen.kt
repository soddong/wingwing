//package com.ssafy.shieldroneapp.ui.map.screens
//
//import androidx.compose.foundation.layout.Arrangement
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.Spacer
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.height
//import androidx.compose.foundation.layout.padding
//import androidx.compose.material.Button
//import androidx.compose.material.MaterialTheme
//import androidx.compose.material.Text
//import androidx.compose.material.TextButton
//import androidx.compose.material.TextField
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.unit.dp
//
//@Composable
//fun DroneCodeInputScreen(
//    onCodeSubmit: (String) -> Unit,
//    onCancel: () -> Unit
//) {
//    var code = remember { mutableStateOf("") }
//
//    Column (
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(16.dp),
//        verticalArrangement = Arrangement.Center,
//        horizontalAlignment = Alignment.CenterHorizontally
//    ) {
//        Text(text = "드론 코드 입력", style = MaterialTheme.typography.h5)
//
//        TextField(
//            value = code,
//            onValueChange = { code = it },
//            label = { Text("코드") },
//            singleLine = true,
//            modifier = Modifier.fillMaxWidth()
//        )
//
//        Spacer(modifier = Modifier.height(16.dp))
//
//        Button (
//            onClick = { onCodeSubmit(code) },
//            modifier = Modifier.fillMaxWidth()
//        ) {
//            Text("제출")
//        }
//        TextButton (
//            onClick = onCancel,
//            modifier = Modifier.align(Alignment.End)
//        ) {
//            Text("취소")
//        }
//    }
//}
