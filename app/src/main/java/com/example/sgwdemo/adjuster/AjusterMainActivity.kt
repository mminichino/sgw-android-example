package com.example.sgwdemo.adjuster

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.sgwdemo.R
import com.example.sgwdemo.cbdb.CouchbaseConnect
import com.example.sgwdemo.login.LoginActivity
import com.example.sgwdemo.models.Adjuster
import com.example.sgwdemo.models.ClaimGrid
import com.google.android.material.progressindicator.CircularProgressIndicator
import kotlinx.coroutines.*
import java.lang.Runnable
import java.util.Locale


class AdjusterMainActivity : AppCompatActivity() {

    private var TAG = "AdjusterMain"
    private var cntx: Context = this
    var logoutButton: Button? = null
    var listView: ListView? = null
    var documentCount: TextView? = null
    var adjusterView: TextView? = null
    var regionView: TextView? = null
    var userIdValue: String? = null
    var regionValue: String? = null
    var adjuster = Adjuster()
    var progress: CircularProgressIndicator? = null
    var handler: Handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_adjuster_main)
        val db: CouchbaseConnect = CouchbaseConnect.getInstance(cntx)
        val scope = CoroutineScope(Dispatchers.Default)

        userIdValue = intent.getStringExtra("UserName")
        regionValue = intent.getStringExtra("Region")
        logoutButton = findViewById(R.id.logoutButton)
        listView = findViewById(R.id.listView)
        documentCount = findViewById(R.id.openClaimCount)
        progress = findViewById(R.id.progressBarLoadWait)
        adjusterView = findViewById(R.id.adjusterNameHeader)
        regionView = findViewById(R.id.adjusterRegionHeader)

        scope.launch {
            adjuster = db.getAdjusterByUserName(userIdValue!!)
            adjusterView?.text = String.format("Adjuster: %s %s", adjuster.firstName, adjuster.lastName)
        }

        regionView?.text = String.format("Region: %s",
            regionValue?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() })

        createClaimList()
        handler.post(runnableCode)
        Log.i(TAG, "Adjuster Activity Started")
    }

    private fun createClaimList() {
        val db: CouchbaseConnect = CouchbaseConnect.getInstance(cntx)
        val scope = CoroutineScope(Dispatchers.Default)
        val claimAdapter = ClaimAdapter()
        var claims: ArrayList<ClaimGrid>

        scope.launch {
            claims = db.queryClaims()
            withContext(Dispatchers.Main) {
                claimAdapter.claimListAdapter(cntx, claims)
                listView!!.adapter = claimAdapter

                listView!!.setOnItemClickListener { _, _, position, _ ->
                    val intent = Intent(cntx, EditClaimActivity::class.java)
                    intent.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                                or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    )
                    intent.putExtra("ClaimId", claims[position].claimId)
                    intent.putExtra("AdjusterId", adjuster.employeeId)
                    intent.putExtra("Region", regionValue)
                    intent.putExtra("UserName", userIdValue)
                    startActivity(intent)
                }

                val refreshButton = findViewById<Button>(R.id.refreshButton)
                refreshButton.setOnClickListener {
                    scope.launch {
                        claims = db.queryClaims()
                        withContext(Dispatchers.Main) {
                            claimAdapter.updateListAdapter(claims)
                        }
                    }
                }

                if (claims.isEmpty()) {
                    val builder = AlertDialog.Builder(cntx)
                    builder.setTitle("No Data")
                    builder.setMessage("The database is empty")
                    builder.setPositiveButton("Ok") { dialog, which ->
                        Toast.makeText(
                            applicationContext,
                            "Ok", Toast.LENGTH_SHORT
                        ).show()
                    }
                    builder.show()
                }
            }
        }
    }

    private val runnableCode: Runnable = object : Runnable {
        override fun run() {
            val scope = CoroutineScope(Dispatchers.Default)
            scope.launch {
                val db: CouchbaseConnect = CouchbaseConnect.getInstance(cntx)
                val currentCount = db.getClaimCounts()
                documentCount?.text = String.format("Claims: %d", currentCount.total)
            }
            handler.postDelayed(this, 1000)
        }
    }

    fun onLogoutTapped(view: View?) {
        val db: CouchbaseConnect = CouchbaseConnect.getInstance(cntx)
        handler.removeCallbacks(runnableCode)
        db.closeDatabase()
        val intent = Intent(cntx, LoginActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
    }
}
