package com.applock.guard.ui.components

import android.graphics.drawable.Drawable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.applock.guard.ui.theme.*

@Composable
fun AppListItem(
    appName: String,
    packageName: String,
    appIcon: Drawable?,
    isLocked: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        targetValue = if (isLocked) AccentBlue.copy(alpha = 0.08f) else Color.Transparent,
        animationSpec = tween(300),
        label = "bg_color"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isLocked) AccentBlue.copy(alpha = 0.3f) else TextMuted.copy(alpha = 0.1f),
        animationSpec = tween(300),
        label = "border_color"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App icon
        if (appIcon != null) {
            Image(
                painter = rememberDrawablePainter(drawable = appIcon),
                contentDescription = appName,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(SurfaceLight),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = appName.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // App name & package
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = appName,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Text(
                text = packageName,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Lock toggle
        Switch(
            checked = isLocked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = ToggleThumb,
                checkedTrackColor = ToggleOn,
                uncheckedThumbColor = ToggleThumb.copy(alpha = 0.7f),
                uncheckedTrackColor = ToggleOff
            )
        )
    }
}
