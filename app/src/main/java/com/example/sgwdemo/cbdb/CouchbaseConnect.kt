package com.example.sgwdemo.cbdb

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import com.couchbase.lite.*
import com.example.sgwdemo.models.ClaimGrid
import com.example.sgwdemo.models.Claim
import com.google.gson.Gson
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.couchbase.lite.Collection
import com.couchbase.lite.Function
import com.couchbase.lite.Document
import com.example.sgwdemo.models.Adjuster
import com.example.sgwdemo.models.ClaimTotal
import com.example.sgwdemo.models.Employee
import com.example.sgwdemo.models.EmployeeDao
import com.example.sgwdemo.models.EmployeeTotal
import com.example.sgwdemo.models.Location
import com.example.sgwdemo.models.Picture
import com.example.sgwdemo.models.PictureList
import com.example.sgwdemo.models.Timecard
import com.example.sgwdemo.models.TimecardDao
import com.example.sgwdemo.models.TimecardId
import kotlinx.coroutines.*
import java.lang.IllegalStateException
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

    var timecardDb: Database? = null
    var insuranceDb: Database? = null

    var timecardDbOpen: Boolean = false
    var insuranceDbOpen: Boolean = false

    private val timecardScope = "data"
    private val timecardCollections = hashMapOf(
        "employees" to listOf(
            "record_id",
            "location_id",
            "employee_id",
            "user_id"
        ),
        "locations" to listOf(
            "record_id",
            "location_id",
            "name"
        ),
        "timecards" to listOf(
            "record_id",
            "location_id",
            "employee_id"
        )
    )

    private val insuranceScope = "data"
    private val insuranceCollections = hashMapOf(
        "company" to listOf(
            "record_id",
            "region"
        ),
        "claims" to listOf(
            "record_id",
            "claim_id",
            "customer_id",
            "adjuster_id",
            "region"
        ),
        "customer" to listOf(
            "record_id",
            "customer_id",
            "user_id",
            "email",
            "region"
        ),
        "adjuster" to listOf(
            "record_id",
            "adjuster_id",
            "user_id",
            "email",
            "region"
        ),
        "picture" to listOf(
            "record_id",
            "claim_id",
            "date",
            "region"
        )
    )

    private val employeeIndexes = listOf(
        "record_id",
        "location_id",
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
    var employees: Collection? = null
    var locations: Collection? = null
    var timecards: Collection? = null
    var company: Collection? = null
    var claims: Collection? = null
    var customer: Collection? = null
    var adjuster: Collection? = null
    var picture: Collection? = null
    var scopeName: String = "_default"
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

        val cfg = DatabaseConfigurationFactory.newConfig(cntx.filesDir.toString())

        try {
            when (database) {
                DatabaseType.TIMECARD -> {
                    Log.i(TAG, "Creating database of type timecard")
                    scopeName = timecardScope
                    db = Database(
                        database,
                        DatabaseConfigurationFactory.newConfig()
                    )
                    timecardCollections.forEach { item ->
                        if (item.key == "employees") {
                            employees = db!!.createCollection("employees", timecardScope)
                            createIndexes(employees, item.value)
                        }
                        if (item.key == "locations") {
                            locations = db!!.createCollection("locations", timecardScope)
                            createIndexes(locations, item.value)
                        }
                        if (item.key == "timecards") {
                            timecards = db!!.createCollection("timecards", timecardScope)
                            createIndexes(timecards, item.value)
                        }
                    }
                    timecardDbOpen = true
                }
                DatabaseType.INSURANCE -> {
                    Log.i(TAG, "Creating database of type insurance")
                    scopeName = insuranceScope
                    db = Database(
                        database,
                        DatabaseConfigurationFactory.newConfig()
                    )
                    insuranceCollections.forEach { item ->
                        if (item.key == "company") {
                            company = db!!.createCollection("company", insuranceScope)
                            createIndexes(company, item.value)
                        }
                        if (item.key == "claims") {
                            claims = db!!.createCollection("claims", insuranceScope)
                            createIndexes(claims, item.value)
                        }
                        if (item.key == "customer") {
                            customer = db!!.createCollection("customer", insuranceScope)
                            createIndexes(customer, item.value)
                        }
                        if (item.key == "adjuster") {
                            adjuster = db!!.createCollection("adjuster", insuranceScope)
                            createIndexes(adjuster, item.value)
                        }
                        if (item.key == "picture") {
                            picture = db!!.createCollection("picture", insuranceScope)
                            createIndexes(picture, item.value)
                        }
                    }
                    insuranceDbOpen = true
                }
            }
            syncDatabase(database, gateway, session, cookie)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        db ?: throw IllegalStateException("Database open failed")
        db?.let { lite ->
            lite.scopes.forEach { scope ->
                Log.i(TAG, "Scope :: " + scope.name)
                scope.collections.forEach { collection ->
                    Log.i(TAG,"    Collection :: " + collection.name)
                }
            }
        }
    }

    private fun createIndexes(collection: Collection?, indexes: List<String>) {
        try {
            collection?.let {
                indexes.forEach { indexField ->
                    val indexName = "idx_${indexField}"
                    Log.i(TAG, "Processing Index $indexName")
                    if (!collection.indexes.contains(indexName)) {
                        collection.createIndex(
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

        db?.let { liteDatabase ->
            replicator = Replicator(
                ReplicatorConfigurationFactory.newConfig(
                    collections = mapOf(liteDatabase.getCollections(scopeName) to null),
                    target = URLEndpoint(URI(connectString)),
                    type = ReplicatorType.PUSH_AND_PULL,
                    authenticator = SessionAuthenticator(session, cookie),
                    continuous = true
                )
            )
        }
        replicator ?: throw IllegalStateException("Can not create replicator for database $database")

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
            DatabaseType.TIMECARD -> {
                timecardDbOpen
            }
            DatabaseType.INSURANCE -> {
                insuranceDbOpen
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
        val claimData = arrayListOf<ClaimGrid>()
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
                .from(DataSource.collection(claims!!).`as`("claim"))
                .join(
                    Join.join(DataSource.collection(customer!!).`as`("customer"))
                        .on(
                            Expression.property("customer_id").from("claim")
                                .equalTo(Expression.property("customer_id").from("customer"))
                        )
                )
                .orderBy(Ordering.expression(Expression.property("claim_id").from("claim")))
            query.execute().allResults().forEach { item ->
                val gson = Gson()
                val json = item.toJSON()
                val claim = gson.fromJson(json, ClaimGrid::class.java)
                claimData.add(claim)
            }
           if (claimData.size == 0) throw DocNotFoundException("No claim records found")
        }
        return claimData
    }

    suspend fun queryEmployees(): ArrayList<Employee> {
        return withContext(Dispatchers.IO) {
            Log.i(TAG, "Begin Employee Query")
            val employeeData = arrayListOf<Employee>()
            try {
                val query = QueryBuilder
                    .select(
                        SelectResult.all()
                    )
                    .from(DataSource.collection(employees!!).`as`("employees"))
                    .orderBy(Ordering.expression(Expression.property("employee_id").from("employees")))
                query.execute().allResults().forEach { item ->
                    val json = item.toJSON()
                    val employee = gson.fromJson(json, EmployeeDao::class.java).item
                    employeeData.add(employee)
                }
            } catch (e: Exception){
                Log.e(e.message, e.stackTraceToString())
            }
            return@withContext employeeData
        }
    }

    fun getDocument(collection: Collection, documentId: String): MutableDocument {
        return collection.getDocument(documentId)!!.toMutable()
    }

    fun removeDocument(collection: Collection, documentId: String) {
        val document = collection.getDocument(documentId)
        document?.let { doc ->
            collection.delete(doc)
        }
    }

    fun getImage(documentId: String): PictureList? {
        var pictureData: PictureList? = null
        picture?.let { pic ->
            val doc = pic.getDocument(documentId)
            val bytes = doc?.getBlob("image")?.content
            val date = doc?.getString("date")
            bytes?.let {
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                pictureData = PictureList(bitmap, date!!)
            }
        }
        return pictureData
    }

    fun documentExists(collection: Collection, documentId: String): Boolean {
        return collection.getDocument(documentId) != null
    }

    suspend fun getCompletedTimeCards(employeeId: String): ArrayList<Timecard> {
        val timeCardData = arrayListOf<Timecard>()
        retryBlock {
            val query = QueryBuilder
                .select(
                    SelectResult.all()
                )
                .from(DataSource.collection(timecards!!))
                .where(
                    Expression.property("employee_id").equalTo(Expression.string(employeeId))
                        .and(
                            Expression.property("status").equalTo(Expression.booleanValue(false))
                        )
                )
                .orderBy(Ordering.property("time_out").descending())
                .limit(Expression.intValue(10))
            query.execute().allResults().forEach { item ->
                val json = item.toJSON()
                val card = gson.fromJson(json, TimecardDao::class.java).item
                timeCardData.add(card)
            }
            if (timeCardData.size == 0) Log.i(TAG,"No timecards found for EID $employeeId")
        }
        return timeCardData
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
        var claimData = Claim()
        retryBlock {
            claims?.let { claim ->
                val doc = claim.getDocument(documentId)
                doc?.let { document ->
                    val json = document.toJSON()
                    json?.let {
                        claimData = gson.fromJson(json, Claim::class.java)
                    }
                }
            }
            if (claimData.claimId.isEmpty()) throw DocNotFoundException("Doc $documentId not found")
        }
        return claimData
    }

    suspend fun getEmployeeByUserName(userName: String): Employee {
        var employee = Employee()
        retryBlock {
            val query = QueryBuilder
                .select(
                    SelectResult.all()
                )
                .from(DataSource.collection(employees!!))
                .where(
                    Expression.property("user_id").equalTo(Expression.string(userName))
                )
            query.execute().allResults().forEach { item ->
                val json = item.getDictionary(0)?.toJSON()
                employee = gson.fromJson(json, Employee::class.java)
            }
            if (employee.employeeId.isEmpty()) throw DocNotFoundException("Employee $userName not found")
        }
        return employee
    }

    suspend fun getLocationById(documentId: String): Location {
        var locationData = Location()
        retryBlock {
            locations?.let { loc ->
                val doc = loc.getDocument(documentId)
                doc?.let { document ->
                    val json = document.toJSON()
                    json?.let {
                        locationData = gson.fromJson(json, Location::class.java)
                    }
                }
            }
            if (locationData.locationId.isEmpty()) throw DocNotFoundException("Doc $documentId not found")
        }
        return locationData
    }

    suspend fun getAdjusterByUserName(userName: String): Adjuster {
        var adjusterData = Adjuster()
        retryBlock {
            val query = QueryBuilder
                .select(
                    SelectResult.all()
                )
                .from(DataSource.collection(adjuster!!))
                .where(
                    Expression.property("user_id").equalTo(Expression.string(userName))
                )
            query.execute().allResults().forEach { item ->
                val json = item.getDictionary(0)?.toJSON()
                adjusterData = gson.fromJson(json, Adjuster::class.java)
            }
            if (adjusterData.userId.isEmpty()) throw DocNotFoundException("Adjuster $userName not found")
        }
        return adjusterData
    }

    suspend fun getAdjusterById(adjusterId: String): Adjuster {
        var adjusterData = Adjuster()
        retryBlock {
            val query = QueryBuilder
                .select(
                    SelectResult.all()
                )
                .from(DataSource.collection(adjuster!!))
                .where(
                    Expression.property("adjuster_id").equalTo(Expression.string(adjusterId))
                )
            query.execute().allResults().forEach { item ->
                val json = item.getDictionary(0)?.toJSON()
                adjusterData = gson.fromJson(json, Adjuster::class.java)
            }
            if (adjusterData.userId.isEmpty()) throw DocNotFoundException("Adjuster $adjusterId not found")
        }
        return adjusterData
    }

    suspend fun getPictureIdByClaim(claimId: String): ArrayList<Picture> {
        val pictureData = arrayListOf<Picture>()
        retryBlock {
            val query = QueryBuilder
                .select(
                    SelectResult.expression(Meta.id),
                    SelectResult.expression(Expression.property(("record_id"))),
                    SelectResult.expression(Expression.property(("date")))
                )
                .from(DataSource.collection(picture!!))
                .where(
                    Expression.property("claim_id").equalTo(Expression.string(claimId))
                )
                .orderBy(Ordering.expression(Expression.property("record_id")))
            query.execute().allResults().forEach { item ->
                val gson = Gson()
                val json = item.toJSON()
                val picture = gson.fromJson(json, Picture::class.java)
                pictureData.add(picture)
            }
        }
        return pictureData
    }

    suspend fun getClaimCounts(): ClaimTotal {
        var total = ClaimTotal()
        retryBlock {
            claims?.let { claim ->
                val query = QueryBuilder
                    .select(
                        SelectResult.expression(Function.count(Expression.string("*"))).`as`("total")
                    )
                    .from(DataSource.collection(claim))
                query.execute().allResults().forEach { item ->
                    val json = item.toJSON()
                    total = gson.fromJson(json, ClaimTotal::class.java)
                }
            }
        }
        return total
    }

    suspend fun getEmployeeCounts(): EmployeeTotal {
        var total = EmployeeTotal()
        retryBlock {
            employees?.let { employee ->
                val query = QueryBuilder
                    .select(
                        SelectResult.expression(Function.count(Expression.string("*"))).`as`("total")
                    )
                    .from(DataSource.collection(employee))
                query.execute().allResults().forEach { item ->
                    val json = item.toJSON()
                    total = gson.fromJson(json, EmployeeTotal::class.java)
                }
            }
        }
        return total
    }

    fun updateDocument(collection: Collection, doc: MutableDocument) {
        collection.save(doc)
    }

    fun dbCount(collection: Collection): String {
        return collection.count.toString()
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

    fun closeDatabase() {
        try {
            db?.let { database ->
                stopSync()
                database.close()
                db = null
            }
        } catch (e: CouchbaseLiteException) {
            e.printStackTrace()
        }
    }

    private fun stopSync() {
        listenerToken?.let {
            replicator?.stop()
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
    const val TIMECARD = "timecard"
    const val INSURANCE = "insurance"
}
