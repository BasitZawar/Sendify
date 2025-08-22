package com.smartswitch.utils

import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesHelper @Inject constructor(val preferences: SharedPreferences) {

    // Generic function to save any type of value in SharedPreferences
    fun <T> set(key: String, value: T) {
        with(preferences.edit()) {
            when (value) {
                is String -> putString(key, value)
                is Int -> putInt(key, value)
                is Boolean -> putBoolean(key, value)
                is Float -> putFloat(key, value)
                is Long -> putLong(key, value)
                else -> throw IllegalArgumentException("This type can't be saved into Preferences")
            }.apply()
        }
    }

    // Generic function to retrieve any type of value from SharedPreferences
     inline fun <reified T> get(key: String, defaultValue: T): T {
        return with(preferences) {
            when (T::class) {
                String::class -> getString(key, defaultValue as String) as T
                Int::class -> getInt(key, defaultValue as Int) as T
                Boolean::class -> getBoolean(key, defaultValue as Boolean) as T
                Float::class -> getFloat(key, defaultValue as Float) as T
                Long::class -> getLong(key, defaultValue as Long) as T
                else -> throw IllegalArgumentException("This type can't be retrieved from Preferences")
            }
        }
    }

    // Function to remove a specific key from SharedPreferences
    fun remove(key: String) {
        with(preferences.edit()) {
            remove(key).apply()
        }
    }

    // Function to clear all data from SharedPreferences
    fun clear() {
        with(preferences.edit()) {
            clear().apply()
        }
    }
}