package com.example.myapplication.media
//
//data class MediaItem(
//    val type: MediaType,
//    val name: String,
//    val path: String,
//    var isSelected: Boolean = false
//)
//data class MediaItem(
//    val name: String,
//    val path: String,
//    val isFolder: Boolean = false // 기본값 false로 설정
//)

data class MediaItem(
    val name: String,
    val path: String,
    val isFolder: Boolean = false, // 기본값 false로 설정
    val creationTime: String = "", // 생성 시기 추가
    val photoCount: Int = 0 // 폴더 내 사진 수 추가
)

