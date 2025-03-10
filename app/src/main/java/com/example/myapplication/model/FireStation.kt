package com.example.myapplication.model

import org.json.JSONArray
import org.json.JSONObject

/**
 * 소방서 정보를 담는 데이터 클래스
 */
data class FireStation(
    val id: Int? = null,
    val city: String? = null,
    val stationName: String? = null,
    val centerName: String? = null,
    val regionNm: String? = null
    // Add other fields as needed
) {
    companion object {
        /**
         * JSONObject에서 FireStation 객체를 생성
         * @param json 파싱할 JSONObject
         * @return FireStation 객체
         */
        fun fromJson(json: JSONObject): FireStation {
            return FireStation(
                id = json.optInt("id", 0),
                city = json.optString("city"),
                stationName = json.optString("stationName", ""),
                centerName = json.optString("centerName"),
                regionNm = json.optString("regionNm")
            )
        }
        
        /**
         * JSONArray에서 FireStation 객체 리스트를 생성
         * @param jsonArray 파싱할 JSONArray
         * @return FireStation 객체 리스트
         */
        fun fromJsonArray(jsonArray: JSONArray): List<FireStation> {
            val list = mutableListOf<FireStation>()
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
