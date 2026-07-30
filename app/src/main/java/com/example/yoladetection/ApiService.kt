package com.example.yoladetection
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
interface ApiService {
    @Multipart
    @POST("detect")
    suspend fun detect(
        @Part image: MultipartBody.Part
    ): Response<DetectionResponse>
}