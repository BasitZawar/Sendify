package com.smartswitch.subscriptions

import android.content.Context
import android.content.SharedPreferences

class SharedPreferencesClass(var mConext: Context) {

    val IS_SUBSCRIBED: String = "is_remove_ad_subscribed"
    val IS_AGREED_PRIVACY: String = "is_agreed_privacy"
    val TOKEN_SUBSCRIPTION: String = "token_subs"
    val REMOVE_AD_ACTIVITY_OPEN: String = "removead_activity_open"

    private var mSharedPreferences: SharedPreferences? = null

    init {
        mSharedPreferences = mConext.getSharedPreferences("PhonecloneApp", 0)
    }

    fun isSubscribed(): Boolean {
        var isSubscribed = mSharedPreferences?.getBoolean(IS_SUBSCRIBED, false) == true

        return isSubscribed
    }

    fun setBooleanPreferences(key: String, value: Boolean) {
        var editor = mSharedPreferences?.edit()
        editor?.putBoolean(key, value)
        editor?.apply()
    }

    fun getBooleanPreferences(key: String): Boolean {
        var value = mSharedPreferences?.getBoolean(key, false) == true
        return value
    }

    fun setStringPreferences(key: String, value: String) {
        var editor = mSharedPreferences?.edit()
        editor?.putString(key, value)
        editor?.apply()
    }

    fun getStringPreferences(key: String): String? {
        var value = mSharedPreferences?.getString(key, "NoValueAvailable")
        return value
    }

}