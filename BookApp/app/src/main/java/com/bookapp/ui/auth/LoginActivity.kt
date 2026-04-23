package com.bookapp.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bookapp.R
import com.bookapp.data.api.LoginRequest
import com.bookapp.data.api.LoginResponse
import com.bookapp.data.api.RetrofitClient
import com.bookapp.ui.admin.AdminActivity
import com.bookapp.ui.home.HomeActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val username = findViewById<EditText>(R.id.edtUsername)
        val password = findViewById<EditText>(R.id.edtPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvGoToRegister = findViewById<TextView>(R.id.tvGoToRegister)
        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)

        tvGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        btnLogin.setOnClickListener {
            val usernameValue = username.text.toString().trim()
            val passwordValue = password.text.toString()

            if (usernameValue.isEmpty() || passwordValue.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ tài khoản và mật khẩu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val request = LoginRequest(usernameValue, passwordValue)
            btnLogin.isEnabled = false
            btnLogin.text = "Đang xử lý..."

            RetrofitClient.instance.login(request)
                .enqueue(object : Callback<LoginResponse> {
                    override fun onResponse(
                        call: Call<LoginResponse>,
                        response: Response<LoginResponse>
                    ) {
                        btnLogin.isEnabled = true
                        btnLogin.text = "Đăng nhập"

                        if (response.isSuccessful) {
                            val body = response.body()
                            val role = body?.role
                            val user = body?.user

                            // Lưu thông tin user vào SharedPreferences
                            if (user != null) {
                                val prefs = getSharedPreferences("BookAppPrefs", MODE_PRIVATE)
                                prefs.edit().apply {
                                    putString("userId", user.id)
                                    putString("username", user.username)
                                    putString("email", user.email)
                                    putString("fullName", user.fullName ?: "")
                                    putString("role", user.role)
                                    apply()
                                }
                                
                                // Fetch FCM Token
                                com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        val token = task.result
                                        RetrofitClient.instance.updateFcmToken(user.id, com.bookapp.data.api.UpdateFcmTokenRequest(token))
                                            .enqueue(object : Callback<com.bookapp.data.api.MessageResponse> {
                                                override fun onResponse(call: Call<com.bookapp.data.api.MessageResponse>, response: Response<com.bookapp.data.api.MessageResponse>) {}
                                                override fun onFailure(call: Call<com.bookapp.data.api.MessageResponse>, t: Throwable) {}
                                            })
                                    }
                                }
                            }

                            if (role == "ADMIN") {
                                startActivity(Intent(this@LoginActivity, AdminActivity::class.java))
                            } else {
                                startActivity(Intent(this@LoginActivity, HomeActivity::class.java))
                            }
                            finish()
                        } else {
                            val errorText = runCatching { response.errorBody()?.string() }.getOrNull()
                            val message = mapLoginError(response.code(), errorText)
                            Toast.makeText(this@LoginActivity, message, Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                        btnLogin.isEnabled = true
                        btnLogin.text = "Đăng nhập"
                        val message = if (t is IOException) {
                            "Không thể kết nối máy chủ. Vui lòng kiểm tra mạng và thử lại."
                        } else {
                            "Đăng nhập thất bại. Vui lòng thử lại sau."
                        }
                        Toast.makeText(this@LoginActivity, message, Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }

    private fun mapLoginError(code: Int, rawError: String?): String {
        val normalized = rawError.orEmpty().lowercase()
        if (code == 401 || code == 403 || normalized.contains("invalid") || normalized.contains("sai") || normalized.contains("password") || normalized.contains("credential")) {
            return "Sai tài khoản hoặc mật khẩu. Vui lòng kiểm tra lại."
        }

        return when (code) {
            400 -> "Thông tin đăng nhập không hợp lệ."
            404 -> "Không tìm thấy tài khoản."
            429 -> "Bạn thao tác quá nhanh. Vui lòng thử lại sau ít phút."
            in 500..599 -> "Máy chủ đang bận. Vui lòng thử lại sau."
            else -> "Đăng nhập thất bại (HTTP $code)."
        }
    }
}