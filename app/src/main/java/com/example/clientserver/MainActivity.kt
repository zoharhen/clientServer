package com.example.clientserver

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.work.*
import com.example.clientserver.Model.TokenResponse
import com.example.clientserver.Model.UserResponse
import com.example.clientserver.ServerHolder.GetUserTokenWorker
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*


class MainActivity : AppCompatActivity() {

    private val PERMISSION_ID_INTERNET = 123
    private lateinit var sp : SharedPreferences
    private var token: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        handlePermission()

        sp = getPreferences(Context.MODE_PRIVATE)
        token = sp.getString("TOKEN", null)
        if (token.isNullOrBlank()) {
            setLoginUI()
        } else {
            usernameEditText.isEnabled = false
            loadingIcon.visibility = View.VISIBLE
             val user = getUserInfo()!!
            loadingIcon.visibility = View.GONE
            switchToUpdateInfoUI(if (user.pretty_name.isBlank()) user.username else user.pretty_name)
        }
    }

    private fun setLoginUI() {
        usernameEditText.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable) {
                if (usernameEditText.text.toString().matches(Regex("[A-Za-z0-9]+"))) {
                    usernameEditText.error = null
                    okButton.isEnabled = true
                }
                else {
                    okButton.isEnabled = false
                    usernameEditText.error = "Field can not be blank. Only letters and digits allowed, no whitespaces or special characters"
                }
            }
        })

        okButton.setOnClickListener {
            val manager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
            manager.hideSoftInputFromWindow(it.windowToken, 0)
            if (token.isNullOrBlank()) { // login flow
                getUserToken(usernameEditText.text.toString())
                switchToUpdateInfoUI(usernameEditText.text.toString())
            } else { // update pretty name flow
//                updatePrettyName() + update UI and server
//                val name = if (user.pretty_name.isBlank()) user.username else user.pretty_name)
//                welcomeTextView.text = "Welcome, $name!"
                // todo: write updatePrettyName() method!
            }

        }
    }

    private fun saveToken(token: String) {
        this.token = token
        sp.edit().putString("token", token).apply()
    }

    @SuppressLint("SetTextI18n")
    private fun switchToUpdateInfoUI(name : String) {
        usernameEditText.visibility = View.INVISIBLE
        prettynameEditText.visibility = View.VISIBLE
        okButton.text = "Update"
        okButton.isEnabled = true
        welcomeTextView.text = "Welcome, $name!"
        welcomeTextView.visibility = View.VISIBLE
    }

    /////////////////////////// permissions ///////////////////////////////

    private fun handlePermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.INTERNET), PERMISSION_ID_INTERNET)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == PERMISSION_ID_INTERNET) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                // nothing to do
            }
            else {
                Toast.makeText(applicationContext, "App can't operate without internet permission", Toast.LENGTH_LONG).show()
            }
        }
    }

    /////////////////////////// server flow ///////////////////////////////

    private fun getUserToken(username: String) {
        val workTagUniqueId = UUID.randomUUID()
        val getUserTokenWork = OneTimeWorkRequest.Builder(GetUserTokenWorker::class.java)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setInputData(Data.Builder().putString("username", username).build())
            .addTag(workTagUniqueId.toString())
            .build()
        WorkManager.getInstance(applicationContext).enqueue(getUserTokenWork)
        WorkManager.getInstance(applicationContext)
            .getWorkInfosByTagLiveData(workTagUniqueId.toString())
            .observe(this, androidx.lifecycle.Observer<List<WorkInfo>> { workInfo ->
                if (!workInfo.isNullOrEmpty()) {
                    val tokenAsJson = workInfo[0].outputData.getString("token")
                    if (!tokenAsJson.isNullOrBlank()) {
                        Log.d("getUserToken_${username}", tokenAsJson)
                        saveToken(Gson().fromJson(tokenAsJson, TokenResponse::class.java).data)
                    }
                }
            })
    }

    private fun getUserInfo() : Model.User? {
        var userInfoRes: Model.User? = null
        val workTagUniqueId = UUID.randomUUID()
        val getUserInfoWork = OneTimeWorkRequest.Builder(ServerHolder.GetUserInfoWorker::class.java)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setInputData(Data.Builder().putString("token", this.token).build())
            .addTag(workTagUniqueId.toString())
            .build()
        WorkManager.getInstance(applicationContext).enqueue(getUserInfoWork)
        WorkManager.getInstance(applicationContext)
            .getWorkInfosByTagLiveData(workTagUniqueId.toString())
            .observe(this, androidx.lifecycle.Observer<List<WorkInfo>> { workInfo ->
                if (!workInfo.isNullOrEmpty()) {
                    val userInfoAsJson = workInfo[0].outputData.getString("userInfo")
                    if (!userInfoAsJson.isNullOrBlank()) {
                        Log.d("getUserInfo_${this.token}", userInfoAsJson!!)
                        userInfoRes = Gson().fromJson(userInfoAsJson, UserResponse::class.java).data
                    }
                }
            })
        return userInfoRes
        // todo: get user imageURL and present it!
    }


}
