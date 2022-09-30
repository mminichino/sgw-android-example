package com.example.sgwdemo.login

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.example.sgwdemo.R
import com.example.sgwdemo.main.MainActivity
import com.example.sgwdemo.cbdb.CouchbaseConnect


class LoginActivity : AppCompatActivity() {

    var usernameInput: EditText? = null
    var passwordInput: EditText? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        usernameInput = findViewById(R.id.usernameInput)
        passwordInput = findViewById(R.id.passwordInput)
    }

    fun onLoginTapped(view: View?) {
        if (usernameInput!!.length() > 0 && passwordInput!!.length() > 0) {
            val db: CouchbaseConnect = CouchbaseConnect(this).getSharedInstance()
            val username = usernameInput!!.text.toString()
            val password = passwordInput!!.text.toString()
            val cntx: Context = getApplicationContext()
            db.init()
            db.openDatabase()
//            DatabaseManager.startPushAndPullReplicationForCurrentUser(username, password)
            val intent = Intent(cntx, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
        }
    }
}
