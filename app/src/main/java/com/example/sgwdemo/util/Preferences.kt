package com.example.sgwdemo.util

import android.content.Context
import android.content.SharedPreferences
import com.example.sgwdemo.R

object AppPreferences {
    fun setSharedPreferenceData(context: Context) {
        val defaultHost = Util.getProperty("sgwhost", context)
        val defaultEndpoint = Util.getProperty("authEndpoint", context)
        val defaultDatabase = Util.getProperty("database", context)
        val demoList = Util.getProperty("demoList", context)
        val tagList = Util.getProperty("tagList", context)
        val activeDemo = Util.getProperty("activeDemo", context)
        val groupTagField = Util.getProperty("groupTagField", context)
        val pref: SharedPreferences =
            context.getSharedPreferences("APP_SETTINGS", Context.MODE_PRIVATE)

        if (!pref.contains(R.string.servicePropertyKey.toString())) {
            pref.edit().putString(R.string.servicePropertyKey.toString(), defaultEndpoint).apply()
        }

        if (!pref.contains(R.string.gatewayPropertyKey.toString())) {
            pref.edit().putString(R.string.gatewayPropertyKey.toString(), defaultHost).apply()
        }

        if (!pref.contains(R.string.databaseNameKey.toString())) {
            pref.edit().putString(R.string.databaseNameKey.toString(), defaultDatabase).apply()
        }

        if (!pref.contains(R.string.demoListKey.toString())) {
            pref.edit().putString(R.string.demoListKey.toString(), demoList).apply()
        }

        if (!pref.contains(R.string.tagListKey.toString())) {
            pref.edit().putString(R.string.tagListKey.toString(), tagList).apply()
        }

        if (!pref.contains(R.string.activeDemoKey.toString())) {
            pref.edit().putString(R.string.activeDemoKey.toString(), activeDemo).apply()
        }

        if (!pref.contains(R.string.groupTagFieldKey.toString())) {
            pref.edit().putString(R.string.groupTagFieldKey.toString(), groupTagField).apply()
        }

        if (!pref.contains(R.string.demoListChoice.toString())) {
            pref.edit().putInt(R.string.demoListChoice.toString(), 0).apply()
        }
    }
}
