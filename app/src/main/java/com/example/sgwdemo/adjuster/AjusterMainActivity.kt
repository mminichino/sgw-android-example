package com.example.sgwdemo.adjuster

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.couchbase.lite.MutableArray
import com.example.sgwdemo.R
import com.example.sgwdemo.cbdb.CouchbaseConnect
import com.example.sgwdemo.login.LoginActivity
import java.time.Instant
import java.time.format.DateTimeFormatter


class AdjusterMainActivity : AppCompatActivity() {

    private var TAG = "CBL-Demo"
    private var cntx: Context = this
    var logoutButton: Button? = null
    var dumpButton: Button? = null
    var storeNumber: EditText? = null
    var employeeID: EditText? = null
    var textView: ListView? = null
    var listView: ListView? = null
    var documentCount: TextView? = null
    var userIdValue: String? = null
    var regionValue: String? = null
    var employeeName: TextView? = null
    var employeeEmail: TextView? = null
    var employeeAddress: TextView? = null
    var employeePhone: TextView? = null
    var employeeZipCode: TextView? = null

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

    fun onDisplayTapped(view: View?) {
        val db: CouchbaseConnect = CouchbaseConnect.getInstance(cntx)

        val store = storeNumber!!.text
        val employee = employeeID!!.text
        val arrayAdapter: ArrayAdapter<*>
        val results: MutableList<String> = ArrayList()

        if (store!!.isEmpty() || employee!!.isEmpty()) {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Missing Values")
            builder.setMessage("Please provide the store ID and employee ID")
            builder.setPositiveButton("Ok") { dialog, which ->
                Toast.makeText(
                    applicationContext,
                    "Ok", Toast.LENGTH_SHORT
                ).show()
            }
            builder.show()
        } else {
            val time_now = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
            val rs = db.employeeLookup(store.toString(), employee.toString())
            for (result in rs) {
                val builder = StringBuilder()
                val documentId = result.getString("id")
                val timeCardArray = result.getArray("timecards")
                val mutableTimeCardArray: MutableArray? = timeCardArray?.toMutable()

                Log.i(TAG, "ID -> ${result.getString("id")}")
                Log.i(TAG, "name -> ${result.getString("name")}")
                Log.i(TAG, "employee_id -> ${result.getString("employee_id")}")

                builder.append(result.getString("name").toString())
                builder.append("\n")
                builder.append("Employee ID: ${result.getString("employee_id")}")
                results.add(builder.toString())

                val mutableDoc = db.getDocument(documentId.toString())
                    .setString("last_access", time_now.toString())
                mutableTimeCardArray?.addString(time_now.toString())
                mutableDoc.setArray("timecards", mutableTimeCardArray)
                db.updateDocument(mutableDoc)
            }

            if (results.isEmpty()) {
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Not Found")
                builder.setMessage("The employee was not found")
                builder.setPositiveButton("Ok") { dialog, which ->
                    Toast.makeText(
                        applicationContext,
                        "Ok", Toast.LENGTH_SHORT
                    ).show()
                }
                builder.show()
            } else {
                arrayAdapter = ArrayAdapter(
                    this,
                    android.R.layout.simple_list_item_1, results
                )
                textView!!.adapter = arrayAdapter
            }
        }
    }

    fun createClaimList() {
        val db: CouchbaseConnect = CouchbaseConnect.getInstance(cntx)
        val rs = db.queryDBByType("claim")
        val arrayAdapter: ArrayAdapter<*>
        val results: MutableList<String> = ArrayList()

        for (result in rs) {
            Log.i(TAG, result.toString())
            val builder = StringBuilder()

            builder.append("Customer: ${result.getString("customer_id").toString()}")
            builder.append("\n")
            builder.append("Amount: ${result.getString("claim_amount")}")
            results.add(builder.toString())
        }

        arrayAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1, results
        )
        listView!!.adapter = arrayAdapter

//        for (result in results) {
//            val docsProps = result.getDictionary(0)
//            val dumpRow = TableRow(this)
//            val dumpRowLabel = TextView(this)
//            val dumpRowElement = TextView(this)
//            val scale = resources.displayMetrics.density
//            val dpAsPixels = (10 * scale)
//
//            val builder = StringBuilder()
//            builder.append(docsProps!!.getString("claim_id").toString())
//            builder.append(")")
//            dumpRowLabel.setTextColor(Color.BLACK)
//            dumpRowLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16F)
//            dumpRowLabel.text = builder.toString()
//            dumpRow.addView(dumpRowLabel)
//            dumpRowElement.setTextColor(Color.BLACK)
//            dumpRowElement.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16F)
//            dumpRowElement.setPadding(dpAsPixels.toInt(), 0, 0, 0);
//            dumpRowElement.text = docsProps.getString("customer_id").toString()
//            dumpRow.addView(dumpRowElement)
//            listView!!.addView(dumpRow)
//        }

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

    private fun populateEmployeeInfo(store: String, employee: String) {
        val db: CouchbaseConnect = CouchbaseConnect.getInstance(cntx)
        val rs = db.employeeLookup(store, employee)
        val results = rs.allResults()
        val docsProps = results.first().getDictionary(0)
        val timeNow = DateTimeFormatter.ISO_INSTANT.format(Instant.now())

        employeeName?.text = docsProps?.getString("name").toString()
        employeeAddress?.text = docsProps?.getString("address").toString()
        employeeEmail?.text = docsProps?.getString("email").toString()
        employeePhone?.text = docsProps?.getString("phone").toString()
        employeeZipCode?.text = docsProps?.getString("zip_code").toString()

        val documentId = db.getDocId(employee)
        val mutableTimeCardArray: MutableArray? = docsProps?.getArray("timecards")?.toMutable()
        val mutableDoc = db.getDocument(documentId.toString())
            .setString("last_access", timeNow.toString())
        mutableTimeCardArray?.addString(timeNow.toString())
        mutableDoc.setArray("timecards", mutableTimeCardArray)
        db.updateDocument(mutableDoc)
    }

    fun onLogoutTapped(view: View?) {
        val db: CouchbaseConnect = CouchbaseConnect.getInstance(cntx)
        db.closeDatabase()
        val intent = Intent(cntx, LoginActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
    }
}
