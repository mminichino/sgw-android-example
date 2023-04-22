package com.example.sgwdemo.cbdb

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.couchbase.lite.*
import com.example.sgwdemo.models.ClaimGrid
import com.example.sgwdemo.models.Claim
import com.google.gson.Gson
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.couchbase.lite.Function
import com.example.sgwdemo.models.Adjuster
import com.example.sgwdemo.models.ClaimTotal
import com.example.sgwdemo.models.Employee
import com.example.sgwdemo.models.EmployeeDao
import com.example.sgwdemo.models.Picture
import com.example.sgwdemo.models.PictureList
import kotlinx.coroutines.*
import kotlin.math.pow
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

class DocNotFoundException(message: String) : Exception(message)

class CouchbaseConnect(context: Context) {

    private var TAG = "CouchbaseConnect"
    private var cntx: Context = context

    var employeesDatabase: Database? = null
    var adjusterDatabase: Database? = null

    var employeesDatabaseOpen: Boolean = false
    var adjusterDatabaseOpen: Boolean = false

    private val employeeIndexes = listOf(
        "store_id",
        "employee_id",
        "user_id")
    private val adjusterIndexes = listOf(
        "claim_id",
        "customer_id",
        "adjuster_id",
        "region",
        "type",
        "claim_status",
        "claim_amount")

    private val gson = Gson()
    var db: Database? = null
    var replicator: Replicator? = null
    var listenerToken: ListenerToken? = null
    private val replicationStatus: MutableState<String> = mutableStateOf("")
    private val replicationProgress: MutableState<String> = mutableStateOf("Not Started")
    private val retryCount: Int = 10

    companion object : CouchbaseConnectHolder<CouchbaseConnect, Context>(::CouchbaseConnect)

    init {
        replicationStatus.value = ReplicationStatus.UNINITIALIZED
        CouchbaseLite.init(cntx)
    }

    fun openDatabase(database: String, gateway: String, username: String, session: String, cookie: String) {
        val filteredName = (username.filterNot { it.isWhitespace() }).lowercase()
        val databaseFileName = filteredName.plus("_").plus(database)
        Log.i(TAG, "Database Open -> user $username db $database")

        val cfg = DatabaseConfigurationFactory.create(cntx.filesDir.toString())

        try {
            when (database) {
                DatabaseType.EMPLOYEE -> {
                    employeesDatabase = Database(databaseFileName, cfg)
                    createIndexes(DatabaseType.EMPLOYEE)
                    db = employeesDatabase
                    employeesDatabaseOpen = true
                }
                DatabaseType.ADJUSTER -> {
                    adjusterDatabase = Database(databaseFileName, cfg)
                    createIndexes(DatabaseType.ADJUSTER)
                    db = adjusterDatabase
                    adjusterDatabaseOpen = true
                }
            }
            syncDatabase(database, gateway, session, cookie)
        } catch (e: CouchbaseLiteException) {
            e.printStackTrace()
        }
    }

    private fun createIndexes(databaseType: String) {
        var database: Database? = null
        var indexes: Iterator<String> = iterator {  }
        try {
            when (databaseType) {
                DatabaseType.EMPLOYEE -> {
                    database = employeesDatabase
                    indexes = employeeIndexes.listIterator()
                }
                DatabaseType.ADJUSTER -> {
                    database = adjusterDatabase
                    indexes = adjusterIndexes.listIterator()
                }
            }
            database?.let {
                while (indexes.hasNext()) {
                    val indexField = indexes.next()
                    val indexName = "idx_${indexField}"
                    Log.i(TAG, "Processing Index $indexName")
                    if (!it.indexes.contains(indexName)) {
                        it.createIndex(
                            indexName, IndexBuilder.valueIndex(
                                ValueIndexItem.property(indexField)
                            )
                        )
                    }
                }
            }
        } catch (err: Exception){
            Log.e(err.message, err.stackTraceToString())
        }
    }

