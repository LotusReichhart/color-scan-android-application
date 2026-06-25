package com.lotusreichhart.colorscan.core.util

import android.graphics.Color
import androidx.camera.core.ImageProxy
import androidx.core.graphics.get

object ColorConverter {

    fun getCenterPixelColor(image: ImageProxy): Int {
        val bitmap = image.toBitmap()
        val centerX = bitmap.width / 2
        val centerY = bitmap.height / 2
        val color = bitmap[centerX, centerY]
        bitmap.recycle()
        return color
    }

    fun intToHex(color: Int): String {
        return String.format("#%06X", 0xFFFFFF and color)
    }

    fun intToRgbString(color: Int): String {
        return "${Color.red(color)}, ${Color.green(color)}, ${Color.blue(color)}"
    }
}
