package com.example.sgwdemo.cbdb

import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import android.widget.*
import com.couchbase.lite.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.net.URISyntaxException
import java.util.*
import java.util.zip.ZipInputStream


class CouchbaseConnect(base: Context?) : ContextWrapper(base) {

    private var TAG = "CBL-Demo"
    private var cntx: Context = this
    private var instance: CouchbaseConnect? = null
    var db: Database? = null
    var connectString: String? = null
    val props = Properties()
    val propfile = cntx.getAssets().open("config.properties")

    fun getSharedInstance(): CouchbaseConnect {
        if (instance == null) {
            instance = CouchbaseConnect(this)
        }
        return instance as CouchbaseConnect
    }

    fun init() {
        val connectStringBuilder = StringBuilder()
        props.load(propfile)
        connectStringBuilder.append("ws://")
        connectStringBuilder.append(props.getProperty("sgwhost"))
        connectStringBuilder.append(":4984/")
        connectStringBuilder.append(props.getProperty("database"))
        connectString = connectStringBuilder.toString()
        CouchbaseLite.init(cntx)
    }

    fun openDatabase() {
        val cfg = DatabaseConfigurationFactory.create()
        try {
            db = Database("employees", cfg)
        } catch (e: CouchbaseLiteException) {
            e.printStackTrace()
        }
    }
}
