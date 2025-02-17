package com.android.reservasiservismobil.ui.barcode

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.android.reservasiservismobil.databinding.FragmentBarcodeBinding
import com.google.zxing.BarcodeFormat
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import java.util.Collections
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.content.Context
import com.android.reservasiservismobil.R

class BarcodeFragment : Fragment() {
    private var _binding: FragmentBarcodeBinding? = null
    private val binding get() = _binding!!
    private lateinit var barcodeViewModel: BarcodeViewModel
    private lateinit var barcodeScanner: DecoratedBarcodeView

    private val barcodeCallback = object : BarcodeCallback {
    override fun barcodeResult(result: BarcodeResult?) {
        result?.let { barcodeResult ->
            val content = barcodeResult.text
            try {
                val parts = content.split(":")
                if (parts.size == 2 && parts[0].trim() == "Total Harga") {
                    val rawAmount = parts[1].trim()
                    val amount = parseAmount(rawAmount)
                    amount?.let {
                        binding.textQrAmount.text = "Rp." + it.toInt().toString()
                        barcodeScanner.pause()
                        Log.e("Nominal", "Value : $amount")
                        Log.e("Nominal", "Raw Value : $rawAmount")
                    } ?: run {
                        Toast.makeText(requireContext(), "Format QR Code salah", Toast.LENGTH_SHORT).show()
                        Log.e("Barcode", "Invalid amount format: $content")
                    }
                } else {
                    Toast.makeText(requireContext(), "Invalid QR Code", Toast.LENGTH_SHORT).show()
                    Log.e("Barcode", "Content : $content")
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error reading QR Code", Toast.LENGTH_SHORT).show()
                Log.e("Barcode", "Exception : ${e.message}")
            }
        }
    }

        override fun possibleResultPoints(resultPoints: List<ResultPoint>) {}
    }

    private fun parseAmount(rawAmount: String): Double? {
        return try {
            rawAmount
                .replace("Rp.", "")
                .replace("Rp ", "")
                .replace(",", "")
                .trim()
                .toDoubleOrNull()
        } catch (e: Exception) {
            null
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBarcodeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Initialize ViewModel
        barcodeViewModel = ViewModelProvider(this).get(BarcodeViewModel::class.java)

        // Setup barcode scanner
        barcodeScanner = binding.zxingBarcodeScanner
        barcodeScanner.decoderFactory = DefaultDecoderFactory(
            Collections.singletonList(BarcodeFormat.QR_CODE)
        )
        barcodeScanner.setStatusText("Scan QR Code")

        // Setup pay button
        binding.btnPay.setOnClickListener {
            if (binding.textQrAmount.text.isNotEmpty()) {
                showPaymentDialog()
            } else {
                Toast.makeText(requireContext(), "Scan QR Code terlebih dahulu", Toast.LENGTH_SHORT).show()
            }
        }

        return root
    }
    private fun showPaymentDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_payment, null)
        val dialogBuilder = AlertDialog.Builder(requireContext())
        dialogBuilder.setView(dialogView)

        val amountTextView = dialogView.findViewById<TextView>(R.id.text_payment_amount)
        val passwordEditText = dialogView.findViewById<EditText>(R.id.edit_payment_password)
        val confirmButton = dialogView.findViewById<Button>(R.id.btn_confirm_payment)
        val cancelButton = dialogView.findViewById<Button>(R.id.btn_cancel_payment)

        // Set the amount from the scanned QR code
        val amount = binding.textQrAmount.text.toString()
        amountTextView.text = "Total Pembayaran: $amount"

        val dialog = dialogBuilder.create()

        confirmButton.setOnClickListener {
            val password = passwordEditText.text.toString()
            if (password.isNotEmpty()) {
                // Perform payment validation
                performPayment(amount, password, dialog)
            } else {
                Toast.makeText(requireContext(), "Masukkan password", Toast.LENGTH_SHORT).show()
            }
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun performPayment(amount: String, password: String, dialog: AlertDialog) {
        // Retrieve NIM from SharedPreferences
        val sharedPref = requireActivity().getSharedPreferences("login_data", Context.MODE_PRIVATE)
        val nim = sharedPref.getString("nim", "") ?: ""

        // Use parseAmount to extract the raw numeric value
        val cleanAmount = parseAmount(amount)?.toInt()?.toString()
            ?: run {
                Toast.makeText(requireContext(), "Invalid Amount", Toast.LENGTH_SHORT).show()
                return
            }

        // Log for debugging to ensure cleanAmount is correct
        Log.e("Payment", "Raw Amount: $amount, Parsed Amount: $cleanAmount")

        // Make API call with the cleaned numeric value
        val apiCall = barcodeViewModel.processPayment(nim, cleanAmount, password)
        apiCall.observe(viewLifecycleOwner) { result ->
            when {
                result.success -> {
                    // Dismiss the payment dialog first
                    dialog.dismiss()

                    // Create a custom success alert dialog
                    val successDialogBuilder = AlertDialog.Builder(requireContext(), R.style.CustomSuccessDialog)
                    val successDialogView = layoutInflater.inflate(R.layout.dialog_payment_success, null)

                    // Find views in the custom dialog layout
                    val titleTextView = successDialogView.findViewById<TextView>(R.id.text_success_title)
                    val messageTextView = successDialogView.findViewById<TextView>(R.id.text_success_message)
                    val amountTextView = successDialogView.findViewById<TextView>(R.id.text_success_amount)
                    val closeButton = successDialogView.findViewById<Button>(R.id.btn_success_close)

                    // Set dialog content
                    titleTextView.text = "Pembayaran Berhasil"
                    messageTextView.text = "Terima kasih telah melakukan pembayaran"
                    amountTextView.text = "Total: $amount"

                    // Build and show the dialog
                    val successDialog = successDialogBuilder.setView(successDialogView).create()
                    successDialog.show()

                    // Close button listener
                    closeButton.setOnClickListener {
                        successDialog.dismiss()
                        barcodeScanner.resume()
                    }

                    // Auto-dismiss after 3 seconds
                    successDialog.window?.decorView?.postDelayed({
                        successDialog.dismiss()
                        barcodeScanner.resume()
                    }, 300000)
                }
                else -> {
                    Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (checkCameraPermission()) {
            barcodeScanner.resume()
            barcodeScanner.decodeContinuous(barcodeCallback)
        } else {
            requestCameraPermission()
        }
    }

    override fun onPause() {
        super.onPause()
        barcodeScanner.pause()
    }

    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_REQUEST_CODE
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 100
    }
}