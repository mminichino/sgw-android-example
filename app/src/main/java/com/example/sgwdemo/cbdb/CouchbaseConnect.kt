package com.example.sgwdemo.cbdb

import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import com.couchbase.lite.*
import java.net.URI
import java.util.*


class CouchbaseConnect(base: Context?) : ContextWrapper(base) {

    private var TAG = "CBL-Demo"
    private var cntx: Context = this
    private var instance: CouchbaseConnect? = null
    var db: Database? = null
    var replicator: Replicator? = null
    var listenerToken: ListenerToken? = null
    var connectString: String? = null
    var password: String? = null
    val props = Properties()
    val propfile = cntx.getAssets().open("config.properties")

    fun getSharedInstance(): CouchbaseConnect {
        if (instance == null) {
            Log.i(TAG, "DBM: Creating new instance")
            instance = CouchbaseConnect(this)
        }
        Log.i(TAG, "DBM: Returning existing instance")
        return instance as CouchbaseConnect
    }

//    fun getDatabase(): Database {
//        return db
//    }

    fun init() {
        props.load(propfile)
        val connectStringBuilder = StringBuilder()
        connectStringBuilder.append("ws://")
        connectStringBuilder.append(props.getProperty("sgwhost"))
        connectStringBuilder.append(":4984/")
        connectStringBuilder.append(props.getProperty("database"))
        connectString = connectStringBuilder.toString()
        password = props.getProperty("password")
        Log.i(TAG, "DB init -> $connectStringBuilder")
        CouchbaseLite.init(cntx)
    }

    fun openDatabase(username: String) {
        Log.i(TAG, "DB open -> $username")
        val cfg = DatabaseConfigurationFactory.create()
        cfg.setDirectory(String.format("%s/%s", cntx.getFilesDir(), username))
        try {
            db = Database("employees", cfg)
            db!!.createIndex(
                "StoreEmployeeIndex",
                IndexBuilder.valueIndex(
                    ValueIndexItem.property("store_id"),
                    ValueIndexItem.property("employee_id")
                )
            )
        } catch (e: CouchbaseLiteException) {
            e.printStackTrace()
        }
    }

    fun syncDatabase(username: String) {
        Log.i(TAG, "DB Sync -> $username")
        replicator = Replicator(
            ReplicatorConfigurationFactory.create(
                database = db,
                target = URLEndpoint(URI(connectString)),
                type = ReplicatorType.PUSH_AND_PULL,
                authenticator = BasicAuthenticator(username, password!!.toCharArray()),
                continuous = true
            )
        )

        listenerToken = replicator!!.addChangeListener { change ->
            val err = change.status.error
            if (err != null) {
                Log.i(TAG, "Error code ::  ${err.code}")
            }
        }

        replicator!!.start()
    }

    fun employeeLookup(store: String, employee: String): ResultSet {
        return QueryBuilder
            .select(
                SelectResult.expression(Meta.id),
                SelectResult.property("name"),
                SelectResult.property("store_id"),
                SelectResult.property("employee_id"),
                SelectResult.property("timecards")
            )
            .from(DataSource.database(db!!))
            .where(
                Expression.property("store_id").equalTo(Expression.string(store))
                    .and(
                        Expression.property("employee_id")
                            .equalTo(Expression.string(employee))
                    )
            )
            .orderBy(Ordering.expression(Meta.id))
            .execute()
    }

    fun getDocument(documentId: String): MutableDocument {
        return db!!.getDocument(documentId)!!.toMutable()
    }

    fun updateDocument(doc: MutableDocument) {
        db!!.save(doc)
    }

    fun dbCount(): String {
        return db!!.count.toString()
    }

    fun closeDatabase() {
        try {
            if (db != null) {
                stopSync()
                db!!.close()
                db = null
            }
        } catch (e: CouchbaseLiteException) {
            e.printStackTrace()
        }
    }

    private fun stopSync() {
        if (listenerToken != null) {
            replicator!!.removeChangeListener(listenerToken!!)
        }
    }
}
