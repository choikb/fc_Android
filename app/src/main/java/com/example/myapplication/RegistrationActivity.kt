package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class RegistrationActivity : AppCompatActivity() {

    private lateinit var editTextUserName: EditText
    private lateinit var buttonRegister: Button
    private lateinit var deviceId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)

        editTextUserName = findViewById(R.id.editTextUserName)
        buttonRegister = findViewById(R.id.buttonRegister)

        // 디바이스 ID 가져오기
        deviceId = getDeviceId(this)
        findViewById<TextView>(R.id.textDeviceId).text = deviceId
//        // 서버에 디바이스 ID가 있는지 확인
//        Thread {
//            val isRegistered = checkIfDeviceRegistered(deviceId)
//            runOnUiThread {
//                if (isRegistered) {
//                    Toast.makeText(this, "이미 등록된 디바이스입니다.", Toast.LENGTH_SHORT).show()
//                    startActivity(Intent(this, LoginActivity::class.java))
//                    finish()
//                }
//            }
//        }.start()

        // 취소 버튼을 눌렀을 때 현재 화면을 종료하고 이전 화면으로 돌아가기
        findViewById<Button>(R.id.buttonLogout).setOnClickListener {
            finish() // 현재 Activity 종료 -> 이전 화면으로 돌아감
        }


        buttonRegister.setOnClickListener {
            val userName = editTextUserName.text.toString().trim()
            if (userName.isEmpty()) {
                Toast.makeText(this, "사용자명을 입력하세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // ✅ 사용자 등록 요청 (동기식 처리)
            Thread {
                val success = registerUser(deviceId, userName)
                runOnUiThread {
                    if (success) {
                        Toast.makeText(this, "사용자 등록 성공!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this, "사용자 등록 실패!", Toast.LENGTH_SHORT).show()
                    }
                }
            }.start()
        }
    }

    // 디바이스 ID 가져오기
    private fun getDeviceId(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }

    // 서버에 디바이스 ID 존재 여부 확인 (동기식 API 요청)
    private fun checkIfDeviceRegistered(deviceId: String): Boolean {
        val url = "https://ev0tafewtt.apigw.ntruss.com/firecapture-api/v0001/users?blocking=true&result=true"
        val jsonObject = JSONObject().apply {
            put("action", "checkDevice")
            put("device_id", deviceId)
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
                jsonResponse.optBoolean("registered", false)
            } else {
                false
            }
        } catch (e: IOException) {
            Log.e("API_ERROR", "🚨 API 요청 실패: ${e.message}", e)
            false
        }
    }

    // 사용자 등록 요청 (동기식 API 요청)
    private fun registerUser(deviceId: String, userName: String): Boolean {
        val url = "https://ev0tafewtt.apigw.ntruss.com/firecapture-api/v0001/users?blocking=true&result=true"

        val jsonObject = JSONObject().apply {
            put("action", "addUser")
            put("userId", deviceId)  // 사용자 ID를 디바이스 ID와 동일하게 설정
            put("deviceId", deviceId)
            put("organization", "충남소방") // 조직 정보 (임시 값)
            put("rank", "Admin") // 기본 등급
            put("name", userName)
            put("role", "화재조사") // 기본 역할
            put("email", "") // 이메일 (추후 입력)
            put("phoneNumber", "") // 전화번호 (추후 입력)

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
                val bodyJson = JSONObject(jsonResponse.optString("body", "{}"))

                val success = bodyJson.optBoolean("success", false)

                // ✅ `affectedRows` 값 확인 후 성공 여부 반환
                val affectedRows = bodyJson.optJSONObject("data")?.optInt("affectedRows", 0) ?: 0
                Log.d("API_RESULT", "affectedRows: $affectedRows")

                return success && affectedRows > 0
            } else {
                false
            }
        } catch (e: IOException) {
            Log.e("API_ERROR", "🚨 API 요청 실패: ${e.message}", e)
            false
        }
    }
}
