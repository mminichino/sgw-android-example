package com.example.sgwdemo.preferences

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        pref = applicationContext.getSharedPreferences("APP_SETTINGS", Context.MODE_PRIVATE)
        serviceAddressInput = findViewById(R.id.serviceAddress)
        gatewayAddressInput = findViewById(R.id.gatewayAddress)
        serviceAddressInput!!.setText(
            pref!!.getString(
                R.string.servicePropertyKey.toString(), ""))
        gatewayAddressInput!!.setText(
            pref!!.getString(
                R.string.gatewayPropertyKey.toString(), ""))
    }

    fun onSaveTapped(view: View?) {
        pref!!.edit()
            .putString(R.string.servicePropertyKey.toString(),
                serviceAddressInput!!.text.toString()).apply()
        pref!!.edit()
            .putString(R.string.gatewayPropertyKey.toString(),
                gatewayAddressInput!!.text.toString()).apply()
        val intent = Intent(cntx, LoginActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
    }
}
