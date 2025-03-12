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
import android.widget.AdapterView
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
    private lateinit var viewCurrentColor: View
    private lateinit var viewIconColor: View
    private lateinit var spinnerIconSelect: Spinner
    private lateinit var colorPickerLayout: LinearLayout
    private lateinit var editTextMemo: EditText
    private lateinit var buttonEraser: Button

    private var selectedColor: Int = Color.RED
    private var selectedIconColor: Int = Color.RED
    private var selectedIcon: Int = 0 // 0: 선택 없음, 1-5: 아이콘 번호
    private var isDrawMode: Boolean = true // true: 그리기 모드, false: 아이콘 모드
    private var isEraserMode: Boolean = false // 지우기 모드 상태
    private val iconMemos = HashMap<Int, String>() // 아이콘별 메모 저장
    private val iconColors = HashMap<Int, Int>() // 아이콘별 색상 저장

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
        viewCurrentColor = view.findViewById(R.id.viewCurrentColor)
        viewIconColor = view.findViewById(R.id.viewIconColor)
        spinnerIconSelect = view.findViewById(R.id.spinnerIconSelect)
        colorPickerLayout = view.findViewById(R.id.colorPickerLayout)
        editTextMemo = view.findViewById(R.id.editTextMemo)
        buttonEraser = view.findViewById(R.id.buttonEraser)
        
        // 초기에는 메모 필드 비활성화
        editTextMemo.isEnabled = false
        
        // 스피너 설정
        setupIconSpinner()

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
            
            if (isChecked) {
                // 초기에는 그리기 모드로 설정
                setDrawMode(true)
            }
        }

        // 그리기 색상 선택
        viewCurrentColor.setOnClickListener {
            setDrawMode(true);
            showColorPickerDialog(true)
        }
        
        // 아이콘 색상 선택
        viewIconColor.setOnClickListener {
            if (selectedIcon > 0) {
                showColorPickerDialog(false)
            } else {
                Toast.makeText(requireContext(), "아이콘을 먼저 선택해주세요", Toast.LENGTH_SHORT).show()
            }
        }
        
        // 지우기 버튼 처리
        buttonEraser.setOnClickListener {
            toggleEraserMode()
        }
        
        // 메모 필드 변경 시 현재 선택된 아이콘에 메모 연결
        editTextMemo.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus && !isDrawMode) {
                saveCurrentMemo()
            }
        }

        buttonSave.setOnClickListener {
            val isEditing = checkBoxEditMode.isChecked
            val memoText = editTextMemo.text.toString()


            // 현재 메모 저장
            saveCurrentMemo()
            
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


    private fun showColorPickerDialog(isDrawingColor: Boolean) {
        val colors = intArrayOf(Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW, Color.BLACK, Color.MAGENTA, Color.CYAN)
        val colorNames = arrayOf("빨강", "파랑", "초록", "노랑", "검정", "보라", "하늘")
        
        val builder = AlertDialog.Builder(requireContext())
        val title = if (isDrawingColor) "펜 색상 선택" else "아이콘 색상 선택"
        builder.setTitle(title)
        
        builder.setItems(colorNames) { _, which ->
            val selectedColorValue = colors[which]
            
            if (isDrawingColor) {
                // 그리기 색상 업데이트
                selectedColor = selectedColorValue
                viewCurrentColor.setBackgroundColor(selectedColor)
                drawView.setDrawingColor(selectedColor)
            } else {
                // 아이콘 색상 업데이트
                selectedIconColor = selectedColorValue
                viewIconColor.setBackgroundColor(selectedIconColor)
                
                // 현재 선택된 아이콘의 색상 저장
                if (selectedIcon > 0) {
                    iconColors[selectedIcon] = selectedIconColor
                    drawView.setIconColor(selectedIcon, selectedIconColor)
                }
            }
        }
        
        builder.show()
    }
    
    private fun setupIconSpinner() {
        // 스피너 아이템 설정
        val items = arrayOf("선택", "1", "2", "3", "4", "5")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, items)
        spinnerIconSelect.adapter = adapter
        
        // 스피너 선택 이벤트 처리
        spinnerIconSelect.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // 현재 메모와 색상 저장
                if (selectedIcon > 0) {
                    saveCurrentMemo()
                    saveCurrentIconColor()
                }
                
                // 아이콘 번호 업데이트 (0은 선택없음, 1-5는 아이콘 번호)
                selectedIcon = position
                
                if (position > 0) {
                    // 아이콘 모드로 전환 (선택이 있을 때만)
                    setDrawMode(false)
                    // 저장된 메모와 색상 불러오기
                    loadMemoForCurrentIcon()
                    loadColorForCurrentIcon()
                    // 메모 필드 활성화
                    editTextMemo.isEnabled = true
                } else {
                    // 그리기 모드로 전환 (선택이 없을 때)
                    setDrawMode(true)
                    // 메모 필드 비활성화 및 비우기
                    editTextMemo.setText("")
                    editTextMemo.isEnabled = false
                }
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // 아무것도 선택되지 않았을 때
                setDrawMode(true)
            }
        }
    }
    
    private fun setDrawMode(drawMode: Boolean) {
        isDrawMode = drawMode
        
        // 지우기 모드 비활성화
        if (isEraserMode) {
            isEraserMode = false
            drawView.setEraserMode(false)
            updateEraserButtonState()
        }
        
        // 아이콘/그리기 모드 전환
        drawView.setIconMode(!drawMode)
        
        // UI 업데이트
        if (drawMode) {
            // 그리기 모드
            // 스피너를 '선택'으로 설정
            if (spinnerIconSelect.selectedItemPosition != 0) {
                spinnerIconSelect.setSelection(0, false)
            }
        } else {
            // 아이콘 모드
            // 현재 선택된 아이콘 설정
            if (selectedIcon > 0) {
                drawView.setSelectedIcon(selectedIcon)
            }
        }
    }
    
    private fun saveCurrentMemo() {
        if (!isDrawMode && selectedIcon > 0) {
            val memo = editTextMemo.text.toString()
            iconMemos[selectedIcon] = memo
            drawView.setIconMemo(selectedIcon, memo)
        }
    }
    
    private fun saveCurrentIconColor() {
        if (selectedIcon > 0) {
            iconColors[selectedIcon] = selectedIconColor
            drawView.setIconColor(selectedIcon, selectedIconColor)
        }
    }
    
    private fun loadMemoForCurrentIcon() {
        // 현재 아이콘에 저장된 메모 불러오기
        if (selectedIcon > 0) {
            val memo = iconMemos[selectedIcon] ?: drawView.getIconMemo(selectedIcon)
            editTextMemo.setText(memo)
        } else {
            editTextMemo.setText("")
        }
    }
    
    private fun loadColorForCurrentIcon() {
        // 현재 아이콘에 저장된 색상 불러오기
        if (selectedIcon > 0) {
            val iconColor = iconColors[selectedIcon] ?: drawView.getIconColor(selectedIcon)
            selectedIconColor = iconColor
            viewIconColor.setBackgroundColor(selectedIconColor)
        }
    }
    
    /**
     * 지우기 모드 토글
     */
    private fun toggleEraserMode() {
        // 지우기 모드와 다른 모드 사이의 전환
        isEraserMode = !isEraserMode
        
        if (isEraserMode) {
            // 지우기 모드 활성화 시 그리기/아이콘 모드 비활성화
            isDrawMode = false
            drawView.setIconMode(false)
            drawView.setEraserMode(true)
            
            // 스피너 비활성화 및 메모 필드 비활성화
            spinnerIconSelect.setSelection(0)
            editTextMemo.setText("")
            editTextMemo.isEnabled = false
        } else {
            // 지우기 모드 비활성화 시 그리기 모드로 전환
            isDrawMode = true
            drawView.setEraserMode(false)
            setDrawMode(true)
        }
        
        // 지우기 버튼 상태 업데이트
        updateEraserButtonState()
    }
    
    /**
     * 지우기 버튼 상태 업데이트
     */
    private fun updateEraserButtonState() {
        if (isEraserMode) {
            // 지우기 모드가 활성화되어 있을 때
            buttonEraser.setBackgroundColor(Color.LTGRAY)
            buttonEraser.setTextColor(Color.BLACK)
        } else {
            // 지우기 모드가 비활성화되어 있을 때
            buttonEraser.setBackgroundResource(android.R.drawable.btn_default)
            buttonEraser.setTextColor(Color.BLACK)
        }
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
