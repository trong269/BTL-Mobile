package com.bookapp.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bookapp.R
import com.bookapp.data.api.MessageResponse
import com.bookapp.data.api.ResetPasswordRequest
import com.bookapp.data.api.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ResetPasswordActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        val edtEmail = findViewById<EditText>(R.id.edtEmail)
        val edtOtp = findViewById<EditText>(R.id.edtOtp)
        val edtNewPassword = findViewById<EditText>(R.id.edtNewPassword)
        val btnResetPassword = findViewById<Button>(R.id.btnResetPassword)

        val email = intent.getStringExtra("email") ?: ""
        edtEmail.setText(email)

        btnResetPassword.setOnClickListener {
            val otp = edtOtp.text.toString().trim()
            val newPassword = edtNewPassword.text.toString()

            if (otp.isEmpty() || newPassword.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnResetPassword.isEnabled = false
            btnResetPassword.text = "Đang xử lý..."

            val request = ResetPasswordRequest(email, otp, newPassword)
            RetrofitClient.instance.resetPassword(request).enqueue(object : Callback<MessageResponse> {
                override fun onResponse(call: Call<MessageResponse>, response: Response<MessageResponse>) {
                    btnResetPassword.isEnabled = true
                    btnResetPassword.text = "Đổi mật khẩu"

                    if (response.isSuccessful) {
                        Toast.makeText(this@ResetPasswordActivity, "Đổi mật khẩu thành công. Vui lòng đăng nhập lại.", Toast.LENGTH_LONG).show()
                        val intent = Intent(this@ResetPasswordActivity, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                    } else {
                        Toast.makeText(this@ResetPasswordActivity, "Mã xác nhận không đúng hoặc đã hết hạn", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<MessageResponse>, t: Throwable) {
                    btnResetPassword.isEnabled = true
                    btnResetPassword.text = "Đổi mật khẩu"
                    Toast.makeText(this@ResetPasswordActivity, "Lỗi kết nối", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
}
