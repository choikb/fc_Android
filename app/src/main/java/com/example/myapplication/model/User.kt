package com.example.myapplication.model

import org.json.JSONArray
import org.json.JSONObject

data class User (
    val userId: String,
    val deviceId: String,
    val organization: String,
    val rank: String,
    val name: String,
    val role: String,
    val email: String,
    val phoneNumber: String,
    val firestationId: Int?,
    val city: String,
    val stationName: String,
    val centerName: String,
    val regionNm: String
) {
    companion object {
        /**
         * JSONObject에서 UserVO 객체를 생성
         * @param json 파싱할 JSONObject
         * @return UserVO 객체
         */
        fun fromJson(json: JSONObject): User {
            return User(
                userId = json.optString("userId", ""),
                deviceId = json.optString("deviceId", ""),
                organization = json.optString("organization", ""),
                rank = json.optString("rank", ""),
                name = json.optString("name", ""),
                role = json.optString("role", ""),
                email = json.optString("email", ""),
                phoneNumber = json.optString("phoneNumber", ""),
                firestationId = json.optInt("firestationId").takeIf { it != 0 },
                city = json.optString("city"),
                stationName = json.optString("stationName", ""),
                centerName = json.optString("centerName"),
                regionNm = json.optString("regionNm")
            )
        }

        /**
         * JSONArray에서 UserVO 객체 리스트를 생성
         * @param jsonArray 파싱할 JSONArray
         * @return UserVO 객체 리스트
         */
        fun fromJsonArray(jsonArray: JSONArray): List<User> {
            val list = mutableListOf<User>()
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
