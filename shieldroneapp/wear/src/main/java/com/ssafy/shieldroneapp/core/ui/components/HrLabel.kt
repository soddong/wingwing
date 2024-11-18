package com.ssafy.shieldroneapp.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.health.services.client.data.DataTypeAvailability
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.ssafy.shieldroneapp.R
import androidx.compose.animation.*
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun HrLabel(
    hr: Double,
    availability: DataTypeAvailability
) {
    val icon = when (availability) {
        DataTypeAvailability.AVAILABLE -> Icons.Default.Favorite
        DataTypeAvailability.ACQUIRING -> Icons.Default.MonitorHeart
        DataTypeAvailability.UNAVAILABLE,
        DataTypeAvailability.UNAVAILABLE_DEVICE_OFF_BODY -> Icons.Default.HeartBroken
        else -> Icons.Default.QuestionMark
    }

    val iconColor = when (availability) {
        DataTypeAvailability.AVAILABLE -> Color.Red
        DataTypeAvailability.ACQUIRING -> Color(0xFFFFA500)
        else -> Color.Gray
    }

    val text = if (availability == DataTypeAvailability.AVAILABLE) {
        hr.toInt().toString()
    } else {
        stringResource(id = R.string.no_hr_reading)
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = stringResource(R.string.icon),
            tint = iconColor,
            modifier = Modifier.size(32.dp)
        )

        AnimatedVisibility(
            visible = availability == DataTypeAvailability.AVAILABLE,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.display1,
                fontWeight = FontWeight.Bold,
            )
        }

        if (availability != DataTypeAvailability.AVAILABLE) {
            Text(
                text = text,
                style = MaterialTheme.typography.body2,
            )
        }
    }
}