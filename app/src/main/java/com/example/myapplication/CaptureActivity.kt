/*
package com.example.myapplication

import android.Manifest
import android.content.ContentValues
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.myapplication.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class CaptureActivity : AppCompatActivity() {

    private lateinit var previewViewCamera: PreviewView
    private lateinit var radioGroupUploadMode: RadioGroup
    private lateinit var radioButtonAuto: RadioButton
    private lateinit var radioButtonManual: RadioButton
    private lateinit var editTextMemo: EditText
    private lateinit var buttonCapture: Button
    private lateinit var buttonUpload: Button
    private lateinit var buttonGoSelectMedia: Button
    private lateinit var buttonGoMainMenu: Button


    private var imageCapture: ImageCapture? = null
    private var cameraProvider: ProcessCameraProvider? = null

    // 사진 촬영 상태
    private var isCaptured = false
    private var capturedImageUri: Uri? = null

    // 사용자 정보
    private var affiliation = ""
    private var rank = ""
    private var nameStr = ""
    private var caseName = ""


    private lateinit var previewView: PreviewView
    private lateinit var zoomSeekBar: SeekBar
    private lateinit var camera: Camera


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_capture)

        // 인텐트로부터 데이터 받기
        affiliation = intent.getStringExtra("AFFILIATION") ?: ""
        rank = intent.getStringExtra("RANK") ?: ""
        nameStr = intent.getStringExtra("NAME") ?: ""
        caseName = intent.getStringExtra("CASE_NAME") ?: ""

//        Toast.makeText(
//            this,
//            "소속: $affiliation, 계급: $rank, 성명: $nameStr, 사건명: $caseName",
//            Toast.LENGTH_LONG
//        ).show()

        // 뷰 초기화
        initViews()

        // 권한 확인 및 요청
        checkPermissions()


        // 카메라 권한 체크
        checkCameraPermission()

        // 라디오 그룹 (자동/수동) 리스너
        setupRadioGroupListener()

        // 버튼(촬영/재촬영, 업로드, 화면이동) 리스너
        setupButtonListeners()


        // 뷰 초기화
        previewView = findViewById(R.id.previewViewCamera)
        zoomSeekBar = findViewById(R.id.zoomSeekBar)

        // 카메라 초기화
        startCamera()

        // 확대/축소 SeekBar 동작 설정
        zoomSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val zoomRatio = progress / 10.0f // 0.1배 ~ 10배
                camera.cameraControl.setZoomRatio(zoomRatio)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // 카메라 선택: 후면 카메라
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            // 프리뷰 설정
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            try {
                // 기존 바인딩 해제
                cameraProvider.unbindAll()

                // 카메라 바인딩
                camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview)
            } catch (exc: Exception) {
                exc.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun initViews() {
        previewViewCamera = findViewById(R.id.previewViewCamera)
        radioGroupUploadMode = findViewById(R.id.radioGroupUploadMode)
        radioButtonAuto = findViewById(R.id.radioButtonAuto)
        radioButtonManual = findViewById(R.id.radioButtonManual)
        editTextMemo = findViewById(R.id.editTextMemo)
        buttonCapture = findViewById(R.id.buttonCapture)
        buttonUpload = findViewById(R.id.buttonUpload)
        buttonGoSelectMedia = findViewById(R.id.buttonGoSelectMedia)
        buttonGoMainMenu = findViewById(R.id.buttonGoMainMenu)
    }

    /**
     * 카메라 권한 확인
     */
    private fun checkCameraPermission() {
        val permission = Manifest.permission.CAMERA
        if (ContextCompat.checkSelfPermission(this, permission)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), 1000)
        } else {
            startCameraPreview()
        }
    }

    /**
     * 권한 요청 결과 처리
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 1001) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                startCameraPreview()
            } else {
                showPermissionRationale()
            }
        }
    }
    private fun showPhotoPicker() {
        val intent = Intent(MediaStore.ACTION_PICK_IMAGES).apply {
            putExtra(MediaStore.EXTRA_PICK_IMAGES_MAX, 10) // 최대 선택 가능 수
        }
        startActivityForResult(intent, 2001)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 2001 && resultCode == RESULT_OK) {
            val clipData = data?.clipData
            val singleUri = data?.data

            if (clipData != null) {
                for (i in 0 until clipData.itemCount) {
                    val uri = clipData.getItemAt(i).uri
                    Log.d("PhotoPicker", "선택된 URI: $uri")
                }
            } else if (singleUri != null) {
                Log.d("PhotoPicker", "선택된 URI: $singleUri")
            }
        }
    }


    private fun showPermissionRationale() {
        AlertDialog.Builder(this)
            .setTitle("권한 요청")
            .setMessage("앱 기능을 사용하려면 필수 권한이 필요합니다. 설정에서 권한을 허용해주세요.")
            .setPositiveButton("설정으로 이동") { _, _ ->
                val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }
            .setNegativeButton("취소") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
    private fun startCameraPreview() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9) // 비율을 화면에 맞게 설정
                .build().also {
                    it.setSurfaceProvider(previewViewCamera.surfaceProvider)
                }


            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY) // 빠른 촬영 모드
                //.setTargetResolution(Size(1920, 1080)) // 해상도 명시 (갤럭시 A34 5G에 맞춤)
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .setTargetRotation(previewViewCamera.display.rotation) // 회전 방향 설정
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageCapture
                )
            } catch (e: Exception) {
                Toast.makeText(this, "카메라 실행 실패: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    /**
     * 자동/수동 모드 라디오 그룹 리스너
     */
    private fun setupRadioGroupListener() {
        radioGroupUploadMode.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radioButtonAuto -> {
                    buttonUpload.isEnabled = false
                }
                R.id.radioButtonManual -> {
                    buttonUpload.isEnabled = true
                }
            }
        }
    }

    /**
     * 버튼 리스너(촬영/재촬영, 업로드, 화면 이동)
     */
    private fun setupButtonListeners() {
        buttonCapture.setOnClickListener {
            if (!isCaptured) captureMedia() else retakeMedia()
        }

        buttonUpload.setOnClickListener {
            if (!isCaptured) {
                Toast.makeText(this, "먼저 촬영하세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            uploadToNaverCloud()
        }

        // 미디어 선택 화면으로 이동
        buttonGoSelectMedia.setOnClickListener {
            val intent = Intent(this, SelectMediaActivity::class.java).apply {
                putExtra("AFFILIATION", affiliation)
                putExtra("RANK", rank)
                putExtra("NAME", nameStr)
                putExtra("CASE_NAME", caseName)
            }
            startActivity(intent)
        }

        // 메인 메뉴로 이동
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

    /**
     * 사진 촬영
     */
    private fun captureMedia() {
        val imageCapture = imageCapture ?: return


        val caseName = "25012001"
        val normalizedCaseName = caseName.replace("[^a-zA-Z0-9_-]".toRegex(), "_") // 안전한 문자만 허용


        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "IMG_${System.currentTimeMillis()}.jpg")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/$normalizedCaseName")
        }

        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                  //  Toast.makeText(this@CaptureActivity, "사진 저장 성공!", Toast.LENGTH_SHORT).show()
                   // Log.d("CaptureActivity", "사진 저장 경로: ${outputFileResults.savedUri}")

                    val savedUri = outputFileResults.savedUri
                    showPhotoResultDialog(savedUri)
                }

                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(this@CaptureActivity, "사진 저장 실패: ${exception.message}", Toast.LENGTH_SHORT).show()
                    Log.e("CaptureActivity", "사진 저장 오류: ${exception.message}")
                }
            }
        )
    }
    private fun showPhotoResultDialog(savedUri: Uri?) {
        val dialog = PhotoResultDialogFragment.newInstance(savedUri)
        dialog.show(supportFragmentManager, "PhotoResultDialog")
    }


    private fun checkPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.CAMERA
        )


        // Android 13(T) 이상
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            // Android 12(S) 이하
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        // 승인되지 않은 권한 필터링
        val deniedPermissions = permissions.filter {
            checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }

        // 권한 요청
        if (deniedPermissions.isNotEmpty()) {
            requestPermissions(deniedPermissions.toTypedArray(), 1001)
        } else {
            // 모든 권한이 승인된 경우
            startCameraPreview()
        }
    }
