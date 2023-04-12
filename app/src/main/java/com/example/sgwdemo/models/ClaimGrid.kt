package com.example.sgwdemo.models

import com.google.gson.annotations.SerializedName

class ClaimGrid(
    @SerializedName("claim_id") val claimId: String,
    @SerializedName("name") val customerName: String,
    @SerializedName("phone") val customerPhone: String,
    @SerializedName("claim_amount") val claimAmount: Float,
    @SerializedName("claim_status") val claimStatus: Int
    )
