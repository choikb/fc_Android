package com.example.myapplication.util

import android.util.Log
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class CmmnUtil {
    companion object {
        private const val TAG = "CmmnUtil"
        private val client by lazy {
            OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .protocols(listOf(Protocol.HTTP_1_1))
                .build()
        }

        fun requestBody(url: String, jsonObject: JSONObject): String? {

            Log.d(TAG, "Request JSON: ${jsonObject.toString()}")

            val requestBody = jsonObject.toString().toRequestBody("application/json".toMediaTypeOrNull())
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .header("Content-Type", "application/json")
                .header("User-Agent", "okhttp")
                .build()

            return try {
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                Log.d(TAG, "Status Code: ${response.code}")
                Log.d(TAG, "Response Body: $responseBody")

                if (!responseBody.isNullOrEmpty() && response.isSuccessful) {
                    responseBody
                } else {
                    Log.d(TAG, "API 응답이 비어 있거나 요청이 실패했습니다: ${response.code}")
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "🚨 API 요청 실패: ${e.message}", e)
                null
            }


        }
    }
}