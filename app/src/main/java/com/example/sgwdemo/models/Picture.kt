package com.example.sgwdemo.models

import android.graphics.Bitmap
import com.google.gson.annotations.SerializedName

data class Picture (
    @SerializedName("id") val metaId: String,
    @SerializedName("record_id") val recordId: Int,
    @SerializedName("date") val date: String
)

data class PictureList (
    val bitmap: Bitmap,
    val date: String
)
