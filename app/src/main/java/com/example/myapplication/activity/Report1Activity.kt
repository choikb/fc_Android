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

class Report1Activity : AppCompatActivity() {

    private lateinit var timeEditText: EditText
    private lateinit var locationEditText: EditText
    private lateinit var targetEditText: EditText
    private lateinit var summaryEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report1)
        
        // Initialize EditText fields
        timeEditText = findViewById<EditText>(R.id.time_edit_text)
        locationEditText = findViewById<EditText>(R.id.location_edit_text)
        targetEditText = findViewById<EditText>(R.id.target_edit_text)
        summaryEditText = findViewById<EditText>(R.id.summary_edit_text)
        
        // Initialize buttons
        val loadButton: Button = findViewById(R.id.btn_load)
        val prev: Button = findViewById(R.id.prev)
        val next: Button = findViewById(R.id.next)
        
        // Set click listener for load button
        loadButton.setOnClickListener {
            loadHardcodedData()
        }
        
        prev.setOnClickListener {
            super.onBackPressed()
        }
        next.setOnClickListener {
            val intent = Intent(this, Report2Activity::class.java).apply {}
            startActivity(intent)
        }
    }
    
    private fun loadHardcodedData() {
        // Load hardcoded data into the fields
        timeEditText.setText("2025.1.1 05:58 (신고접수)")
        locationEditText.setText("충청남도 홍성군 결성면 무량리 492")
        // Clear other fields
        targetEditText.setText("")
        summaryEditText.setText("")
        
        Toast.makeText(this, "데이터를 불러왔습니다.", Toast.LENGTH_SHORT).show()
    }
}