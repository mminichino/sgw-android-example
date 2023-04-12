package com.example.sgwdemo.models

data class Customer (
        val recordId: Int,
        val customerId: String,
        val name: String,
        val userId: String,
        val email: String,
        val emailVerified: Boolean,
        val firstName: String,
        val lastName: String,
        val address: String,
        val city: String,
        val state: String,
        val zipCode: String,
        val phone: String,
        val dateOfBirth: String,
        val password: String,
        val region: String,
        val type: String
        )
