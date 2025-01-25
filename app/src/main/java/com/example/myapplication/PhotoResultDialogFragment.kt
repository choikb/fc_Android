package com.example.myapplication

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.camera.core.internal.utils.ImageUtil.rotateBitmap
import androidx.fragment.app.DialogFragment
import java.io.ByteArrayOutputStream

class PhotoResultDialogFragment : DialogFragment() {

    private var photoBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 전달받은 Bitmap 복원
        val byteArray = arguments?.getByteArray("PHOTO_BITMAP")
        photoBitmap = byteArray?.let { byteArrayToBitmap(it) }
        setStyle(STYLE_NORMAL, android.R.style.Theme_Light_NoTitleBar_Fullscreen)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_photo_result, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val imageViewPhoto: ImageView = view.findViewById(R.id.imageViewPhoto)
        val drawView: DrawView = view.findViewById(R.id.drawView)
       // val buttonAddText: Button = view.findViewById(R.id.buttonAddText)
      //  val buttonAddIcon: Button = view.findViewById(R.id.buttonAddIcon)
        //val buttonClear: Button = view.findViewById(R.id.buttonClear)

        val editTextMemo: EditText = view.findViewById(R.id.editTextMemo)
       // val checkBoxAutoUpload: CheckBox = view.findViewById(R.id.checkBoxAutoUpload)
       // val buttonRetake: Button = view.findViewById(R.id.buttonRetake)
        val buttonSave: Button = view.findViewById(R.id.buttonSave)


        // 가변 Bitmap 생성
        val mutableBitmap = photoBitmap?.copy(Bitmap.Config.ARGB_8888, true)



        // Bitmap 회전 적용
        photoBitmap?.let { originalBitmap ->
            val rotationDegrees = getCameraRotation()
            val rotatedBitmap = rotateBitmapByOrientation(originalBitmap, rotationDegrees)
            imageViewPhoto.setImageBitmap(rotatedBitmap) // 회전된 Bitmap을 ImageView에 적용
        }

// 텍스트 추가
//        buttonAddText.setOnClickListener {
//            mutableBitmap?.let { bitmap ->
//                // 기존 Bitmap을 회전
//                val rotationDegrees = getCameraRotation()
//                val rotatedBitmap = rotateBitmapByOrientation(bitmap, rotationDegrees)
//
//
//                val canvas = Canvas(bitmap)
//                val paint = Paint().apply {
//                    color = Color.BLUE
//                    textSize = 40f
//                    isAntiAlias = true
//                }
//                canvas.drawText("텍스트 예제", 50f, 50f, paint)
//
//                // 변경된 Bitmap을 ImageView에 적용
//                imageViewPhoto.setImageBitmap(bitmap)
//
//            }
//        }
//
//        // 아이콘 추가
//        buttonAddIcon.setOnClickListener {
//            mutableBitmap?.let { bitmap ->
//                val canvas = Canvas(bitmap)
//                val icon = BitmapFactory.decodeResource(resources, androidx.constraintlayout.widget.R.drawable.notification_bg_normal)
//                canvas.drawBitmap(icon, 100f, 100f, null)
//
//                // 변경된 Bitmap을 ImageView에 적용
//                imageViewPhoto.setImageBitmap(bitmap)
//            }
//        }
//        // 초기화
//        buttonClear.setOnClickListener {
//            drawView.clear()
//        }
//
//
//        // 재촬영 버튼 클릭
//        buttonRetake.setOnClickListener {
//            dismiss() // 다이얼로그 닫기
//        }

