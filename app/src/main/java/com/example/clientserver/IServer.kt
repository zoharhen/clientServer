package com.example.clientserver

import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Body

interface IServer {

    @GET("/users/0")
    fun connectivityCheck(): Call<Model.UserResponse>

    @GET("/users/{username}/token/")
    fun getUserToken(@Path("username") username: String? ): Call<Model.TokenResponse>

    @GET("/user/")
    fun getUserInfo(@Header("Authorization") token: String ): Call<Model.UserResponse>

    @Headers("Content-Type: application/json")
    @POST("/user/edit/")
    fun updateUserInfo(@Header("Authorization") token: String , @Body prettyName: JsonObject): Call<Model.UserResponse>

}