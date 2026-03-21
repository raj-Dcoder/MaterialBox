package com.rajveer.materialbox.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object HapticUtils {
    fun playClick(context: Context) {
        // High quality predefined system click
        vibratePredefined(context, VibrationEffect.EFFECT_CLICK, 30)
    }

    fun playHeavyClick(context: Context) {
        // High quality predefined system heavy click
        vibratePredefined(context, VibrationEffect.EFFECT_HEAVY_CLICK, 50)
    }

    @Suppress("DEPRECATION")
    private fun vibratePredefined(context: Context, effectId: Int, fallbackDuration: Long) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (!vibrator.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(effectId))
        } else {
            vibrator.vibrate(fallbackDuration)
        }
    }
}
