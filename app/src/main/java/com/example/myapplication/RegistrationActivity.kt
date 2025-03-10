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
    private lateinit var spinnerFireStation: Spinner
    private lateinit var spinnerFireCenter: Spinner
    private lateinit var spinnerFireRegion: Spinner

    private var selectedCity: String? = null
    private var selectedFireSrtation: String? = null
    private var selectedFireCenter: String? = null
    private var selectedFireRegion: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)

        editTextUserName = findViewById(R.id.editTextUserName)
        buttonRegister = findViewById(R.id.buttonRegister)
        spinnerCity = findViewById(R.id.spinnerCity)
        spinnerFireStation = findViewById(R.id.spinnerFireStation)
        spinnerFireCenter = findViewById(R.id.spinnerCenter)
        spinnerFireRegion = findViewById(R.id.spinnerRegion)

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
     * 스피너 초기화 (빈 목록)
     */
    private fun setupEmptySpinner(spinnerGb: String) {
        val emptyList = listOf("선택")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, emptyList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        if(spinnerGb == "fireStation"){
            spinnerFireStation.adapter = adapter
            spinnerFireCenter.adapter = adapter
            spinnerFireRegion.adapter = adapter
        }else if(spinnerGb == "fireCenter"){
            spinnerFireCenter.adapter = adapter
            spinnerFireRegion.adapter = adapter
        }else if(spinnerGb == "fireRegion"){
            spinnerFireRegion.adapter = adapter
        }
    }

    /**
     * 도시 목록으로 스피너 설정
     */
    private fun setupCitySpinner(cityList: List<FireStation>) {
        // 도시 이름만 추출 (중복 제거)
        val cityNamesFromApi = cityList.mapNotNull { it.city }.distinct()
        
        // "선택" 옵션을 리스트 제일 앞에 추가
        val cityNames = mutableListOf<String>()
        cityNames.add("선택")
        cityNames.addAll(cityNamesFromApi)

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
                
                // "선택" 옵션이 아닐 때만 소방서 목록 가져오기
                if (position > 0) {
                    loadFireStationList(selectedCity!!)
                } else {
                    // 소방서 스피너 초기화
                    setupEmptySpinner("fireStation");
                }
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

                // UI 업데이트는 메인 스레드에서 수행해야 함
                runOnUiThread {
                    setupFireStationSpinner(fireStationList)
                }

                // 여기에 소방서 목록을 사용하는 코드 추가 (예: 다른 스피너에 표시)

            } catch (e: Exception) {
                Log.e("RegistrationActivity", "소방서 목록 가져오기 실패", e)
                runOnUiThread {
                    Toast.makeText(this@RegistrationActivity, "소방서 목록을 가져오는데 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    
    /**
     * 소방서 목록으로 스피너 설정
     */
    private fun setupFireStationSpinner(fireStationList: List<FireStation>) {
        // 소방서 이름만 추출 (중복 제거)
        val fireStationNamesFromApi = fireStationList.mapNotNull { it.stationName }.distinct()
        
        // "선택" 옵션을 리스트 제일 앞에 추가
        val fireStationNames = mutableListOf<String>()
        fireStationNames.add("선택")
        fireStationNames.addAll(fireStationNamesFromApi)

        // 어댑터 생성
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, fireStationNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        // 스피너에 어댑터 설정
        spinnerFireStation.adapter = adapter

        // 스피너 선택 이벤트 처리
        spinnerFireStation.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedFireSrtation = fireStationNames[position]
                Log.d("RegistrationActivity", "선택된 소방서: $selectedFireSrtation")

                // "선택" 옵션이 아닐 때만 안전센터 목록 가져오기
                if (position > 0) {
                    loadFireCenterList(selectedFireSrtation!!)
                } else {
                    // 안전센터 스피너 초기화
                    setupEmptySpinner("fireCenter");
                }

            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedFireSrtation = null
            }
        }
    }

    /**
     * 선택된 소방서의 안전센터 목록 가져오기
     */
    private fun loadFireCenterList(stationName: String) {
        lifecycleScope.launch {
            try {
                val fireCenterList = ApiService.selectFireCenterList(stationName)
                Log.d("RegistrationActivity", "안전센터 목록 가져오기 성공: ${fireCenterList.size} 개")

                // UI 업데이트는 메인 스레드에서 수행해야 함
                runOnUiThread {
                    setupFireCenterSpinner(fireCenterList)
                }

            } catch (e: Exception) {
                Log.e("RegistrationActivity", "안전센터 목록 가져오기 실패", e)
                runOnUiThread {
                    Toast.makeText(this@RegistrationActivity, "안전센터 목록을 가져오는데 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * 안전센터 목록으로 스피너 설정
     */
    private fun setupFireCenterSpinner(fireCenterList: List<FireStation>) {
        // 안전센터 이름만 추출 (중복 제거)
        val fireCenterNamesFromApi = fireCenterList.mapNotNull { it.centerName }.distinct()

        // "선택" 옵션을 리스트 제일 앞에 추가
        val fireCenterNames = mutableListOf<String>()
        fireCenterNames.add("선택")
        fireCenterNames.addAll(fireCenterNamesFromApi)

        // 어댑터 생성
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, fireCenterNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        // 스피너에 어댑터 설정
        spinnerFireCenter.adapter = adapter

        // 스피너 선택 이벤트 처리
        spinnerFireCenter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedFireCenter = fireCenterNames[position]
                Log.d("RegistrationActivity", "선택된 안전센터: $selectedFireCenter")

                // "선택" 옵션이 아닐 때만  목록 가져오기
                if (position > 0) {
                    loadFireRegionList(selectedFireCenter!!)
                } else {
                    // 지역대 스피너 초기화
                    setupEmptySpinner("fireRegion");
                }

            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedFireCenter = null
            }
        }
    }

    /**
     * 선택된 안전센터의 지역대 목록 가져오기
     */
    private fun loadFireRegionList(centerName: String) {
        lifecycleScope.launch {
            try {
                val fireRegionList = ApiService.selectFireRegionList(centerName);
                Log.d("RegistrationActivity", "지역대 목록 가져오기 성공: ${fireRegionList.size} 개")

                // UI 업데이트는 메인 스레드에서 수행해야 함
                runOnUiThread {
                    setupFireRegionSpinner(fireRegionList)
                }

            } catch (e: Exception) {
                Log.e("RegistrationActivity", "지역대 목록 가져오기 실패", e)
                runOnUiThread {
                    Toast.makeText(this@RegistrationActivity, "지역대 목록을 가져오는데 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupFireRegionSpinner(fireRegionList: List<FireStation>) {
        // 지역대 이름만 추출 (중복 제거)
        val fireRegionNamesFromApi = fireRegionList.mapNotNull { it.regionNm }.distinct()

        // "선택" 옵션을 리스트 제일 앞에 추가
        val fireRegionNames = mutableListOf<String>()
        fireRegionNames.add("선택")
        fireRegionNames.addAll(fireRegionNamesFromApi)

        // 어댑터 생성
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, fireRegionNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        // 스피너에 어댑터 설정
        spinnerFireRegion.adapter = adapter

        // 스피너 선택 이벤트 처리
        spinnerFireRegion.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedFireRegion = fireRegionNames[position]
                Log.d("RegistrationActivity", "선택된 지역대: $selectedFireRegion")

                if (position > 0) {
                    // 선택된 지역대 이름과 일치하는 FireStation 객체 찾기
                    val selectedRegion = fireRegionList.find { it.regionNm == selectedFireRegion }
                    
                    // 찾은 객체에서 ID 값 가져오기
                    val regionId = selectedRegion?.id
                    
                    // ID 값 로그로 출력
                    Log.d("RegistrationActivity", "선택된 지역대 ID: $regionId")
                    
                    // 여기서 ID 값을 사용하여 추가 작업 수행
                    // 예: 저장하거나 다른 API 호출에 사용
                }


            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedFireRegion = null
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
