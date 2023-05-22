package com.example.sgwdemo.main

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.sgwdemo.R
import com.example.sgwdemo.cbdb.CouchbaseConnect
import com.example.sgwdemo.models.Employee
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DumpActivity : AppCompatActivity() {

    private var cntx: Context = this
    private var dumpView: ListView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dump)
        dumpView = findViewById(R.id.dumpTable)
        val db: CouchbaseConnect = CouchbaseConnect.getInstance(cntx)
        val scope = CoroutineScope(Dispatchers.Default)
        var employees: ArrayList<Employee>

        scope.launch {
            employees = db.queryEmployees()
            withContext(Dispatchers.Main) {
                val adapter = EmployeeAdapter(cntx, employees)
                dumpView!!.adapter = adapter

                dumpView!!.setOnItemClickListener { _, _, position, _ ->
                    val intent = Intent(cntx, MainActivity::class.java)
                    intent.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                                or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    )
                    intent.putExtra("LocationID", employees[position].locationId)
                    intent.putExtra("UserName", employees[position].userId)
                    startActivity(intent)
                }

                if (employees.isEmpty()) {
                    val builder = AlertDialog.Builder(cntx)
                    builder.setTitle("No Data")
                    builder.setMessage("The database is empty")
                    builder.setPositiveButton("Ok") { _, _ ->
                        Toast.makeText(
                            applicationContext,
                            "Ok", Toast.LENGTH_SHORT
                        ).show()
                    }
                    builder.show()
                }
            }
        }

        val backButton = findViewById<Button>(R.id.backButton)
        backButton.setOnClickListener {
            finish()
        }
    }
}
