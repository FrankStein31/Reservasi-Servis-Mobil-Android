package com.android.reservasiservismobilandroid

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.reservasiservismobilandroid.api.RetrofitClient
import com.android.reservasiservismobilandroid.databinding.ActivityReservationsBinding
import com.android.reservasiservismobilandroid.databinding.ItemReservationBinding
import com.android.reservasiservismobilandroid.utils.SharedPrefs
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Locale

class ReservationsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityReservationsBinding
    private lateinit var adapter: ReservationAdapter
    private lateinit var sharedPrefs: SharedPrefs
    private var reservations = mutableListOf<Map<String, Any>>()

    private val newReservationLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            loadReservations()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReservationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPrefs = SharedPrefs(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupRecyclerView()
        loadReservations()

        binding.fabNewReservation.setOnClickListener {
            newReservationLauncher.launch(Intent(this, ReservationFormActivity::class.java))
        }
    }

    private fun setupRecyclerView() {
        adapter = ReservationAdapter(
            reservations,
            onViewDetailClick = { reservation ->
                val intent = Intent(this, ReservationDetailActivity::class.java)
                intent.putExtra("reservation_id", (reservation["id"] as Double).toInt())
                intent.putExtra("reservation_date", reservation["reservation_date"].toString())
                intent.putExtra("reservation_time", reservation["reservation_time"].toString())
                intent.putExtra("vehicle_name", reservation["vehicle_name"].toString())
                intent.putExtra("plate_number", reservation["plate_number"].toString())
                intent.putExtra("package_name", reservation["package_name"].toString())
                intent.putExtra("complaint", reservation["vehicle_complaint"].toString())
                intent.putExtra("status", reservation["service_status"]?.toString() ?: "")
                newReservationLauncher.launch(intent)
            },
            onCancelClick = { reservation ->
                showCancelConfirmationDialog(reservation)
            }
        )
        binding.rvReservations.apply {
            layoutManager = LinearLayoutManager(this@ReservationsActivity)
            adapter = this@ReservationsActivity.adapter
        }
    }

    private fun loadReservations() {
        val customerId = sharedPrefs.getUserId()

        RetrofitClient.apiService.getReservations(customerId).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                if (response.isSuccessful) {
                    val responseData = response.body()
                    if (responseData?.get("status") == "success") {
                        val data = responseData["data"] as? List<*>
                        if (data != null) {
                            reservations.clear()
                            reservations.addAll(data.filterIsInstance<Map<String, Any>>())
                            adapter.notifyDataSetChanged()
                        }
                    } else {
                        Toast.makeText(this@ReservationsActivity, "Gagal memuat data reservasi", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@ReservationsActivity, "Gagal memuat data reservasi", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                Toast.makeText(this@ReservationsActivity, "Terjadi kesalahan: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showCancelConfirmationDialog(reservation: Map<String, Any>) {
        AlertDialog.Builder(this)
            .setTitle("Batalkan Reservasi")
            .setMessage("Apakah Anda yakin ingin membatalkan reservasi ini?")
            .setPositiveButton("Ya") { _, _ ->
                cancelReservation(reservation)
            }
            .setNegativeButton("Tidak", null)
            .show()
    }

    private fun cancelReservation(reservation: Map<String, Any>) {
        val reservationId = (reservation["id"] as Double).toInt()
        val customerId = sharedPrefs.getUserId()

        val data = mapOf(
            "id" to reservationId.toString(),
            "customer_id" to customerId.toString()
        )

        RetrofitClient.apiService.deleteReservation(data).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                if (response.isSuccessful) {
                    val responseData = response.body()
                    if (responseData?.get("status") == "success") {
                        Toast.makeText(this@ReservationsActivity, "Reservasi berhasil dibatalkan", Toast.LENGTH_SHORT).show()
                        loadReservations()
                    } else {
                        Toast.makeText(this@ReservationsActivity, responseData?.get("message") as? String 
                            ?: "Gagal membatalkan reservasi", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@ReservationsActivity, "Gagal membatalkan reservasi", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                Toast.makeText(this@ReservationsActivity, "Terjadi kesalahan: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        loadReservations()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private inner class ReservationAdapter(
        private val reservations: List<Map<String, Any>>,
        private val onViewDetailClick: (Map<String, Any>) -> Unit,
        private val onCancelClick: (Map<String, Any>) -> Unit
    ) : RecyclerView.Adapter<ReservationAdapter.ReservationViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReservationViewHolder {
            val binding = ItemReservationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ReservationViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ReservationViewHolder, position: Int) {
            holder.bind(reservations[position])
        }

        override fun getItemCount() = reservations.size

        inner class ReservationViewHolder(private val binding: ItemReservationBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(reservation: Map<String, Any>) {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                val displayFormat = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault())

                val date = reservation["reservation_date"]?.toString() ?: ""
                val time = reservation["reservation_time"]?.toString() ?: ""
                
                val displayDate = try {
                    val dateObj = dateFormat.parse(date)
                    val timeObj = timeFormat.parse(time)
                    val combinedDate = dateObj?.time?.plus((timeObj?.time ?: 0) % (24 * 60 * 60 * 1000))
                    if (combinedDate != null) displayFormat.format(combinedDate) else "$date $time"
                } catch (e: Exception) {
                    "$date $time"
                }

                binding.apply {
                    tvServiceDate.text = displayDate
                    tvVehicle.text = "Kendaraan: ${reservation["vehicle_name"] ?: "-"} (${reservation["plate_number"] ?: "-"})"
                    tvPackage.text = "Paket: ${reservation["package_name"] ?: "-"}"
                    
                    val serviceStatus = when(reservation["service_status"]) {
                        "Pending" -> "Menunggu"
                        "Process" -> "Sedang Diproses"
                        "Selesai" -> "Servis Selesai || Belum Bayar"
                        "Finish" -> "Selesai || Lunas"
                        else -> "Belum Diproses"
                    }
                    tvStatus.text = "Status: $serviceStatus"

                    btnViewDetails.setOnClickListener { onViewDetailClick(reservation) }
                    btnCancel.setOnClickListener { onCancelClick(reservation) }
                    
                    // Disable cancel button if service is already in process or finished
                    btnCancel.isEnabled = serviceStatus == "Menunggu" || serviceStatus == "Belum Diproses"
                }
            }
        }
    }
} 