package com.example.myapplication

import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.Locale

object IncidentManager {
    var incidentId: Long? = null
    var incidentName: String? = null
    var recentIncidents: MutableList<String> = mutableListOf() // 최근 사건명 저장

    fun clear() {
        incidentId = null
        incidentName = null
    }


    // API 호출: 최근 3개월 사건 가져오기
    fun fetchRecentIncidents(onComplete: (Boolean) -> Unit) {
        val url = "https://ev0tafewtt.apigw.ntruss.com/firecapture-api/v0001/incidents?blocking=true&result=true"

        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

// 오늘 날짜 (23:59:59)
        val endDate = dateFormat.format(calendar.apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }.time)

// 3개월 전 날짜 (00:00:00)
        calendar.add(Calendar.MONTH, -3)
        val startDate = dateFormat.format(calendar.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.time)
        val jsonObject = JSONObject().apply {
            put("action", "getIncidentsByDate")
            put("startDate", startDate) // 동적으로 설정된 3개월 전 날짜
            put("endDate", endDate)    // 동적으로 설정된 현재 날짜
        }


        val requestBody = jsonObject.toString().toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .header("Content-Type", "application/json")
            .build()

        val client = OkHttpClient()
        Thread {
            try {
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()
                if (!responseBody.isNullOrEmpty()) {
                    val jsonResponse = JSONObject(responseBody)
                    val bodyJson = JSONObject(jsonResponse.optString("body", "{}"))
                    val dataArray = bodyJson.optJSONArray("data")
                    if (dataArray != null) {
                        recentIncidents.clear()
                        for (i in 0 until dataArray.length()) {
                            val incident = dataArray.getJSONObject(i)

                            // "incident_name" 필드 처리 (Buffer 형식 변환)
                            val incidentName = if (incident.has("incident_name")) {
                                val buffer = incident.getJSONObject("incident_name")
                                val dataArray = buffer.getJSONArray("data")
                                val byteArray = ByteArray(dataArray.length()) { index ->
                                    dataArray.getInt(index).toByte()
                                }
                                String(byteArray) // ByteArray를 문자열로 변환
                            } else {
                                ""
                            }
                            // 변환된 문자열 추가
                            recentIncidents.add(incidentName)
                        }
                        onComplete(true)
                    } else {
                        onComplete(false)
                    }
                } else {
                    onComplete(false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false)
            }
        }.start()
    }
}
