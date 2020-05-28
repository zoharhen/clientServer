package com.example.clientserver
//
//import android.content.Context
//import android.util.Log
//import android.widget.Toast
//import androidx.lifecycle.LifecycleOwner
//import androidx.work.*
//import com.google.gson.Gson
//import java.util.*
//
//class FlowHelper {
//
//    companion object { // static methods
//
//        fun getUserToken(context: Context, owner: LifecycleOwner, username: String): String {
//            var tokenRes = ""
//            val workTagUniqueId = UUID.randomUUID()
//            val getUserTokenWork = OneTimeWorkRequest.Builder(ServerHolder.GetUserTokenWorker::class.java)
//                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
//                .setInputData(Data.Builder().putString("username", username).build())
//                .addTag(workTagUniqueId.toString())
//                .build()
//            WorkManager.getInstance(context).enqueue(getUserTokenWork)
//            WorkManager.getInstance(context)
//                .getWorkInfosByTagLiveData(workTagUniqueId.toString())
//                .observe(owner, androidx.lifecycle.Observer<List<WorkInfo>> { workInfo ->
//                    if (!workInfo.isNullOrEmpty()) {
//                        val tokenAsJson = workInfo[0].outputData.getString("token")
//                        if (!tokenAsJson.isNullOrBlank()) {
//                            Log.d("getUserToken_${username}", tokenAsJson)
//                            tokenRes = Gson().fromJson(tokenAsJson, Model.TokenResponse::class.java).data
//                        }
//                    }
//                })
//            return tokenRes
//        }
//
//        fun getUserInfo(context: Context, owner: LifecycleOwner, token: String) : Model.User? {
//            var userInfoRes: Model.User? = null
//            val workTagUniqueId = UUID.randomUUID()
//            val getUserInfoWork = OneTimeWorkRequest.Builder(ServerHolder.GetUserInfoWorker::class.java)
//                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
//                .setInputData(Data.Builder().putString("token", token).build())
//                .addTag(workTagUniqueId.toString())
//                .build()
//            WorkManager.getInstance(context).enqueue(getUserInfoWork)
//            WorkManager.getInstance(context)
//                .getWorkInfosByTagLiveData(workTagUniqueId.toString())
//                .observe(owner, androidx.lifecycle.Observer<List<WorkInfo>> { workInfo ->
//                    if (!workInfo.isNullOrEmpty()) {
//                        val userInfoAsJson = workInfo[0].outputData.getString("userInfo")
//                        if (!userInfoAsJson.isNullOrBlank()) {
//                            Log.d("getUserInfo_${token}", userInfoAsJson)
//                            userInfoRes = Gson().fromJson(userInfoAsJson, Model.UserResponse::class.java).data
//                        }
//                    }
//                })
//            return userInfoRes
//        }
//
//        fun updatePrettyName(context: Context, owner: LifecycleOwner, token: String, prettyName: String): Model.User? {
//            var userInfoRes: Model.User? = null
//            val workTagUniqueId = UUID.randomUUID()
//            val updateUserInfoWork = OneTimeWorkRequest.Builder(ServerHolder.UpdateUserInfoWorker::class.java)
//                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
//                .setInputData(Data.Builder().putString("token", token).putString("pretty_name", prettyName).build())
//                .addTag(workTagUniqueId.toString())
//                .build()
//            WorkManager.getInstance(context).enqueue(updateUserInfoWork)
//            WorkManager.getInstance(context)
//                .getWorkInfosByTagLiveData(workTagUniqueId.toString())
//                .observe(owner, androidx.lifecycle.Observer<List<WorkInfo>> { workInfo ->
//                    if (!workInfo.isNullOrEmpty()) {
//                        if (workInfo[0].state == WorkInfo.State.FAILED) Toast.makeText(context, "Update userInfo failed", Toast.LENGTH_LONG).show()
//                        else {
//                            val userInfoAsJson = workInfo[0].outputData.getString("userInfo")
//                            if (!userInfoAsJson.isNullOrBlank()) {
//                                Log.d("getUserInfo_${token}", userInfoAsJson)
//                                userInfoRes = Gson().fromJson(userInfoAsJson, Model.UserResponse::class.java).data
//                            }
//                        }
//                    }
//                })
//            return userInfoRes
//        }
//    }
//
//}