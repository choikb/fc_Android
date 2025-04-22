package com.example.myapplication.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.example.myapplication.ApiClient
import com.example.myapplication.IncidentManager
import com.example.myapplication.R
import com.example.myapplication.api.ApiService
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class LoginActivity : AppCompatActivity() {

    private val permissions = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.CAMERA
        )

        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
        )

        else -> arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
        )
    }


    private lateinit var textCity: TextView
    private lateinit var textFireStation: TextView
    private lateinit var textCenter: TextView
    private lateinit var textRegion: TextView
    private lateinit var spinnerExistingCase: Spinner

    private lateinit var editTextCaseName: EditText
    private lateinit var buttonLogin: Button
    private lateinit var textUserName: TextView

    private lateinit var radioNewCase: RadioButton

    private val prefs by lazy { PreferenceManager.getDefaultSharedPreferences(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//            if (!Environment.isExternalStorageManager()) {
//                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
//                intent.data = Uri.parse("package:" + packageName)
//                startActivity(intent)
//            }
//        }
        requestAllPermissions()
        //gui setting-------------------------------------------------------------------
        setContentView(R.layout.activity_login)
        initViews()
        setupSpinners()
        restoreSelections() // 이전 선택 데이터 복원

        val radioGroupCase: RadioGroup = findViewById(R.id.radioGroupCase)
        val editTextNewCase: EditText = findViewById(R.id.editTextNewCase)
        val spinnerExistingCase: Spinner = findViewById(R.id.spinnerExistingCase)
        // 초기 설정: 신규 입력만 표시
        editTextNewCase.visibility = View.VISIBLE
        spinnerExistingCase.visibility = View.GONE

        // 라디오 버튼 선택 시 처리
        radioGroupCase.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radioNewCase -> {
                    // 신규 입력 활성화
                    editTextNewCase.visibility = View.VISIBLE
                    spinnerExistingCase.visibility = View.GONE
                }

                R.id.radioExistingCase -> {
                    // 기존 선택 활성화
                    editTextNewCase.visibility = View.GONE
                    spinnerExistingCase.visibility = View.VISIBLE
                }
            }
        }
        //gui setting-------------------------------------------------------------------


        // Android ID 가져오기
        val androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        val prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putString("ANDROID_ID", androidId)  // androidId를 "ANDROID_ID" 키로 저장
        editor.apply()


        lifecycleScope.launch {
            // DB에서 Android ID 확인
            if (!isAndroidIdRegistered(androidId)) {
                // Android ID가 없으면 등록 화면 표시
                showRegistrationScreen(androidId)
                Log.d("API_RESULT", "사용자가 등록되지 않음")
            } else {
                Log.d("API_RESULT", "사용자가 등록됨")
                val userName = getValuePreferences("userName")
                val cityName = getValuePreferences("cityName")
                val fireStationName = getValuePreferences("fireStationName")
                val centerName = getValuePreferences("centerName")
                val regionName = getValuePreferences("regionName")
                textUserName.text = userName
                textCity.text = cityName
                textFireStation.text = fireStationName
                textCenter.text = centerName
                textRegion.text = regionName

            }

        }

        // 사건명 가져오기 및 스피너 초기화
        lifecycleScope.launch {
            IncidentManager.fetchRecentIncidents { success ->
                runOnUiThread {
                    if (success) {
                        setupExistingCaseSpinner(IncidentManager.recentIncidents)
                    } else {
                        Toast.makeText(
                            this@LoginActivity,
                            "사건 데이터를 불러오지 못했습니다.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }


        Log.d("DEBUG", "editTextCaseName initialized: $editTextCaseName")

        // 신규 입력 시 중복 검증
        editTextCaseName.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val newCaseName = s.toString().trim()
                Log.d("TEXT_CHANGE", "입력된 사건명: $newCaseName")

                if (IncidentManager.recentIncidents.contains(newCaseName)) {
                    Log.d("TEXT_CHANGE", "❌ 중복된 사건명입니다!")
                    editTextCaseName.setTextColor(Color.RED)
                    Toast.makeText(this@LoginActivity, "이미 존재하는 사건명입니다.", Toast.LENGTH_SHORT).show()
                    buttonLogin.isEnabled = false //
                } else {
                    editTextCaseName.setTextColor(Color.BLACK)
                    buttonLogin.isEnabled = true //
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // 텍스트 수정 시 색상 원복
        editTextCaseName.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                editTextCaseName.setTextColor(Color.BLACK)
            }
        }

        // 사용자명 전시



        buttonLogin.setOnClickListener {

            saveSelections() // 선택 데이터 저장

            val city = textCity.text.toString()
            val fireStation = textFireStation.text.toString()
            val center = textCenter.text.toString()
            val region = textRegion.text.toString()
            val caseName: String

            // 신규 입력 / 기존 선택 여부 확인
            if (radioNewCase.isChecked) {
                caseName = editTextCaseName.text.toString().trim()
                if (caseName.isEmpty()) {
                    Toast.makeText(this, "사건명을 입력하세요.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                AlertDialog.Builder(this)
                    .setTitle("사건 등록")
                    .setMessage("사건 등록 하시겠습니까?")
                    .setPositiveButton("네") { dialog, _ ->
                        dialog.dismiss()

                // ✅ 신규 사건 등록 API 호출 (동기 방식)
                    lifecycleScope.launch {
                        val incidentId = registerIncident(caseName)
                        runOnUiThread {
                            if (incidentId != null) {

                                Toast.makeText(this@LoginActivity, "사건 등록 성공!", Toast.LENGTH_SHORT).show()

                                // ✅ **신규 사건 정보 전역 변수 저장**
                                IncidentManager.clear()
                                IncidentManager.incidentId = incidentId
                                IncidentManager.incidentName = caseName

                                // ✅ MainMenuActivity로 이동
                                navigateToMainMenu(city, fireStation, center, caseName)
                            } else {
                                Toast.makeText(this@LoginActivity, "사건 등록 실패!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                .setNegativeButton("취소") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
            } else {
                // 기존 사건 선택 시
                caseName = spinnerExistingCase.selectedItem.toString()

                if (caseName.isEmpty()) {
                    Toast.makeText(this, "기존 사건을 선택하세요.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // ✅ 기존 사건 조회 후 incident_id 가져오기
                lifecycleScope.launch {
                    val incidentId = getIncidentIdByName(caseName)
                    runOnUiThread {
                        if (incidentId != null) {
                            Toast.makeText(this@LoginActivity, "기존 사건 선택 완료!", Toast.LENGTH_SHORT).show()

                            // ✅ 사건 정보 전역 변수 저장
                            IncidentManager.clear()
                            IncidentManager.incidentId = incidentId
                            IncidentManager.incidentName = caseName

                            // ✅ MainMenuActivity로 이동
                            navigateToMainMenu(city, fireStation, center, caseName)
                        } else {
                            Toast.makeText(this@LoginActivity, "사건 조회 실패!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

    }
    // ✅ 권한 요청 함수
    private fun requestAllPermissions() {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES) // Android 13+
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE) // Android 12 이하
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
           // permissions.add(Manifest.permission.ACCESS_MEDIA_LOCATION) // GPS 정보 포함된 미디어 접근
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
          //  permissions.add(Manifest.permission.MANAGE_EXTERNAL_STORAGE) // Android 11+ 전체 파일 접근
        }

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), 1001)
        } else {
            Log.d("Permission", "✅ 모든 권한이 이미 허용됨!")
        }
    }


    // ✅ 권한 요청 결과 처리
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001) {
            val deniedPermissions = permissions.zip(grantResults.toList())
                .filter { it.second != PackageManager.PERMISSION_GRANTED }
                .map { it.first }

            if (deniedPermissions.isEmpty()) {
                Log.d("Permission", "✅ 모든 권한 허용됨!")
            } else {
                Log.e("Permission", "⛔ 거부된 권한: $deniedPermissions")
                Toast.makeText(this, "일부 권한이 거부되었습니다. 기능이 제한될 수 있습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }
//    private fun requestStoragePermission() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13 이상
//            requestPermissions(
//                arrayOf(
//                    Manifest.permission.READ_MEDIA_IMAGES,
//                    Manifest.permission.READ_MEDIA_VIDEO,
//                    Manifest.permission.READ_MEDIA_AUDIO
//                ), STORAGE_PERMISSION_REQUEST
//            )
//        } else { // Android 12 이하
//            requestPermissions(
//                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
//                STORAGE_PERMISSION_REQUEST
//            )
//        }
//    }





    private suspend fun getIncidentIdByName(caseName: String): Long? {

        return try {
            var incidentList = ApiService.selectIncidentList(incidentName = caseName)

            if(incidentList.isNotEmpty()){
                incidentList.get(0).incidentId!!.toLong()
            }else{
                null
            }

        } catch (e: Exception) {
            Log.e("API_ERROR", "🚨 사건 조회 API 요청 실패: ${e.message}", e)
            null
        }
    }

//    private fun navigateToMainMenu(city: String, fireStation: String, center: String, caseName: String) {
//        val intent = Intent(this, MainMenuActivity::class.java).apply {
//            putExtra("CITY", city)
//            putExtra("FIRE_STATION", fireStation)
//            putExtra("CENTER", center)
//            putExtra("CASE_NAME", caseName)
//        }
//        startActivity(intent)
//        finish() // 로그인 화면 종료
//    }
private fun navigateToMainMenu(city: String, fireStation: String, center: String, caseName: String) {
    val prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
    val userName = prefs.getString("userName", "알 수 없음") ?: "알 수 없음"  // ✅ 저장된 사용자명 불러오기

    // ✅ 소속 정보 통합하여 전달
    val affiliation = "$city $fireStation $center"

    val intent = Intent(this, MainMenuActivity::class.java).apply {
        putExtra("AFFILIATION", affiliation)  // ✅ 소속 정보 전달
        putExtra("NAME", userName)            // ✅ 사용자명 전달
        putExtra("CASE_NAME", caseName)       // ✅ 사건명 전달
    }
    startActivity(intent)
    finish() // 로그인 화면 종료
}
    //setupExistingCaseSpinner 함수로 기존 사건명을 설정합니다
    private fun setupExistingCaseSpinner(incidentList: List<String>) {
        val adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, incidentList)
        spinnerExistingCase.adapter = adapter
    }

    // 사건명 중복 확인
    private fun checkCaseNameDuplicate(caseName: String, callback: (Boolean) -> Unit) {
        val url = "https://your-api-endpoint-url" // API Gateway URL
        val jsonObject = JSONObject().apply {
            put("action", "checkIncidentName")
            put("caseName", caseName)
        }

        val requestBody =
            jsonObject.toString().toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .header("Content-Type", "application/json")
            .build()

        val client = OkHttpClient()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                callback(false)
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.d("API_RESPONSE", "Response: $responseBody")
                if (!responseBody.isNullOrEmpty()) {
                    val jsonResponse = JSONObject(responseBody)
                    val isDuplicate = jsonResponse.optBoolean("isDuplicate", false)
                    callback(isDuplicate)
                } else {
                    callback(false)
                }
            }
        })
    }

    // 사용자명 가져오기 (SharedPreferences 예제)
    private fun getValuePreferences(key: String): String {
        // SharedPreferences에서 사용자명 가져오기
        val prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        return prefs.getString(key, "알 수 없음") ?: "알 수 없음"
    }

    private suspend fun registerIncident(caseName: String): Long? {

        return try {

            var incidentId = ApiService.registIncident(caseName, Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID))

            if(incidentId > -1){
                IncidentManager.incidentId = incidentId.toLong()
                IncidentManager.incidentName = caseName  // 사건명도 저장
                return IncidentManager.incidentId
            }else{
                null
            }
        } catch (e: Exception) {
            Log.e("API_ERROR", "🚨 사건 등록 API 요청 실패: ${e.message}", e)
            null
        }
    }

    private suspend fun isAndroidIdRegistered(androidId: String): Boolean { //쓰레드 사용코드

        return try {
            var user = ApiService.selectUserDtl(androidId);

            if(user !== null){
                val prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
                val editor = prefs.edit()
                editor.putString("userName", user.name)
                editor.putString("cityName", user.city)
                editor.putString("fireStationName", user.stationName)
                editor.putString("centerName", user.centerName)
                editor.putString("regionName", user.regionNm)

                editor.apply()

                return true
            }else {
                return false
            }
        } catch (e: Exception) {
            Log.e("API_ERROR", "🚨 API 요청 실패: ${e.message}", e)
            false
        }
    }


    private fun showRegistrationScreen(androidId: String) {
        val intent = Intent(this, RegistrationActivity::class.java).apply {
            putExtra("ANDROID_ID", androidId)
        }
        startActivity(intent)
        finish()
    }

    private fun initViews() {
        textCity = findViewById(R.id.textCity)
        textFireStation = findViewById(R.id.textFireStation)
        textCenter = findViewById(R.id.textCenter)
        textRegion = findViewById(R.id.textRegion)

        buttonLogin = findViewById(R.id.buttonLogin)
        spinnerExistingCase = findViewById(R.id.spinnerExistingCase)
        editTextCaseName = findViewById(R.id.editTextNewCase)
        textUserName = findViewById(R.id.textUserName)

        radioNewCase = findViewById(R.id.radioNewCase)

    }

    private fun setupSpinners() {
        // TextView로 변환되어 더 이상 필요하지 않음
    }

    // 예시 데이터 함수
    private fun fetchCityData(): List<String> = listOf("충남")
    private fun fetchFireStationData(): List<String> = listOf("홍성")
    private fun fetchCenterData(): List<String> = listOf("-")

    // 이전 선택 데이터 복원
    private fun restoreSelections() {
        // TextView로 변환되어 더 이상 필요하지 않음
        val caseName = prefs.getString("CASE_NAME", "")
        // editTextCaseName.setText(caseName)
    }

    // 선택 데이터 저장
    private fun saveSelections() {
        val editor = prefs.edit()
        editor.putString("CITY", textCity.text.toString())
        editor.putString("FIRE_STATION", textFireStation.text.toString())
        editor.putString("CENTER", textCenter.text.toString())
        editor.putString("REGION", textRegion.text.toString())
        //editor.putString("CASE_NAME", editTextCaseName.text.toString())
        editor.apply()
    }

    // Spinner에서 선택된 값의 인덱스 가져오기
    private fun getSpinnerIndex(spinner: Spinner, value: String?): Int {
        val adapter = spinner.adapter as ArrayAdapter<String>
        return adapter.getPosition(value ?: "")
    }


    private fun postCheckUserCount() {

        val baseUrl =
            "https://ev0tafewtt.apigw.ntruss.com/firecapture-api/v0001/user" // ✨ 동적으로 설정 가능
        val apiClient = ApiClient(baseUrl)

        //val jsonBody = """{"userId": 1234, "data": "someData"}"""
        val jsonBody = ""



        // ✅ 2️⃣ 동기 방식 (Blocking 처리)
        Thread {
            val response = apiClient.requestProcess(jsonBody, isBlocking = true)
            Log.d("API", "동기 요청 응답: $response")
        }.start()



    }

}