package com.example.yoladetection

import com.google.gson.annotations.SerializedName

/*data class Detection(
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
    val confidence: Float,
    val `class`: String
)*/
data class Detection(
    @SerializedName("class_id")
    val classId: Int,

    @SerializedName("class_name")
    val className: String,

    val confidence: Float,

    val box: List<Float>
)