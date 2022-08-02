package com.example.sgwdemo

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import com.couchbase.lite.*
import java.net.URI
import java.util.*
import kotlin.collections.ArrayList


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
        Log.i(TAG, "SGW Target -> $connectStringBuilder")

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
                authenticator = BasicAuthenticator("demouser", "CouchBase321".toCharArray())
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

            val rs = QueryBuilder
                .select(
                    SelectResult.expression(Meta.id),
                    SelectResult.property("name"),
                    SelectResult.property("store_id"),
                    SelectResult.property("employee_id")
                )
                .from(DataSource.database(database))
                .where(Expression.property("store_id").equalTo(Expression.string(store.toString()))
                    .and(Expression.property("employee_id").equalTo(Expression.string(employee.toString()))))
                .orderBy(Ordering.expression(Meta.id))
                .execute()

            for (result in rs) {
                val builder = StringBuilder()
                Log.i(TAG, "name ->${result.getString("name")}")
                Log.i(TAG, "employee_id -> ${result.getString("employee_id")}")
                builder.append(result.getString("name").toString())
                builder.append("\n")
                builder.append("Employee ID: ${result.getString("employee_id")}")
                results.add(builder.toString())
            }

            arrayAdapter = ArrayAdapter(this,
                android.R.layout.simple_list_item_1, results)
            textView.adapter = arrayAdapter
        }

    }
}
