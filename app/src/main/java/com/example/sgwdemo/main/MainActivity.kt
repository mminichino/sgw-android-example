package com.example.sgwdemo.main

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.couchbase.lite.MutableArray
import com.couchbase.lite.MutableDocument
import com.example.sgwdemo.R
import com.example.sgwdemo.cbdb.CouchbaseConnect
import com.example.sgwdemo.login.LoginActivity
import com.example.sgwdemo.models.Employee
import com.example.sgwdemo.models.Timecard
import kotlinx.coroutines.*
import java.lang.Runnable
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale


class MainActivity : AppCompatActivity() {

    private var TAG = "MainActivity"
    private var cntx: Context = this
    var showButton: Button? = null
    var clockButton: Button? = null
    var dumpView: ListView? = null
    var timeCardView: ListView? = null
    var locationName: TextView? = null
    var employeeStatusField: TextView? = null
    var employeeIdValue: String? = null
    var locationIdValue: String? = null
    var employeeName: TextView? = null
    var employeeEmail: TextView? = null
    var employeeAddress: TextView? = null
    var employeePhone: TextView? = null
    var employeeZipCode: TextView? = null
    var employeeHoursWorked: TextView? = null
    var employeePayDue: TextView? = null
    var employeeStatus: Boolean = false
    var employeePayRate: Float = 0.00F
    var employeeTimeWorked: String = "0 Hours 0 Minutes"
    var employeePayBalance: Float = 0.00F
    var employeeIdNum: Int = 0
    var handler: Handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        employeeIdValue = intent.getStringExtra("UserName")
        locationIdValue = intent.getStringExtra("LocationID")
        showButton = findViewById(R.id.showData)
        clockButton = findViewById(R.id.clockButton)
        locationName = findViewById(R.id.locationName)
        employeeStatusField = findViewById(R.id.employeeStatus)
        employeeName = findViewById(R.id.employeeName)
        employeeEmail = findViewById(R.id.employeeEmail)
        employeeAddress = findViewById(R.id.employeeAddress)
        employeePhone = findViewById(R.id.employeePhone)
        employeeZipCode = findViewById(R.id.employeeZipCode)
        employeeHoursWorked = findViewById(R.id.workedHours)
        employeePayDue = findViewById(R.id.payDue)
        timeCardView = findViewById(R.id.timeCardTable)

