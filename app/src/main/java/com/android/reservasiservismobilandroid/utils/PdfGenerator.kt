package com.android.reservasiservismobilandroid.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.content.FileProvider
import com.itextpdf.text.Document
import com.itextpdf.text.Element
import com.itextpdf.text.Font
import com.itextpdf.text.PageSize
import com.itextpdf.text.Paragraph
import com.itextpdf.text.Phrase
import com.itextpdf.text.Rectangle
import com.itextpdf.text.pdf.PdfPCell
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfWriter
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PdfGenerator {
    companion object {
        private const val TAG = "PdfGenerator"
        
        // Warna dan font
        private val FONT_TITLE = Font(Font.FontFamily.HELVETICA, 16f, Font.BOLD)
        private val FONT_SUBTITLE = Font(Font.FontFamily.HELVETICA, 12f, Font.BOLD)
        private val FONT_NORMAL = Font(Font.FontFamily.HELVETICA, 10f, Font.NORMAL)
        private val FONT_SMALL = Font(Font.FontFamily.HELVETICA, 8f, Font.NORMAL)
        private val FONT_BOLD = Font(Font.FontFamily.HELVETICA, 10f, Font.BOLD)
        
        /**
         * Membuat bukti reservasi dalam format PDF
         */
        fun createReservationPdf(context: Context, reservationData: Map<String, Any?>, packageProducts: List<Map<String, Any>>, totalBill: Double): Uri? {
            try {
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val pdfFileName = "Reservasi_${timeStamp}.pdf"
                val pdfFolder = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Reservasi")
                
                if (!pdfFolder.exists()) {
                    pdfFolder.mkdirs()
                }
                
                val pdfFile = File(pdfFolder, pdfFileName)
                val document = Document(PageSize.A4)
                val writer = PdfWriter.getInstance(document, FileOutputStream(pdfFile))
                
                document.open()
                
                // Header
                val title = Paragraph("BUKTI RESERVASI", FONT_TITLE)
                title.alignment = Element.ALIGN_CENTER
                document.add(title)
                
                val subTitle = Paragraph("RESERVASI SERVIS MOBIL", FONT_SUBTITLE)
                subTitle.alignment = Element.ALIGN_CENTER
                document.add(subTitle)
                
                document.add(Paragraph(" "))
                
                // Informasi Reservasi
                document.add(createInfoSection("INFORMASI RESERVASI", arrayOf(
                    arrayOf("ID Reservasi", reservationData["id"]?.toString() ?: "-"),
                    arrayOf("Tanggal Reservasi", reservationData["reservation_date"]?.toString() ?: "-"),
                    arrayOf("Waktu", reservationData["reservation_time"]?.toString() ?: "-"),
                    arrayOf("Status", getStatusLabel(reservationData["service_status"]?.toString() ?: "-"))
                )))
                
                document.add(Paragraph(" "))
                
                // Informasi Kendaraan
                document.add(createInfoSection("INFORMASI KENDARAAN", arrayOf(
                    arrayOf("Nama Kendaraan", reservationData["vehicle_name"]?.toString() ?: "-"),
                    arrayOf("Plat Nomor", reservationData["plate_number"]?.toString() ?: "-"),
                    arrayOf("Keluhan", reservationData["vehicle_complaint"]?.toString() ?: "-")
                )))
                
                document.add(Paragraph(" "))
                
                // Informasi Paket
                document.add(createInfoSection("PAKET SERVIS", arrayOf(
                    arrayOf("Nama Paket", reservationData["package_name"]?.toString() ?: "-")
                )))
                
                document.add(Paragraph(" "))
                
                // Tabel Produk
                if (packageProducts.isNotEmpty()) {
                    val sectionTitle = Paragraph("DETAIL PRODUK", FONT_SUBTITLE)
                    document.add(sectionTitle)
                    
                    val productTable = PdfPTable(3)
                    productTable.widthPercentage = 100f
                    productTable.setWidths(floatArrayOf(1f, 3f, 1f))
                    
                    // Header tabel
                    addTableHeader(productTable, arrayOf("No", "Nama Produk", "Harga"))
                    
                    // Isi tabel
                    packageProducts.forEachIndexed { index, product ->
                        val no = PdfPCell(Phrase("${index + 1}", FONT_NORMAL))
                        no.horizontalAlignment = Element.ALIGN_CENTER
                        
                        val name = PdfPCell(Phrase(product["name"]?.toString() ?: "-", FONT_NORMAL))
                        
                        val price = PdfPCell(Phrase(formatCurrency(product["price"] as? Double ?: 0.0), FONT_NORMAL))
                        price.horizontalAlignment = Element.ALIGN_RIGHT
                        
                        productTable.addCell(no)
                        productTable.addCell(name)
                        productTable.addCell(price)
                    }
                    
                    // Total
                    val emptyCell = PdfPCell(Phrase(""))
                    emptyCell.border = Rectangle.NO_BORDER
                    
                    val totalLabel = PdfPCell(Phrase("Total", FONT_BOLD))
                    totalLabel.horizontalAlignment = Element.ALIGN_RIGHT
                    totalLabel.border = Rectangle.NO_BORDER
                    
                    val totalValue = PdfPCell(Phrase(formatCurrency(totalBill), FONT_BOLD))
                    totalValue.horizontalAlignment = Element.ALIGN_RIGHT
                    
                    productTable.addCell(emptyCell)
                    productTable.addCell(totalLabel)
                    productTable.addCell(totalValue)
                    
                    document.add(productTable)
                }
                
                document.add(Paragraph(" "))
                document.add(Paragraph(" "))
                
                // Footer
                val footer = Paragraph("Dokumen ini adalah bukti resmi reservasi servis mobil. " +
                        "Tunjukkan dokumen ini saat datang ke bengkel.", FONT_SMALL)
                footer.alignment = Element.ALIGN_CENTER
                document.add(footer)
                
                document.close()
                
                // Kembalikan URI untuk file PDF
                return FileProvider.getUriForFile(
                    context,
                    context.applicationContext.packageName + ".provider",
                    pdfFile
                )
                
            } catch (e: Exception) {
                Log.e(TAG, "Error creating reservation PDF: ${e.message}")
                e.printStackTrace()
                return null
            }
        }
        
        /**
         * Membuat bukti pembayaran dalam format PDF
         */
        fun createPaymentPdf(context: Context, paymentData: Map<String, Any?>, serviceData: Map<String, Any?>, customerName: String): Uri? {
            try {
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val pdfFileName = "Pembayaran_${timeStamp}.pdf"
                val pdfFolder = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Pembayaran")
                
                if (!pdfFolder.exists()) {
                    pdfFolder.mkdirs()
                }
                
                val pdfFile = File(pdfFolder, pdfFileName)
                val document = Document(PageSize.A4)
                val writer = PdfWriter.getInstance(document, FileOutputStream(pdfFile))
                
                document.open()
                
                // Header
                val title = Paragraph("BUKTI PEMBAYARAN", FONT_TITLE)
                title.alignment = Element.ALIGN_CENTER
                document.add(title)
                
                val subTitle = Paragraph("RESERVASI SERVIS MOBIL", FONT_SUBTITLE)
                subTitle.alignment = Element.ALIGN_CENTER
                document.add(subTitle)
                
                document.add(Paragraph(" "))
                
                // Tanggal dan No. Pembayaran
                val currentDate = SimpleDateFormat("dd MMMM yyyy HH:mm:ss", Locale("id", "ID")).format(Date())
                document.add(createInfoSection("INFORMASI PEMBAYARAN", arrayOf(
                    arrayOf("No. Pembayaran", paymentData["id"]?.toString() ?: "-"),
                    arrayOf("Tanggal Pembayaran", paymentData["created_at"]?.toString() ?: currentDate),
                    arrayOf("Metode Pembayaran", paymentData["method"]?.toString() ?: "-"),
                    arrayOf("Status", "LUNAS")
                )))
                
                document.add(Paragraph(" "))
                
                // Informasi Pelanggan
                document.add(createInfoSection("INFORMASI PELANGGAN", arrayOf(
                    arrayOf("Nama", customerName),
                    arrayOf("Kendaraan", serviceData["vehicle_name"]?.toString() ?: "-"),
                    arrayOf("Plat Nomor", serviceData["plate_number"]?.toString() ?: "-")
                )))
                
                document.add(Paragraph(" "))
                
                // Detail Pembayaran
                val paymentTable = PdfPTable(2)
                paymentTable.widthPercentage = 100f
                
                val bill = paymentData["bill"] as? Number ?: 0
                val pay = paymentData["pay"] as? Number ?: 0
                val change = paymentData["change"] as? Number ?: 0
                
                addPaymentDetail(paymentTable, "Subtotal", bill.toDouble())
                addPaymentDetail(paymentTable, "Total", bill.toDouble())
                addPaymentDetail(paymentTable, "Dibayar", pay.toDouble())
                addPaymentDetail(paymentTable, "Kembalian", change.toDouble())
                
                document.add(paymentTable)
                
                document.add(Paragraph(" "))
                document.add(Paragraph(" "))
                
                // Footer
                val footer = Paragraph("Dokumen ini adalah bukti resmi pembayaran servis mobil. " +
                        "Simpan sebagai bukti bahwa Anda telah melakukan pembayaran.", FONT_SMALL)
                footer.alignment = Element.ALIGN_CENTER
                document.add(footer)
                
                val thankYou = Paragraph("Terima kasih atas kepercayaan Anda.", FONT_SMALL)
                thankYou.alignment = Element.ALIGN_CENTER
                document.add(thankYou)
                
                document.close()
                
                // Kembalikan URI untuk file PDF
                return FileProvider.getUriForFile(
                    context,
                    context.applicationContext.packageName + ".provider",
                    pdfFile
                )
                
            } catch (e: Exception) {
                Log.e(TAG, "Error creating payment PDF: ${e.message}")
                e.printStackTrace()
                return null
            }
        }
        
        // Fungsi utilitas untuk membuat section informasi
        private fun createInfoSection(title: String, data: Array<Array<String>>): PdfPTable {
            val sectionTitle = Paragraph(title, FONT_SUBTITLE)
            
            val table = PdfPTable(2)
            table.widthPercentage = 100f
            table.setWidths(floatArrayOf(1f, 2f))
            
            for (row in data) {
                val labelCell = PdfPCell(Phrase(row[0], FONT_BOLD))
                labelCell.border = Rectangle.NO_BORDER
                
                val valueCell = PdfPCell(Phrase(row[1], FONT_NORMAL))
                valueCell.border = Rectangle.NO_BORDER
                
                table.addCell(labelCell)
                table.addCell(valueCell)
            }
            
            return table
        }
        
        // Fungsi utilitas untuk menambahkan header tabel
        private fun addTableHeader(table: PdfPTable, headers: Array<String>) {
            for (header in headers) {
                val cell = PdfPCell(Phrase(header, FONT_BOLD))
                cell.horizontalAlignment = Element.ALIGN_CENTER
                cell.backgroundColor = com.itextpdf.text.BaseColor(240, 240, 240)
                table.addCell(cell)
            }
        }
        
        // Fungsi utilitas untuk menambahkan detail pembayaran
        private fun addPaymentDetail(table: PdfPTable, label: String, value: Double) {
            val labelCell = PdfPCell(Phrase(label, FONT_NORMAL))
            labelCell.border = Rectangle.NO_BORDER
            labelCell.horizontalAlignment = Element.ALIGN_LEFT
            
            val valueCell = PdfPCell(Phrase(formatCurrency(value), FONT_NORMAL))
            valueCell.border = Rectangle.NO_BORDER
            valueCell.horizontalAlignment = Element.ALIGN_RIGHT
            
            table.addCell(labelCell)
            table.addCell(valueCell)
        }
        
        // Format mata uang
        private fun formatCurrency(value: Double): String {
            val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
            return formatter.format(value)
        }
        
        // Mendapatkan label status
        private fun getStatusLabel(status: String): String {
            return when(status) {
                "Pending" -> "Menunggu"
                "Process" -> "Sedang Diproses"
                "Finish" -> "Selesai || Lunas"
                "Selesai" -> "Selesai || Belum Bayar"
                "Cancelled" -> "Dibatalkan"
                else -> status
            }
        }
        
        // Fungsi untuk menampilkan PDF
        fun viewPdf(context: Context, pdfUri: Uri) {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(pdfUri, "application/pdf")
            intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "Tidak ada aplikasi untuk membuka PDF", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Fungsi untuk berbagi PDF
        fun sharePdf(context: Context, pdfUri: Uri, title: String) {
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "application/pdf"
            intent.putExtra(Intent.EXTRA_STREAM, pdfUri)
            intent.putExtra(Intent.EXTRA_SUBJECT, title)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            
            context.startActivity(Intent.createChooser(intent, "Bagikan PDF"))
        }
    }
} 