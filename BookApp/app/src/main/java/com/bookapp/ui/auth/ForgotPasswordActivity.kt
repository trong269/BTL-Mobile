package com.bookapp.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bookapp.R
import com.bookapp.data.api.ForgotPasswordRequest
import com.bookapp.data.api.MessageResponse
import com.bookapp.data.api.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ForgotPasswordActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        val edtEmail = findViewById<EditText>(R.id.edtEmail)
        val btnSendOtp = findViewById<Button>(R.id.btnSendOtp)

        btnSendOtp.setOnClickListener {
            val email = edtEmail.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnSendOtp.isEnabled = false
            btnSendOtp.text = "Đang gửi..."

            val request = ForgotPasswordRequest(email)
            RetrofitClient.instance.forgotPassword(request).enqueue(object : Callback<MessageResponse> {
                override fun onResponse(call: Call<MessageResponse>, response: Response<MessageResponse>) {
                    btnSendOtp.isEnabled = true
                    btnSendOtp.text = "Gửi mã xác nhận"
                    
                    if (response.isSuccessful) {
                        Toast.makeText(this@ForgotPasswordActivity, "Mã xác nhận đã được gửi đến email", Toast.LENGTH_LONG).show()
                        val intent = Intent(this@ForgotPasswordActivity, ResetPasswordActivity::class.java)
                        intent.putExtra("email", email)
                        startActivity(intent)
                    } else {
                        Toast.makeText(this@ForgotPasswordActivity, "Email không tồn tại trong hệ thống", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<MessageResponse>, t: Throwable) {
                    btnSendOtp.isEnabled = true
                    btnSendOtp.text = "Gửi mã xác nhận"
                    Toast.makeText(this@ForgotPasswordActivity, "Lỗi kết nối", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
}
