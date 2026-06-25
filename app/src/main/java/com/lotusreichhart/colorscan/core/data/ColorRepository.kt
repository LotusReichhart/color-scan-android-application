package com.lotusreichhart.colorscan.core.data

import android.annotation.SuppressLint
import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.lotusreichhart.colorscan.core.model.ColorItem
import timber.log.Timber
import kotlin.math.pow
import kotlin.math.sqrt

class ColorRepository private constructor(private val context: Context) {

    private var colorList: List<ColorItem> = emptyList()

    init {
        loadColorsFromAssets()
    }

    private fun loadColorsFromAssets() {
        try {
            val jsonString = context.assets.open("colors.json").bufferedReader().use { it.readText() }
            val listType = object : TypeToken<List<ColorItem>>() {}.type
            colorList = Gson().fromJson(jsonString, listType)
            Timber.d("Successfully loaded ${colorList.size} colors from assets.")
        } catch (e: Exception) {
            Timber.e(e, "Failed to load colors from assets.")
        }
    }

    fun findClosestColor(targetHex: String): ColorItem? {
        if (colorList.isEmpty()) return null

        val targetRgb = hexToRgb(targetHex) ?: return null
        var minDistance = Double.MAX_VALUE
        var closestColor: ColorItem? = null

        for (color in colorList) {
            val currentRgb = hexToRgb(color.hex) ?: continue
            val distance = sqrt(
                (targetRgb.first - currentRgb.first).toDouble().pow(2.0) +
                (targetRgb.second - currentRgb.second).toDouble().pow(2.0) +
                (targetRgb.third - currentRgb.third).toDouble().pow(2.0)
            )
            if (distance < minDistance) {
                minDistance = distance
                closestColor = color
            }
        }
        return closestColor
    }

    private fun hexToRgb(hex: String): Triple<Int, Int, Int>? {
        val cleanHex = hex.replace("#", "")
        if (cleanHex.length != 6) return null
        val r = cleanHex.substring(0, 2).toInt(16)
        val g = cleanHex.substring(2, 4).toInt(16)
        val b = cleanHex.substring(4, 6).toInt(16)
        return Triple(r, g, b)
    }

    fun searchByName(query: String): List<ColorItem> {
        if (colorList.isEmpty()) return emptyList()
        val cleanQuery = query.lowercase().trim()
        val matches = colorList.filter { it.name.lowercase().contains(cleanQuery) }
        return matches.sortedWith(compareBy(
            { !it.name.lowercase().equals(cleanQuery) },
            { !it.name.lowercase().startsWith(cleanQuery) },
            { it.name.length }
        ))
    }

    fun findClosestColors(targetColorInt: Int, limit: Int = 10): List<ColorItem> {
        if (colorList.isEmpty()) return emptyList()

        val targetR = android.graphics.Color.red(targetColorInt)
        val targetG = android.graphics.Color.green(targetColorInt)
        val targetB = android.graphics.Color.blue(targetColorInt)

        return colorList.map { color ->
            val currentRgb = hexToRgb(color.hex)
            val distance = if (currentRgb != null) {
                sqrt(
                    (targetR - currentRgb.first).toDouble().pow(2.0) +
                    (targetG - currentRgb.second).toDouble().pow(2.0) +
                    (targetB - currentRgb.third).toDouble().pow(2.0)
                )
            } else {
                Double.MAX_VALUE
            }
            Pair(color, distance)
        }.sortedBy { it.second }
            .map { it.first }
            .take(limit)
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var INSTANCE: ColorRepository? = null

        fun getInstance(context: Context): ColorRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ColorRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
