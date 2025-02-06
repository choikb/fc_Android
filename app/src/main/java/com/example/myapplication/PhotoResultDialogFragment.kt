package com.example.myapplication


import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context.MODE_PRIVATE
import android.database.Cursor
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.DialogFragment
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PhotoResultDialogFragment : DialogFragment() {

    private var photoBitmap: Bitmap? = null
    private lateinit var imageViewPhoto: ImageView
    private lateinit var drawView: DrawView
    private lateinit var checkBoxEditMode: CheckBox
    private lateinit var buttonSave: Button
    private lateinit var buttonCancel: Button
    private lateinit var buttonPickColor: Button
    private lateinit var viewCurrentColor: View
    private lateinit var colorPickerLayout: LinearLayout
    private lateinit var editTextMemo: EditText

    private var selectedColor: Int = Color.RED

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val byteArray = arguments?.getByteArray("PHOTO_BITMAP")
        //photoBitmap = byteArray?.let { byteArrayToBitmap(it) }
        photoBitmap = byteArray?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
        setStyle(STYLE_NORMAL, android.R.style.Theme_Light_NoTitleBar_Fullscreen)
    }

//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        return inflater.inflate(R.layout.dialog_photo_result, container, false)
//    }
override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
    return inflater.inflate(R.layout.dialog_photo_result, container, false)
}

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        val imageViewPhoto: ImageView = view.findViewById(R.id.imageViewPhoto)
//        val drawView: DrawView = view.findViewById(R.id.drawView)
//        val buttonSave: Button = view.findViewById(R.id.buttonSave)
//        val buttonCancel: Button = view.findViewById(R.id.buttonCancel)
//        val checkBoxEditMode: CheckBox = view.findViewById(R.id.checkBoxEditMode)
//        val editToolsLayout: LinearLayout = view.findViewById(R.id.editToolsLayout)
        imageViewPhoto = view.findViewById(R.id.imageViewPhoto)
        drawView = view.findViewById(R.id.drawView)
        checkBoxEditMode = view.findViewById(R.id.checkBoxEditMode)
        buttonSave = view.findViewById(R.id.buttonSave)
        buttonCancel = view.findViewById(R.id.buttonCancel)
        buttonPickColor = view.findViewById(R.id.buttonPickColor)
        viewCurrentColor = view.findViewById(R.id.viewCurrentColor)
        colorPickerLayout = view.findViewById(R.id.colorPickerLayout)
        editTextMemo = view.findViewById(R.id.editTextMemo)

        // 화면에 그대로 전시
        photoBitmap?.let {
            imageViewPhoto.setImageBitmap(it)
        }

