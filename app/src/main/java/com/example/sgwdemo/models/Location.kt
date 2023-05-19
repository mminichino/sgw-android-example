package com.example.sgwdemo.models

import com.google.gson.annotations.SerializedName

data class Location (
    @SerializedName("record_id") val recordId: Int,
    @SerializedName("location_id") val locationId: String,
    @SerializedName("name") val name: String,
    @SerializedName("address") val address: String,
    @SerializedName("city") val city: String,
    @SerializedName("state") val state: String,
    @SerializedName("zip_code") val zipCode: String,
    @SerializedName("phone") val phone: String
) {
    constructor() : this(
        0,
        "",
        "",
        "",
        "",
        "",
        "",
        ""
    )
}
