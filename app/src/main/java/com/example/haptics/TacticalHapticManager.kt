package com.example.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View

/**
 * Tactical Hardware Haptic Feedback Controller for SiteView Pro.
 * Provides distinct tactile vibration waveforms for crosshair structural alignment,
 * job mode changes, pin acquisitions, and laser lock.
 */
class TacticalHapticManager(private val context: Context) {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private var lastAlignmentVibrationTimestamp: Long = 0L

    /**
     * Triggered when crosshairs align with a detected structural edge, datum line, or plumb level.
     * Produces a sharp, crisp tactile tick. Throttled to prevent vibration fatigue during continuous panning.
     */
    fun triggerEdgeAlignmentHaptic(view: View? = null) {
        val now = System.currentTimeMillis()
        if (now - lastAlignmentVibrationTimestamp < 250L) return // 250ms debounce
        lastAlignmentVibrationTimestamp = now

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && vibrator?.hasVibrator() == true) {
            try {
                vibrator.vibrate(
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                )
                return
            } catch (_: Exception) {
                // Fallback to one-shot
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && vibrator?.hasVibrator() == true) {
            try {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(18L, VibrationEffect.DEFAULT_AMPLITUDE)
                )
                return
            } catch (_: Exception) {
                // Ignore
            }
        }

        // View-based fallback
        view?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    /**
     * Triggered when toggling between Job Modes (Painting, Plastering, Screeding).
     * Produces a distinct double-pulse mechanical notch confirmation.
     */
    fun triggerJobModeSwitchHaptic(view: View? = null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && vibrator?.hasVibrator() == true) {
            try {
                vibrator.vibrate(
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
                )
                return
            } catch (_: Exception) {
                // Fallback to waveform
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && vibrator?.hasVibrator() == true) {
            try {
                // Double pulse waveform: [delay, pulse1, rest, pulse2]
                val timings = longArrayOf(0, 25, 40, 35)
                val amplitudes = intArrayOf(0, 180, 0, 255)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                return
            } catch (_: Exception) {
                // Ignore
            }
        }

        // Fallback
        @Suppress("DEPRECATION")
        vibrator?.vibrate(40L)
        view?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }

    /**
     * Triggered on placing or snapping a target pin in survey mode.
     */
    fun triggerPinAcquiredHaptic(view: View? = null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && vibrator?.hasVibrator() == true) {
            try {
                vibrator.vibrate(
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                )
                return
            } catch (_: Exception) {
                // Fallback
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && vibrator?.hasVibrator() == true) {
            try {
                vibrator.vibrate(VibrationEffect.createOneShot(20L, 200))
                return
            } catch (_: Exception) {
                // Ignore
            }
        }

        view?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    /**
     * Triggered when Laser Rangefinder acquires/locks on target.
     */
    fun triggerLaserLockHaptic(view: View? = null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && vibrator?.hasVibrator() == true) {
            try {
                val timings = longArrayOf(0, 30, 30, 30)
                val amplitudes = intArrayOf(0, 160, 0, 220)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                return
            } catch (_: Exception) {
                // Ignore
            }
        }
        view?.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }
}
