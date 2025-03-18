package com.example.myapplication

import android.content.ContentUris
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.api.ApiService
import java.io.File
import com.example.myapplication.media.MediaAdapter

import com.example.myapplication.media.MediaItem // 필요한 패키지 경로 확인 후 수정
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class SelectMediaActivity : AppCompatActivity() {

    private lateinit var mediaRecyclerView: RecyclerView
    private lateinit var radioGroupFilter: RadioGroup
    private lateinit var uploadButton: Button
    private lateinit var memoEditText: EditText
    private lateinit var layoutBackToFolders: LinearLayout

    private val mediaItems = mutableListOf<MediaItem>()
    private val selectedPhotoUris = mutableListOf<Uri>()
    private var selectedFolder: MediaItem? = null
    private lateinit var uploadedFiles: Array<String>
    private lateinit var mediaAdapter: MediaAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_media) // activity 레이아웃 설정

        mediaRecyclerView = findViewById(R.id.mediaRecyclerView)
        radioGroupFilter = findViewById(R.id.radioGroupFilter)
        uploadButton = findViewById(R.id.uploadButton)
        memoEditText = findViewById(R.id.memoEditText)
        layoutBackToFolders = findViewById(R.id.layoutBackToFolders)

        setupRecyclerView()
        loadFolders() // 초기 폴더 목록 로드

        Thread {
            getUploadedMediaNames(IncidentManager.incidentId.toString())
        }.start()

