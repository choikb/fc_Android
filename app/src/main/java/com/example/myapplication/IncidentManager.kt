package com.example.myapplication

object IncidentManager {
    var incidentId: Long? = null
    var incidentName: String? = null

    fun clear() {
        incidentId = null
        incidentName = null
    }
}
