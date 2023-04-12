package com.example.sgwdemo.models

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class ClaimDao(var item: Claim)

@Keep
@Serializable
data class Claim (
    val adjusterId: Int,
    val claimAmount: Float,
    val claimData: String,
    val claimId: String,
    val claimPaid: Boolean,
    val claimStatus: Int,
    val customerId: String,
    val recordId: Int,
    val region: String,
    val type: String
    )
