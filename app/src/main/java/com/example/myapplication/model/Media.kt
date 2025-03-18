package com.example.myapplication.model


import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime

data class Media(
    val mediaId: Long? = null,
    val storageUrl: String? = null,
    val thumbnailUrl: String? = null,
    val mediaType: String? = null,
    val fileName: String? = null,
    val incidentId: Long? = null,
    val creatorId: String? = null,
    val size: Long? = null,
    val version: String? = null,

    val latitude: Double? = null,
    val longitude: Double? = null,
    val altitude: Double? = null,
    val locationAddress: String? = null,
    val resolution: String? = null,
    val viewCount: Int? = null,
    val downloadCount: Int? = null,
    val hashtags: String? = null,
    val memo: String? = null,
    val address: String? = null,
    val region: String? = null,

    // Transient fields (not stored in DB)
    val fileData: String? = null,
    val mimeType: String? = null
) {
    companion object {
        /**
         * JSONObject에서 MediaVO 객체를 생성
         * @param json 파싱할 JSONObject
         * @return MediaVO 객체
         */
        fun fromJson(json: JSONObject): Media {
            return Media(
                mediaId = json.optLong("mediaId"),
                storageUrl = json.optString("storageUrl"),
                thumbnailUrl = json.optString("thumbnailUrl"),
                mediaType = json.optString("mediaType"),
                fileName = json.optString("fileName"),
                incidentId = json.optLong("incidentId"),
                creatorId = json.optString("creatorId"),
                size = json.optLong("size"),
                version = json.optString("version"),
                latitude = json.optDouble("latitude"),
                longitude = json.optDouble("longitude"),
                altitude = json.optDouble("altitude"),
                locationAddress = json.optString("locationAddress"),
                resolution = json.optString("resolution"),
                viewCount = json.optInt("viewCount"),
                downloadCount = json.optInt("downloadCount"),
                hashtags = json.optString("hashtags"),
                memo = json.optString("memo"),
                address = json.optString("address"),
                region = json.optString("region"),
                fileData = json.optString("fileData"),
                mimeType = json.optString("mimeType")
            )
        }

        /**
         * JSONArray에서 MediaVO 객체 리스트를 생성
         * @param jsonArray 파싱할 JSONArray
         * @return MediaVO 객체 리스트
         */
        fun fromJsonArray(jsonArray: JSONArray): List<Media> {
            val list = mutableListOf<Media>()
            for (i in 0 until jsonArray.length()) {
                val json = jsonArray.optJSONObject(i)
                if (json != null) {
                    list.add(fromJson(json))
                }
            }
            return list
        }
    }
}
