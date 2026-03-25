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
                Toast.makeText(this, "Vui long nhap day du thong tin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Email khong hop le", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "Mat khau toi thieu 6 ky tu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Xac nhan mat khau khong khop", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnRegister.isEnabled = false
            btnRegister.text = "Dang tao tai khoan..."

            val request = RegisterRequest(
                username = username,
                email = email,
                password = password
            )

            RetrofitClient.instance.register(request).enqueue(object : Callback<User> {
                override fun onResponse(call: Call<User>, response: Response<User>) {
                    btnRegister.isEnabled = true
                    btnRegister.text = "Dang ky"

                    if (response.isSuccessful) {
                        Toast.makeText(
                            this@RegisterActivity,
                            "Dang ky thanh cong. Moi ban dang nhap.",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    } else {
                        val errorText = response.errorBody()?.string()?.take(120)
                        val message = if (errorText.isNullOrBlank()) {
                            "Dang ky that bai (HTTP ${response.code()})"
                        } else {
                            "Dang ky that bai (HTTP ${response.code()}): $errorText"
                        }
                        Toast.makeText(this@RegisterActivity, message, Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<User>, t: Throwable) {
                    btnRegister.isEnabled = true
                    btnRegister.text = "Dang ky"
                    Toast.makeText(this@RegisterActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
}