    private fun syncDatabase(database: String, gateway: String, session: String, cookie: String) {
        val connectString = "ws://$gateway:4984/$database"
        Log.i(TAG, "Sync init -> $connectString")
        Log.i(TAG, "Sync Session Cookie -> $cookie")

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
        replicationWait()
    }

    fun isDbOpen(database: String): Boolean {
        return when (database) {
            DatabaseType.EMPLOYEE -> {
                employeesDatabaseOpen
            }
            DatabaseType.ADJUSTER -> {
                adjusterDatabaseOpen
            }
            else -> {
                false
            }
        }
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
        val claims = arrayListOf<ClaimGrid>()
        Log.i(TAG, "Begin Claim Query")
        retryBlock {
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
           if (claims.size == 0) throw DocNotFoundException("No claim records found")
        }
        return claims
    }

    suspend fun queryEmployees(): ArrayList<Employee> {
        return withContext(Dispatchers.IO) {
            Log.i(TAG, "Begin Employee Query")
            val employees = arrayListOf<Employee>()
            try {
                val query = QueryBuilder
                    .select(
                        SelectResult.all()
                    )
                    .from(DataSource.database(db!!).`as`("employees"))
                    .orderBy(Ordering.expression(Expression.property("employee_id").from("employees")))
                query.execute().allResults().forEach { item ->
                    val json = item.toJSON()
                    val employee = gson.fromJson(json, EmployeeDao::class.java).item
                    employees.add(employee)
                }
            } catch (e: Exception){
                Log.e(e.message, e.stackTraceToString())
            }
            return@withContext employees
        }
    }

    fun getDocument(documentId: String): MutableDocument {
        return db!!.getDocument(documentId)!!.toMutable()
    }

    fun getImage(documentId: String): PictureList? {
        var picture: PictureList? = null
        db?.let { database ->
            val doc = database.getDocument(documentId)
            val bytes = doc?.getBlob("image")?.content
            val date = doc?.getString("date")
            bytes?.let {
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                picture = PictureList(bitmap, date!!)
            }
        }
        return picture
    }

    fun documentExists(documentId: String): Boolean {
        return db!!.getDocument(documentId) != null
    }

    suspend fun getMutableDocById(documentId: String): MutableDocument {
        var mutableDoc = MutableDocument()
        return withContext(Dispatchers.IO) {
            try {
                db?.let { database ->
                    val doc = database.getDocument(documentId)
                    doc?.let { document ->
                        mutableDoc = document.toMutable()
                    }
                }
            } catch (e: Exception) {
                Log.e(e.message, e.stackTraceToString())
            }
            return@withContext mutableDoc
        }
    }

    private suspend fun <T> retryBlock(retryCount: Int = 10, waitFactor: Long = 100, block: suspend () -> T): T {
        for (retry_number in 1..retryCount) {
            try {
                return block()
            } catch (e: DocNotFoundException) {
                Log.w(TAG, "Retry due to ${e.message}")
                val wait = waitFactor * 2.toDouble().pow(retry_number.toDouble()).toLong()
                delay(wait)
            }
        }
        return block()
    }

    suspend fun getClaimById(documentId: String): Claim {
        var claim = Claim()
        retryBlock {
            db?.let { database ->
                val doc = database.getDocument(documentId)
                doc?.let { document ->
                    val json = document.toJSON()
                    json?.let {
                        claim = gson.fromJson(json, Claim::class.java)
                    }
                }
            }
            if (claim.recordId == 0) throw DocNotFoundException("Doc $documentId not found")
        }
        return claim
    }

    suspend fun getEmployeeById(documentId: String): Employee {
        var employee = Employee()
        retryBlock {
            db?.let { database ->
                val doc = database.getDocument(documentId)
                doc?.let { document ->
                    val json = document.toJSON()
                    json?.let {
                        employee = gson.fromJson(json, Employee::class.java)
                    }
                }
            }
            if (employee.recordId == 0) throw DocNotFoundException("Doc $documentId not found")
        }
        return employee
    }

