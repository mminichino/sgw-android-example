package com.example.sgwdemo.login

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.sgwdemo.R
import com.example.sgwdemo.main.MainActivity
import com.example.sgwdemo.cbdb.CouchbaseConnect
import java.security.MessageDigest
import java.util.*


class LoginActivity : AppCompatActivity() {

    private var TAG = "LoginActivity"
    var usernameInput: EditText? = null
    var passwordInput: EditText? = null
    var storeIdInput: EditText? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        usernameInput = findViewById(R.id.usernameInput)
        passwordInput = findViewById(R.id.passwordInput)
        storeIdInput = findViewById(R.id.storeIdInput)
    }

    fun onLoginTapped(view: View?) {
        val cntx: Context = getApplicationContext()
        val db: CouchbaseConnect = CouchbaseConnect.getInstance(this)
        val storeId = storeIdInput!!.text.toString()
        val username = usernameInput!!.text.toString()
        val password = passwordInput!!.text.toString()
        val passwordHashed = hashPassword(password)

        if (usernameInput!!.length() == 0 || passwordInput!!.length() == 0 || storeId.isEmpty()) {
            showMessageDialog("Missing Information",
                "Please provide your login information")
            return
        }

        setupDb(storeId)
        val employeePassword = db.getEmployeePassword(username)

        Log.i(TAG, "Login employee ID -> $username")

        if (employeePassword != passwordHashed) {
            showMessageDialog("Incorrect Password",
                "The password you entered is incorrect")
            return
        }

        val intent = Intent(cntx, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        intent.putExtra("StoreID",storeId)
        intent.putExtra("UserName",username)
        startActivity(intent)
    }

    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-1")
        val result = digest.digest(password.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(result)
    }

    private fun showMessageDialog(title: String, message: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setPositiveButton("Ok") { dialog, which ->
            Toast.makeText(
                applicationContext,
                "Ok", Toast.LENGTH_SHORT
            ).show()
        }
        builder.show()
    }

    fun setupDb(storeId: String) {
        val db: CouchbaseConnect = CouchbaseConnect.getInstance(this)

        if (!db.isDbOpen()) {
            val userStringBuilder = StringBuilder()
            userStringBuilder.append("store_id@")
            userStringBuilder.append(storeId)
            val dbUser = userStringBuilder.toString()

            db.init()
            db.openDatabase(dbUser)
            db.syncDatabase(dbUser)
        }
    }
}
