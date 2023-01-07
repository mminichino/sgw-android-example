package com.example.sgwdemo.util

import android.content.Context
import android.content.SharedPreferences
import com.example.sgwdemo.R

object AppPreferences {
    fun setSharedPreferenceData(context: Context) {
        val defaultHost = Util.getProperty("sgwhost", context)
        val defaultEndpoint = Util.getProperty("authEndpoint", context)
        val pref: SharedPreferences =
            context.getSharedPreferences("APP_SETTINGS", Context.MODE_PRIVATE)

        if (!pref.contains(R.string.servicePropertyKey.toString())) {
            pref.edit().putString(R.string.servicePropertyKey.toString(), defaultEndpoint).apply()
        }

        if (!pref.contains(R.string.gatewayPropertyKey.toString())) {
            pref.edit().putString(R.string.gatewayPropertyKey.toString(), defaultHost).apply()
        }
    }
}
