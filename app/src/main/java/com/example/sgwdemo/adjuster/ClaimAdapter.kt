package com.example.sgwdemo.adjuster

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.example.sgwdemo.R
import com.example.sgwdemo.models.ClaimGrid

class ClaimAdapter(context: Context, private val arrayList: ArrayList<ClaimGrid>) : BaseAdapter() {
    private val inflater: LayoutInflater
            = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private lateinit var claimId: TextView
    private lateinit var customer: TextView
    private lateinit var phone: TextView
    private lateinit var amount: TextView
    private lateinit var status: TextView
    override fun getCount(): Int {
        return arrayList.size
    }
    override fun getItem(position: Int): Any {
        return arrayList[position]
    }
    override fun getItemId(position: Int): Long {
        return position.toLong()
    }
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
        val rowView = convertView ?: inflater.inflate(R.layout.claim_row, parent, false)
        claimId = rowView.findViewById(R.id.rowClaimId)
        customer = rowView.findViewById(R.id.rowCustomer)
        phone = rowView.findViewById(R.id.rowPhone)
        amount = rowView.findViewById(R.id.rowAmount)
        status = rowView.findViewById(R.id.rowStatus)

        val claim = getItem(position) as ClaimGrid

        claimId.text = claim.claimId
        customer.text = claim.customerName
        phone.text = claim.customerPhone
        amount.text = claim.claimAmount.toString()
        status.text = claim.claimStatus.toString()

        return rowView
    }
}
