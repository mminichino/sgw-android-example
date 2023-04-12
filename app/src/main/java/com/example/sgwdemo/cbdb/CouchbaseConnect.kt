package com.example.sgwdemo.cbdb

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.couchbase.lite.*
import com.example.sgwdemo.R
import com.example.sgwdemo.models.ClaimGrid
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
//import kotlinx.serialization.decodeFromString
//import kotlinx.serialization.json.Json
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

    private var TAG = "CBL-Demo"
    private var cntx: Context = context
    var db: Database? = null
    var replicator: Replicator? = null
    var listenerToken: ListenerToken? = null
    var connectString: String? = null
    var dbOpen: Boolean = false
    var replicatorBusy: Boolean = false
    var replicatorConnecting: Boolean = false
    val pref: SharedPreferences =
        context.getSharedPreferences("APP_SETTINGS", Context.MODE_PRIVATE)

    companion object : CouchbaseConnectHolder<CouchbaseConnect, Context>(::CouchbaseConnect)

    fun init() {
        val gatewayAddress = pref.getString(R.string.gatewayPropertyKey.toString(), "")
        val databaseName = pref.getString(R.string.databaseNameKey.toString(), "")

        connectString = "ws://$gatewayAddress:4984/$databaseName"
        Log.i(TAG, "DB init -> $connectString")

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
                    Log.i(TAG, "Item : $json")
                    val claim = gson.fromJson(json, ClaimGrid::class.java)
                    Log.i(TAG, "Found Claim: ${claim.claimId}")
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
