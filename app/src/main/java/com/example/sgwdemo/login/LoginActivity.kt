package com.example.sgwdemo.login

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.sgwdemo.R
import com.example.sgwdemo.cbdb.CouchbaseConnect
import com.example.sgwdemo.main.MainActivity
import com.example.sgwdemo.util.Util
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.security.MessageDigest
import java.util.*


class LoginActivity : AppCompatActivity() {

    private val TAG = "LoginActivity"
    var usernameInput: EditText? = null
    var passwordInput: EditText? = null
    var storeIdInput: EditText? = null
    var progress: CircularProgressIndicator? = null
    var authEndpoint: String? = null
    var authUrl: String? = null
    var gson: Gson? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        usernameInput = findViewById(R.id.usernameInput)
        passwordInput = findViewById(R.id.passwordInput)
        storeIdInput = findViewById(R.id.storeIdInput)
        progress = findViewById(R.id.progressBarLoginWait)
        authEndpoint = Util.getProperty("authEndpoint", applicationContext)
        gson = GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
            .create()
        authUrl = "http://$authEndpoint:8080"
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_settings,menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId){
            R.id.settings -> Toast.makeText(this,"Settings Selected",Toast.LENGTH_SHORT).show()
        }
        return super.onOptionsItemSelected(item)
    }

    fun onLoginTapped(view: View?) {
        val cntx: Context = getApplicationContext()
        val storeId = storeIdInput!!.text.toString()
        val username = usernameInput!!.text.toString()
        val password = passwordInput!!.text.toString()

        if (usernameInput!!.length() == 0 || passwordInput!!.length() == 0 || storeId.isEmpty()) {
            showMessageDialog("Missing Information",
                "Please provide your login information")
            return
        }

        progress!!.visibility = View.VISIBLE

        val sgwLogin = "store_id@$storeId"
        val credentials = "$username:$password"
        val authHeaderValue = "Basic " + Base64
            .getEncoder()
            .encodeToString(credentials.toByteArray(Charsets.UTF_8))

        val interceptor = Interceptor { chain ->
            val newRequest: Request =
                chain.request()
                    .newBuilder()
                    .addHeader("Authorization", authHeaderValue).build()
            chain.proceed(newRequest)
        }

        val builder = OkHttpClient.Builder()
        builder.interceptors().add(interceptor)
        val client = builder.build()

        Log.d(TAG, "Auth URL: $authUrl")

        val service = Retrofit.Builder()
            .baseUrl(authUrl!!)
            .addConverterFactory(GsonConverterFactory.create(gson!!))
            .client(client)
            .build()
            .create(SessionService::class.java)

        val call: Call<SessionResponse> = service.getSession(sgwLogin)
        call.enqueue(object : Callback<SessionResponse> {

            override fun onFailure(call: Call<SessionResponse>, t: Throwable) {
                Log.d(TAG, "Auth API called failed")
                t.printStackTrace()
                showMessageDialog("Error",
                    "Authorization Service Unavailable")
            }

            override fun onResponse(call: Call<SessionResponse>, response: Response<SessionResponse>) {
                if (response.isSuccessful) {
                    Log.d(TAG, "Session " + response.body()!!.session_id)
                    Log.d(TAG, "Cookie " + response.body()!!.cookie_name)

                    setupDb(storeId, response.body()!!.cookie_name, response.body()!!.session_id)

                    val intent = Intent(cntx, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                            or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    intent.putExtra("StoreID",storeId)
                    intent.putExtra("UserName",username)
                    startActivity(intent)
                } else {
                    showMessageDialog("Unauthorized",
                        "Login was not successful")
                }
                progress!!.visibility = View.INVISIBLE
            }
        })
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

    fun setupDb(storeId: String, sessionCookie: String, sessionId: String) {
        val db: CouchbaseConnect = CouchbaseConnect.getInstance(this)

        if (!db.isDbOpen()) {
            val userStringBuilder = StringBuilder()
            userStringBuilder.append("store_id@")
            userStringBuilder.append(storeId)
            val dbUser = userStringBuilder.toString()

            db.init()
            db.openDatabase(dbUser)
            db.syncDatabase(sessionId, sessionCookie)
        }
    }
}

data class SessionResponse(
    val cookie_name: String,
    val expires: String,
    val session_id: String
    )

interface SessionService {
    @GET("/api/v1/auth/name/{username}")
    fun getSession(@Path("username") username: String): Call<SessionResponse>
}
