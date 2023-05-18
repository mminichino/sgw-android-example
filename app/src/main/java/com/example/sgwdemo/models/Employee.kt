package com.example.sgwdemo.models

import com.google.gson.annotations.SerializedName

data class EmployeeTotal(
    @SerializedName("total") var total: Int
) {
    constructor() : this(
        0
    )
}

data class EmployeeDao(
    @SerializedName("employees") var item: Employee
    )

data class Employee (
    @SerializedName("record_id") val recordId: String,
    @SerializedName("location_id") val storeId: String,
    @SerializedName("employee_id") val employeeId: String,
    @SerializedName("name") val name: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("email") val email: String,
    @SerializedName("email_verified") val emailVerified: String,
    @SerializedName("first_name") val firstName: String,
    @SerializedName("last_name") val lastName: String,
    @SerializedName("address") val address: String,
    @SerializedName("city") val city: String,
    @SerializedName("state") val state: String,
    @SerializedName("zip_code") val zipCode: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("date_of_birth") val dateOfBirth: String,
    @SerializedName("password") val password: String,
    @SerializedName("rate") val rate: Float
) {
    constructor() : this(
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
        "",
        "",
        "",
        0.00F
    )
}
