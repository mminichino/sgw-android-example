package com.example.sgwdemo.models

import com.google.gson.annotations.SerializedName

data class Adjuster (
        @SerializedName("record_id") val recordId: Int,
        @SerializedName("employee_id") val employeeId: String,
        @SerializedName("division") val division: String,
        @SerializedName("user_id") val userId: String,
        @SerializedName("email") val email: String,
        @SerializedName("first_name") val firstName: String,
        @SerializedName("last_name") val lastName: String,
        @SerializedName("address") val address: String,
        @SerializedName("city") val city: String,
        @SerializedName("state") val state: String,
        @SerializedName("zip_code") val zipCode: String,
        @SerializedName("phone") val phone: String,
        @SerializedName("password") val password: String,
        @SerializedName("region") val region: String,
        @SerializedName("type") val type: String
        ) {
        constructor() : this(
                0,
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                ""
        )
}
