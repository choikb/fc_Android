package com.example.myapplication
import okhttp3.*
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class ApiClient(private val baseUrl: String) {
    private val client = OkHttpClient.Builder()
        .readTimeout(30, TimeUnit.SECONDS) // 최대 30초 대기
        .build()

    private val gson = Gson()

    /**
     * ✅ API 요청 수행 (동기/비동기 선택)
     * @param jsonBody 요청 데이터
     * @param isBlocking true: 동기 방식 / false: 비동기 방식
     * @param callback 비동기 처리 시 응답 콜백 (동기 호출 시 null)
     */
    fun requestProcess(jsonBody: String, isBlocking: Boolean, callback: ((String?) -> Unit)? = null): String? {
        val url = "$baseUrl" + if (isBlocking) "?blocking=true" else ""
        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toRequestBody("application/json".toMediaTypeOrNull()))
            .build()

        return if (isBlocking) {
            try {
                val response = client.newCall(request).execute()
                response.body?.string()
            } catch (e: IOException) {
                null
            }
        } else {
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    callback?.invoke(null)
                }

                override fun onResponse(call: Call, response: Response) {
                    val body = response.body?.string()
                    val activationId = gson.fromJson(body, Map::class.java)["activationId"] as? String
                    callback?.invoke(activationId)
                }
            })
            null
        }
    }

    /**
     * ✅ Activation ID로 결과 조회 (POST 방식)
     * @param activationId 조회할 Activation ID
     * @param isBlocking true: 동기 방식 / false: 비동기 방식
     * @param callback 비동기 처리 시 응답 콜백 (동기 호출 시 null)
     */
    fun fetchResult(activationId: String, isBlocking: Boolean, callback: ((String?) -> Unit)? = null): String? {
        val url = "$baseUrl"
        val requestBody = """{"activationId": "$activationId"}"""
            .toRequestBody("application/json".toMediaTypeOrNull())

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        return if (isBlocking) {
            try {
                val response = client.newCall(request).execute()
                response.body?.string()
            } catch (e: IOException) {
                null
            }
        } else {
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    callback?.invoke(null)
                }

                override fun onResponse(call: Call, response: Response) {
                    callback?.invoke(response.body?.string())
                }
            })
            null
        }
    }

    /**
     * ✅ 비동기 방식에서 일정 간격으로 결과 조회 (POST 기반)
     * @param activationId 조회할 Activation ID
     * @param maxRetries 최대 재시도 횟수 (기본 5회)
     * @param delayMs 각 요청 사이 대기 시간 (기본 5초)
     * @param callback 최종 결과 콜백
     */
    fun fetchResultPeriodically(activationId: String, maxRetries: Int = 5, delayMs: Long = 5000, callback: (String?) -> Unit) {
        Thread {
            repeat(maxRetries) { attempt ->
                Thread.sleep(delayMs)
                val response = fetchResult(activationId, isBlocking = true)
                if (response != null && response.contains("completed")) {
                    callback(response)
                    return@Thread
                }
            }
            callback(null)
        }.start()
    }
}
