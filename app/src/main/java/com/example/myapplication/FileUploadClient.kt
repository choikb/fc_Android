package com.example.myapplication

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.Settings
import android.util.Base64
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import com.example.myapplication.api.ApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

//class FileUploadClient(private val context: Context) {
//
//
//    fun uploadFileToCloud(fileUri: Uri) {
//        val url =
//            "https://ev0tafewtt.apigw.ntruss.com/firecapture-api/v0001/media?blocking=true&result=true"
//        val file = File(fileUri.path)
//        val fileName = file.name
//        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension)
//            ?: "application/octet-stream"
//
//// Base64로 인코딩된 이미지 파일
//        val fileData = file.readBytes()
//        val encodedFileData = Base64.encodeToString(fileData, Base64.NO_WRAP)
//
//        val jsonBody = """
//    {
//        "fileName": "/1/1.jpg",
//        "fileData": "$encodedFileData",
//        "mimeType": "$mimeType"
//    }
//"""
//
//        val requestBody = RequestBody.create(
//            "application/json".toMediaTypeOrNull(),
//            jsonBody
//        )
//
//        val request = Request.Builder()
//            .url(url)
//            .post(requestBody)
//            .header("Content-Type", "application/json")
//            .build()
//
//        val client = OkHttpClient()
//
//        client.newCall(request).enqueue(object : Callback {
//            override fun onFailure(call: Call, e: IOException) {
//                println("파일 업로드 실패: ${e.message}")
//            }
//
//            override fun onResponse(call: Call, response: Response) {
//                if (response.isSuccessful) {
//                    println("파일 업로드 성공!")
//                } else {
//                    println("파일 업로드 실패, 상태 코드: ${response.code}")
//                }
//            }
//        })
//    }
//}

fun compressImageAndEncode(file: File): String {
    // 이미지 파일을 Bitmap으로 디코딩
    val originalBitmap = BitmapFactory.decodeFile(file.absolutePath)

    // 리사이징 및 품질 최소화 (80% 품질)
    val byteArrayOutputStream = ByteArrayOutputStream()
    originalBitmap.compress(Bitmap.CompressFormat.JPEG, 30, byteArrayOutputStream)  // 30% 품질로 압축
    val compressedBytes = byteArrayOutputStream.toByteArray()

    // Base64로 인코딩
    return Base64.encodeToString(compressedBytes, Base64.NO_WRAP)
}

class FileUploadClient(private val context: Context) {

    // 전역변수 예시
    var incidentId = IncidentManager.incidentId

    // 다른 클래스에서 androidId 불러오기
    val prefs = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
    val creatorId = prefs.getString("ANDROID_ID", null)  // "ANDROID_ID" 키로 저장된 값을 가져옴

    //val regionName = prefs.getString("CITY", null)
    val regionName = "충남"

    suspend fun uploadFileToCloud(fileUri: Uri, fileName:String, memoText:String): Boolean {
        return try {
            // Call the new ApiService that prioritizes EXIF metadata for location
            val result = ApiService.uploadFileWithExifMetadata(
                context = context,
                fileUri = fileUri,
                fileName = fileName,
                memoText = memoText,
                incidentId = incidentId,
                creatorId = creatorId,
                regionName = regionName
            )
            
            // Return the result from the API call
            result
        } catch (e: Exception) {
            Log.e("FileUploadClient", "Error uploading file", e)
            false
        }
    }

    // 파일명 생성: 현재 날짜와 시간 기반으로 유니크한 파일명 생성
    private fun generateFileName(): String {
        val dateFormat = SimpleDateFormat("yyMMddHHmmss", Locale.getDefault())
        val currentDate = dateFormat.format(Date())
        return "${currentDate}_ORI.jpg"
    }
}