package com.example.myapplication

import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import com.example.myapplication.api.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    suspend fun fetchRecentIncidents(onComplete: (Boolean) -> Unit) {
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


        // Use coroutine instead of Thread to call suspend function
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val incidentList = ApiService.selectIncidentList(startDate, endDate)

                if (incidentList.isNotEmpty()) {
                    recentIncidents.clear()
                    // 도시 이름만 추출 (중복 제거)
                    val incidentNamesFromApi = incidentList.mapNotNull { it.incidentName }.distinct()

                    recentIncidents.addAll(incidentNamesFromApi)

                    // Switch to Main dispatcher to call the callback
                    withContext(Dispatchers.Main) {
                        onComplete(true)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        onComplete(false)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onComplete(false)
                }
            }
        }
    }
}