        populateHeaderInfo(locationIdValue!!)
        populateEmployeeInfo(employeeIdValue!!)
        handler.post(runnableCode)
        Log.i(TAG, "Started Main Activity")
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_dump,menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val cntx: Context = applicationContext
        when (item.itemId){
            R.id.dump -> {
                val intent = Intent(cntx, DumpActivity::class.java)
                startActivity(intent)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun onClockTapped(view: View?) {
        val db: CouchbaseConnect = CouchbaseConnect.getInstance(cntx)
        val scope = CoroutineScope(Dispatchers.Default)
        val currentTimeFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US)
        val currentDate = currentTimeFormat.format(Date())
        val time = Instant.now()
        val activeDocId = "timecards::$employeeIdNum::$locationIdValue::active"

        if (db.documentExists(db.timecards!!, activeDocId)) {
            val completeDocId = "timecards::$employeeIdNum::$locationIdValue::$currentDate"
            val oldDoc = db.getDocument(db.timecards!!, activeDocId)
            val newDoc = MutableDocument(completeDocId)
            newDoc.setString("location_id", oldDoc.getString("location_id"))
                .setString("employee_id", oldDoc.getString("employee_id"))
                .setBoolean("status", false)
                .setString("time_in", oldDoc.getString("time_in"))
                .setString("time_out", time.epochSecond.toString())
                .setInt("duration", oldDoc.getInt("duration"))
                .setFloat("rate", oldDoc.getFloat("rate"))
                .setBoolean("paid", oldDoc.getBoolean("paid"))
            db.updateDocument(db.timecards!!, newDoc)
            db.removeDocument(db.timecards!!, activeDocId)
            refreshTimeCardTable()
            populateTimeWorked(employeeIdNum.toString())
        } else {
            val mutableDoc = MutableDocument(activeDocId)
            mutableDoc.setString("location_id", locationIdValue)
                .setString("employee_id", employeeIdNum.toString())
                .setBoolean("status", true)
                .setString("time_in", time.epochSecond.toString())
                .setString("time_out", "")
                .setInt("duration", 0)
                .setFloat("rate", employeePayRate)
                .setBoolean("paid", false)
            db.updateDocument(db.timecards!!, mutableDoc)
        }
    }

    private fun refreshTimeCardTable() {
        val db: CouchbaseConnect = CouchbaseConnect.getInstance(cntx)
        val scope = CoroutineScope(Dispatchers.Default)
        var cards: ArrayList<Timecard>

        scope.launch {
            cards = db.getCompletedTimeCards(employeeIdNum.toString())
            withContext(Dispatchers.Main) {
                val adapter = TimecardAdapter(cntx, cards)
                timeCardView!!.adapter = adapter
            }
        }
    }

    private val runnableCode: Runnable = object : Runnable {
        override fun run() {
            val db: CouchbaseConnect = CouchbaseConnect.getInstance(cntx)
            val docId = "timecards::$employeeIdNum::$locationIdValue::active"
            db.db?.let {
                if (db.documentExists(db.timecards!!, docId)) {
                    val statusText = "Active"
                    employeeStatus = true
                    clockButton?.text = String.format("Check Out")
                    employeeStatusField?.text = String.format("Status: %s", statusText)
                } else {
                    val statusText = "Out"
                    employeeStatus = false
                    clockButton?.text = String.format("Check In")
                    employeeStatusField?.text = String.format("Status: %s", statusText)
                }
            }
            handler.postDelayed(this, 1000)
        }
    }

    private fun populateEmployeeInfo(employeeId: String) {
        val db: CouchbaseConnect = CouchbaseConnect.getInstance(cntx)
        val scope = CoroutineScope(Dispatchers.Default)

        scope.launch {
            val employee = db.getEmployeeByUserName(employeeId)
            withContext(Dispatchers.Main) {
                employeeName?.text = employee.name
                employeeAddress?.text = employee.address
                employeeEmail?.text = employee.email
                employeePhone?.text = employee.phone
                employeeZipCode?.text = employee.zipCode
                employeeIdNum = employee.employeeId.toInt()
                employeePayRate = employee.rate
                refreshTimeCardTable()
                populateTimeWorked(employee.employeeId)
            }
        }
    }

    private fun populateHeaderInfo(locationId: String) {
        val db: CouchbaseConnect = CouchbaseConnect.getInstance(cntx)
        val scope = CoroutineScope(Dispatchers.Default)

        scope.launch {
            val location = db.getLocationById("locations::$locationId")
            withContext(Dispatchers.Main) {
                locationName?.text = location.name
            }
        }
    }

    private fun populateTimeWorked(employeeId: String) {
        val db: CouchbaseConnect = CouchbaseConnect.getInstance(cntx)
        val scope = CoroutineScope(Dispatchers.Default)
        var cards: ArrayList<Timecard>

        scope.launch {
            cards = db.queryTimecards(employeeIdNum.toString())
            withContext(Dispatchers.Main) {
                var minutes: Int = 0
                var pay = 0.00F
                cards.forEach { card ->
                    val payMinRate: Float = card.rate / 60
                    val cardSeconds = card.timeOut.toLong() - card.timeIn.toLong()
                    val cardMinutes = kotlin.math.ceil(cardSeconds.toFloat() / 60).toInt()
                    minutes += cardMinutes
                    pay += cardMinutes * payMinRate
                }
                val hours = kotlin.math.floor(minutes.toFloat() / 60).toInt()
                val remainder = minutes % 60
                employeeHoursWorked?.text = String.format("%d Hours %d Minutes", hours, remainder)
                employeePayDue?.text = String.format("$ %.2f", pay)
            }
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
