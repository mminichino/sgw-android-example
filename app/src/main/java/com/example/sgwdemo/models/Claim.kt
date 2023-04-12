package com.example.sgwdemo.models

import com.google.gson.annotations.SerializedName

data class Claim (
    @SerializedName("adjuster_id") val adjusterId: Int,
    @SerializedName("claim_amount") val claimAmount: Float,
    @SerializedName("claim_date") val claimDate: String,
    @SerializedName("claim_id") val claimId: String,
    @SerializedName("claim_paid") val claimPaid: Boolean,
    @SerializedName("claim_status") val claimStatus: Int,
    @SerializedName("customer_id") val customerId: String,
    @SerializedName("record_id") val recordId: Int,
    @SerializedName("region") val region: String,
    @SerializedName("type") val type: String
    ) {
    constructor() : this(
        0,
        0.0F,
        "",
        "",
        false,
        0,
        "",
        0,
        "",
        "null"
    )
}
