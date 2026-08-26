package com.example.ui.hud

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.Foundation
import androidx.compose.material.icons.filled.SquareFoot
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.JobMode

@Composable
fun JobModeSelectorBar(
    activeMode: JobMode,
    onSelectMode: (JobMode) -> Unit,
    onOpenSpecEditor: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val modes = JobMode.values()
    val primaryColor by animateColorAsState(
        targetValue = activeMode.primaryColor,
        animationSpec = tween(400),
        label = "primaryColor"
    )

    Surface(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .shadow(12.dp, RoundedCornerShape(14.dp), ambientColor = primaryColor, spotColor = primaryColor)
            .testTag("job_mode_selector_bar"),
        color = Color(0xFF0A0C0B).copy(alpha = 0.95f),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, primaryColor.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .padding(6.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Mode Segmented Buttons
            Row(
                modifier = Modifier
                    .weight(1f)
                    .background(Color(0xFF1A1C1B), RoundedCornerShape(10.dp))
                    .border(1.dp, Color(0xFF262C2A), RoundedCornerShape(10.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                modes.forEach { mode ->
                    val isSelected = mode == activeMode
                    val modeColor = mode.primaryColor
                    val icon: ImageVector = when (mode) {
                        JobMode.PAINTING -> Icons.Default.FormatPaint
                        JobMode.PLASTERING -> Icons.Default.Foundation
                        JobMode.SCREEDING -> Icons.Default.SquareFoot
                    }

                    val buttonBg by animateColorAsState(
                        targetValue = if (isSelected) modeColor else Color.Transparent,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "buttonBg"
                    )

                    val contentColor by animateColorAsState(
                        targetValue = if (isSelected) Color(0xFF0A0C0B) else Color(0xFF94A3B8),
                        animationSpec = tween(200),
                        label = "contentColor"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(buttonBg)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSelectMode(mode)
                            }
                            .testTag("mode_button_${mode.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = mode.title,
                                tint = contentColor,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = mode.title,
                                color = contentColor,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Parameter Configuration Button
            IconButton(
                onClick = onOpenSpecEditor,
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1A1C1B))
                    .border(1.dp, primaryColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                    .testTag("open_job_spec_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Adjust Specifications",
                    tint = primaryColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
