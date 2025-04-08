package com.android.reservasiservismobilandroid.model

data class Customer(
    val id: Int,
    val name: String,
    val username: String,
    val email: String,
    val password: String,
    val gender: String,
    val phone: String?,
    val address: String?,
    val photo: String?
) 