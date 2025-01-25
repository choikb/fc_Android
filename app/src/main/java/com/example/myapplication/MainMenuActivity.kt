package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainMenuActivity : AppCompatActivity() {


    private lateinit var textViewUserInfo: TextView
    private lateinit var textViewUserRank: TextView
    private lateinit var textViewUserName: TextView
    private lateinit var textViewCaseName: TextView
    private lateinit var buttonGoCapture: Button
    private lateinit var buttonGoSelectMedia: Button
    private lateinit var buttonLogout: Button

    // 로그인 시 넘겨받은 정보
    private var affiliation = ""
    private var rank = ""
    private var name = ""
    private var caseName = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_menu)

        initViews()
        getUserDataFromIntent()
        showUserInfo()
        setupButtonListeners()
    }

    private fun initViews() {
        textViewUserInfo = findViewById(R.id.textViewUserInfo)
        textViewUserRank = findViewById(R.id.textViewUserRank)
        textViewUserName = findViewById(R.id.textViewUserName)
        textViewCaseName = findViewById(R.id.textViewCaseName)
        buttonGoCapture = findViewById(R.id.buttonGoCapture)
        buttonGoSelectMedia = findViewById(R.id.buttonGoSelectMedia)
        buttonLogout = findViewById(R.id.buttonLogout)
    }

    private fun getUserDataFromIntent() {
        affiliation = intent.getStringExtra("AFFILIATION") ?: ""
        rank = intent.getStringExtra("RANK") ?: ""
        name = intent.getStringExtra("NAME") ?: ""
        caseName = intent.getStringExtra("CASE_NAME") ?: ""
    }
    private fun showUserInfo() {
        textViewUserInfo.text = "소속: $affiliation"
        textViewUserRank.text = "계급: $rank"
        textViewUserName.text = "성명: $name"
        textViewCaseName.text = "사건명: $caseName"
    }

    private fun setupButtonListeners() {
        // 촬영 화면 이동
        buttonGoCapture.setOnClickListener {
            val intent = Intent(this, CaptureActivity::class.java).apply {
                putExtra("AFFILIATION", affiliation)
                putExtra("RANK", rank)
                putExtra("NAME", name)
                putExtra("CASE_NAME", caseName)
            }
            startActivity(intent)
        }

        // 미디어 선택 화면 이동
        buttonGoSelectMedia.setOnClickListener {
            val intent = Intent(this, SelectMediaActivity::class.java).apply {
                putExtra("AFFILIATION", affiliation)
                putExtra("RANK", rank)
                putExtra("NAME", name)
                putExtra("CASE_NAME", caseName)
            }
            startActivity(intent)
        }

        // 로그아웃(또는 앱 종료)
        buttonLogout.setOnClickListener {
            // 다시 LoginActivity로 돌아가거나, 앱을 종료하는 로직
            finishAffinity() // 앱 종료
        }
    }
}
