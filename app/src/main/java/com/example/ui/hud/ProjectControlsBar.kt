package com.example.ui.hud

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import com.example.model.ProcessingState

/**
 * Floating Tactical Project Control Action Bar for SiteView Pro HUD.
 * Houses [NEW], [RESTART], [CLEAR], [SAVE], and [AUTO] One-Touch Analysis.
 */
@Composable
fun ProjectControlsBar(
    processingState: ProcessingState,
    overlaysVisible: Boolean,
    themeColor: Color,
    onNewProject: () -> Unit,
    onRestartCapture: () -> Unit,
    onToggleClearScreen: () -> Unit,
    onSaveWorkspace: () -> Unit,
    onAutoAnalyze: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val isAutoActive = processingState == ProcessingState.AUTO_ANALYZING
    val isSaving = processingState == ProcessingState.SAVING

    val infiniteTransition = rememberInfiniteTransition(label = "auto_pulse")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_anim"
    )

    Box(
        modifier = modifier
            .shadow(12.dp, RoundedCornerShape(14.dp), ambientColor = Color.Black, spotColor = themeColor)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF0A0C0B).copy(alpha = 0.94f))
            .border(BorderStroke(1.dp, themeColor.copy(alpha = 0.45f)), RoundedCornerShape(14.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag("project_controls_bar")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 1. [NEW PROJECT] Button
            TacticalControlButton(
                label = "NEW",
                icon = Icons.Default.CreateNewFolder,
                tint = Color.White.copy(alpha = 0.85f),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onNewProject()
                },
                testTag = "btn_new_project"
            )

            // 2. [RESTART CAPTURE] Button
            TacticalControlButton(
                label = "RESTART",
                icon = Icons.Default.Refresh,
                tint = Color(0xFFFFB800), // Industrial Amber
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onRestartCapture()
                },
                testTag = "btn_restart_capture"
            )

            // 3. [CLEAR SCREEN] Button
            TacticalControlButton(
                label = if (overlaysVisible) "CLEAR" else "SHOW",
                icon = if (overlaysVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                tint = if (overlaysVisible) Color.White.copy(alpha = 0.65f) else themeColor,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onToggleClearScreen()
                },
                testTag = "btn_clear_screen"
            )

            // 4. [SAVE WORKSPACE] Button
            TacticalControlButton(
                label = if (isSaving) "SAVING" else "SAVE",
                icon = if (isSaving) Icons.Default.HourglassTop else Icons.Default.Save,
                tint = Color(0xFF00E5FF), // Precision Cyan
                isLoading = isSaving,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSaveWorkspace()
                },
                testTag = "btn_save_workspace"
            )

            // Vertical Tactical Divider
            Box(
                modifier = Modifier
                    .height(28.dp)
                    .width(1.dp)
                    .background(Color(0xFF262C2A))
            )

            // 5. [AUTO - ONE-TOUCH ANALYSIS] Trigger
            val autoBgBrush = if (isAutoActive) {
                Brush.linearGradient(
                    listOf(
                        themeColor.copy(alpha = pulseGlow * 0.35f),
                        themeColor.copy(alpha = pulseGlow * 0.35f)
                    )
                )
            } else {
                Brush.horizontalGradient(
                    listOf(
                        themeColor,
                        themeColor.copy(alpha = 0.85f)
                    )
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(autoBgBrush)
                    .border(
                        BorderStroke(
                            1.5.dp,
                            if (isAutoActive) themeColor.copy(alpha = pulseGlow) else Color.White.copy(alpha = 0.3f)
                        ),
                        RoundedCornerShape(8.dp)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (!isAutoActive) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onAutoAnalyze()
                        }
                    }
                    .padding(horizontal = 12.dp, vertical = 7.dp)
                    .testTag("btn_auto_analyze"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    if (isAutoActive) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = themeColor,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "One-Touch Auto Capture",
                            tint = Color(0xFF0A0C0B),
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    Text(
                        text = if (isAutoActive) "SCANNING..." else "AUTO",
                        color = if (isAutoActive) themeColor else Color(0xFF0A0C0B),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun TacticalControlButton(
    label: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
    testTag: String,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF161918))
            .border(BorderStroke(1.dp, Color(0xFF2A302E)), RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    color = tint,
                    strokeWidth = 1.5.dp
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = tint,
                    modifier = Modifier.size(13.dp)
                )
            }

            Text(
                text = label,
                color = tint,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
