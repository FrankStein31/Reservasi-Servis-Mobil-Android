package com.android.reservasiservismobilandroid.api

import com.android.reservasiservismobilandroid.model.Customer
import com.android.reservasiservismobilandroid.model.Vehicle
import retrofit2.Call
import retrofit2.http.*

interface ApiService {
    @POST("login.php")
    fun login(@Body loginRequest: Map<String, String>): Call<Map<String, Any>>

    @POST("register.php")
    fun register(@Body registerRequest: Map<String, String>): Call<Map<String, Any>>

    @GET("vehicles.php")
    fun getVehicles(@Query("customer_id") customerId: Int): Call<Map<String, Any>>

    @POST("vehicles.php")
    fun addVehicle(@Body vehicle: Map<String, String>): Call<Map<String, Any>>

    @PUT("vehicles.php")
    fun updateVehicle(@Body vehicle: Map<String, String>): Call<Map<String, Any>>

    @HTTP(method = "DELETE", path = "vehicles.php", hasBody = true)
    fun deleteVehicle(@Body deleteRequest: Map<String, String>): Call<Map<String, Any>>

    @GET("reservations.php")
    fun getReservations(@Query("customer_id") customerId: Int): Call<Map<String, Any>>

    @GET("reservations.php")
    fun getReservationDetail(@Query("id") reservationId: Int): Call<Map<String, Any>>

    @POST("reservations.php")
    fun createReservation(@Body reservation: Map<String, String>): Call<Map<String, Any>>

    @HTTP(method = "DELETE", path = "reservations.php", hasBody = true)
    fun deleteReservation(@Body deleteRequest: Map<String, String>): Call<Map<String, Any>>

    @GET("packages.php")
    fun getPackages(): Call<Map<String, Any>>

    @GET("packages.php")
    fun getPackageDetail(@Query("id") packageId: Int): Call<Map<String, Any>>

    @GET("payments.php")
    fun getPaymentStatus(@Query("customer_id") customerId: Int): Call<Map<String, Any>>

    @GET("payments.php")
    fun getPaymentDetail(@Query("service_id") serviceId: Int): Call<Map<String, Any>>

    @POST("payments.php")
    fun createPayment(@Body payment: Map<String, Any>): Call<Map<String, Any>>

    @GET("profile.php")
    fun getProfile(@Query("id") customerId: Int): Call<Map<String, Any>>

    @PUT("profile.php")
    fun updateProfile(@Body profile: Map<String, String>): Call<Map<String, Any>>

    @GET("services.php")
    fun getServiceDetail(@Query("id") serviceId: Int): Call<Map<String, Any>>
} 