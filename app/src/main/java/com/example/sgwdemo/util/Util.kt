package com.example.sgwdemo.util

import android.content.Context
import android.content.res.AssetManager
import java.io.InputStream
import java.util.*


object Util {
    fun getProperty(key: String?, context: Context): String {
        val properties = Properties()
        val assetManager: AssetManager = context.assets
        val inputStream: InputStream = assetManager.open("config.properties")
        properties.load(inputStream)
        return properties.getProperty(key)
    }
}
