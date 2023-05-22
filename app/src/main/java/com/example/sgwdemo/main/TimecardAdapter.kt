package com.example.sgwdemo.main

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.example.sgwdemo.R
import com.example.sgwdemo.models.Timecard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TimecardAdapter(context: Context, private val arrayList: ArrayList<Timecard>) : BaseAdapter() {
    private val inflater: LayoutInflater
            = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private lateinit var entryNumber: TextView
    private lateinit var entryText: TextView
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
        val rowView = convertView ?: inflater.inflate(R.layout.timecard_row, parent, false)
        entryNumber = rowView.findViewById(R.id.rowEntryNumber)
        entryText = rowView.findViewById(R.id.rowEntryText)

        val timecard = getItem(position) as Timecard

        val itemNumber = position + 1
        val timeFormat = SimpleDateFormat("M/d/yy h:mm a", Locale.US)
        val inTimeText = timeFormat.format(Date(timecard.timeIn.toLong() * 1000))
        val outTimeText = timeFormat.format(Date(timecard.timeOut.toLong() * 1000))
        entryNumber.text = itemNumber.toString()
        entryText.text = String.format("%s - %s", inTimeText, outTimeText)

        return rowView
    }
}
