package com.example.sgwdemo.main

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.example.sgwdemo.R
import com.example.sgwdemo.models.Employee

class EmployeeAdapter(context: Context, private val arrayList: ArrayList<Employee>) : BaseAdapter() {
    private val inflater: LayoutInflater
            = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private lateinit var employeeId: TextView
    private lateinit var employeeName: TextView
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
        val rowView = convertView ?: inflater.inflate(R.layout.employee_row, parent, false)
        employeeId = rowView.findViewById(R.id.rowEmployeeId)
        employeeName = rowView.findViewById(R.id.rowEmployeeName)

        val employee = getItem(position) as Employee

        employeeId.text = employee.employeeId
        employeeName.text = employee.name

        return rowView
    }
}
