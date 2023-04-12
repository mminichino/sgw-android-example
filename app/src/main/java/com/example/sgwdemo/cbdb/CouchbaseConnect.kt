package com.example.sgwdemo.cbdb

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.couchbase.lite.*
import com.example.sgwdemo.R
import com.example.sgwdemo.models.ClaimGrid
import com.example.sgwdemo.models.Claim
import com.google.gson.Gson
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.*
import java.net.URI


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

    private var TAG = "CouchbaseConnect"
    private var cntx: Context = context
    val gson = Gson()
    var db: Database? = null
    var replicator: Replicator? = null
    var listenerToken: ListenerToken? = null
    var connectString: String? = null
    var dbOpen: Boolean = false
    val replicationStatus: MutableState<String> = mutableStateOf("")
    val replicationProgress: MutableState<String> = mutableStateOf("Not Started")
    val pref: SharedPreferences =
        context.getSharedPreferences("APP_SETTINGS", Context.MODE_PRIVATE)

    companion object : CouchbaseConnectHolder<CouchbaseConnect, Context>(::CouchbaseConnect)

    fun init() {
        val gatewayAddress = pref.getString(R.string.gatewayPropertyKey.toString(), "")
        val databaseName = pref.getString(R.string.databaseNameKey.toString(), "")

        connectString = "ws://$gatewayAddress:4984/$databaseName"
        Log.i(TAG, "DB init -> $connectString")
        replicationStatus.value = ReplicationStatus.UNINITIALIZED

        CouchbaseLite.init(cntx)
    }

    fun openDatabase(username: String) {
        val databaseName = pref.getString(R.string.databaseNameKey.toString(), "")
        Log.i(TAG, "DB open -> $username")
        val cfg = DatabaseConfigurationFactory.create()
        cfg.setDirectory(String.format("%s/%s", cntx.getFilesDir(), username))
        try {
            db = Database(databaseName!!, cfg)
            db!!.createIndex(
                "DemoIndex",
                IndexBuilder.valueIndex(
                    ValueIndexItem.property("store_id"),
                    ValueIndexItem.property("employee_id"),
                    ValueIndexItem.property("type"),
                    ValueIndexItem.property("region"),
                    ValueIndexItem.property("claim_id"),
                    ValueIndexItem.property("customer_id")
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
                authenticator = SessionAuthenticator(session, cookie),
                continuous = true
            )
        )

        listenerToken = replicator!!.addChangeListener { change ->
            val err = change.status.error

            if (err != null) {
                Log.i(TAG, "Error ${err.code} : ${err.message}")
            }

            when (change.status.activityLevel) {
                ReplicatorActivityLevel.OFFLINE -> {
                    replicationStatus.value = ReplicationStatus.OFFLINE
                    Log.i(TAG, "Replication Status OFFLINE")
                }
                ReplicatorActivityLevel.IDLE -> {
                    replicationStatus.value = ReplicationStatus.IDlE
                    Log.i(TAG,"Replication Status IDLE")
                }
                ReplicatorActivityLevel.STOPPED -> {
                    replicationStatus.value = ReplicationStatus.STOPPED
                    Log.i(TAG,"Replication Status STOPPED")
                }
                ReplicatorActivityLevel.BUSY -> {
                    replicationStatus.value = ReplicationStatus.BUSY
                    Log.i(TAG,"Replication Status BUSY")
                }
                ReplicatorActivityLevel.CONNECTING -> {
                    replicationStatus.value = ReplicationStatus.CONNECTING
                    Log.i(TAG,"Replication Status CONNECTING")
                }
            }

            if (change.status.progress.completed == change.status.progress.total || change.status.progress.completed.toInt() == 0) {
                replicationProgress.value = "Completed"
                Log.i(TAG,"Replication Status COMPLETED")
            } else {
                replicationProgress.value =
                    "${change.status.progress.total / change.status.progress.completed}"
            }
        }

        replicator!!.start()
        dbOpen = true
        replicationWait()
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

    fun queryDB(where: String, value: String, type: String = ""): List<Result> {
        var whereExpression = Expression.property(where).equalTo(Expression.string(value))
        if (type.isNotEmpty()) {
            whereExpression = whereExpression.and(Expression.property("type")
                .equalTo(Expression.string(type)))
        }
        replicationWait()
        return QueryBuilder
            .select(
                SelectResult.all()
            )
            .from(DataSource.database(db!!))
            .where(
                whereExpression
            )
            .execute().allResults()
    }

    fun queryDBDocID(where: String, value: String, type: String = ""): String? {
        var whereExpression = Expression.property(where).equalTo(Expression.string(value))
        if (type.isNotEmpty()) {
            whereExpression = whereExpression.and(Expression.property("type")
                .equalTo(Expression.string(type)))
        }
        replicationWait()
        val rs = QueryBuilder
            .select(
                SelectResult.expression(Meta.id)
            )
            .from(DataSource.database(db!!))
            .where(
                whereExpression
            )
            .execute().allResults()
        return rs.firstOrNull()?.getString("id")
    }

    fun queryDBByType(type: String, orderBy: String): ResultSet {
        replicationWait()
        return QueryBuilder
            .select(
                SelectResult.all()
            )
            .from(DataSource.database(db!!))
            .where(
                Expression.property("type")
                    .equalTo(Expression.string(type))
            )
            .orderBy(Ordering.property(orderBy))
            .execute()
    }

    suspend fun queryClaims(): ArrayList<ClaimGrid> {
        return withContext(Dispatchers.IO) {
            Log.i(TAG, "Begin Claim Query")
            val claims = arrayListOf<ClaimGrid>()
            try {
                val query = QueryBuilder
                    .select(
                        SelectResult.expression(Expression.property("claim_id").from("claim")),
                        SelectResult.expression(Expression.property("name").from("customer")),
                        SelectResult.expression(Expression.property("phone").from("customer")),
                        SelectResult.expression(Expression.property("claim_amount").from("claim")),
                        SelectResult.expression(Expression.property("claim_status").from("claim")),
                    )
                    .from(DataSource.database(db!!).`as`("claim"))
                    .join(
                        Join.join(DataSource.database(db!!).`as`("customer"))
                            .on(
                                Expression.property("customer_id").from("claim")
                                    .equalTo(Expression.property("customer_id").from("customer"))
                            )
                    )
                    .where(
                        Expression.property("type").from("claim").equalTo(Expression.string("claim"))
                            .and(
                                Expression.property("type").from("customer").equalTo(Expression.string("customer"))
                            )
                    )
                    .orderBy(Ordering.expression(Expression.property("claim_id").from("claim")))
                query.execute().allResults().forEach { item ->
                    val gson = Gson()
                    val json = item.toJSON()
                    val claim = gson.fromJson(json, ClaimGrid::class.java)
                    claims.add(claim)
                }
            } catch (e: Exception){
                Log.e(e.message, e.stackTraceToString())
            }
            return@withContext claims
        }
    }

    fun getDocument(documentId: String): MutableDocument {
        return db!!.getDocument(documentId)!!.toMutable()
    }

    suspend fun getClaimById(documentId: String): Claim {
        var claim = Claim()
        return withContext(Dispatchers.IO) {
            try {
                db?.let { database ->
                    val doc = database.getDocument(documentId)
                    doc?.let { document ->
                        val json = document.toJSON()
                        json?.let {
                            claim = gson.fromJson(json, Claim::class.java)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(e.message, e.stackTraceToString())
            }
            return@withContext claim
        }
    }

    fun updateDocument(doc: MutableDocument) {
        db!!.save(doc)
    }

    fun dbCount(): String {
        return db!!.count.toString()
    }

    private fun replicationWait() {
        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            while (replicationProgress.value != "Completed") {
                delay(100)
            }
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

object ReplicationStatus {
    const val STOPPED = "Stopped"
    const val OFFLINE = "Offline"
    const val IDlE = "Idle"
    const val BUSY = "Busy"
    const val CONNECTING = "Connecting"
    const val UNINITIALIZED = "Not Initialized"
}
