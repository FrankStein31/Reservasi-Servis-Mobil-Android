package com.android.reservasiservismobilandroid.model

data class Vehicle(
    val id: Int,
    val customerId: Int,
    val name: String,
    val brand: String,
    val year: String?,
    val plateNumber: String,
    val chassisNumber: String?
) 