        // 저장 버튼 클릭
        buttonSave.setOnClickListener {

            val finalBitmap = mutableBitmap?.let { mergeDrawViewToBitmap(it, drawView) }
            // 수정된 사진 저장
            if (finalBitmap != null) {
                saveModifiedPhoto(finalBitmap, "modified_image_${System.currentTimeMillis()}.jpg")
                Toast.makeText(requireContext(), "수정된 사진이 저장되었습니다.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "수정된 이미지를 저장할 수 없습니다.", Toast.LENGTH_SHORT).show()
            }

            // 원본 사진 저장
            if (photoBitmap != null) {
                saveOriginalPhoto(photoBitmap!!, "original_image_${System.currentTimeMillis()}.jpg")
                Toast.makeText(requireContext(), "원본 사진이 저장되었습니다.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "원본 이미지를 저장할 수 없습니다.", Toast.LENGTH_SHORT).show()
            }

            // 다이얼로그 닫기
            dismiss()

//            val memo = editTextMemo.text.toString()
//            val isAutoUpload = checkBoxAutoUpload.isChecked
//
//            if (photoBitmap != null) {
//                savePhotoWithMemo(photoBitmap!!, memo)
//                Toast.makeText(
//                    requireContext(),
//                    "사진 저장 완료\n메모: $memo\n자동 업로드: $isAutoUpload",
//                    Toast.LENGTH_SHORT
//                ).show()
//            } else {
//                Toast.makeText(requireContext(), "Bitmap을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
//            }
//
//            dismiss() // 다이얼로그 닫기
        }
    }

    private fun getCameraRotation(): Int {
        val displayRotation = requireActivity().windowManager.defaultDisplay.rotation
        val cameraRotation = 90 // 일반적으로 후면 카메라는 90도 회전
        return (cameraRotation - displayRotation * 90 + 360) % 360
    }

    private fun rotateBitmapByOrientation(bitmap: Bitmap, rotationDegrees: Int): Bitmap {
        val matrix = android.graphics.Matrix()
        matrix.postRotate(rotationDegrees.toFloat())
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun mergeDrawViewToBitmap(bitmap: Bitmap, drawView: DrawView): Bitmap {
        val resultBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(resultBitmap)
        drawView.draw(canvas) // DrawView의 내용을 Canvas에 그립니다.
        return resultBitmap
    }

    private fun saveModifiedPhoto(bitmap: Bitmap, fileName: String) {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/MyApp")
        }

        val resolver = requireContext().contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let {
            resolver.openOutputStream(it).use { outputStream ->
                if (outputStream != null) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                }
            }
        }
    }

    private fun saveOriginalPhoto(bitmap: Bitmap, fileName: String) {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/MyApp/Original")
        }

        val resolver = requireContext().contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let {
            resolver.openOutputStream(it).use { outputStream ->
                if (outputStream != null) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                }
            }
        }
    }


    /**
     * Bitmap을 저장하면서 메모 추가
     */
    private fun savePhotoWithMemo(bitmap: Bitmap, memo: String) {

        val correctedBitmap = rotateBitmap(bitmap, 0f) // 0f로 회전 없이 사용

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "IMG_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.DESCRIPTION, memo)
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/MyApp")
        }

        val resolver = requireContext().contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let {
            resolver.openOutputStream(it).use { outputStream ->
                if (outputStream != null) {
                    correctedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                }
                // 반환 값 명시적으로 지정 (Unit)
                Unit
            }
        }
    }
    private fun rotateBitmap(bitmap: Bitmap, rotationDegrees: Float): Bitmap {
        val matrix = android.graphics.Matrix()
        matrix.postRotate(rotationDegrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * Bitmap을 ByteArray로 변환
     */
    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        return stream.toByteArray()
    }

    /**
     * ByteArray를 Bitmap으로 변환
     */
    private fun byteArrayToBitmap(byteArray: ByteArray): Bitmap {
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    }

    companion object {
        /**
         * Bitmap을 인수로 받아 PhotoResultDialogFragment 생성
         */
        fun newInstance(bitmap: Bitmap): PhotoResultDialogFragment {
            val fragment = PhotoResultDialogFragment()
            val args = Bundle().apply {
                putByteArray("PHOTO_BITMAP", fragment.bitmapToByteArray(bitmap))
            }
            fragment.arguments = args
            return fragment
        }
    }
}
