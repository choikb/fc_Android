package com.example.myapplication

object AppConfig {
    // Base URL
    const val URL_BASE = "https://ev0tafewtt.apigw.ntruss.com/firecapture-api/v0001"

    // Sub URLs
    const val SURL_USER_API = "/user"
    const val SURL_USER_APIPROCESS_API = "/user/process"
    const val SURL_USER_APIRESULT_API = "/user/result"

    // Common Headers
    const val HEADER_CONTENT_TYPE = "Content-Type"
    const val HEADER_ACCEPT = "application/json"

    // Other global constants
    const val TIMEOUT_SECONDS = 30L
}