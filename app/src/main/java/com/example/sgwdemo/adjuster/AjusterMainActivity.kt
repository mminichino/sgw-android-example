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
import com.google.android.material.progressindicator.CircularProgressIndicator


class AdjusterMainActivity : AppCompatActivity() {

    private var TAG = "AdjusterMain"
    private var cntx: Context = this
    var logoutButton: Button? = null
    var listView: ListView? = null
    var documentCount: TextView? = null
    var userIdValue: String? = null
    var regionValue: String? = null
    var progress: CircularProgressIndicator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_adjuster_main)

        userIdValue = intent.getStringExtra("UserName")
        regionValue = intent.getStringExtra("Region")
        logoutButton = findViewById(R.id.logoutButton)
        listView = findViewById(R.id.listView)
        documentCount = findViewById(R.id.documentCount)
        progress = findViewById(R.id.progressBarLoadWait)

        createClaimList()
        startCountUpdateThread(documentCount)
    }

    fun createClaimList() {
        val db: CouchbaseConnect = CouchbaseConnect.getInstance(cntx)
        val rs = db.queryDBByType("claim")
        val claimIdList: MutableList<String> = mutableListOf()
        val claimList: ArrayList<ClaimModel> = ArrayList()

        progress!!.visibility = View.VISIBLE

        for (result in rs) {
            val thisDoc = result.getDictionary(0)
            val customerId = thisDoc!!.getString("customer_id").toString()
            val customer = db.queryDB("customer_id", customerId, "customer")
                .firstOrNull()?.getDictionary(0)

            val claimId = thisDoc.getString("claim_id")
            val customerName = customer?.getString("name")
            val customerPhone = customer?.getString("phone")
            val claimAmount = thisDoc.getFloat("claim_amount").toString()
            val claimStatus = convertStatusId(thisDoc.getInt("claim_status"))
            claimIdList.add(claimId!!)
            claimList.add(ClaimModel(claimId, customerName!!, customerPhone!!, claimAmount, claimStatus))
        }

        val adapter = ClaimAdapter(this, claimList)
        listView!!.adapter = adapter

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

        progress!!.visibility = View.INVISIBLE

        if (claimList.isEmpty()) {
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

    private fun convertStatusId(id: Int) : String {
        return if (id == 0) {
            "Open"
        } else {
            "Complete"
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
