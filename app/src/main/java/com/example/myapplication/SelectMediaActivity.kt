package com.example.myapplication

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.media.MediaType
import com.example.myapplication.media.MediaItem
import com.example.myapplication.media.MediaAdapter

class SelectMediaActivity : AppCompatActivity() {

    private lateinit var radioGroupMediaFilter: RadioGroup
    private lateinit var radioButtonAll: RadioButton
    private lateinit var radioButtonPhoto: RadioButton
    private lateinit var radioButtonVideo: RadioButton

    private lateinit var gridViewMedia: GridView
    private lateinit var buttonSelectAll: Button
    private lateinit var editTextMemo: EditText
    private lateinit var buttonUpload: Button
    private lateinit var buttonGoCapture: Button
    private lateinit var buttonGoMainMenu: Button

    private var affiliation = ""
    private var rank = ""
    private var nameStr = ""
    private var caseName = ""

    private val allMediaList = mutableListOf<MediaItem>()
    private val filteredList = mutableListOf<MediaItem>()
    private lateinit var adapter: MediaAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_media)

        initViews()
        getUserDataFromIntent()
        checkPermissions()
        setupFilterListeners()
        setupButtonListeners() // 버튼 리스너 추가
        updateGridView()
    }
    /**
     * 라디오 버튼(전체/사진/동영상) 상태 변화에 따라 목록 갱신
     */
    private fun setupFilterListeners() {
        radioGroupMediaFilter.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radioButtonAll -> {
                    filterMedia(MediaType.ALL)
                }
                R.id.radioButtonPhoto -> {
                    filterMedia(MediaType.PHOTO)
                }
                R.id.radioButtonVideo -> {
                    filterMedia(MediaType.VIDEO)
                }
            }
        }
    }
    private fun filterMedia(filterType: MediaType) {
        filteredList.clear()

        when (filterType) {
            MediaType.ALL -> {
                filteredList.addAll(allMediaList)
            }

            MediaType.PHOTO -> {
                filteredList.addAll(allMediaList.filter { it.type == MediaType.PHOTO })
            }

            MediaType.VIDEO -> {
                filteredList.addAll(allMediaList.filter { it.type == MediaType.VIDEO })
            }
        }
        updateGridView()
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11 이상
            if (!Environment.isExternalStorageManager()) {
                requestManageStoragePermission()
            } else {
                loadAllMediaList()
            }
        } else {
            // Android 11 미만
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1)
            } else {
                loadAllMediaList()
            }
        }
    }

    private fun requestManageStoragePermission() {
        val intent = Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
        intent.data = Uri.parse("package:$packageName")
        startActivity(intent)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadAllMediaList() // 권한 승인 시 미디어 로드
            } else {
                Toast.makeText(this, "권한이 필요합니다. 설정에서 권한을 허용해주세요.", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun loadAllMediaList() {
        allMediaList.clear()

        // 사진 가져오기
        val imageCursor = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATA
            ),
            null,
            null,
            "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )

        imageCursor?.use {
            val nameColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val dataColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)

            while (it.moveToNext()) {
                val name = it.getString(nameColumn)
                val data = it.getString(dataColumn)

                allMediaList.add(MediaItem(MediaType.PHOTO, name, data))
            }
        }

        // 동영상 가져오기
        val videoCursor = contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DATA
            ),
            null,
            null,
            "${MediaStore.Video.Media.DATE_ADDED} DESC"
        )

        videoCursor?.use {
            val nameColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val dataColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)

            while (it.moveToNext()) {
                val name = it.getString(nameColumn)
                val data = it.getString(dataColumn)

                allMediaList.add(MediaItem(MediaType.VIDEO, name, data))
            }
        }

        // 디버깅 코드 추가
        Log.d("MediaStore", "Media List Size: ${allMediaList.size}")
        allMediaList.forEach {
            Log.d("MediaStore", "Item: ${it.name}, Path: ${it.path}")
        }

        // 필터 초기화 및 GridView 업데이트
        filteredList.clear()
        filteredList.addAll(allMediaList)
        updateGridView()
    }

    private fun initViews() {
        radioGroupMediaFilter = findViewById(R.id.radioGroupMediaFilter)
        radioButtonAll = findViewById(R.id.radioButtonAll)
        radioButtonPhoto = findViewById(R.id.radioButtonPhoto)
        radioButtonVideo = findViewById(R.id.radioButtonVideo)

        gridViewMedia = findViewById(R.id.gridViewMedia)
        buttonSelectAll = findViewById(R.id.buttonSelectAll)
        editTextMemo = findViewById(R.id.editTextMemo)
        buttonUpload = findViewById(R.id.buttonUpload)
        buttonGoCapture = findViewById(R.id.buttonGoCapture)
        buttonGoMainMenu = findViewById(R.id.buttonGoMainMenu)

        adapter = MediaAdapter(this, filteredList)
        gridViewMedia.adapter = adapter
    }

    private fun getUserDataFromIntent() {
        affiliation = intent.getStringExtra("AFFILIATION") ?: ""
        rank = intent.getStringExtra("RANK") ?: ""
        nameStr = intent.getStringExtra("NAME") ?: ""
        caseName = intent.getStringExtra("CASE_NAME") ?: ""
    }

    private fun setupButtonListeners() {
        buttonSelectAll.setOnClickListener {
            for (i in filteredList.indices) {
                filteredList[i].isSelected = true
            }
            adapter.notifyDataSetChanged()
        }

        buttonUpload.setOnClickListener {
            val memoText = editTextMemo.text.toString()
            val selectedItems = filteredList.filter { it.isSelected }

            if (selectedItems.isEmpty()) {
                Toast.makeText(this, "선택된 미디어가 없습니다.", Toast.LENGTH_SHORT).show()
            } else {
                uploadToServer(selectedItems, memoText)
            }
        }

        buttonGoCapture.setOnClickListener {
            val intent = Intent(this, CaptureActivity::class.java).apply {
                putExtra("AFFILIATION", affiliation)
                putExtra("RANK", rank)
                putExtra("NAME", nameStr)
                putExtra("CASE_NAME", caseName)
            }
            startActivity(intent)
        }

        buttonGoMainMenu.setOnClickListener {
            val intent = Intent(this, MainMenuActivity::class.java).apply {
                putExtra("AFFILIATION", affiliation)
                putExtra("RANK", rank)
                putExtra("NAME", nameStr)
                putExtra("CASE_NAME", caseName)
            }
            startActivity(intent)
        }
    }

    private fun uploadToServer(selectedItems: List<MediaItem>, memo: String) {
        Toast.makeText(
            this,
            "업로드 중...\n선택된 항목 수: ${selectedItems.size}\n메모: $memo",
            Toast.LENGTH_LONG
        ).show()

        // 업로드 완료 예시
        Toast.makeText(this, "업로드가 완료되었습니다!", Toast.LENGTH_SHORT).show()
    }

    private fun updateGridView() {
        adapter.notifyDataSetChanged()
    }
}
