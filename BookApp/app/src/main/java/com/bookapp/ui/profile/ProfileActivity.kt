package com.bookapp.ui.profile

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bookapp.R
import com.bookapp.data.api.ChangePasswordRequest
import com.bookapp.data.api.RetrofitClient
import com.bookapp.data.api.UpdateProfileRequest
import com.bookapp.data.model.User
import com.bookapp.theme.ThemePreferenceManager
import com.bookapp.ui.auth.LoginActivity
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProfileActivity : AppCompatActivity() {

    private lateinit var edtFullName: EditText
    private lateinit var edtUsername: EditText
    private lateinit var edtEmail: EditText
    private lateinit var edtCurrentPassword: EditText
    private lateinit var edtNewPassword: EditText
    private lateinit var edtConfirmNewPassword: EditText
    private lateinit var btnSave: Button
    private lateinit var btnChangePassword: Button
    private lateinit var btnLogout: Button

    private lateinit var currentUserId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val userIdFromPrefs = prefs.getString(KEY_USER_ID, null)

        if (userIdFromPrefs.isNullOrBlank()) {
            navigateToLoginAndClearBackStack()
            return
        }
        currentUserId = userIdFromPrefs

        edtFullName = findViewById(R.id.edtProfileFullName)
        edtUsername = findViewById(R.id.edtProfileUsername)
        edtEmail = findViewById(R.id.edtProfileEmail)
        edtCurrentPassword = findViewById(R.id.edtCurrentPassword)
        edtNewPassword = findViewById(R.id.edtNewPassword)
        edtConfirmNewPassword = findViewById(R.id.edtConfirmNewPassword)
        btnSave = findViewById(R.id.btnSaveProfile)
        btnChangePassword = findViewById(R.id.btnChangePassword)
        btnLogout = findViewById(R.id.btnLogout)
        val btnBack = findViewById<ImageButton>(R.id.btnProfileBack)
        val switchThemeMode = findViewById<SwitchCompat>(R.id.switchThemeMode)

        edtFullName.setText(prefs.getString(KEY_FULL_NAME, ""))
        edtUsername.setText(prefs.getString(KEY_USERNAME, ""))
        edtEmail.setText(prefs.getString(KEY_EMAIL, ""))

        loadProfileFromServer()

        btnBack.setOnClickListener {
            finish()
        }

        val savedMode = ThemePreferenceManager.getSavedThemeMode(this)
        val isDarkThemeEnabled = when (savedMode) {
            ThemePreferenceManager.THEME_DARK -> true
            ThemePreferenceManager.THEME_LIGHT -> false
            else -> {
                val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                currentNightMode == Configuration.UI_MODE_NIGHT_YES
            }
        }
        switchThemeMode.isChecked = isDarkThemeEnabled
        switchThemeMode.setOnCheckedChangeListener { _, isChecked ->
            val nextMode = if (isChecked) {
                ThemePreferenceManager.THEME_DARK
            } else {
                ThemePreferenceManager.THEME_LIGHT
            }
            if (ThemePreferenceManager.getSavedThemeMode(this) != nextMode) {
                ThemePreferenceManager.saveThemeMode(this, nextMode)
            }
        }

        btnSave.setOnClickListener {
            updateProfile(prefs)
        }

        btnChangePassword.setOnClickListener {
            changePassword()
        }

        btnLogout.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc muốn đăng xuất khỏi tài khoản?")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Đăng xuất") { _, _ ->
                    prefs.edit().clear().apply()
                    navigateToLoginAndClearBackStack()
                }
                .show()
        }
    }

    private fun loadProfileFromServer() {
        RetrofitClient.instance.getUserProfile(currentUserId).enqueue(object : Callback<User> {
            override fun onResponse(call: Call<User>, response: Response<User>) {
                val body = response.body()
                if (!response.isSuccessful || body == null) {
                    return
                }

                bindUserToViews(body)
                saveUserToPrefs(body)
            }

            override fun onFailure(call: Call<User>, t: Throwable) {
                // Keep cached local data when network is unavailable.
            }
        })
    }

    private fun updateProfile(prefs: android.content.SharedPreferences) {
        val fullName = edtFullName.text.toString().trim()
        val username = edtUsername.text.toString().trim()
        val email = edtEmail.text.toString().trim()

        if (username.isEmpty()) {
            edtUsername.error = "Vui lòng nhập tên đăng nhập"
            edtUsername.requestFocus()
            return
        }

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            edtEmail.error = "Email không hợp lệ"
            edtEmail.requestFocus()
            return
        }

        btnSave.isEnabled = false
        val request = UpdateProfileRequest(
            username = username,
            email = email,
            fullName = fullName
        )

        RetrofitClient.instance.updateUserProfile(currentUserId, request)
            .enqueue(object : Callback<User> {
                override fun onResponse(call: Call<User>, response: Response<User>) {
                    btnSave.isEnabled = true
                    val updatedUser = response.body()

                    if (response.isSuccessful && updatedUser != null) {
                        bindUserToViews(updatedUser)
                        prefs.edit().apply {
                            putString(KEY_USERNAME, updatedUser.username)
                            putString(KEY_EMAIL, updatedUser.email)
                            putString(KEY_FULL_NAME, updatedUser.fullName ?: "")
                            apply()
                        }
                        Toast.makeText(
                            this@ProfileActivity,
                            "Đã cập nhật thông tin tài khoản",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            this@ProfileActivity,
                            getErrorMessage(response, "Cập nhật thông tin thất bại"),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<User>, t: Throwable) {
                    btnSave.isEnabled = true
                    Toast.makeText(
                        this@ProfileActivity,
                        "Không thể kết nối máy chủ",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun changePassword() {
        val currentPassword = edtCurrentPassword.text.toString()
        val newPassword = edtNewPassword.text.toString()
        val confirmNewPassword = edtConfirmNewPassword.text.toString()

        if (currentPassword.isBlank()) {
            edtCurrentPassword.error = "Vui lòng nhập mật khẩu hiện tại"
            edtCurrentPassword.requestFocus()
            return
        }

        if (newPassword.length < 6) {
            edtNewPassword.error = "Mật khẩu mới tối thiểu 6 ký tự"
            edtNewPassword.requestFocus()
            return
        }

        if (newPassword != confirmNewPassword) {
            edtConfirmNewPassword.error = "Mật khẩu xác nhận không khớp"
            edtConfirmNewPassword.requestFocus()
            return
        }

        btnChangePassword.isEnabled = false
        val request = ChangePasswordRequest(
            currentPassword = currentPassword,
            newPassword = newPassword
        )

        RetrofitClient.instance.changePassword(currentUserId, request)
            .enqueue(object : Callback<com.bookapp.data.api.MessageResponse> {
                override fun onResponse(
                    call: Call<com.bookapp.data.api.MessageResponse>,
                    response: Response<com.bookapp.data.api.MessageResponse>
                ) {
                    btnChangePassword.isEnabled = true
                    if (response.isSuccessful) {
                        edtCurrentPassword.text?.clear()
                        edtNewPassword.text?.clear()
                        edtConfirmNewPassword.text?.clear()
                        Toast.makeText(
                            this@ProfileActivity,
                            "Đổi mật khẩu thành công",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            this@ProfileActivity,
                            getErrorMessage(response, "Đổi mật khẩu thất bại"),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<com.bookapp.data.api.MessageResponse>, t: Throwable) {
                    btnChangePassword.isEnabled = true
                    Toast.makeText(
                        this@ProfileActivity,
                        "Không thể kết nối máy chủ",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun bindUserToViews(user: User) {
        edtFullName.setText(user.fullName ?: "")
        edtUsername.setText(user.username)
        edtEmail.setText(user.email)
    }

    private fun saveUserToPrefs(user: User) {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        prefs.edit().apply {
            putString(KEY_USERNAME, user.username)
            putString(KEY_EMAIL, user.email)
            putString(KEY_FULL_NAME, user.fullName ?: "")
            apply()
        }
    }

    private fun getErrorMessage(response: Response<*>, fallback: String): String {
        if (response.code() == 404) {
            return "Server chưa hỗ trợ API tài khoản. Vui lòng cập nhật và khởi động lại backend"
        }

        if (response.code() == 409) {
            return "Email hoặc tên đăng nhập đã tồn tại"
        }

        val body = response.errorBody()?.string()?.trim()
        if (body.isNullOrEmpty()) {
            return "$fallback (HTTP ${response.code()})"
        }

        return try {
            val json = JSONObject(body)
            when {
                json.has("message") -> json.getString("message")
                json.has("error") -> json.getString("error")
                else -> "$fallback (HTTP ${response.code()})"
            }
        } catch (_: Exception) {
            "$fallback (HTTP ${response.code()})"
        }
    }

    private fun navigateToLoginAndClearBackStack() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    companion object {
        private const val PREFS_NAME = "BookAppPrefs"
        private const val KEY_USER_ID = "userId"
        private const val KEY_USERNAME = "username"
        private const val KEY_EMAIL = "email"
        private const val KEY_FULL_NAME = "fullName"
    }
}
