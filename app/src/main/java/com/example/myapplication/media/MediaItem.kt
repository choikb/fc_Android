package com.example.myapplication.media

data class MediaItem(
    val type: MediaType,
    val name: String,
    val path: String,
    var isSelected: Boolean = false
)
