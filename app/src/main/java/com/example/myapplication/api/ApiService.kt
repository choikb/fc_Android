package com.example.myapplication.api

import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.webkit.MimeTypeMap
import com.example.myapplication.compressImageAndEncode
import com.example.myapplication.model.FireStation
import com.example.myapplication.model.Incident
import com.example.myapplication.model.User
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
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * ApiService class for handling all API communications with the server
 */
class ApiService {
    companion object {
        // Get BASE_URL from ConfigUtil instead of hardcoding
        private val BASE_URL = ConfigUtil.getRootUrl() + "/admin/api/"
        private val TAG = "ApiService"


        /**
         * 사용자 등록
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

    /**
     * 지역대 조회
      */
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

        /**
         * 사용자 상세 조회
         */
    suspend fun selectUserDtl(androidId: String): User? {
        // Dispatchers.IO를 사용하여 백그라운드 스레드에서 실행
        return withContext(Dispatchers.IO) {
            try {
                val jsonObject = JSONObject().apply {
                    put("deviceId", androidId)
                }
                val responseBody = CmmnUtil.requestBody(BASE_URL + "user/selectUserDtl", jsonObject)
                if (!responseBody.isNullOrEmpty()) {
                    val dataObject = JSONObject(responseBody)
                    val user = User.fromJson(dataObject)

                    Log.d(TAG, "사용자 가져오기 성공: ${user.name}")
                    user
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "🚨 사용자 가져오기 실패: ${e.message}", e)
                null
            }
        }

    }

    /**
     * 사건 등록
     */
    suspend fun registIncident(caseName: String, creatorId: String): Int {

        return withContext(Dispatchers.IO) {
            try {
                val jsonObject = JSONObject().apply {
                    put("action", "addIncident")
                    put("incidentName", caseName)
                    put(
                        "creatorId",
                        creatorId
                    ) // 현재 사용자 ID
                    put("description", "신규 사건 등록") // 기본 설명 (필요 시 수정 가능)
                    put("type", "화재") // 기본 사건 유형 (필요 시 수정 가능)
                }

                val resultCnt = CmmnUtil.requestBody(BASE_URL + "incident/registIncident", jsonObject)

                resultCnt!!.toInt()
            } catch (e: Exception) {
                Log.e(TAG, "🚨 API 요청 실패: ${e.message}", e)
                -1
            }
        }

    }

        /**
         * 사건 목록 조회
          */
    suspend fun selectIncidentList(startDate: String? = null, endDate: String? = null, incidentName: String? = null): List<Incident> {
        // Dispatchers.IO를 사용하여 백그라운드 스레드에서 실행
        return withContext(Dispatchers.IO) {
            try {
                val jsonObject = JSONObject().apply {
                    put("startDate", startDate)
                    put("endDate", endDate)
                    put("incidentName", incidentName)
                }

                val responseBody = CmmnUtil.requestBody(BASE_URL + "incident/selectIncidentList", jsonObject)

                if (!responseBody.isNullOrEmpty()) {
                    val dataArray = JSONArray(responseBody)

                    val dataList = Incident.fromJsonArray(dataArray)

                    Log.d(TAG, "사건 목록 가져오기 성공: ${dataList.size} 개 항목")
                    dataList
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                Log.e(TAG, "🚨 사건 목록 가져오기 실패: ${e.message}", e)
                emptyList()
            }
        }

    }


    suspend fun uploadFileToCloud(fileUri: Uri, fileName:String, memoText:String, incidentId: Long?, creatorId: String?, regionName: String?) {

        return withContext(Dispatchers.IO) {
            try {

                val file = File(fileUri.path)
                //val fileName = generateFileName() // 파일 이름 생성
                val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension)
                    ?: "application/octet-stream"

                // Base64로 인코딩된 이미지 파일
                // val fileData = file.readBytes()
                // val encodedFileData = Base64.encodeToString(fileData, Base64.NO_WRAP)
                val encodedFileData = compressImageAndEncode(file)
                // JSON 본문 작성

                val inputString = fileName.replace(" ", "")
                val splitArray = inputString.split("_")
                val caseName = splitArray[0]  // 사건명
                val timestamp = splitArray[1]  // 타임스탬프
                val fileType = splitArray[2].substringBeforeLast(".")   // 파일 타입
                var type = ""  // var로 선언하여 변경 가능하도록 함
                if (fileType == "ORI") {
                    type = "original"
                } else {
                    type = "edited"
                }

                var memo = memoText
                if (memoText.isEmpty()) {
                    memo = "내용없음"
                }

                val jsonObject = JSONObject().apply {
                    put("fileName", inputString)
                    put("fileData", encodedFileData)
                    put("mimeType", mimeType)
                    put("mediaType", "photo")
                    put("incidentId", incidentId)
                    put("creatorId", creatorId)
                    put("latitude", 37.5665)
                    put("longitude", 126.978)
                    put("altitude", 10.5)
                    put("locationAddress", "서울특별시 종로구")
                    put("resolution", "1920x1080")
                    put("hashtags", "#서울 #사진")
                    put("memo", memo)
                    put("address", "서울시 종로구")
                    put("regionName", regionName)
                    put("capturedAt", "2023-04-01 12:00:00")
                    put("version", type)
                }


                val responseBody = CmmnUtil.requestBody(BASE_URL + "media/uploadPhotoData", jsonObject)


            } catch (e: Exception) {
                Log.e(TAG, "🚨 사건 목록 가져오기 실패: ${e.message}", e)
            }
        }

    }






    }
}
