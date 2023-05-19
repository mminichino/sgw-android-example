package com.example.sgwdemo.models

import com.google.gson.annotations.SerializedName

data class TimecardDao(
    @SerializedName("timecards") var item: Timecard
)

data class TimecardId (
    @SerializedName("id") val docId: String,
    @SerializedName("location_id") val locationId: String,
    @SerializedName("employee_id") val employeeId: String,
    @SerializedName("time_in") val timeIn: String,
) {
    constructor() : this(
        "",
        "",
        "",
        ""
    )
}

data class Timecard (
    @SerializedName("location_id") val locationId: String,
    @SerializedName("employee_id") val employeeId: String,
    @SerializedName("status") val status: Boolean,
    @SerializedName("time_in") val timeIn: String,
    @SerializedName("time_out") val timeOut: String,
    @SerializedName("duration") val duration: Int,
    @SerializedName("paid") val paid: Boolean
) {
    constructor() : this(
        "",
        "",
        false,
        "",
        "",
        0,
        false
    )
}
