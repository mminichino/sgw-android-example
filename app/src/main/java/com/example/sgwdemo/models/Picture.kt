package com.example.sgwdemo.models

import com.google.gson.annotations.SerializedName

data class Picture (
    @SerializedName("id") val metaId: String,
    @SerializedName("record_id") val recordId: Int
)
