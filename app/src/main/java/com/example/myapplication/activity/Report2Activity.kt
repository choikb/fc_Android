package com.example.myapplication.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
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
import com.example.myapplication.util.ConfigUtil
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class Report2Activity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report2)
        var prev: Button = findViewById(R.id.prev)
        var save: Button = findViewById(R.id.save)
        prev.setOnClickListener {
            super.onBackPressed()
        }
        save.setOnClickListener {
            // 다운로드
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(ConfigUtil.getRootUrl() +"/report.jpg")).apply {
                // 새 태스크로 실행하고 싶다면 플래그 추가 (옵션)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
        }
    }

}