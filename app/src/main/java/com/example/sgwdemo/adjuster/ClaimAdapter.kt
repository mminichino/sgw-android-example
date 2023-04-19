package com.example.sgwdemo.adjuster

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.example.sgwdemo.R
import com.example.sgwdemo.models.ClaimGrid

class ClaimAdapter : BaseAdapter() {
    private lateinit var arrayList: ArrayList<ClaimGrid>
    private lateinit var inflater: LayoutInflater
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

    public fun claimListAdapter(context: Context, initialList: ArrayList<ClaimGrid>) {
        arrayList = initialList
        inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    }

    public fun updateListAdapter(newList: ArrayList<ClaimGrid>) {
        arrayList = newList
        notifyDataSetChanged()
    }

    private fun convertStatusId(id: Int) : String {
        return if (id == 0) {
            "Open"
        } else {
            "Complete"
        }
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
        amount.text = String.format("$ %.2f", claim.claimAmount)
        status.text = convertStatusId(claim.claimStatus)

        return rowView
    }
}
