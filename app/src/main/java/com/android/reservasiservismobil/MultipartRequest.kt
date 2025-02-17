package com.android.reservasiservismobil

import com.android.volley.AuthFailureError
import com.android.volley.NetworkResponse
import com.android.volley.ParseError
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.HttpHeaderParser
import java.io.*

open class MultipartRequest(
    method: Int,
    url: String,
    private val listener: Response.Listener<String>,
    errorListener: Response.ErrorListener
) : Request<String>(method, url, errorListener) {

    private val twoHyphens = "--"
    private val lineEnd = "\r\n"
    private val boundary = "apiclient-${System.currentTimeMillis()}"

    override fun getBodyContentType(): String {
        return "multipart/form-data;boundary=$boundary"
    }

    @Throws(AuthFailureError::class)
    override fun getBody(): ByteArray {
        val bos = ByteArrayOutputStream()
        val dos = DataOutputStream(bos)

        try {
            // Add params
            val params = getParams()
            if (params != null && params.isNotEmpty()) {
                textParse(dos, params, getParamsEncoding())
            }

            // Add data
            val data = getByteData()
            if (data != null && data.isNotEmpty()) {
                dataParse(dos, data)
            }

            // Close multipart form data after text and file data
            dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd)

            return bos.toByteArray()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return super.getBody()
    }

    @Throws(IOException::class)
    private fun textParse(dos: DataOutputStream, params: Map<String, String>, encoding: String) {
        try {
            for ((key, value) in params) {
                dos.writeBytes(twoHyphens + boundary + lineEnd)
                dos.writeBytes("Content-Disposition: form-data; name=\"$key\"$lineEnd")
                dos.writeBytes(lineEnd)
                dos.writeBytes(value + lineEnd)
            }
        } catch (e: IOException) {
            throw e
        }
    }

    @Throws(IOException::class)
    private fun dataParse(dos: DataOutputStream, data: Map<String, DataPart>) {
        try {
            for ((key, value) in data) {
                dos.writeBytes(twoHyphens + boundary + lineEnd)
                dos.writeBytes("Content-Disposition: form-data; name=\"$key\"; filename=\"${value.fileName}\"$lineEnd")
                if (value.type != null && value.type.trim().isNotEmpty()) {
                    dos.writeBytes("Content-Type: ${value.type}$lineEnd")
                }
                dos.writeBytes(lineEnd)

                val fileInputStream = ByteArrayInputStream(value.content)
                var bytesAvailable = fileInputStream.available()
                val maxBufferSize = 1024 * 1024
                var bufferSize = Math.min(bytesAvailable, maxBufferSize)
                val buffer = ByteArray(bufferSize)

                var bytesRead = fileInputStream.read(buffer, 0, bufferSize)
                while (bytesRead > 0) {
                    dos.write(buffer, 0, bufferSize)
                    bytesAvailable = fileInputStream.available()
                    bufferSize = Math.min(bytesAvailable, maxBufferSize)
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize)
                }
                dos.writeBytes(lineEnd)
            }
        } catch (e: IOException) {
            throw e
        }
    }

    override fun parseNetworkResponse(response: NetworkResponse): Response<String> {
        try {
            val data = String(response.data, charset(HttpHeaderParser.parseCharset(response.headers)))
            return Response.success(data, HttpHeaderParser.parseCacheHeaders(response))
        } catch (e: Exception) {
            return Response.error(ParseError(e))
        }
    }

    override fun deliverResponse(response: String) {
        listener.onResponse(response)
    }

    @Throws(AuthFailureError::class)
    open fun getByteData(): Map<String, DataPart>? {
        return null
    }

    class DataPart(
        val fileName: String,
        val content: ByteArray,
        val type: String = ""
    )
}