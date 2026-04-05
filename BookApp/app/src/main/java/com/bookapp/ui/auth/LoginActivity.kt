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

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val username = findViewById<EditText>(R.id.edtUsername)
        val password = findViewById<EditText>(R.id.edtPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvGoToRegister = findViewById<TextView>(R.id.tvGoToRegister)

        tvGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        btnLogin.setOnClickListener {
            val usernameValue = username.text.toString().trim()
            val passwordValue = password.text.toString()

            if (usernameValue.isEmpty() || passwordValue.isEmpty()) {
                Toast.makeText(this, "Vui long nhap day du tai khoan va mat khau", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val request = LoginRequest(usernameValue, passwordValue)
            btnLogin.isEnabled = false
            btnLogin.text = "Dang xu ly..."

            RetrofitClient.instance.login(request)
                .enqueue(object : Callback<LoginResponse> {
                    override fun onResponse(
                        call: Call<LoginResponse>,
                        response: Response<LoginResponse>
                    ) {
                        btnLogin.isEnabled = true
                        btnLogin.text = "Dang nhap"

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
                            }

                            if (role == "ADMIN") {
                                startActivity(Intent(this@LoginActivity, AdminActivity::class.java))
                            } else {
                                startActivity(Intent(this@LoginActivity, HomeActivity::class.java))
                            }
                            finish()
                        } else {
                            val errorText = response.errorBody()?.string()?.take(120)
                            val message = if (errorText.isNullOrBlank()) {
                                "Login failed (HTTP ${response.code()})"
                            } else {
                                "Login failed (HTTP ${response.code()}): $errorText"
                            }
                            Toast.makeText(this@LoginActivity, message, Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                        btnLogin.isEnabled = true
                        btnLogin.text = "Dang nhap"
                        Toast.makeText(this@LoginActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }
}