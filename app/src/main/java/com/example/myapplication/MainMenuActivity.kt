package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager

class MainMenuActivity : AppCompatActivity() {

    private lateinit var textViewUserInfo: TextView
    private lateinit var textViewUserName: TextView
    private lateinit var textViewCaseName: TextView
    private lateinit var buttonGoCapture: Button
    private lateinit var buttonGoSelectMedia: Button
    private lateinit var buttonLogout: Button

    // 사용자 정보
    private var affiliation = ""
    private var name = ""
    private var caseName = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_menu)

        initViews()
        getUserData()  // ✅ 통합된 데이터 불러오기
        showUserInfo()
        setupButtonListeners()
    }

    private fun initViews() {
        textViewUserInfo = findViewById(R.id.textViewUserInfo)
        textViewUserName = findViewById(R.id.textViewUserName)
        textViewCaseName = findViewById(R.id.textViewCaseName)
        buttonGoCapture = findViewById(R.id.buttonGoCapture)
        buttonGoSelectMedia = findViewById(R.id.buttonGoSelectMedia)
        buttonLogout = findViewById(R.id.buttonLogout)
    }

    private fun getUserData() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)

        // ✅ Intent로 데이터 전달받기 (없으면 SharedPreferences에서 불러오기)
        affiliation = intent.getStringExtra("AFFILIATION") ?: prefs.getString("AFFILIATION", "정보 없음") ?: "정보 없음"
        name = intent.getStringExtra("NAME") ?: prefs.getString("userName", "알 수 없음") ?: "알 수 없음"
        caseName = intent.getStringExtra("CASE_NAME") ?: prefs.getString("CASE_NAME", "없음") ?: "없음"

        // ✅ 불러온 데이터를 SharedPreferences에 저장 (필요 시 업데이트)
        val editor = prefs.edit()
        editor.putString("AFFILIATION", affiliation)
        editor.putString("userName", name)
        editor.putString("CASE_NAME", caseName)
        editor.apply()
    }


    private fun showUserInfo() {
        textViewUserInfo.text = affiliation
        textViewUserName.text = name
        textViewCaseName.text = caseName
    }

    private fun setupButtonListeners() {
        // 촬영 화면 이동
        buttonGoCapture.setOnClickListener {
            val intent = Intent(this, CaptureActivity::class.java).apply {
                putExtra("AFFILIATION", affiliation)
                putExtra("NAME", name)
                putExtra("CASE_NAME", caseName)
            }
            startActivity(intent)
        }

        // 미디어 선택 화면 이동
        buttonGoSelectMedia.setOnClickListener {
            val intent = Intent(this, SelectMediaActivity::class.java).apply {
                putExtra("AFFILIATION", affiliation)
                putExtra("NAME", name)
                putExtra("CASE_NAME", caseName)
            }
            startActivity(intent)
        }

        // 로그아웃 (앱 종료)
        buttonLogout.setOnClickListener {
            finishAffinity()
        }
    }
}