/*
    private fun createImageUri(): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "IMG_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
            put(MediaStore.Images.Media.DATE_MODIFIED, System.currentTimeMillis() / 1000)
            put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
        }
        return try {
            contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        } catch (e: Exception) {
            Log.e("CaptureActivity", "이미지 URI 생성 실패: ${e.message}")
            null
        }
    }
    */
private fun createImageUri(caseName: String): Uri? {
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, "IMG_${System.currentTimeMillis()}.jpg") // 파일 이름
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg") // 파일 형식
        put(
            MediaStore.MediaColumns.RELATIVE_PATH,
            "Pictures/$caseName" // 사건명 폴더 지정
        )
    }
    return contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
}
    /**
     * 재촬영
     */
    private fun retakeMedia() {
        isCaptured = false
        buttonCapture.text = "촬영"
        capturedImageUri = null
        Toast.makeText(this, "재촬영을 시작합니다.", Toast.LENGTH_SHORT).show()
    }

    /**
     * 자동 모드 업로드 확인창
     */
    private fun showAutoUploadConfirmDialog() {
        AlertDialog.Builder(this)
            .setTitle("자동 업로드")
            .setMessage("촬영이 완료되었습니다. 자동으로 업로드하시겠습니까?")
            .setPositiveButton("예") { _: DialogInterface, _: Int ->
                uploadToNaverCloud()
            }
            .setNegativeButton("아니오") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    /**
     * 네이버 클라우드 업로드 (예시)
     */
    private fun uploadToNaverCloud() {
        if (capturedImageUri == null) {
            Toast.makeText(this, "파일이 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }
        val memo = editTextMemo.text.toString()

        // TODO: 실제 업로드 로직 (API Gateway + Cloud Functions + Object Storage)
        Toast.makeText(
            this,
            "네이버클라우드 업로드 중... (메모: $memo)",
            Toast.LENGTH_SHORT
        ).show()

        // 업로드 완료 예시
        Toast.makeText(this, "업로드가 완료되었습니다(예시).", Toast.LENGTH_LONG).show()
    }
}
*/


package com.example.myapplication

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.Surface

class CaptureActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var zoomSeekBar: SeekBar
    private lateinit var buttonCapture: Button
    private lateinit var buttonGoMainMenu: Button

    private var imageCapture: ImageCapture? = null
    private lateinit var camera: Camera

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_capture)

        // 뷰 초기화
        previewView = findViewById(R.id.previewViewCamera)
        zoomSeekBar = findViewById(R.id.zoomSeekBar)
        buttonCapture = findViewById(R.id.buttonCapture)
        buttonGoMainMenu = findViewById(R.id.buttonGoMainMenu)

        // 권한 확인 및 요청
        checkPermissions()

        // 촬영 버튼 리스너
        buttonCapture.setOnClickListener {
            capturePhoto()
        }

        // 메인 메뉴로 이동
        buttonGoMainMenu.setOnClickListener {
            val intent = Intent(this, MainMenuActivity::class.java)
            startActivity(intent)
        }
    }

    private fun checkPermissions() {
        val permissions = arrayOf(Manifest.permission.CAMERA)
        if (permissions.all {
                ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
            }) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, permissions, 1001)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            Toast.makeText(this, "카메라 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            // Preview 설정
            val preview = Preview.Builder()
                .setTargetRotation(Surface.ROTATION_0) // 고정된 회전 설정
                .build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            // ImageCapture 설정
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setTargetRotation(Surface.ROTATION_0) // 고정된 회전 설정
                .build()

            try {
                // 카메라 바인딩
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)

                // 확대/축소 설정
                setupZoomControl()
            } catch (e: Exception) {
                Toast.makeText(this, "카메라 시작 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun setupZoomControl() {
        zoomSeekBar.max = 100 // 확대 비율 (1배 ~ 10배)
        zoomSeekBar.progress = 10
        zoomSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val zoomRatio = progress / 10.0f
                camera.cameraControl.setZoomRatio(zoomRatio)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }
//    private fun capturePhoto() {
//        val imageCapture = imageCapture ?: return
//
//        val name = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
//        val contentValues = ContentValues().apply {
//            put(MediaStore.MediaColumns.DISPLAY_NAME, "IMG_$name.jpg")
//            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
//            put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/MyApp")
//        }
//
//        val outputOptions = ImageCapture.OutputFileOptions.Builder(
//            contentResolver,
//            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
//            contentValues
//        ).build()
//
//        imageCapture.takePicture(
//            outputOptions,
//            ContextCompat.getMainExecutor(this),
//            object : ImageCapture.OnImageSavedCallback {
//                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
//                    val savedUri = outputFileResults.savedUri
//                    showPhotoResultDialog(savedUri)
//                }
//
//                override fun onError(exception: ImageCaptureException) {
//                    Toast.makeText(this@CaptureActivity, "사진 촬영 실패: ${exception.message}", Toast.LENGTH_SHORT).show()
//                }
//            }
//        )
//    }

//    private fun showPhotoResultDialog(photoUri: Uri?) {
//        val dialog = PhotoResultDialogFragment.newInstance(photoUri)
//        dialog.show(supportFragmentManager, "PhotoResultDialog")
//    }
    private fun capturePhoto() {
        val imageCapture = imageCapture ?: return

        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    super.onCaptureSuccess(image)

                    // 촬영된 이미지를 Bitmap으로 변환
                    val bitmap = imageProxyToBitmap(image)
                    image.close() // ImageProxy 해제

                    // 다이얼로그로 Bitmap 전달
                    showPhotoResultDialog(bitmap)
                }

                override fun onError(exception: ImageCaptureException) {
                    super.onError(exception)
                    Toast.makeText(this@CaptureActivity, "사진 촬영 실패: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    private fun showPhotoResultDialog(bitmap: Bitmap) {
        val dialog = PhotoResultDialogFragment.newInstance(bitmap)
        dialog.show(supportFragmentManager, "PhotoResultDialog")
    }


}
