package com.lotusreichhart.colorscan.core.model

import com.google.gson.annotations.SerializedName

data class ColorItem(
    @SerializedName("hex") val hex: String,
    @SerializedName("name") val name: String,
    @SerializedName("rgb") val rgb: String
)
