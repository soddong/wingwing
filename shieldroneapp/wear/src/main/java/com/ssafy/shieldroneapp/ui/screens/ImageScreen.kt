package com.ssafy.shieldroneapp.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.wear.compose.material.MaterialTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun ImageScreen(
    jsonMessage: String?,
    onTimeout: () -> Unit
) {
    val scope = rememberCoroutineScope()

    val bitmap = remember(jsonMessage) {
        jsonMessage?.let { decodeFrame(it) }
    }
 
    LaunchedEffect(jsonMessage) {
        scope.launch {
            delay(7000)
            onTimeout()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background),
        contentAlignment = Alignment.Center
    ) {
        bitmap?.let { bmp ->
            Image(
                painter = BitmapPainter(bmp.asImageBitmap()),
                contentDescription = "위험 상황 이미지",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

private fun decodeFrame(jsonMessage: String): Bitmap? {
    return try {
        val jsonObject = JSONObject(jsonMessage)
        if (jsonObject.has("frame")) {
            val frameEncoded = jsonObject.getString("frame")
            val decodedBytes = Base64.decode(frameEncoded, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } else {
            null
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