    suspend fun getAdjusterByUserName(userName: String): Adjuster {
        var adjuster = Adjuster()
        retryBlock {
            val query = QueryBuilder
                .select(
                    SelectResult.all()
                )
                .from(DataSource.database(db!!))
                .where(
                    Expression.property("type").equalTo(Expression.string("adjuster"))
                        .and(
                            Expression.property("user_id").equalTo(Expression.string(userName))
                        )
                )
            query.execute().allResults().forEach { item ->
                val json = item.getDictionary(0)?.toJSON()
                adjuster = gson.fromJson(json, Adjuster::class.java)
            }
            if (adjuster.recordId == 0) throw DocNotFoundException("Adjuster $userName not found")
        }
        return adjuster
    }

    suspend fun getAdjusterById(adjusterId: String): Adjuster {
        var adjuster = Adjuster()
        retryBlock {
            val query = QueryBuilder
                .select(
                    SelectResult.all()
                )
                .from(DataSource.database(db!!))
                .where(
                    Expression.property("type").equalTo(Expression.string("adjuster"))
                        .and(
                            Expression.property("employee_id").equalTo(Expression.string(adjusterId))
                        )
                )
            query.execute().allResults().forEach { item ->
                val json = item.getDictionary(0)?.toJSON()
                adjuster = gson.fromJson(json, Adjuster::class.java)
            }
            if (adjuster.recordId == 0) throw DocNotFoundException("Adjuster $adjusterId not found")
        }
        return adjuster
    }

    suspend fun getPictureIdByClaim(claimId: String): ArrayList<Picture> {
        val pictures = arrayListOf<Picture>()
        retryBlock {
            val query = QueryBuilder
                .select(
                    SelectResult.expression(Meta.id),
                    SelectResult.expression(Expression.property(("record_id"))),
                    SelectResult.expression(Expression.property(("date")))
                )
                .from(DataSource.database(db!!))
                .where(
                    Expression.property("type").equalTo(Expression.string("picture"))
                        .and(
                            Expression.property("claim_id").equalTo(Expression.string(claimId))
                        )
                )
                .orderBy(Ordering.expression(Expression.property("record_id")))
            query.execute().allResults().forEach { item ->
                val gson = Gson()
                val json = item.toJSON()
                val picture = gson.fromJson(json, Picture::class.java)
                pictures.add(picture)
            }
        }
        return pictures
    }

    suspend fun getClaimCounts(): ClaimTotal {
        var total = ClaimTotal()
        retryBlock {
            val query = QueryBuilder
                .select(
                    SelectResult.expression(Function.count(Expression.string("*"))).`as`("total")
                )
                .from(DataSource.database(db!!))
                .where(
                    Expression.property("type").equalTo(Expression.string("claim"))
                )
            query.execute().allResults().forEach { item ->
                val json = item.toJSON()
                total = gson.fromJson(json, ClaimTotal::class.java)
            }
        }
        return total
    }

    fun updateDocument(doc: MutableDocument) {
        db!!.save(doc)
    }

    fun dbCount(): String {
        return db!!.count.toString()
    }

    fun replicationWait() {
        val scope = CoroutineScope(Dispatchers.Default)
        var retry = retryCount
        scope.launch {
            while (replicationProgress.value != "Completed" && retry > 0) {
                Log.i(TAG, "Waiting for replication to complete")
                delay(100)
                retry -= 1
            }
        }
    }

    suspend fun waitForDocuments(maxDelay: Long, checkPeriod: Long) : Boolean{
        if(maxDelay < 0) return false
        if(dbCount().toInt() > 0) return true
        delay(checkPeriod)
        return waitForDocuments(maxDelay - checkPeriod, checkPeriod)
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

object ReplicationStatus {
    const val STOPPED = "Stopped"
    const val OFFLINE = "Offline"
    const val IDlE = "Idle"
    const val BUSY = "Busy"
    const val CONNECTING = "Connecting"
    const val UNINITIALIZED = "Not Initialized"
}

object DatabaseType {
    const val EMPLOYEE = "employees"
    const val ADJUSTER = "adjuster"
}
