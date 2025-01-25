package com.example.myapplication
import com.example.myapplication.DatabaseConfig
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.sql.DriverManager

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

        // Android ID 가져오기
        val androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)

// DB에서 Android ID 확인
        runBlocking {
            val isRegistered = checkAndroidId(androidId)
            if (!isRegistered) {
                // Android ID가 없으면 등록 화면 표시
                showRegistrationScreen(androidId)
            } else {
                // Android ID가 등록되어 있는 경우
              //  proceedToMainScreen()
            }
        }


        setupSpinners()
        restoreSelections() // 이전 선택 데이터 복원

        buttonLogin.setOnClickListener {
            saveSelections() // 선택 데이터 저장

            val city = spinnerCity.selectedItem.toString()
            val fireStation = spinnerFireStation.selectedItem.toString()
            val center = spinnerCenter.selectedItem.toString()
            val caseName = editTextCaseName.text.toString()

            // MainMenuActivity로 이동하며, 입력값을 전달
            val intent = Intent(this, MainMenuActivity::class.java).apply {
                putExtra("CITY", city)
                putExtra("FIRE_STATION", fireStation)
                putExtra("CENTER", center)
                putExtra("CASE_NAME", caseName)
            }
            startActivity(intent)
            finish() // 로그인 화면은 종료
        }
    }

    private suspend fun checkAndroidId(androidId: String): Boolean {
        return DatabaseHelper.isAndroidIdRegistered(androidId)
    }

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
    private fun fetchCityData(): List<String> = listOf("서울", "충남")
    private fun fetchFireStationData(): List<String> = listOf("성북", "종로", "천안서북", "공주")
    private fun fetchCenterData(): List<String> = listOf("차암", "성거")

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
}