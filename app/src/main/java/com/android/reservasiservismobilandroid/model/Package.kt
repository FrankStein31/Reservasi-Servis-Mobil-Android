package com.android.reservasiservismobilandroid.model

import com.google.gson.annotations.SerializedName

data class Package(
    val id: Int,
    val name: String,
    val description: String,
    val products: List<Product>,
    val price: Double
)

data class Product(
    val id: Int,
    val name: String,
    val price: Double
)

data class PackageResponse(
    val status: Boolean,
    val message: String,
    val data: Package
) 