package com.example.sgwdemo.adjuster

import android.app.AlertDialog
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
import java.util.regex.Matcher
import java.util.regex.Pattern


class EditClaimActivity : AppCompatActivity() {

    private var TAG = "EditClaim"
    private var cntx: Context = this
    var claimAmountInput: EditText? = null
    var claimId: String? = null
    var adjusterId: String? = null
    var userIdValue: String? = null
    var regionValue: String? = null
    var documentId: String? = null
    var claimStatus: Int = 0
    var claimIdView: TextView? = null
    var claimDateView: TextView? = null
    var claimPaidView: TextView? = null
    var claimAdjusterView: TextView? = null
    var spinner: Spinner? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_claim)
        val claimStatusList = arrayOf<String>("Open", "Closed")
        val db: CouchbaseConnect = CouchbaseConnect.getInstance(cntx)
        claimId = intent.getStringExtra("ClaimId")
        adjusterId = intent.getStringExtra("AdjusterId")
        userIdValue = intent.getStringExtra("UserName")
        regionValue = intent.getStringExtra("Region")
        val scope = CoroutineScope(Dispatchers.Default)

        scope.launch {
            val claim = db.getClaimById("claim::${claimId}")

            withContext(Dispatchers.Main) {

                claimIdView = findViewById(R.id.claimId)
                claimAmountInput = findViewById(R.id.claimAmount)
                claimDateView = findViewById(R.id.claimDate)
                claimPaidView = findViewById(R.id.claimPaid)
                claimAdjusterView = findViewById(R.id.claimAdjuster)

                claimIdView!!.text = claim.claimId
                claimAmountInput!!.setText(String.format("%.2f", claim.claimAmount))
                if (claim.adjusterId == 0 ) {
                    claimAdjusterView!!.text = resources.getString(R.string.noneText)
                } else {
                    val adjuster = db.getAdjusterById(claim.adjusterId.toString())
                    claimAdjusterView!!.text =
                        String.format("%s %s", adjuster.firstName, adjuster.lastName)
                }

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
        val claimAmountString = claimAmountInput!!.text

        val regex = "^(\\d+)(\\.\\d{2})?$"
        val p: Pattern = Pattern.compile(regex)
        val m: Matcher = p.matcher(claimAmountString)

        if (!m.matches()) {
            showMessageDialog("Invalid Amount",
                "Please provide a valid dollar amount")
            return
        }

        val claimAmount = claimAmountString.toString().toFloat()
        val mutableDoc = db.getDocument(documentId.toString())
            .setFloat("claim_amount", claimAmount)
            .setInt("claim_status", claimStatus)
            .setInt("adjuster_id", adjusterId!!.toInt())
        db.updateDocument(mutableDoc)

        returnToMainView()
    }

    fun onPhotoTapped(view: View?) {
        val intent = Intent(cntx, EditPhotos::class.java)
        intent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK
                    or Intent.FLAG_ACTIVITY_CLEAR_TASK
        )
        intent.putExtra("ClaimId", claimId)
        intent.putExtra("AdjusterId", adjusterId)
        intent.putExtra("Region", regionValue)
        intent.putExtra("UserName", userIdValue)
        startActivity(intent)
    }

    fun onCancelTapped(view: View?) {
        returnToMainView()
    }

    private fun returnToMainView() {
        val intent = Intent(cntx, AdjusterMainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        intent.putExtra("Region", regionValue)
        intent.putExtra("UserName", userIdValue)
        startActivity(intent)
    }

    private fun showMessageDialog(title: String, message: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setPositiveButton("Ok") { dialog, which ->
            Toast.makeText(
                applicationContext,
                "Ok", Toast.LENGTH_SHORT
            ).show()
        }
        builder.show()
    }
}
