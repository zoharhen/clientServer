package com.example.clientserver

import android.content.Context
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import okhttp3.OkHttpClient
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException


object Model {
    data class TokenResponse(val data: String)
    data class User(val username: String, var pretty_name: String, val image_url: String)
    data class UserResponse (val data: User)
}

class ServerHolder private constructor(val serverInterface: IServer) {

    companion object {
        private const val SERVER_URL = "http://hujipostpc2019.pythonanywhere.com"

        @get:Synchronized
        var instance: ServerHolder? = null
            get() {
                if (field != null) return field
                val client = OkHttpClient.Builder().build()
                val retrofit = Retrofit.Builder()
                    .client(client)
                    .baseUrl(SERVER_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                val serverInterface: IServer = retrofit.create(IServer::class.java)
                field = ServerHolder(serverInterface)
                return field
            }
            private set
    }

    class GetUserTokenWorker(context: Context, workerParams: WorkerParameters): Worker(context, workerParams) {
        @Synchronized
        override fun doWork(): Result {
            val serverInterface: IServer? = instance?.serverInterface
            try {
                val response= serverInterface!!.getUserToken(inputData.getString("username")).execute()
                val token= Gson().toJson(response.body())
                val outputData = Data.Builder()
                    .putString("token", token)
                    .build()
                return Result.success(outputData)
            } catch (e: IOException) {
                e.printStackTrace()
                return Result.retry()
            }
        }
    }

    class GetUserInfoWorker(context: Context, workerParams: WorkerParameters): Worker(context, workerParams) {
        @Synchronized
        override fun doWork(): Result {
            val serverInterface: IServer? = instance?.serverInterface
            return try {
                val tokenKey = "token ${inputData.getString("token")}"
                val response= serverInterface!!.getUserInfo(tokenKey).execute()
                val userInfo= Gson().toJson(response.body())
                val outputData = Data.Builder()
                    .putString("userInfo", userInfo)
                    .build()
                Result.success(outputData)
            } catch (e: IOException) {
                e.printStackTrace()
                Result.retry()
            }
        }
    }

    class UpdateUserInfoWorker(context: Context, workerParams: WorkerParameters): Worker(context, workerParams) {
        @Synchronized
        override fun doWork(): Result {
            val serverInterface: IServer? = instance?.serverInterface
            return try {
                val prettyName = inputData.getString("key_pretty_name") ?: ""
                val imageUrl = inputData.getString("key_image_url") ?: ""
                val tokenKey = "token ${inputData.getString("token")}"
                val json = JSONObject("""{"pretty_name":"$prettyName", "image_url":$imageUrl}""")
                val response= serverInterface!!.updateUserInfo(tokenKey, json).execute()
                val userInfo = Gson().toJson(response.body() ?: Result.failure())
                val outputData = Data.Builder()
                    .putString("userInfo", userInfo)
                    .build()
                Result.success(outputData)
            } catch (e: IOException) {
                e.printStackTrace()
                Result.retry()
            }
        }
    }

}