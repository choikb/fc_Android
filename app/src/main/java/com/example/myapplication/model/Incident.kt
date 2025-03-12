package com.example.myapplication.model

import org.json.JSONArray
import org.json.JSONObject
import java.util.Date

data class Incident(
    val incidentId: Int? = null,
    val incidentName: String? = null,
    val creatorId: String? = null,
    val editorId: String? = null,
    val description: String? = null,
    val type: String? = null,
    val relatedIncidentId: Int? = null,
    val photoCount: Int? = null,
    val videoCount: Int? = null,
    val status: String? = null,
    val createdAt: String? = null,
    val startDate: String? = null,
    val endDate: String? = null
) {
    companion object {
        /**
         * JSONObject에서 Incident 객체를 생성
         * @param json 파싱할 JSONObject
         * @return Incident 객체
         */
        fun fromJson(json: JSONObject): Incident {
            return Incident(
                incidentId = json.optInt("incidentId"),
                incidentName = json.optString("incidentName"),
                creatorId = json.optString("creatorId"),
                editorId = json.optString("editorId"),
                description = json.optString("description"),
                type = json.optString("type"),
                relatedIncidentId = json.optInt("relatedIncidentId"),
                photoCount = json.optInt("photoCount"),
                videoCount = json.optInt("videoCount"),
                status = json.optString("status"),
                createdAt = json.optString("createdAt"),
                startDate = json.optString("startDate"),
                endDate = json.optString("endDate")
            )
        }

        /**
         * JSONArray에서 Incident 객체 리스트를 생성
         * @param jsonArray 파싱할 JSONArray
         * @return Incident 객체 리스트
         */
        fun fromJsonArray(jsonArray: JSONArray): List<Incident> {
            val list = mutableListOf<Incident>()
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