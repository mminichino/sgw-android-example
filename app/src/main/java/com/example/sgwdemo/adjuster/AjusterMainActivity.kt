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


class AdjusterMainActivity : AppCompatActivity() {

    private var TAG = "AdjusterMain"
    private var cntx: Context = this
    var logoutButton: Button? = null
    var listView: ListView? = null
    var documentCount: TextView? = null
    var userIdValue: String? = null
    var regionValue: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_adjuster_main)

        userIdValue = intent.getStringExtra("UserName")
        regionValue = intent.getStringExtra("Region")
        logoutButton = findViewById(R.id.logoutButton)
        listView = findViewById(R.id.listView)
        documentCount = findViewById(R.id.documentCount)

        createClaimList()
        startCountUpdateThread(documentCount)
    }

    fun createClaimList() {
        val db: CouchbaseConnect = CouchbaseConnect.getInstance(cntx)
        val rs = db.queryDBByType("claim")
        val arrayAdapter: ArrayAdapter<*>
        val results: MutableList<String> = ArrayList()
        val claimIdList: MutableList<String> = mutableListOf()

        for (result in rs) {
            val thisDoc = result.getDictionary(0)
            val builder = StringBuilder()
            val customerId = thisDoc!!.getString("customer_id").toString()
            val customer = db.queryDB("customer_id", customerId, "customer")
                .firstOrNull()?.getDictionary(0)

            val claimId = thisDoc.getString("claim_id")
            claimIdList.add(claimId!!)
            builder.append("Claim ID: ${claimId?.padStart(7, '0')}")
            builder.append("\n")
            builder.append("Customer: ${customer?.getString("name")}")
            builder.append("\n")
            builder.append("Phone   : ${customer?.getString("phone")}")
            results.add(builder.toString())
        }

        arrayAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1, results
        )
        listView!!.adapter = arrayAdapter

        listView!!.setOnItemClickListener { _, _, position, _ ->
            Log.i(TAG, "Click Position $position")
            Log.i(TAG, "Claim IDs $claimIdList")
            val intent = Intent(cntx, EditClaimActivity::class.java)
            intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        or Intent.FLAG_ACTIVITY_CLEAR_TASK
            )
            intent.putExtra("ClaimId", claimIdList[position])
            startActivity(intent)
        }

        if (results.isEmpty()) {
            val builder = AlertDialog.Builder(this)
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

    private fun startCountUpdateThread(documentCount: TextView?) {
        val db: CouchbaseConnect = CouchbaseConnect.getInstance(cntx)
        val runnable: Runnable = object : Runnable {
            override fun run() {
                try {
                    Thread.sleep(1000)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
                Handler(Looper.getMainLooper()).post(object : Runnable {
                    override fun run() {
                        val currentCount = db.dbCount()
                        val countDisplay = "Documents: $currentCount"
                        documentCount?.text = countDisplay
                    }
                })
            }
        }
        Thread(runnable).start()
    }

    fun onLogoutTapped(view: View?) {
        val db: CouchbaseConnect = CouchbaseConnect.getInstance(cntx)
        db.closeDatabase()
        val intent = Intent(cntx, LoginActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
    }
}
