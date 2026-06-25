package com.lotusreichhart.colorscan.core.data

import android.content.Context
import android.content.SharedPreferences
import com.lotusreichhart.colorscan.core.model.UserPreferences
import com.lotusreichhart.colorscan.core.model.HistorySort
import androidx.core.content.edit
import timber.log.Timber

class UserPreferencesManager private constructor(context: Context) {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        "color_scan_prefs",
        Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_IS_FIRST_TIME = "key_is_first_time"
        private const val KEY_HISTORY_SORT = "key_history_sort"

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
        val sortName = sharedPreferences.getString(KEY_HISTORY_SORT, HistorySort.DESC.name)
            ?: HistorySort.DESC.name
        val sort = try {
            HistorySort.valueOf(sortName)
        } catch (e: Exception) {
            Timber.e(e)
            HistorySort.DESC
        }
        return UserPreferences(isFirst, sort)
    }

    fun setFirstTime(isFirst: Boolean) {
        sharedPreferences.edit { putBoolean(KEY_IS_FIRST_TIME, isFirst) }
    }

    fun setHistorySort(sort: HistorySort) {
        sharedPreferences.edit { putString(KEY_HISTORY_SORT, sort.name) }
    }
}
