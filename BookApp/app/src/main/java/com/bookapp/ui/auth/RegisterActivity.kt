package com.bookapp.ui.auth

import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bookapp.R
import com.bookapp.data.api.RegisterRequest
import com.bookapp.data.api.RetrofitClient
import com.bookapp.data.model.User
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val edtUsername = findViewById<EditText>(R.id.edtRegisterUsername)
        val edtEmail = findViewById<EditText>(R.id.edtRegisterEmail)
        val edtPassword = findViewById<EditText>(R.id.edtRegisterPassword)
        val edtConfirmPassword = findViewById<EditText>(R.id.edtRegisterConfirmPassword)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val tvGoToLogin = findViewById<TextView>(R.id.tvGoToLogin)

        tvGoToLogin.setOnClickListener { finish() }

        btnRegister.setOnClickListener {
            val username = edtUsername.text.toString().trim()
            val email = edtEmail.text.toString().trim()
            val password = edtPassword.text.toString()
            val confirmPassword = edtConfirmPassword.text.toString()

            if (username.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Email không hợp lệ", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "Mật khẩu tối thiểu 6 ký tự", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Xác nhận mật khẩu không khớp", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnRegister.isEnabled = false
            btnRegister.text = "Đang tạo tài khoản..."

            val request = RegisterRequest(
                username = username,
                email = email,
                password = password
            )

            RetrofitClient.instance.register(request).enqueue(object : Callback<User> {
                override fun onResponse(call: Call<User>, response: Response<User>) {
                    btnRegister.isEnabled = true
                    btnRegister.text = "Đăng ký"

                    if (response.isSuccessful) {
                        Toast.makeText(
                            this@RegisterActivity,
                            "Đăng ký thành công. Mời bạn đăng nhập.",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    } else {
                        val errorText = runCatching { response.errorBody()?.string() }.getOrNull()
                        val message = mapRegisterError(response.code(), errorText)
                        Toast.makeText(this@RegisterActivity, message, Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<User>, t: Throwable) {
                    btnRegister.isEnabled = true
                    btnRegister.text = "Đăng ký"
                    val message = if (t is IOException) {
                        "Không thể kết nối máy chủ. Vui lòng kiểm tra mạng và thử lại."
                    } else {
                        "Đăng ký thất bại. Vui lòng thử lại sau."
                    }
                    Toast.makeText(this@RegisterActivity, message, Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun mapRegisterError(code: Int, rawError: String?): String {
        val normalized = rawError.orEmpty().lowercase()

        if (normalized.contains("username") || normalized.contains("tên đăng nhập") || normalized.contains("tai khoan") || normalized.contains("tài khoản")) {
            return "Tên tài khoản đã tồn tại. Vui lòng chọn tên khác."
        }
        if (normalized.contains("email") || normalized.contains("mail")) {
            return "Email đã được sử dụng. Vui lòng dùng email khác."
        }

        return when (code) {
            400 -> "Thông tin đăng ký chưa hợp lệ. Vui lòng kiểm tra lại."
            409 -> "Tài khoản hoặc email đã tồn tại."
            422 -> "Dữ liệu đăng ký không đúng định dạng."
            429 -> "Bạn thao tác quá nhanh. Vui lòng thử lại sau ít phút."
            in 500..599 -> "Máy chủ đang bận. Vui lòng thử lại sau."
            else -> "Đăng ký thất bại (HTTP $code)."
        }
    }
}