package com.example.sgwdemo.main

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
import com.couchbase.lite.MutableArray
import com.example.sgwdemo.R
import com.example.sgwdemo.cbdb.CouchbaseConnect
import com.example.sgwdemo.login.LoginActivity
import com.example.sgwdemo.models.Employee
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.format.DateTimeFormatter


class MainActivity : AppCompatActivity() {

    private var TAG = "MainActivity"
    private var cntx: Context = this
    var showButton: Button? = null
    var dumpButton: Button? = null
    var dumpView: ListView? = null
    var documentCount: TextView? = null
    var employeeIdValue: String? = null
    var storeIdValue: String? = null
    var employeeName: TextView? = null
    var employeeEmail: TextView? = null
    var employeeAddress: TextView? = null
    var employeePhone: TextView? = null
    var employeeZipCode: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        employeeIdValue = intent.getStringExtra("UserName")
        storeIdValue = intent.getStringExtra("StoreID")
        showButton = findViewById(R.id.showData)
        dumpButton = findViewById(R.id.showDump)
        dumpView = findViewById(R.id.dumpTable)
        documentCount = findViewById(R.id.documentCount)
        employeeName = findViewById(R.id.employeeName)
        employeeEmail = findViewById(R.id.employeeEmail)
        employeeAddress = findViewById(R.id.employeeAddress)
        employeePhone = findViewById(R.id.employeePhone)
        employeeZipCode = findViewById(R.id.employeeZipCode)

        populateEmployeeInfo(employeeIdValue!!)
        startCountUpdateThread(documentCount)
        Log.i(TAG, "Started Main Activity")
    }

    fun onDumpTapped(view: View?) {
        val db: CouchbaseConnect = CouchbaseConnect.getInstance(cntx)
        val scope = CoroutineScope(Dispatchers.Default)
        var employees: ArrayList<Employee>

        scope.launch {
            employees = db.queryEmployees()
            withContext(Dispatchers.Main) {
                val adapter = EmployeeAdapter(cntx, employees)
                dumpView!!.adapter = adapter

                dumpView!!.setOnItemClickListener { _, _, position, _ ->
                    populateEmployeeInfo(employees[position].employeeId)
                }

                if (employees.isEmpty()) {
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

    private fun populateEmployeeInfo(employeeId: String) {
        val db: CouchbaseConnect = CouchbaseConnect.getInstance(cntx)
        val scope = CoroutineScope(Dispatchers.Default)
        val timeNow = DateTimeFormatter.ISO_INSTANT.format(Instant.now())

        scope.launch {
        val employee = db.getEmployeeById("employees:${employeeId}")
            withContext(Dispatchers.Main) {
                employeeName?.text = employee.name
                employeeAddress?.text = employee.address
                employeeEmail?.text = employee.email
                employeePhone?.text = employee.phone
                employeeZipCode?.text = employee.zipCode

                val timeCards = ArrayList<String>(employee.timeCards)
                val mutableDoc = db.getDocument("employees:${employeeId}")
                    .setString("last_access", timeNow.toString())
                timeCards.add(timeNow.toString())
                val timeCardsUpdate = MutableArray(timeCards.toMutableList() as List<Any>)
                mutableDoc.setArray("timecards", timeCardsUpdate)
                db.updateDocument(mutableDoc)
            }
        }
    }

    fun onLogoutTapped(view: View?) {
        val db: CouchbaseConnect = CouchbaseConnect.getInstance(cntx)
        db.closeDatabase()
        val intent = Intent(cntx, LoginActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
    }
}
