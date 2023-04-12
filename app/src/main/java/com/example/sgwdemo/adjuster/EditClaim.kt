package com.example.sgwdemo.adjuster

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.sgwdemo.R
import com.example.sgwdemo.cbdb.CouchbaseConnect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*


class EditClaimActivity : AppCompatActivity() {

    private var TAG = "EditClaim"
    private var cntx: Context = this
    var claimAmountInput: EditText? = null
    var claimId: String? = null
    var documentId: String? = null
    var claimStatus: Int = 0
    var claimIdView: TextView? = null
    var claimDateView: TextView? = null
    var claimPaidView: TextView? = null
    var spinner: Spinner? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_claim)
        val claimStatusList = arrayOf<String>("Open", "Closed")
        val db: CouchbaseConnect = CouchbaseConnect.getInstance(cntx)
        claimId = intent.getStringExtra("ClaimId")
        val scope = CoroutineScope(Dispatchers.Default)

        scope.launch {
            val claim = db.getClaimById("claim::${claimId}")

            withContext(Dispatchers.Main) {

                claimIdView = findViewById(R.id.claimId)
                claimAmountInput = findViewById(R.id.claimAmount)
                claimDateView = findViewById(R.id.claimDate)
                claimPaidView = findViewById(R.id.claimPaid)

                claimIdView!!.text = claim.claimId
                claimAmountInput!!.setText(claim.claimAmount.toString())

                val dateString = claim.claimDate
                val readFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US)
                val writeFormat = SimpleDateFormat("M/d/yy", Locale.US)
                val date = readFormat.parse(dateString)
                claimDateView!!.text = writeFormat.format(date!!)

                val claimPaid: Boolean = claim.claimPaid
                if (claimPaid) {
                    "Paid".also { claimPaidView!!.text = it }
                } else {
                    "Not Paid".also { claimPaidView!!.text = it }
                }

                claimStatus = claim.claimStatus

                spinner = findViewById(R.id.claimStatus)
                if (spinner != null) {
                    val adapter = ArrayAdapter(
                        cntx,
                        R.layout.demo_spinner, claimStatusList
                    )
                    spinner!!.adapter = adapter
                    spinner!!.setSelection(claimStatus)

                    spinner!!.onItemSelectedListener = object :
                        AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(
                            parent: AdapterView<*>, view: View, position: Int, id: Long
                        ) {
                            claimStatus = position
                        }

                        override fun onNothingSelected(parent: AdapterView<*>) {
                            claimStatus = claim.claimStatus
                        }
                    }
                }
            }
        }
    }

    fun onSaveTapped(view: View?) {
        val db: CouchbaseConnect = CouchbaseConnect.getInstance(cntx)
        documentId = "claim::${claimId}"
        val claimAmount = claimAmountInput!!.text.toString().toFloat()

        val mutableDoc = db.getDocument(documentId.toString())
            .setFloat("claim_amount", claimAmount)
            .setInt("claim_status", claimStatus)
        db.updateDocument(mutableDoc)

        val intent = Intent(cntx, AdjusterMainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
    }
}
