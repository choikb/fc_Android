package com.example.myapplication

import android.Manifest
import android.content.Context
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
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.media.ExifInterface
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Size
import android.view.Surface
import java.io.ByteArrayInputStream
import java.io.IOException

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

//    private fun startCamera() {
//        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
//        cameraProviderFuture.addListener({
//            val cameraProvider = cameraProviderFuture.get()
//            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
//
//            // Preview 설정
//            val preview = Preview.Builder()
//              //  .setTargetRotation(Surface.ROTATION_0) // 고정된 회전 설정
//                .build().also {
//                    it.setSurfaceProvider(previewView.surfaceProvider)
//                }
//
//            // ImageCapture 설정
//            val maxResolution = getMaxResolution()
//            imageCapture = ImageCapture.Builder()
//                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
//                .apply {
//                    if (maxResolution != null) {
//                        // 최대 해상도를 기반으로 비율 설정
//                        val aspectRatio = if (maxResolution.width > maxResolution.height) {
//                            AspectRatio.RATIO_16_9
//                        } else {
//                            AspectRatio.RATIO_4_3
//                        }
//                        setTargetAspectRatio(aspectRatio)
//                    }
//                }
//                .setTargetRotation(Surface.ROTATION_0) // 고정 회전 설정
//                .build()
//
//            try {
//                // 카메라 바인딩
//                cameraProvider.unbindAll()
//                camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
//
//                // 확대/축소 설정
//                setupZoomControl()
//            } catch (e: Exception) {
//                Toast.makeText(this, "카메라 시작 실패: ${e.message}", Toast.LENGTH_SHORT).show()
//            }
//        }, ContextCompat.getMainExecutor(this))
//    }
private fun startCamera() {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()

        // 후면 카메라 선택
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        // Preview 설정
        val preview = Preview.Builder()
            .setTargetRotation(previewView.display.rotation) // 화면 회전 기준
            .build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

        // 최대 해상도 동적 확인
        val maxResolution = getMaxResolution()

        // ImageCapture 설정
        imageCapture = ImageCapture.Builder().apply {
            setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY) // 최대 품질 우선
            setTargetRotation(previewView.display.rotation) // 화면 회전 기준
            maxResolution?.let { setTargetResolution(it) } // 최대 해상도 동적 적용
        }.build()

        try {
            // 기존 바인딩 해제
            cameraProvider.unbindAll()

            // Preview와 ImageCapture 바인딩
            cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                preview,
                imageCapture
            )
        } catch (e: Exception) {
            Toast.makeText(this, "카메라 실행 실패: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }, ContextCompat.getMainExecutor(this))
}


    private fun getMaxResolution(): Size? {
        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        return try {
            val cameraId = cameraManager.cameraIdList.find { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val lensFacing = characteristics[CameraCharacteristics.LENS_FACING]
                lensFacing == CameraCharacteristics.LENS_FACING_BACK
            } ?: return null

            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            val map = characteristics[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP]
            map?.getOutputSizes(ImageFormat.JPEG)?.maxByOrNull { it.width * it.height }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
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

        // 🔹 EXIF 정보를 읽고 회전값 적용
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        return rotateBitmapByExif(bytes, bitmap)
    }

    private fun showPhotoResultDialog(bitmap: Bitmap) {
        val dialog = PhotoResultDialogFragment.newInstance(bitmap)
        dialog.show(supportFragmentManager, "PhotoResultDialog")
    }
    private fun rotateBitmapByExif(imageData: ByteArray, bitmap: Bitmap): Bitmap {
        return try {
            val inputStream = ByteArrayInputStream(imageData)
            val exif = ExifInterface(inputStream)
            val rotation = when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }

            if (rotation == 0) return bitmap // 🔹 회전할 필요 없으면 원본 반환

            val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: IOException) {
            bitmap // 🔹 EXIF 데이터를 읽을 수 없으면 원본 반환
        }
    }

}