//        // 편집 모드 활성화/비활성화
//        checkBoxEditMode.setOnCheckedChangeListener { _, isChecked ->
//            drawView.visibility = if (isChecked) View.VISIBLE else View.GONE
//            editToolsLayout.visibility = if (isChecked) View.VISIBLE else View.GONE
//            Toast.makeText(requireContext(), if (isChecked) "편집 모드 활성화" else "편집 모드 해제", Toast.LENGTH_SHORT).show()
//        }
        checkBoxEditMode.setOnCheckedChangeListener { _, isChecked ->
            drawView.visibility = if (isChecked) View.VISIBLE else View.GONE
            colorPickerLayout.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        buttonPickColor.setOnClickListener {
            showColorPickerDialog()
        }

        buttonSave.setOnClickListener {
            val isEditing = checkBoxEditMode.isChecked
            val memoText = editTextMemo.text.toString()


            val finalBitmap = if (isEditing) {
                drawView.getBitmapWithBackground(photoBitmap!!)  // 🎨 원본 포함된 비트맵 가져오기
            } else {
                photoBitmap
            }

            //Boolean isori
            // 원본 사진 저장
            photoBitmap?.let {

               // savePhoto(it, fileName, "Pictures/MyApp/Original")  // 경로는 이대로 두고
               // val fileName = "ori_${System.currentTimeMillis()}.jpg"
                var caseName = IncidentManager.incidentName
                val fileName = caseName?.let { it1 -> generateFileName(false, it1) }
                val path = "Pictures/MyApp/${caseName}" // 상대 경로

                // 원본 사진 저장
                val fileUri = fileName?.let { it1 -> savePhoto(it, it1, path) }
                if (fileUri != null) {
                    uploadImage(fileUri, fileName, memoText)  // 클라우드로 업로드
                    Toast.makeText(context, "원본 파일 저장 및 업로드 완료.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "원본 파일 저장 실패", Toast.LENGTH_SHORT).show()
                }
            }
            // 편집된 사진 저장 및 업로드
            if (isEditing) {
                finalBitmap?.let {
                   // val fileName = "edit_${System.currentTimeMillis()}.jpg"
                   // val path = "Pictures/MyApp/사건1"  // 편집된 사진 경로 설정
                    var caseName = IncidentManager.incidentName
                    val fileName = caseName?.let { it1 -> generateFileName(true, it1) }
                    val path = "Pictures/MyApp/${caseName}" // 상대 경로

                    // 편집된 사진 저장
                    val fileUri = fileName?.let { it1 -> savePhoto(it, it1, path) }
                    if (fileUri != null) {
                        uploadImage(fileUri, fileName, memoText)  // 클라우드로 업로드
                        Toast.makeText(context, "편집된 파일 저장 및 업로드 완료.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "편집 파일 저장 실패", Toast.LENGTH_SHORT).show()
                    }
                }
            }
//            if (isEditing) {
//                // 편집된 사진 저장
//                if (finalBitmap != null) {
//                    savePhoto(
//                        finalBitmap,
//                        "edited_image_${System.currentTimeMillis()}.jpg",
//                        "Pictures/MyApp/Edited"
//                    )
//
//                } else {
//                }
//            }


            dismiss() // 다이얼로그 닫기
        }
        // 취소 버튼 클릭
        buttonCancel.setOnClickListener {
            dismiss()
        }
    }

    // 파일명 생성: 사건명 포함하여 유니크한 파일명 생성
    private fun generateFileName(isEdited: Boolean, caseName: String): String {
        val dateFormat = SimpleDateFormat("yyMMddHHmmss", Locale.getDefault())
        val currentDate = dateFormat.format(Date())

        // 사건명을 포함한 파일명 생성 (편집본은 "_EDIT", 원본은 "_ORI")
        return if (isEdited) {
            "${caseName}_${currentDate}_EDIT.jpg"
        } else {
            "${caseName}_${currentDate}_ORI.jpg"
        }
    }

    private fun getRealFilePathFromUri(uri: Uri): String? {
        var cursor: Cursor? = null
        try {
            val projection = arrayOf(MediaStore.Images.Media.DATA)
            cursor = requireContext().contentResolver.query(uri, projection, null, null, null)
            val columnIndex = cursor?.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            cursor?.moveToFirst()
            return cursor?.getString(columnIndex ?: -1)
        } finally {
            cursor?.close()
        }
    }

    private fun getFileUriFromPath(filePath: String): Uri? {
        val file = File(filePath)
        return Uri.fromFile(file)
    }
    private fun uploadImage(uri: Uri, fileName:String, memoText: String = "" ) {
        // URI에서 실제 파일 경로 얻기
        val filePath = getRealFilePathFromUri(uri)

        if (filePath != null) {
            val file = File(filePath)
            if (file.exists()) {
                // 파일이 존재하면 클라우드에 업로드
                val fileUploadClient = FileUploadClient(requireContext())

                fileUploadClient.uploadFileToCloud(Uri.fromFile(file), fileName, memoText)
                //fileUploadClient.uploadEditedFileToCloud(Uri.fromFile(file))
            } else {
                Toast.makeText(context, "파일이 존재하지 않습니다.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "잘못된 파일 경로입니다.", Toast.LENGTH_SHORT).show()
        }
    }


    private fun showColorPickerDialog() {
        val colors = intArrayOf(Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW, Color.BLACK)
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("펜 색상 선택")
        builder.setItems(arrayOf("빨강", "파랑", "초록", "노랑", "검정")) { _, which ->
            selectedColor = colors[which]
            viewCurrentColor.setBackgroundColor(selectedColor)
            drawView.setDrawingColor(selectedColor)
        }
        builder.show()
    }
    /**
     * 📌 편집 내용을 이미지에 합성
     */
    private fun mergeDrawViewToBitmap(bitmap: Bitmap, drawView: DrawView): Bitmap {
        val resultBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(resultBitmap)
        drawView.draw(canvas)
        return resultBitmap
    }

    /**
     * 📌 이미지 저장
     */
    @SuppressLint("RestrictedApi")
//    private fun savePhoto(bitmap: Bitmap, fileName: String, path: String) {
//        val contentValues = ContentValues().apply {
//            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
//            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
//            put(MediaStore.Images.Media.RELATIVE_PATH, path)
//        }
//
//        val resolver = requireContext().contentResolver
//        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
//
//        uri?.let {
//            resolver.openOutputStream(it).use { outputStream ->
//                if (outputStream != null) {
//                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
//                }
//            }
//        }
//    }
    private fun savePhoto(bitmap: Bitmap, fileName: String, path: String): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, path)
        }

        val resolver = requireContext().contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let {
            resolver.openOutputStream(it).use { outputStream ->
                outputStream?.let {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
                }
            }
            return uri
        }
        return null
    }

    /**
     * 📌 ByteArray를 Bitmap으로 변환
     */
    private fun byteArrayToBitmap(byteArray: ByteArray): Bitmap {
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    }

    companion object {
        /**
         * 📌 Bitmap을 인수로 받아 PhotoResultDialogFragment 생성
         */
        fun newInstance(bitmap: Bitmap): PhotoResultDialogFragment {
            val fragment = PhotoResultDialogFragment()
            val args = Bundle().apply {
                putByteArray("PHOTO_BITMAP", bitmapToByteArray(bitmap)) // Companion Object 내부의 함수로 호출
            }
            fragment.arguments = args
            return fragment
        }

        /**
         * 📌 Bitmap을 ByteArray로 변환
         */
        private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            return stream.toByteArray()
        }
    }

}
