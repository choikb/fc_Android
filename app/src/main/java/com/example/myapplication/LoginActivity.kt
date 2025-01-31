package com.example.myapplication
import com.example.myapplication.DatabaseConfig
import android.content.Intent
import android.os.Bundle
import android.os.StrictMode
import android.provider.Settings
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.sql.DriverManager

import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

class LoginActivity : AppCompatActivity() {

    private lateinit var spinnerCity: Spinner
    private lateinit var spinnerFireStation: Spinner
    private lateinit var spinnerCenter: Spinner
    private lateinit var editTextCaseName: EditText
    private lateinit var buttonLogin: Button

    private val prefs by lazy { PreferenceManager.getDefaultSharedPreferences(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        initViews()

        //postCheckUserCount()

        // Android ID 가져오기
        val androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)

        Thread {

            // DB에서 Android ID 확인
            if (!isAndroidIdRegistered(androidId)) {
                // Android ID가 없으면 등록 화면 표시
                showRegistrationScreen(androidId)
                Log.d("API_RESULT", "사용자가 등록되지 않음")
            }
            else
            {
                Log.d("API_RESULT", "사용자가 등록됨")
            }

        }.start()



        setupSpinners()
        restoreSelections() // 이전 선택 데이터 복원

        buttonLogin.setOnClickListener {



            saveSelections() // 선택 데이터 저장

            val city = spinnerCity.selectedItem.toString()
            val fireStation = spinnerFireStation.selectedItem.toString()
            val center = spinnerCenter.selectedItem.toString()
            val caseName = editTextCaseName.text.toString()

            if (caseName.isEmpty()) {
                Toast.makeText(this, "사건명을 입력하세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // ✅ 신규 사건 등록 API 호출 (동기 방식)
            Thread {
                val incidentId = registerIncident(caseName)
                runOnUiThread {
                    if (incidentId != null) {
                        Toast.makeText(this, "사건 등록 성공!", Toast.LENGTH_SHORT).show()

                        // ✅ 사건 정보 전역 변수 저장
                        IncidentManager.clear()
                        IncidentManager.incidentId = incidentId
                        IncidentManager.incidentName = caseName

                        // ✅ MainMenuActivity로 이동
                        val intent = Intent(this, MainMenuActivity::class.java).apply {
                            putExtra("CITY", city)
                            putExtra("FIRE_STATION", fireStation)
                            putExtra("CENTER", center)
                            putExtra("CASE_NAME", caseName)
                        }
                        startActivity(intent)
                        finish() // 로그인 화면 종료
                    } else {
                        Toast.makeText(this, "사건 등록 실패!", Toast.LENGTH_SHORT).show()
                    }
                }
            }.start()
        }
    }

    private fun registerIncident(caseName: String): Long? {
        val url = "https://ev0tafewtt.apigw.ntruss.com/firecapture-api/v0001/incidents?blocking=true&result=true"

        val jsonObject = JSONObject().apply {
            put("action", "addIncident")
            put("incidentName", caseName)
            put("creatorId", Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)) // 현재 사용자 ID
            put("description", "신규 사건 등록") // 기본 설명 (필요 시 수정 가능)
            put("type", "화재") // 기본 사건 유형 (필요 시 수정 가능)
        }

        val requestBody = jsonObject.toString().toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .header("Content-Type", "application/json")
            .header("User-Agent", "okhttp")
            .build()

        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .protocols(listOf(Protocol.HTTP_1_1))
            .build()

        return try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            Log.d("API_RESPONSE", "Status Code: ${response.code}")
            Log.d("API_RESPONSE", "Response Body: $responseBody")

            if (!responseBody.isNullOrEmpty()) {
                val jsonResponse = JSONObject(responseBody)

                // ✅ "body" 내부 JSON을 다시 파싱하여 incidentId 추출
                val bodyJson = JSONObject(jsonResponse.optString("body", "{}"))
                val incidentId = bodyJson.optJSONObject("data")?.optLong("insertId", -1)?.takeIf { it > 0 }
                if (incidentId != null) {
                    IncidentManager.incidentId = incidentId
                    IncidentManager.incidentName = caseName  // 사건명도 저장
                    return 1
                }
                return bodyJson.optLong("incidentId", -1).takeIf { it > 0 }
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("API_ERROR", "🚨 사건 등록 API 요청 실패: ${e.message}", e)
            null
        }
    }

    private fun isAndroidIdRegistered(androidId: String): Boolean { //쓰레드 사용코드
        val url = "https://ev0tafewtt.apigw.ntruss.com/firecapture-api/v0001/users?blocking=true&result=true"
        val jsonObject = JSONObject().apply {
            put("action", "getUserInfo")
            put("userId", androidId)
        }

        val requestBody = jsonObject.toString().toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .header("Content-Type", "application/json")
            .header("User-Agent", "okhttp")
            .build()

        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .protocols(listOf(Protocol.HTTP_1_1))
            .build()

        return try {
            val response = client.newCall(request).execute()

            // ✅ 응답 상태 코드 출력 (디버깅용)
            Log.d("API_RESPONSE", "Status Code: ${response.code}")

            val responseBody = response.body?.string()
            Log.d("API_RESPONSE", "Response Body: $responseBody")

            if (!responseBody.isNullOrEmpty()) {
                val jsonResponse = JSONObject(responseBody)

                // ✅ "body" 내부 JSON을 파싱해야 제대로 data 배열을 추출할 수 있음
                val bodyJson = JSONObject(jsonResponse.optString("body", "{}"))

                // ✅ data 배열의 길이를 체크하여 0보다 크면 true 반환
                val dataArray = bodyJson.optJSONArray("data")
                return dataArray?.length() ?: 0 > 0
            } else {
                return false
            }
        } catch (e: Exception) {
            Log.e("API_ERROR", "🚨 API 요청 실패: ${e.message}", e)
            false
        }
    }

//    private fun isAndroidIdRegistered(androidId: String): Boolean { //쓰레드 미사용 코드
//
//        // 🚨 StrictMode 설정 (네트워크 요청 제한 해제)
//        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
//        StrictMode.setThreadPolicy(policy)
//
//        val url = "https://ev0tafewtt.apigw.ntruss.com/firecapture-api/v0001/users?blocking=true&result=true"
//        val jsonObject = JSONObject().apply {
//            put("action", "getUserInfo")
//            put("userId", androidId)
//        }
//
//        val requestBody = jsonObject.toString().toRequestBody("application/json".toMediaTypeOrNull())
//        val request = Request.Builder()
//            .url(url)
//            .post(requestBody)
//            .header("Content-Type", "application/json") // 🔹 헤더 추가
//            .header("User-Agent", "okhttp")
//            .build()
//
//        val client = OkHttpClient.Builder()
//            .connectTimeout(30, TimeUnit.SECONDS) // 🔹 연결 타임아웃 증가
//            .readTimeout(30, TimeUnit.SECONDS)    // 🔹 응답 대기 시간 증가
//            .writeTimeout(30, TimeUnit.SECONDS)   // 🔹 요청 전송 타임아웃 증가
//            .protocols(listOf(Protocol.HTTP_1_1))  // 🔹 HTTP/1.1 강제 사용 (API Gateway와의 호환성)
//            .build()
//        return try {
//            val response = client.newCall(request).execute()
//
//            // ✅ 응답 상태 코드 출력 (디버깅용)
//            Log.d("API_RESPONSE", "Status Code: ${response.code}")
//
//            val responseBody = response.body?.string()
//            Log.d("API_RESPONSE", "Response Body: $responseBody")
//
//            if (!responseBody.isNullOrEmpty()) {
//                val jsonResponse = JSONObject(responseBody)
//                jsonResponse.optJSONArray("data")?.length() ?: 0 > 0  // data 배열이 비어있지 않으면 true 반환
//            } else {
//                false
//            }
//        } catch (e: SocketTimeoutException) {
//            Log.e("API_ERROR", "⏳ 서버 응답 시간 초과 (Timeout): ${e.message}", e)
//            false
//        } catch (e: IOException) {
//            Log.e("API_ERROR", "📶 네트워크 오류 발생: ${e.message}", e)
//            false
//        } catch (e: Exception) {
//            Log.e("API_ERROR", "🚨 API 요청 실패 (예외 발생): ${e.message}", e)
//            false
//        }
//    }




//    private suspend fun checkAndroidId(androidId: String): Boolean {
//        return DatabaseHelper.isAndroidIdRegistered(androidId)
//    }

//    private fun isAndroidIdRegistered(androidId: String): Boolean {
//        val query = "SELECT COUNT(*) FROM fc_app_user_info WHERE android_id = ?"
//        try {
//            // DatabaseConfig에서 접속 정보 가져오기
//            DriverManager.getConnection(
//                DatabaseConfig.JDBC_URL,
//                DatabaseConfig.DB_USER,
//                DatabaseConfig.DB_PASSWORD
//            ).use { conn ->
//                conn.prepareStatement(query).use { stmt ->
//                    stmt.setString(1, androidId) // 매개변수 바인딩
//                    stmt.executeQuery().use { rs ->
//                        if (rs.next()) {
//                            return rs.getInt(1) > 0 // 결과 값 반환
//                        }
//                    }
//                }
//            }
//        } catch (e: Exception) {
//            e.printStackTrace() // 예ㅅ외 처리
//        }
//        return false // 기본 반환 값
//    }

    private fun showRegistrationScreen(androidId: String) {
        val intent = Intent(this, RegistrationActivity::class.java).apply {
            putExtra("ANDROID_ID", androidId)
        }
        startActivity(intent)
        finish()
    }

    private fun initViews() {
        spinnerCity = findViewById(R.id.spinnerCity)
        spinnerFireStation = findViewById(R.id.spinnerFireStation)
        spinnerCenter = findViewById(R.id.spinnerCenter)
        editTextCaseName = findViewById(R.id.editTextNewCase)
        buttonLogin = findViewById(R.id.buttonLogin)
    }

    private fun setupSpinners() {
        // 예시 데이터 (나중에 DB 연결 가능)
        spinnerCity.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            fetchCityData()
        )
        spinnerFireStation.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            fetchFireStationData()
        )
        spinnerCenter.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            fetchCenterData()
        )
    }

    // 예시 데이터 함수
    private fun fetchCityData(): List<String> = listOf("충남")
    private fun fetchFireStationData(): List<String> = listOf("홍성")
    private fun fetchCenterData(): List<String> = listOf("-")

    // 이전 선택 데이터 복원
    private fun restoreSelections() {
        val selectedCity = prefs.getString("CITY", null)
        val selectedFireStation = prefs.getString("FIRE_STATION", null)
        val selectedCenter = prefs.getString("CENTER", null)
        val caseName = prefs.getString("CASE_NAME", "")

        spinnerCity.setSelection(getSpinnerIndex(spinnerCity, selectedCity))
        spinnerFireStation.setSelection(getSpinnerIndex(spinnerFireStation, selectedFireStation))
        spinnerCenter.setSelection(getSpinnerIndex(spinnerCenter, selectedCenter))
        editTextCaseName.setText(caseName)
    }

    // 선택 데이터 저장
    private fun saveSelections() {
        val editor = prefs.edit()
        editor.putString("CITY", spinnerCity.selectedItem.toString())
        editor.putString("FIRE_STATION", spinnerFireStation.selectedItem.toString())
        editor.putString("CENTER", spinnerCenter.selectedItem.toString())
        editor.putString("CASE_NAME", editTextCaseName.text.toString())
        editor.apply()
    }

    // Spinner에서 선택된 값의 인덱스 가져오기
    private fun getSpinnerIndex(spinner: Spinner, value: String?): Int {
        val adapter = spinner.adapter as ArrayAdapter<String>
        return adapter.getPosition(value ?: "")
    }


    private fun postCheckUserCount() {

        val baseUrl = "https://ev0tafewtt.apigw.ntruss.com/firecapture-api/v0001/user" // ✨ 동적으로 설정 가능
        val apiClient = ApiClient(baseUrl)

        //val jsonBody = """{"userId": 1234, "data": "someData"}"""
        val jsonBody = ""

//        // ✅ 1️⃣ 비동기 방식 (Activation ID 받고 결과 조회)
//        apiClient.requestProcess(jsonBody, isBlocking = false) { activationId ->
//            if (activationId != null) {
//                Log.d("API", "Received Activation ID: $activationId")
//                // 일정 시간 후 결과 조회 (비동기)
//                apiClient.fetchResultPeriodically(activationId) { result ->
//                    Log.d("API", "비동기 처리 완료: $result")
//                }
//            } else {
//                Log.e("API", "비동기 요청 실패")
//            }
//        }

        // ✅ 2️⃣ 동기 방식 (Blocking 처리)
        Thread {
            val response = apiClient.requestProcess(jsonBody, isBlocking = true)
            Log.d("API", "동기 요청 응답: $response")
        }.start()




//
//        GlobalScope.launch(Dispatchers.IO) {
//            val url = "https://ev0tafewtt.apigw.ntruss.com/firecapture-api/v0001/user?blocking=true"
//            val result = StringBuilder()
//            try {
//                val connection = (java.net.URL(url).openConnection() as java.net.HttpURLConnection).apply {
//                    requestMethod = "POST"
//                    setRequestProperty("Content-Type", "application/json")
//                    doOutput = true
//                }
//
//                // 요청 바디 작성 (필요한 경우)
//                val requestBody = "{}" // 빈 JSON 객체
//                connection.outputStream.use { outputStream ->
//                    outputStream.write(requestBody.toByteArray())
//                }
//
//                // 응답 받기
//                connection.inputStream.use { inputStream ->
//                    inputStream.bufferedReader().use { reader ->
//                        reader.forEachLine { result.append(it) }
//                    }
//                }
//
//                // 결과값 콘솔에 출력
//              //  withContext(Dispatchers.Main) {
//                    println("Response: $result")
//               // }
//            } catch (e: Exception) {
//                // 예외 처리
//                e.printStackTrace()
//             //   withContext(Dispatchers.Main) {
//                    println("Error: ${e.message}")
//              //  }
//            }
//        }
    }

}