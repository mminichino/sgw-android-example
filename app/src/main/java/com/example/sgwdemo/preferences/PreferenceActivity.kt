package com.example.sgwdemo.preferences

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import com.example.sgwdemo.R
import com.example.sgwdemo.login.LoginActivity


class PreferenceActivity : AppCompatActivity() {
    private var cntx: Context = this
    private val TAG = "PreferenceActivity"
    var demoList: List<String>? = null
    var tagList: List<String>? = null
    var portList: List<String>? = null
    var pref: SharedPreferences? = null
    var serviceAddressInput: EditText? = null
    var gatewayAddressInput: EditText? = null
    var servicePortInput: EditText? = null
    var databaseNameInput: EditText? = null
    var activeDemoInput: String? = null
    var groupTagFieldInput: String? = null
    var spinner: Spinner? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        pref = applicationContext.getSharedPreferences("APP_SETTINGS", Context.MODE_PRIVATE)
        val demoListString = pref!!.getString(R.string.demoListKey.toString(), "")
        val tagListString = pref!!.getString(R.string.tagListKey.toString(), "")
        val portListString = pref!!.getString(R.string.servicePortList.toString(), "")
        serviceAddressInput = findViewById(R.id.serviceAddress)
        gatewayAddressInput = findViewById(R.id.gatewayAddress)
        servicePortInput = findViewById(R.id.servicePort)
        databaseNameInput = findViewById(R.id.databaseName)
        serviceAddressInput!!.setText(
            pref!!.getString(
                R.string.servicePropertyKey.toString(), ""))
        servicePortInput!!.setText(
            pref!!.getString(
                R.string.servicePort.toString(), ""))
        gatewayAddressInput!!.setText(
            pref!!.getString(
                R.string.gatewayPropertyKey.toString(), ""))
        databaseNameInput!!.setText(
            pref!!.getString(
                R.string.databaseNameKey.toString(), ""))
        demoList = demoListString!!.split(",").map { it.trim() }
        tagList = tagListString!!.split(",").map { it.trim() }
        portList = portListString!!.split(",").map { it.trim() }
        val spinnerChoice = pref!!.getInt(R.string.demoListChoice.toString(), 0)
        Log.i(TAG, "Auth IP    : ${serviceAddressInput!!.text}")
        Log.i(TAG, "Auth Port  : ${servicePortInput!!.text}")
        Log.i(TAG, "Gateway IP : ${gatewayAddressInput!!.text}")
        Log.i(TAG, "Database   : ${databaseNameInput!!.text}")
        Log.i(TAG, "Choice     : $spinnerChoice")

        spinner = findViewById(R.id.demoSelect)
        if (spinner != null) {
            val adapter = ArrayAdapter(
                this,
                R.layout.demo_spinner, demoList!!
            )
            spinner!!.adapter = adapter
            spinner!!.setSelection(spinnerChoice)

            spinner!!.onItemSelectedListener = object :
                AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>, view: View, position: Int, id: Long) {
                    Log.i(TAG, "Demo List: $demoListString")
                    Log.i(TAG, "Tag List : $tagListString")
                    activeDemoInput = demoList!![position]
                    groupTagFieldInput = tagList!![position]
                    servicePortInput!!.setText(portList!![position])
                    databaseNameInput!!.setText(activeDemoInput!!.toString())
                    pref!!.edit().putInt(R.string.demoListChoice.toString(), position).apply()
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    activeDemoInput = pref!!.getString(
                        R.string.activeDemoKey.toString(), "")
                }
            }
        }
    }

    fun onSaveTapped(view: View?) {
        pref!!.edit()
            .putString(R.string.servicePropertyKey.toString(),
                serviceAddressInput!!.text.toString()).apply()
        pref!!.edit()
            .putString(R.string.servicePort.toString(),
                servicePortInput!!.text.toString()).apply()
        pref!!.edit()
            .putString(R.string.gatewayPropertyKey.toString(),
                gatewayAddressInput!!.text.toString()).apply()
        pref!!.edit()
            .putString(R.string.databaseNameKey.toString(),
                databaseNameInput!!.text.toString()).apply()
        pref!!.edit()
            .putString(R.string.activeDemoKey.toString(),
                activeDemoInput!!.toString()).apply()
        pref!!.edit()
            .putString(R.string.groupTagFieldKey.toString(),
                groupTagFieldInput!!.toString()).apply()

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
