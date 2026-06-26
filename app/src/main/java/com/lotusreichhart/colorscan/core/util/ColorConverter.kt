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

    fun parseHex(hex: String): Int? {
        val cleanHex = hex.trim().replace("#", "")
        return try {
            when (cleanHex.length) {
                3 -> {
                    val r = cleanHex.substring(0, 1).repeat(2)
                    val g = cleanHex.substring(1, 2).repeat(2)
                    val b = cleanHex.substring(2, 3).repeat(2)
                    Color.rgb(r.toInt(16), g.toInt(16), b.toInt(16))
                }
                6 -> {
                    Color.rgb(
                        cleanHex.substring(0, 2).toInt(16),
                        cleanHex.substring(2, 4).toInt(16),
                        cleanHex.substring(4, 6).toInt(16)
                    )
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun parseRgb(rgb: String): Int? {
        return try {
            val parts = rgb.split(",").map { it.trim().toInt() }
            if (parts.size == 3 && parts.all { it in 0..255 }) {
                Color.rgb(parts[0], parts[1], parts[2])
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun getComplementaryColor(color: Int): Int {
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        return Color.rgb(255 - r, 255 - g, 255 - b)
    }

    fun getTriadicColors(color: Int): Pair<Int, Int> {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        
        val hsv1 = floatArrayOf((hsv[0] + 120f) % 360f, hsv[1], hsv[2])
        val hsv2 = floatArrayOf((hsv[0] + 240f) % 360f, hsv[1], hsv[2])
        
        return Pair(Color.HSVToColor(hsv1), Color.HSVToColor(hsv2))
    }

    fun isColorLight(color: Int): Boolean {
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        val luminance = 0.299 * r + 0.587 * g + 0.114 * b
        return luminance > 160
    }
}
