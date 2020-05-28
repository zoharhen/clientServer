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
import com.google.gson.Gson
import com.squareup.picasso.MemoryPolicy
import com.squareup.picasso.Picasso
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

        sp = getSharedPreferences("clientServer", Context.MODE_PRIVATE)
        token = sp.getString("token", null)
        setLoginUI()
        if (!token.isNullOrBlank()) {
            usernameEditText.isEnabled = false
             this.getUserInfo(token!!) // calling switchToUpdateInfoUI func
        }
    }

    @SuppressLint("SetTextI18n")
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
                this.getUserToken(usernameEditText.text.toString()) // calling saveToken() and switchToUpdateInfoUI methods (via getUserInfo observer)
            } else { // update pretty name flow
                val prettyname = prettynameEditText.text.toString()
                prettynameEditText.text.clear()
                this.updatePrettyName(prettyname) // calling updateUiWithUserInfo
            }
        }
    }

    private fun saveToken(token: String) {
        this.token = token
        sp.edit().putString("token", token).apply()
    }

    @SuppressLint("SetTextI18n")
    private fun switchToUpdateInfoUI(name : String, img: String) {
        usernameEditText.visibility = View.INVISIBLE
        prettynameEditText.visibility = View.VISIBLE
        okButton.text = "Update"
        okButton.isEnabled = true
        this.updateUiWithUserInfo(name, img)
        userIconView.visibility = View.VISIBLE
        welcomeTextView.visibility = View.VISIBLE
    }

    @SuppressLint("SetTextI18n")
    private fun updateUiWithUserInfo(name: String, img: String) {
        Picasso
            .with(applicationContext)
            .load(ServerHolder.SERVER_URL + img)
            .memoryPolicy(MemoryPolicy.NO_STORE)
            .into(userIconView)
        userIconView.visibility = View.VISIBLE
        welcomeTextView.text = "Welcome, $name!"
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

    ////////////////// flow helper //////////////////////

    private fun getUserToken(username: String) {
        val workTagUniqueId = UUID.randomUUID()
        val getUserTokenWork = OneTimeWorkRequest.Builder(ServerHolder.GetUserTokenWorker::class.java)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setInputData(Data.Builder().putString("username", username).build())
            .addTag(workTagUniqueId.toString())
            .build()
        WorkManager.getInstance(applicationContext).enqueue(getUserTokenWork)
        WorkManager.getInstance(applicationContext)
            .getWorkInfosByTagLiveData(workTagUniqueId.toString())
            .observe(this, androidx.lifecycle.Observer<List<WorkInfo>> { workInfo ->
                if (!workInfo.isNullOrEmpty() && workInfo[0].state == WorkInfo.State.SUCCEEDED) {
                    val tokenAsJson = workInfo[0].outputData.getString("token")
                    if (!tokenAsJson.isNullOrBlank()) {
                        Log.d("getUserToken_${username}", tokenAsJson)
                        this.saveToken(Gson().fromJson(tokenAsJson, Model.TokenResponse::class.java).data)
                        this.getUserInfo(token!!) // calling switchToUpdateInfoUI func
                    }
                }
            })
    }

    private fun getUserInfo(token: String) {
        loadingIcon.visibility = View.VISIBLE
        val workTagUniqueId = UUID.randomUUID()
        val getUserInfoWork = OneTimeWorkRequest.Builder(ServerHolder.GetUserInfoWorker::class.java)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setInputData(Data.Builder().putString("token", token).build())
            .addTag(workTagUniqueId.toString())
            .build()
        WorkManager.getInstance(applicationContext).enqueue(getUserInfoWork)
        WorkManager.getInstance(applicationContext)
            .getWorkInfosByTagLiveData(workTagUniqueId.toString())
            .observe(this, androidx.lifecycle.Observer<List<WorkInfo>> { workInfo ->
                if (!workInfo.isNullOrEmpty() && workInfo[0].state == WorkInfo.State.SUCCEEDED) {
                    val userInfoAsJson = workInfo[0].outputData.getString("userInfo")
                    if (!userInfoAsJson.isNullOrBlank()) {
                        Log.d("getUserInfo_${token}", userInfoAsJson)
                        val user = Gson().fromJson(userInfoAsJson, Model.UserResponse::class.java).data
                        switchToUpdateInfoUI((if (user.pretty_name.isBlank()) user.username else user.pretty_name), user.image_url)
                        loadingIcon.visibility = View.GONE
                    }
                }
            })
    }

    private fun updatePrettyName(prettyName: String) {
        loadingIcon.visibility = View.VISIBLE
        val workTagUniqueId = UUID.randomUUID()
        val updateUserInfoWork = OneTimeWorkRequest.Builder(ServerHolder.UpdateUserInfoWorker::class.java)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setInputData(Data.Builder().putString("token", token).putString("pretty_name", prettyName).build())
            .addTag(workTagUniqueId.toString())
            .build()
        WorkManager.getInstance(applicationContext).enqueue(updateUserInfoWork)
        WorkManager.getInstance(applicationContext)
            .getWorkInfosByTagLiveData(workTagUniqueId.toString())
            .observe(this, androidx.lifecycle.Observer<List<WorkInfo>> { workInfo ->
                if (!workInfo.isNullOrEmpty()) {
                    if (workInfo[0].state == WorkInfo.State.FAILED) Toast.makeText(applicationContext, "Update userInfo failed", Toast.LENGTH_LONG).show()
                    else if (workInfo[0].state == WorkInfo.State.SUCCEEDED) {
                        val userInfoAsJson = workInfo[0].outputData.getString("userInfo")
                        if (!userInfoAsJson.isNullOrBlank()) {
                            val user = Gson().fromJson(userInfoAsJson, Model.UserResponse::class.java).data
                            Log.d("updatePrettyName_${user.username}", userInfoAsJson)
                            this.updateUiWithUserInfo((if (prettyName.isBlank()) user.username else prettyName), user.image_url)
                            loadingIcon.visibility = View.GONE
                        }
                    }
                }
            })
    }


}
