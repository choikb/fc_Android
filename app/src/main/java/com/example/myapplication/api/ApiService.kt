package com.example.myapplication.api

import android.util.Log
import com.example.myapplication.model.FireStation
import com.example.myapplication.util.CmmnUtil
import com.example.myapplication.util.ConfigUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import org.json.JSONArray
import java.util.concurrent.TimeUnit

/**
 * ApiService class for handling all API communications with the server
 */
class ApiService {
    companion object {
        // Get BASE_URL from ConfigUtil instead of hardcoding
        private val BASE_URL = ConfigUtil.getRootUrl() + "/admin/api/"
        private val TAG = "ApiService"

        // Create a singleton instance of OkHttpClient to be reused across requests
        private val client by lazy {
            OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .protocols(listOf(Protocol.HTTP_1_1))
                .build()
        }

        /**
         * Check if a device is already registered in the system
         * @param deviceId The unique device identifier
         * @return Boolean indicating if the device is registered
         */
        fun checkIfDeviceRegistered(deviceId: String): Boolean {
            val jsonObject = JSONObject().apply {
                put("action", "checkDevice")
                put("device_id", deviceId)
            }

            val requestBody = jsonObject.toString().toRequestBody("application/json".toMediaTypeOrNull())
            val request = Request.Builder()
                .url(BASE_URL)
                .post(requestBody)
                .header("Content-Type", "application/json")
                .header("User-Agent", "okhttp")
                .build()

            return try {
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                Log.d(TAG, "Status Code: ${response.code}")
                Log.d(TAG, "Response Body: $responseBody")

                if (!responseBody.isNullOrEmpty()) {
                    val jsonResponse = JSONObject(responseBody)
                    jsonResponse.optBoolean("registered", false)
                } else {
                    false
                }
            } catch (e: IOException) {
                Log.e(TAG, "🚨 API 요청 실패: ${e.message}", e)
                false
            }
        }

        /**
         * Register a new user in the system
         * @param deviceId The unique device identifier
         * @param userName The name of the user
         * @param city Selected city
         * @param fireStation Selected fire station
         * @param fireCenter Selected fire center
         * @param fireRegion Selected fire region
         * @return Boolean indicating if the registration was successful
         */
        suspend fun registerUser(deviceId: String, userName: String, fireRegionId: Int? = null): Boolean {

            return withContext(Dispatchers.IO) {
                try {
                    val jsonObject = JSONObject().apply {
                        put("userId", deviceId)  // 사용자 ID를 디바이스 ID와 동일하게 설정
                        put("deviceId", deviceId)
                        put("organization", "충남소방") // 조직 정보 (임시 값)
                        put("rank", "Admin") // 기본 등급
                        put("name", userName)
                        put("role", "화재조사") // 기본 역할
                        put("email", "") // 이메일 (추후 입력)
                        put("phoneNumber", "") // 전화번호 (추후 입력)
                        put("firestationId", fireRegionId ?: JSONObject.NULL)

                    }

                    val resultCnt = CmmnUtil.requestBody(BASE_URL + "user/registerUser", jsonObject)

                    if (resultCnt!!.toInt() > 0) {
                        true
                    } else {
                        false
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "🚨 API 요청 실패: ${e.message}", e)
                    false
                }
            }

        }

    /**
     * 도시 목록 조회
     */
    suspend fun selectCityList(): List<FireStation> {
        // Dispatchers.IO를 사용하여 백그라운드 스레드에서 실행
        return withContext(Dispatchers.IO) {
            try {
                val jsonObject = JSONObject()

                val responseBody = CmmnUtil.requestBody(BASE_URL + "cmmn/selectCityList", jsonObject)

                if (!responseBody.isNullOrEmpty()) {
                    val dataArray = JSONArray(responseBody)

                    // FireStation 클래스의 메소드를 사용하여 한 번에 목록 가져오기
                    val fireStationList = FireStation.fromJsonArray(dataArray)

                    Log.d(TAG, "도시 목록 가져오기 성공: ${fireStationList.size} 개 항목")
                    fireStationList
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                Log.e(TAG, "🚨 도시 목록 가져오기 실패: ${e.message}", e)
                emptyList()
            }
        }
    }


    /**
     * 소방서 목록 조회 (주의: 반드시 백그라운드 스레드에서 호출해야 함)
     * @param cityName 도시 이름
     * @return 소방서 목록 또는 오류 발생 시 빈 리스트
     */
    suspend fun selectFireStationList(cityName: String): List<FireStation> {
        // Dispatchers.IO를 사용하여 백그라운드 스레드에서 실행
        return withContext(Dispatchers.IO) {
            try {
                val jsonObject = JSONObject().apply {
                    put("city", cityName)
                }

                val responseBody = CmmnUtil.requestBody(BASE_URL + "cmmn/selectFireStationList", jsonObject)

                if (!responseBody.isNullOrEmpty()) {
                    val dataArray = JSONArray(responseBody)

                    // FireStation 클래스의 메소드를 사용하여 한 번에 목록 가져오기
                    val fireStationList = FireStation.fromJsonArray(dataArray)

                    Log.d(TAG, "소방서 목록 가져오기 성공: ${fireStationList.size} 개 항목")
                    fireStationList
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                Log.e(TAG, "🚨 소방서 목록 가져오기 실패: ${e.message}", e)
                emptyList()
            }
        }

    }

    /**
     * 안전센터 목록 조회
     * @param stationName 소방서 이름
      */
    suspend fun selectFireCenterList(stationName: String, cityName: String): List<FireStation> {
        // Dispatchers.IO를 사용하여 백그라운드 스레드에서 실행
        return withContext(Dispatchers.IO) {
            try {
                val jsonObject = JSONObject().apply {
                    put("stationName", stationName)
                    put("city", cityName)
                }

                val responseBody = CmmnUtil.requestBody(BASE_URL + "cmmn/selectFireCenterList", jsonObject)

                if (!responseBody.isNullOrEmpty()) {
                    val dataArray = JSONArray(responseBody)

                    // FireStation 클래스의 메소드를 사용하여 한 번에 목록 가져오기
                    val fireCenterList = FireStation.fromJsonArray(dataArray)

                    Log.d(TAG, "안전센터 목록 가져오기 성공: ${fireCenterList.size} 개 항목")
                    fireCenterList
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                Log.e(TAG, "🚨 안전센터 목록 가져오기 실패: ${e.message}", e)
                emptyList()
            }
        }

    }

    suspend fun selectFireRegionList(centerName: String, cityName: String, stationName: String): List<FireStation> {
        // Dispatchers.IO를 사용하여 백그라운드 스레드에서 실행
        return withContext(Dispatchers.IO) {
            try {
                val jsonObject = JSONObject().apply {
                    put("centerName", centerName)
                    put("stationName", stationName)
                    put("city", cityName)
                }

                val responseBody = CmmnUtil.requestBody(BASE_URL + "cmmn/selectFireRegionList", jsonObject)

                if (!responseBody.isNullOrEmpty()) {
                    val dataArray = JSONArray(responseBody)

                    // FireStation 클래스의 메소드를 사용하여 한 번에 목록 가져오기
                    val fireRegionList = FireStation.fromJsonArray(dataArray)

                    Log.d(TAG, "지역대 목록 가져오기 성공: ${fireRegionList.size} 개 항목")
                    fireRegionList
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                Log.e(TAG, "🚨 지역대 목록 가져오기 실패: ${e.message}", e)
                emptyList()
            }
        }

    }




    }
}
