package com.example.sgwdemo

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.couchbase.lite.*
import java.net.URI
import java.util.*
import kotlin.collections.ArrayList
import android.widget.*
import java.time.Instant
import java.time.format.DateTimeFormatter


class MainActivity : AppCompatActivity() {

    private var TAG = "CBL-Demo"
    private var cntx: Context = this

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val props = Properties()
        val propfile = getBaseContext().getAssets().open("config.properties")
        props.load(propfile)

        val connectStringBuilder = StringBuilder()
        connectStringBuilder.append("ws://")
        connectStringBuilder.append(props.getProperty("sgwhost"))
        connectStringBuilder.append(":4984/")
        connectStringBuilder.append(props.getProperty("database"))
        val username = props.getProperty("username")
        val password = props.getProperty("password")
        Log.i(TAG, "SGW Target -> $connectStringBuilder")
        Log.i(TAG, "SGW User -> $username")

        CouchbaseLite.init(cntx)
        Log.i(TAG,"Initialized CBL")

        Log.i(TAG, "Starting DB")
        val cfg = DatabaseConfigurationFactory.create()
        val database = Database("employees", cfg)

        database.createIndex(
            "StoreEmployeeIndex",
            IndexBuilder.valueIndex(
                ValueIndexItem.property("store_id"),
                ValueIndexItem.property("employee_id")
            )
        )

        val replicator = Replicator(
            ReplicatorConfigurationFactory.create(
                database = database,
                target = URLEndpoint(URI(connectStringBuilder.toString())),
                type = ReplicatorType.PUSH_AND_PULL,
                authenticator = BasicAuthenticator(username, password.toCharArray()),
                continuous = true
            )
        )

        replicator.addChangeListener { change ->
            val err = change.status.error
            if (err != null) {
                Log.i(TAG, "Error code ::  ${err.code}")
            }
        }

        replicator.start()

        val showButton = findViewById<Button>(R.id.showData)
        val storeNumber = findViewById<EditText>(R.id.editStore)
        val employeeID = findViewById<EditText>(R.id.editEID)
        val textView = findViewById<ListView>(R.id.textView)

        showButton.setOnClickListener {
            val store = storeNumber.text
            val employee = employeeID.text
            val arrayAdapter: ArrayAdapter<*>
            val results: MutableList<String> = ArrayList()

            if (store.isEmpty() || employee.isEmpty()) {
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Missing Values")
                builder.setMessage("Please provide the store ID and employee ID")
                builder.setPositiveButton("Ok") { dialog, which ->
                    Toast.makeText(applicationContext,
                        "Ok", Toast.LENGTH_SHORT).show()
                }
                builder.show()
            } else {
                val time_now = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
                val rs = QueryBuilder
                    .select(
                        SelectResult.expression(Meta.id),
                        SelectResult.property("name"),
                        SelectResult.property("store_id"),
                        SelectResult.property("employee_id"),
                        SelectResult.property("timecards")
                    )
                    .from(DataSource.database(database))
                    .where(
                        Expression.property("store_id").equalTo(Expression.string(store.toString()))
                            .and(
                                Expression.property("employee_id")
                                    .equalTo(Expression.string(employee.toString()))
                            )
                    )
                    .orderBy(Ordering.expression(Meta.id))
                    .execute()
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

                    val mutableDoc = database.getDocument(documentId.toString())!!.toMutable()
                        .setString("last_access", time_now.toString())
                    mutableTimeCardArray?.addString(time_now.toString())
                    mutableDoc.setArray("timecards", mutableTimeCardArray)
                    database.save(mutableDoc)
                }

                if (results.isEmpty()) {
                    val builder = AlertDialog.Builder(this)
                    builder.setTitle("Not Found")
                    builder.setMessage("The employee was not found")
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
                    textView.adapter = arrayAdapter
                }
            }
        }

    }
}
