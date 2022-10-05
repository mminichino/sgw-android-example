package com.example.sgwdemo.main

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.couchbase.lite.*
import com.example.sgwdemo.R
import com.example.sgwdemo.cbdb.CouchbaseConnect
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*


class MainActivity : AppCompatActivity() {

    private var TAG = "CBL-Demo"
    private var cntx: Context = this
    var showButton: Button? = null
    var dumpButton: Button? = null
    var storeNumber: EditText? = null
    var employeeID: EditText? = null
    var textView: ListView? = null
    var documentCount: TextView? = null
    var employeeIdValue: String? = null
    var storeIdValue: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        employeeIdValue = intent.getStringExtra("UserName")
        storeIdValue = intent.getStringExtra("StoreID")
        showButton = findViewById(R.id.showData)
        dumpButton = findViewById(R.id.showDump)
        storeNumber = findViewById(R.id.editStore)
        employeeID = findViewById(R.id.editEID)
        textView = findViewById(R.id.textView)
        documentCount = findViewById(R.id.documentCount)

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

    fun onDumpTapped(view: View?) {
        val db: CouchbaseConnect = CouchbaseConnect.getInstance(cntx)
        val results: MutableList<String> = ArrayList()
        val arrayAdapter: ArrayAdapter<*>

        val rs = db.getAllEmployees()
        for (result in rs) {
            val builder = StringBuilder()
            val documentId = result.getString("id")
            val mutableDoc = db.getDocument(documentId.toString())
            builder.append(mutableDoc.getString("name").toString())
            builder.append("\n")
            builder.append("Employee ID: ${mutableDoc.getString("employee_id")}")
            results.add(builder.toString())
        }

        if (results.isEmpty()) {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("No Data")
            builder.setMessage("The database is empty")
            builder.setPositiveButton("Ok") { dialog, which ->
                Toast.makeText(applicationContext,
                    "Ok", Toast.LENGTH_SHORT).show()
            }
            builder.show()
        } else {
            arrayAdapter = ArrayAdapter(
                this,
                android.R.layout.simple_list_item_1, results
            )
            textView?.adapter = arrayAdapter
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
}
