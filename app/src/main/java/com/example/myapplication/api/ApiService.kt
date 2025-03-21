package com.example.myapplication.api

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.media.ExifInterface
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.content.ContextCompat
import com.example.myapplication.compressImageAndEncode
import com.example.myapplication.model.FireStation
import com.example.myapplication.model.Incident
import com.example.myapplication.model.Media
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
import java.text.SimpleDateFormat
import java.util.*
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


        /**
         * 사진 업로드
         */
    suspend fun uploadFileToCloud(
        context: Context, 
        fileUri: Uri, 
        fileName: String, 
        memoText: String, 
        incidentId: Long?, 
        creatorId: String?, 
        regionName: String?,
        latitude: Double = 37.5665,
        longitude: Double = 126.978,
        altitude: Double = 10.5,
        locationAddress: String = "위치 정보 없음",
        capturedAt: String = "",
        resolution: String = "1920x1080",
        hashtags: String = "#사진"
    ) {

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

                // Extract image metadata for additional information if needed
                val exifInterface = ExifInterface(file.path)
                
                // Use the provided values for location information
                // If capturedAt is empty, generate it
                val finalCapturedAt = if (capturedAt.isEmpty()) {
                    val dateTime = exifInterface.getAttribute(ExifInterface.TAG_DATETIME)
                    if (dateTime != null) {
                        try {
                            // Convert EXIF date format ("yyyy:MM:dd HH:mm:ss") to "yyyy-MM-dd HH:mm:ss"
                            val exifFormat = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.getDefault())
                            val date = exifFormat.parse(dateTime)
                            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(date ?: Date())
                        } catch (e: Exception) {
                            // If date parsing fails, use current time
                            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                        }
                    } else {
                        // If no date in EXIF, use current time
                        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                    }
                } else {
                    capturedAt
                }
                
                // Log the location information being used
                Log.d("ApiService", "Using location: $latitude, $longitude")
                Log.d("ApiService", "Address: $locationAddress")
                
                val jsonObject = JSONObject().apply {
                    put("fileName", inputString)
                    put("fileData", encodedFileData)
                    put("mimeType", mimeType)
                    put("mediaType", "photo")
                    put("incidentId", incidentId)
                    put("creatorId", creatorId)
                    put("latitude", latitude)
                    put("longitude", longitude)
                    put("altitude", altitude)
                    put("locationAddress", locationAddress)
                    put("resolution", resolution)
                    put("hashtags", hashtags)
                    put("memo", memo)
                    put("address", locationAddress)
                    put("regionName", regionName)
                    put("capturedAt", finalCapturedAt)
                    put("version", type)
                }


                val responseBody = CmmnUtil.requestBody(BASE_URL + "media/uploadPhotoData", jsonObject)


            } catch (e: Exception) {
                Log.e(TAG, "🚨 사건 목록 가져오기 실패: ${e.message}", e)
            }
        }

    }


        /**
     * 사진 업로드 (EXIF 메타데이터 우선 사용)
     * 이 함수는 기존 uploadFileToCloud와 유사하지만 현재 위치 대신 이미지의 EXIF 메타데이터를 우선적으로 사용합니다.
     */
    suspend fun uploadFileWithExifMetadata(context: Context, fileUri: Uri, fileName:String, memoText:String, incidentId: Long?, creatorId: String?, regionName: String?): Boolean {

        return withContext(Dispatchers.IO) {
            try {
                val file = File(fileUri.path)
                val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension)
                    ?: "application/octet-stream"

                val encodedFileData = compressImageAndEncode(file)

                val inputString = fileName.replace(" ", "")
                val splitArray = inputString.split("_")

                var fileType = ""
                var type = ""

                if(splitArray.isNotEmpty()){
                    fileType = splitArray[splitArray.size-1].split(".")[0]

                    if (fileType == "ORI"){
                        type = "original"
                    }else if(fileType == "EDIT"){
                        type = "edited"
                    }else{
                        type = "original"
                    }
                }else{
                    type = "original"
                }

                val memo = if (memoText.isEmpty()) "내용없음" else memoText

                // Extract image metadata
                val exifInterface = ExifInterface(file.path)
                
                // Get location from EXIF data only
                val latLong = FloatArray(2)
                val hasExifLocation = exifInterface.getLatLong(latLong)
                
                // Default coordinates (Seoul) if EXIF data is not available
                var latitude = 37.5665
                var longitude = 126.978
                
                if (hasExifLocation) {
                    // Use EXIF data
                    latitude = latLong[0].toDouble()
                    longitude = latLong[1].toDouble()
                    Log.d("ApiService", "Using EXIF location: $latitude, $longitude")
                } else {
                    Log.d("ApiService", "No EXIF location data, using default location")
                }
                
                // Get altitude
                val altitudeValue = exifInterface.getAttribute(ExifInterface.TAG_GPS_ALTITUDE)
                val altitude = altitudeValue?.toDoubleOrNull() ?: 10.5
                
                // Get image resolution
                val width = exifInterface.getAttribute(ExifInterface.TAG_IMAGE_WIDTH)?.toIntOrNull() ?: 1920
                val height = exifInterface.getAttribute(ExifInterface.TAG_IMAGE_LENGTH)?.toIntOrNull() ?: 1080
                val resolution = "${width}x${height}"
                
                // Get capture date and time
                val dateTime = exifInterface.getAttribute(ExifInterface.TAG_DATETIME)
                val capturedAt = if (dateTime != null) {
                    try {
                        // Convert EXIF date format ("yyyy:MM:dd HH:mm:ss") to "yyyy-MM-dd HH:mm:ss"
                        val exifFormat = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.getDefault())
                        val date = exifFormat.parse(dateTime)
                        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(date ?: Date())
                    } catch (e: Exception) {
                        // If date parsing fails, use current time
                        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                    }
                } else {
                    // If no date in EXIF, use current time
                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                }
                
                // Get location address using Geocoder with Korean locale
                val geocoder = Geocoder(context, Locale.KOREA)
                val locationAddress = try {
                    val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                    if (addresses != null && addresses.isNotEmpty()) {
                        val address = addresses[0]
                        val adminArea = address.adminArea ?: ""
                        val locality = address.locality ?: ""
                        val thoroughfare = address.thoroughfare ?: ""
                        val subThoroughfare = address.subThoroughfare ?: ""
                        "$adminArea $locality $thoroughfare $subThoroughfare".trim()
                    } else {
                        "위치 정보 없음"
                    }
                } catch (e: Exception) {
                    Log.e("ApiService", "Geocoding failed", e)
                    "위치 정보 없음"
                }
                
                // Generate hashtags based on location
                val hashtags = if (locationAddress != "위치 정보 없음") {
                    "#${locationAddress.split(" ")[0]} #사진"
                } else {
                    "#사진"
                }
                
                val jsonObject = JSONObject().apply {
                    put("fileName", inputString)
                    put("fileData", encodedFileData)
                    put("mimeType", mimeType)
                    put("mediaType", "photo")
                    put("incidentId", incidentId)
                    put("creatorId", creatorId)
                    put("latitude", latitude)
                    put("longitude", longitude)
                    put("altitude", altitude)
                    put("locationAddress", locationAddress)
                    put("resolution", resolution)
                    put("hashtags", hashtags)
                    put("memo", memo)
                    put("address", locationAddress)
                    put("regionName", regionName)
                    put("capturedAt", capturedAt)
                    put("version", type)
                }

                val responseBody = CmmnUtil.requestBody(BASE_URL + "media/uploadPhotoData", jsonObject)
                
                // Parse the response to determine success
                if (!responseBody.isNullOrEmpty()) {
                    val jsonResponse = JSONObject(responseBody)
                    val success = jsonResponse.optBoolean("success", false)
                    
                    Log.d(TAG, "Upload response: $responseBody")
                    return@withContext success
                }
                return@withContext false
                
            } catch (e: Exception) {
                Log.e(TAG, "🚨 EXIF 메타데이터 기반 파일 업로드 실패: ${e.message}", e)
                return@withContext false
            }
        }
    }

    suspend fun getMediaList(incidentId: String): List<Media> {
            // Dispatchers.IO를 사용하여 백그라운드 스레드에서 실행
            return withContext(Dispatchers.IO) {
                try {
                    val jsonObject = JSONObject().apply {
                        put("incidentId", incidentId)

                    }

                    val responseBody = CmmnUtil.requestBody(BASE_URL + "media/getMediaList", jsonObject)

                    if (!responseBody.isNullOrEmpty()) {
                        val dataArray = JSONArray(responseBody)

                        val dataList = Media.fromJsonArray(dataArray)

                        Log.d(TAG, "미디어 목록 가져오기 성공: ${dataList.size} 개 항목")
                        dataList
                    } else {
                        emptyList()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "🚨 미디어 목록 가져오기 실패: ${e.message}", e)
                    emptyList()
                }
            }

        }






    }
}
