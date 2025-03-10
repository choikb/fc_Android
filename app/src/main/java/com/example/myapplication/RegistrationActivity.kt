package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.api.ApiService
import com.example.myapplication.model.FireStation
import kotlinx.coroutines.launch

class RegistrationActivity : AppCompatActivity() {

    private lateinit var editTextUserName: EditText
    private lateinit var buttonRegister: Button
    private lateinit var deviceId: String
    private lateinit var spinnerCity: Spinner
    private var selectedCity: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)

        editTextUserName = findViewById(R.id.editTextUserName)
        buttonRegister = findViewById(R.id.buttonRegister)
        spinnerCity = findViewById(R.id.spinnerCity)

        // 디바이스 ID 가져오기
        deviceId = getDeviceId(this)
        findViewById<TextView>(R.id.textDeviceId).text = deviceId


        // 코루틴을 사용하여 백그라운드에서 도시 목록 가져오기
        lifecycleScope.launch {
            try {
                val cityList = ApiService.selectCityList()
                Log.d("RegistrationActivity", "도시 목록 가져오기 성공: ${cityList.size} 개")
                
                // UI 업데이트는 메인 스레드에서 수행해야 함
                runOnUiThread {
                    setupCitySpinner(cityList)
                }
            } catch (e: Exception) {
                Log.e("RegistrationActivity", "도시 목록 가져오기 실패", e)
                // 오류 발생 시 사용자에게 알림
                runOnUiThread {
                    Toast.makeText(this@RegistrationActivity, "도시 목록을 가져오는데 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 소방서 목록 가져오기는 도시 선택 후에 수행됨




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

    /**
     * 도시 목록으로 스피너 설정
     */
    private fun setupCitySpinner(cityList: List<FireStation>) {
        // 도시 이름만 추출 (중복 제거)
        val cityNames = cityList.mapNotNull { it.city }.distinct()
        
        // 어댑터 생성
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, cityNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        
        // 스피너에 어댑터 설정
        spinnerCity.adapter = adapter
        
        // 스피너 선택 이벤트 처리
        spinnerCity.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedCity = cityNames[position]
                Log.d("RegistrationActivity", "선택된 도시: $selectedCity")
                
                // 선택된 도시에 해당하는 소방서 목록 가져오기
                loadFireStationList(selectedCity!!)
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedCity = null
            }
        }
    }
    
    /**
     * 선택된 도시의 소방서 목록 가져오기
     */
    private fun loadFireStationList(cityName: String) {
        lifecycleScope.launch {
            try {
                val fireStationList = ApiService.selectFireStationList(cityName)
                Log.d("RegistrationActivity", "소방서 목록 가져오기 성공: ${fireStationList.size} 개")
                
                // 여기에 소방서 목록을 사용하는 코드 추가 (예: 다른 스피너에 표시)
                
            } catch (e: Exception) {
                Log.e("RegistrationActivity", "소방서 목록 가져오기 실패", e)
                runOnUiThread {
                    Toast.makeText(this@RegistrationActivity, "소방서 목록을 가져오는데 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    // 디바이스 ID 가져오기
    private fun getDeviceId(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }

    // 서버에 디바이스 ID 존재 여부 확인 (동기식 API 요청)
    private fun checkIfDeviceRegistered(deviceId: String): Boolean {
        return ApiService.checkIfDeviceRegistered(deviceId)
    }

    // 사용자 등록 요청 (동기식 API 요청)
    private fun registerUser(deviceId: String, userName: String): Boolean {
        return ApiService.registerUser(deviceId, userName)
    }
}
