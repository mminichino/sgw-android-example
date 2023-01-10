package com.example.sgwdemo.preferences

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.example.sgwdemo.R
import com.example.sgwdemo.login.LoginActivity


class PreferenceActivity : AppCompatActivity() {
    private var cntx: Context = this
    private val TAG = "PreferenceActivity"
    var pref: SharedPreferences? = null
    var serviceAddressInput: EditText? = null
    var gatewayAddressInput: EditText? = null
    var databaseNameInput: EditText? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        pref = applicationContext.getSharedPreferences("APP_SETTINGS", Context.MODE_PRIVATE)
        serviceAddressInput = findViewById(R.id.serviceAddress)
        gatewayAddressInput = findViewById(R.id.gatewayAddress)
        databaseNameInput = findViewById(R.id.databaseName)
        serviceAddressInput!!.setText(
            pref!!.getString(
                R.string.servicePropertyKey.toString(), ""))
        gatewayAddressInput!!.setText(
            pref!!.getString(
                R.string.gatewayPropertyKey.toString(), ""))
        databaseNameInput!!.setText(
            pref!!.getString(
                R.string.databaseNameKey.toString(), ""))
    }

    fun onSaveTapped(view: View?) {
        pref!!.edit()
            .putString(R.string.servicePropertyKey.toString(),
                serviceAddressInput!!.text.toString()).apply()
        pref!!.edit()
            .putString(R.string.gatewayPropertyKey.toString(),
                gatewayAddressInput!!.text.toString()).apply()
        pref!!.edit()
            .putString(R.string.databaseNameKey.toString(),
                databaseNameInput!!.text.toString()).apply()
        val intent = Intent(cntx, LoginActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
    }

    fun onClearTapped(view: View?) {
        try {
            if (Build.VERSION_CODES.KITKAT <= Build.VERSION.SDK_INT) {
                (getSystemService(ACTIVITY_SERVICE) as ActivityManager).clearApplicationUserData()
            } else {
                val packageName = applicationContext.packageName
                val runtime = Runtime.getRuntime()
                runtime.exec("pm clear $packageName")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