//        uploadedFiles.forEach { fileName ->
//            Log.d("UploadedFile", "File: $fileName")
//            //이걸로 목록화 하자.
//        }

        // 상위 폴더 이동 버튼 클릭 시
        layoutBackToFolders.setOnClickListener {
            mediaAdapter.clearSelectedItems() // ✅ 선택한 사진 테두리 초기화
            loadFolders()
            layoutBackToFolders.visibility = View.GONE // 다시 숨김
            radioGroupFilter.visibility = View.GONE
            memoEditText.visibility = View.GONE
            uploadButton.visibility = View.GONE
            //  radioGroupFilter.check(R.id.radioAll) // 전체 보기 버튼 기본 선택
        }

        // 라디오 버튼 클릭 시 필터링
        radioGroupFilter.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radioAll -> loadPhotos(null) // 전체
                R.id.radioUploaded -> loadPhotos(true) // 전송 완료
                R.id.radioNotUploaded -> loadPhotos(false) // 미전송
            }
        }

        // 업로드 버튼 클릭
        uploadButton.setOnClickListener {
            uploadSelectedPhotos()
        }

        val buttonMainMenu: Button = findViewById(R.id.buttonMainMenu)
        buttonMainMenu.setOnClickListener {
            val intent = Intent(this, MainMenuActivity::class.java)
            startActivity(intent)
        }

        // 라디오 버튼 선택 이벤트 수정:
        radioGroupFilter.setOnCheckedChangeListener { _, checkedId ->
            val filter = when (checkedId) {
                R.id.radioUploaded -> true
                R.id.radioNotUploaded -> false
                else -> null
            }
            selectedFolder?.let {
                loadPhotosFromFolder(it.path, filter)
            }
        }
    }

    private fun getUploadedMediaNameAll() {

    }

    private fun checkIfUploaded(filePath: String): Boolean {

        val fileName = filePath.substringAfterLast("/")

        return uploadedFiles.contains(fileName)
    }

    private fun getUploadedMediaNames(incidentId: String) {

        try {

            lifecycleScope.launch {
                val dataArray = ApiService.getMediaList(incidentId);

                val fileNames = mutableListOf<String>()

                for (i in 0 until dataArray.size) {
                    val element = dataArray.get(i) // 🔹 요소를 직접 가져오기

                    fileNames.add(element.fileName!!)
                }
                uploadedFiles = fileNames.toTypedArray()
            }

        } catch (e: JSONException) {
            Log.e("API_ERROR", "JSON 파싱 오류: ${e.message}", e)
            uploadedFiles = emptyArray() // JSON 오류 발생 시 처리
        } catch (e: IOException) {
            Log.e("API_ERROR", "네트워크 요청 실패: ${e.message}", e)
            uploadedFiles = emptyArray() // 네트워크 오류 발생 시 처리
        } catch (e: Exception) {
            Log.e("API_ERROR", "알 수 없는 오류: ${e.message}", e)
            uploadedFiles = emptyArray() // 기타 예외 처리
        }

    }

    private fun uploadSelectedPhotos() {
        val memo = memoEditText.text.toString().trim() // Trimmed memo text

        if (selectedPhotoUris.isEmpty()) {
            Toast.makeText(this, "업로드할 사진을 선택하세요.", Toast.LENGTH_SHORT).show()
            return
        }

        val uri = selectedPhotoUris.first() // ✅ 첫 번째 사진만 업로드
        val filePath = getRealFilePathFromUri(uri)

        if (filePath != null) {
            val file = File(filePath)
            if (file.exists()) {
                uploadImageToCloud(uri, file.name, memo)
                Toast.makeText(this, "파일이 업로드 되었습니다.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "파일이 존재하지 않습니다: $filePath", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "유효하지 않은 파일 경로입니다.", Toast.LENGTH_SHORT).show()
        }

//
//        selectedPhotoUris.forEach { uri ->
//            val filePath = getRealFilePathFromUri(uri)
//            if (filePath != null) {
//                val file = File(filePath)
//                if (file.exists()) {
//                    uploadImageToCloud(uri, file.name, memo)
//                } else {
//                    Toast.makeText(this, "파일이 존재하지 않습니다: $filePath", Toast.LENGTH_SHORT).show()
//                }
//            } else {
//                Toast.makeText(this, "유효하지 않은 파일 경로입니다.", Toast.LENGTH_SHORT).show()
//            }
//        }
        mediaAdapter.clearSelectedItems()
        //getUploadedMediaNameAll()
        Toast.makeText(this, "파일이 업로드 되었습니다.", Toast.LENGTH_SHORT).show()
    }

    private fun getRealFilePathFromUri(uri: Uri): String? {
        return when {
            uri.scheme.isNullOrEmpty() -> uri.path // scheme이 null일 경우 직접 경로 반환
            uri.scheme == "file" -> uri.path
            uri.scheme == "content" -> {
                var cursor: Cursor? = null
                try {
                    val projection = arrayOf(MediaStore.Images.Media.DATA)
                    cursor = contentResolver.query(uri, projection, null, null, null)
                    val columnIndex = cursor?.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                    cursor?.moveToFirst()
                    cursor?.getString(columnIndex ?: -1)
                } finally {
                    cursor?.close()
                }
            }

            else -> null
        }
    }


    private fun uploadImageToCloud(uri: Uri, fileName: String, memo: String) {
        val filePath = getRealFilePathFromUri(uri)
        if (filePath != null) {
            val file = File(filePath)
            if (file.exists()) {
                val fileUploadClient = FileUploadClient(this)
                fileUploadClient.uploadFileToCloud(Uri.fromFile(file), fileName = fileName, memo)
            } else {
                Toast.makeText(this, "파일이 존재하지 않습니다: $filePath", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "잘못된 파일 경로입니다.", Toast.LENGTH_SHORT).show()
        }

    }


    private fun getRealPathFromUri(uri: Uri): String? {
        var cursor: Cursor? = null
        try {
            val projection = arrayOf(MediaStore.Images.Media.DATA)
            cursor = contentResolver.query(uri, projection, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                return cursor.getString(columnIndex)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }
        return null
    }


    private fun setupRecyclerView() {
        mediaRecyclerView.layoutManager = GridLayoutManager(this, 3)
        mediaAdapter = MediaAdapter(this, mediaItems) { mediaItem ->
            if (mediaItem.isFolder) {
                mediaAdapter.clearSelectedItems() // ✅ 폴더 이동 시 기존 선택 해제
                selectedFolder = mediaItem
                layoutBackToFolders.visibility = View.VISIBLE
                radioGroupFilter.visibility = View.VISIBLE
                memoEditText.visibility = View.VISIBLE // Make memoEditText visible
                uploadButton.visibility = View.VISIBLE // Make uploadButton visible
                memoEditText.text = null
                val selectedFilter = when (radioGroupFilter.checkedRadioButtonId) {
                    R.id.radioUploaded -> true
                    R.id.radioNotUploaded -> false
                    else -> null
                }
                loadPhotosFromFolder(mediaItem.path, selectedFilter)
            } else {
                val uri = Uri.parse(mediaItem.path)
                if (selectedPhotoUris.contains(uri)) {
                    selectedPhotoUris.remove(uri)
                } else {
                    selectedPhotoUris.add(uri)
                }
            }
        }
        mediaRecyclerView.adapter = mediaAdapter
    }


    override fun onBackPressed() {
        if (layoutBackToFolders.visibility == View.VISIBLE) {
            mediaAdapter.clearSelectedItems() // ✅ 폴더 화면으로 돌아갈 때 선택 초기화
            loadFolders()
        } else {
            super.onBackPressed()
        }
    }


//    private fun loadPhotosFromFolder(folderPath: String, filter: Boolean?) {
//        mediaItems.clear()
//
//        val projection = arrayOf(
//            MediaStore.Images.Media._ID,
//            MediaStore.Images.Media.DISPLAY_NAME
//        )
//
//        val selection: String
//        val selectionArgs: Array<String>
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//            selection = "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?"
//            selectionArgs = arrayOf("%$folderPath%") // 와일드카드 추가
//        } else {
//            selection = "${MediaStore.Images.Media.DATA} LIKE ?"
//            selectionArgs = arrayOf("$folderPath%")
//        }
//
//        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
//
//        val cursor = contentResolver.query(
//            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
//            projection,
//            selection,
//            selectionArgs,
//            sortOrder
//        )
//
//        if (cursor == null || !cursor.moveToFirst()) {
//            Log.e("MediaStore", "❌ MediaStore에서 데이터를 찾을 수 없음: $folderPath")
//            return
//        }
//
//        cursor.use {
//            val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
//            val nameColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
//
//            while (it.moveToNext()) {
//                val id = it.getLong(idColumn)
//                val name = it.getString(nameColumn)
//
//                val contentUri = ContentUris.withAppendedId(
//                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
//                    id
//                )
//
//                Log.d("MediaStore", "파일: $name, 경로: $folderPath")
//                mediaItems.add(MediaItem(name, contentUri.toString(), false))
//            }
//        }
//
//        mediaRecyclerView.adapter?.notifyDataSetChanged()
//    }

    private fun loadPhotosFromFolder(folderPath: String, filter: Boolean?) {
        mediaItems.clear()

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.DISPLAY_NAME
        )

        // Different handling for "Recent" folder vs. other folders
        val selection: String?
        val selectionArgs: Array<String>?
        
        if (selectedFolder?.name == "Recent") {
            // For "Recent" folder, show all photos without folder filtering
            selection = null
            selectionArgs = null
        } else {
            // For other folders, filter by BUCKET_DISPLAY_NAME
            selection = "${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} = ?"
            selectionArgs = arrayOf(selectedFolder?.name ?: "")
        }

        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        val cursor = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )

        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val folderColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)

            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val name = it.getString(nameColumn)
                val folder = it.getString(folderColumn) ?: "Unknown"
                
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id
                )
                
                // Apply filter if specified
                if (filter != null) {
                    val isUploaded = checkIfUploaded(name)
                    if (filter == isUploaded) {
                        mediaItems.add(MediaItem(name, contentUri.toString(), isFolder = false))
                    }
                } else {
                    mediaItems.add(MediaItem(name, contentUri.toString(), isFolder = false))
                }
            }
        }

        mediaRecyclerView.adapter?.notifyDataSetChanged()
    }


    private fun loadFolders() {
        mediaItems.clear()

        layoutBackToFolders.visibility = View.GONE // 상위 폴더 버튼 숨기기
        radioGroupFilter.visibility = View.GONE // 라디오 버튼 숨기기
        memoEditText.visibility = View.GONE
        uploadButton.visibility = View.GONE

        val basicFolders = listOf("Recent", "Camera", "Screenshots", "Favorites","Pictures","Download") // 기본 폴더 이름
        val basicFolderMap = mutableMapOf<String, MediaItem>() // 기본 폴더 매핑
        val otherFolderMap = mutableMapOf<String, MediaItem>() // 기타 폴더 매핑

        val projection = arrayOf(
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME, // 폴더명
            MediaStore.Images.Media._ID,  // 파일 ID (DATA 대신 사용)
            MediaStore.Images.Media.DATE_ADDED  // 추가 날짜
        )

        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC" // 최신순 정렬

        val cursor = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )

        cursor?.use {
            val folderNameIndex =
                it.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            val idIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val dateAddedIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)

            while (it.moveToNext()) {
                // ✅ `BUCKET_DISPLAY_NAME`이 `null`일 경우 대비
                val folderName = it.getString(folderNameIndex) ?: "Unknown"
                val imageId = it.getLong(idIndex)
                val dateAdded = it.getLong(dateAddedIndex)

                // ✅ `MediaStore`에서 파일 URI 생성 (DATA 필드 사용 금지)
                val imageUri = Uri.withAppendedPath(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    imageId.toString()
                )

                val creationTime =
                    SimpleDateFormat("yyMMddHH", Locale.getDefault()).format(Date(dateAdded * 1000))

                val photoCount =
                    if (folderName == "Recent") getPhotoCount("") else getPhotoCount(folderName)

                if (basicFolders.contains(folderName)) {
                    if (!basicFolderMap.containsKey(folderName)) {
                        basicFolderMap[folderName] = MediaItem(
                            name = folderName,
                            path = imageUri.toString(), // ✅ 데이터 경로 대신 URI 저장
                            isFolder = true,
                            photoCount = photoCount,
                            creationTime = creationTime
                        )
                    }
                } else {
                    if (!otherFolderMap.containsKey(folderName)) {
                        otherFolderMap[folderName] = MediaItem(
                            name = folderName,
                            path = imageUri.toString(), // ✅ 데이터 경로 대신 URI 저장
                            isFolder = true,
                            photoCount = photoCount,
                            creationTime = creationTime
                        )
                    }
                }
            }
        }

        // Recent 폴더 추가
        val allPhotosCount = getPhotoCount("")
        basicFolderMap["Recent"] = MediaItem(
            name = "Recent",
            path = "",
            isFolder = true,
            photoCount = allPhotosCount,
            creationTime = SimpleDateFormat("yyMMddHH", Locale.getDefault()).format(Date())
        )

        // 기본 폴더 -> 기타 폴더 순으로 추가
        mediaItems.addAll(basicFolders.mapNotNull { basicFolderMap[it] })
        mediaItems.addAll(otherFolderMap.values)

        mediaRecyclerView.adapter?.notifyDataSetChanged()
    }


    private fun getPhotoCount(folderName: String): Int {
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val selection = "${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(folderName)

        val cursor = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )

        val count = cursor?.count ?: 0
        cursor?.close()

        return count
    }


    private fun loadPhotos(filter: Boolean?) {

        radioGroupFilter.visibility = View.VISIBLE
        layoutBackToFolders.visibility = View.VISIBLE
        memoEditText.visibility = View.VISIBLE
        uploadButton.visibility = View.VISIBLE
        mediaItems.clear()

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATA
        )

        // 📌 선택된 폴더의 사진만 가져오도록 수정
        val selection = selectedFolder?.let { "${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} = ?" }
        val selectionArgs = selectedFolder?.let { arrayOf(it.name) }

        val cursor = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )

        cursor?.use {
            val dataIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            while (it.moveToNext()) {
                val filePath = it.getString(dataIndex)
                val isUploaded = checkIfUploaded(filePath) // 📌 전송 여부 확인

                // 📌 필터 적용 (전송된 사진만, 미전송 사진만, 전체 사진)
                if (filter == null || (filter && isUploaded) || (!filter && !isUploaded)) {
                    mediaItems.add(MediaItem(File(filePath).name, filePath, false))
                }
            }
        }

        mediaRecyclerView.adapter?.notifyDataSetChanged()
    }
}
