package com.example.sgwdemo.cbdb

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.couchbase.lite.*
import com.example.sgwdemo.R
import java.net.URI
import java.util.*


open class CouchbaseConnectHolder<out T: Any, in A>(creator: (A) -> T) {
    private var creator: ((A) -> T)? = creator
    @Volatile private var instance: T? = null

    fun getInstance(arg: A): T {
        val checkInstance = instance
        if (checkInstance != null) {
            return checkInstance
        }

        return synchronized(this) {
            val checkInstanceAgain = instance
            if (checkInstanceAgain != null) {
                checkInstanceAgain
            } else {
                val created = creator!!(arg)
                instance = created
                creator = null
                created
            }
        }
    }
}


class CouchbaseConnect(context: Context) {

    private var TAG = "CBL-Demo"
    private var cntx: Context = context
//    private var instance: CouchbaseConnect? = null
    var db: Database? = null
    var replicator: Replicator? = null
    var listenerToken: ListenerToken? = null
    var connectString: String? = null
    val props = Properties()
    var dbOpen: Boolean = false
    var replicatorBusy: Boolean = false
    var replicatorConnecting: Boolean = false
    val propfile = context.getAssets().open("config.properties")
    val pref: SharedPreferences =
        context.getSharedPreferences("APP_SETTINGS", Context.MODE_PRIVATE)

    companion object : CouchbaseConnectHolder<CouchbaseConnect, Context>(::CouchbaseConnect)

    fun init() {
        val gatewayAddress = pref.getString(R.string.gatewayPropertyKey.toString(), "")
        props.load(propfile)
        val connectStringBuilder = StringBuilder()
        connectStringBuilder.append("ws://")
        connectStringBuilder.append(gatewayAddress)
        connectStringBuilder.append(":4984/")
        connectStringBuilder.append(props.getProperty("database"))
        connectString = connectStringBuilder.toString()
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

    fun syncDatabase(session: String, cookie: String) {
        Log.i(TAG, "DB Session Cookie -> $cookie")
        replicator = Replicator(
            ReplicatorConfigurationFactory.create(
                database = db,
                target = URLEndpoint(URI(connectString)),
                type = ReplicatorType.PUSH_AND_PULL,
//                authenticator = BasicAuthenticator(username, password!!.toCharArray()),
                authenticator = SessionAuthenticator(session, cookie),
                continuous = true
            )
        )

        listenerToken = replicator!!.addChangeListener { change ->
            val err = change.status.error
            if (err != null) {
                Log.i(TAG, "Error code ::  ${err.code}")
            }
            replicatorBusy = change.status.activityLevel === ReplicatorActivityLevel.BUSY
            replicatorConnecting =
                change.status.activityLevel === ReplicatorActivityLevel.CONNECTING
            if (!replicatorBusy && !replicatorConnecting) {
                val total = change.status.progress.total
                val completed = change.status.progress.completed
                Log.i(TAG, "Replication progress => total = $total completed = $completed")
            }
        }

        replicator!!.start()
        dbOpen = true
    }

    fun isDbOpen(): Boolean {
        return dbOpen
    }

    fun employeeLookup(store: String, employee: String): ResultSet {
        replicationWait()
        return QueryBuilder
            .select(
                SelectResult.all()
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

    fun getAllEmployees(): ResultSet {
        replicationWait()
        return QueryBuilder
            .select(
                SelectResult.all()
            )
            .from(DataSource.database(db!!))
            .orderBy(Ordering.property("record_id"))
            .execute()
    }

    fun getDocId(employee: String): String? {
        replicationWait()
        val rs = QueryBuilder
            .select(
                SelectResult.expression(Meta.id)
            )
            .from(DataSource.database(db!!))
            .where(
                Expression.property("employee_id").equalTo(Expression.string(employee))
            )
            .execute()
        val results = rs.allResults()
        return results.firstOrNull()?.getString("id")
    }

//    fun getEmployeePassword(employee: String): String? {
//        replicationWait()
//        val rs = QueryBuilder
//            .select(
//                SelectResult.property("password")
//            )
//            .from(DataSource.database(db!!))
//            .where(
//                Expression.property("employee_id").equalTo(Expression.string(employee))
//            )
//            .execute()
//        val results = rs.allResults()
//        return results.firstOrNull()?.getString("password")
//    }

    fun getDocument(documentId: String): MutableDocument {
        return db!!.getDocument(documentId)!!.toMutable()
    }

    fun updateDocument(doc: MutableDocument) {
        db!!.save(doc)
    }

    fun dbCount(): String {
        return db!!.count.toString()
    }

    fun replicationWait() {
        if (replicatorBusy || replicatorConnecting) {
            Thread.sleep(100)
        }
    }

    fun closeDatabase() {
        try {
            if (db != null) {
                stopSync()
                db!!.close()
                db = null
                dbOpen = false
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
