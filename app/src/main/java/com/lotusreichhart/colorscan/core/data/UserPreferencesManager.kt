package com.lotusreichhart.colorscan.core.data

import android.content.Context
import android.content.SharedPreferences
import com.lotusreichhart.colorscan.core.model.UserPreferences
import androidx.core.content.edit

class UserPreferencesManager private constructor(context: Context) {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        "color_scan_prefs",
        Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_IS_FIRST_TIME = "key_is_first_time"

        @Volatile
        private var INSTANCE: UserPreferencesManager? = null

        fun getInstance(context: Context): UserPreferencesManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserPreferencesManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    fun getUserPreferences(): UserPreferences {
        val isFirst = sharedPreferences.getBoolean(KEY_IS_FIRST_TIME, true)
        return UserPreferences(isFirst)
    }

    fun setFirstTime(isFirst: Boolean) {
        sharedPreferences.edit { putBoolean(KEY_IS_FIRST_TIME, isFirst) }
    }
}
