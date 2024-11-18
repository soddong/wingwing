package com.ssafy.shieldroneapp.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Text
import com.ssafy.shieldroneapp.core.ui.theme.Blue600
import com.ssafy.shieldroneapp.core.ui.theme.Gray200
import com.ssafy.shieldroneapp.core.ui.theme.Gray300
import com.ssafy.shieldroneapp.core.ui.theme.Gray400
import com.ssafy.shieldroneapp.core.ui.theme.Gray50
import com.ssafy.shieldroneapp.core.ui.theme.Gray700

@Composable
fun WearableButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isPrimary: Boolean = true,
    buttonPadding: PaddingValues = PaddingValues(horizontal = 24.dp, vertical = 4.dp)
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 28.dp)
            .padding(buttonPadding),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = when {
                !enabled -> Gray400
                isPrimary -> Blue600
                else -> Gray700
            },
            contentColor = when {
                !enabled -> Gray200
                else -> Gray50
            },
            disabledBackgroundColor = Gray300,
            disabledContentColor = Gray400
        ),
        shape = CircleShape,
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                textAlign = TextAlign.Center,
                color = if (enabled) Gray50 else Gray300
            )
        }
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    WearableButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        isPrimary = true
    )
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    WearableButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        isPrimary = false
    )
}