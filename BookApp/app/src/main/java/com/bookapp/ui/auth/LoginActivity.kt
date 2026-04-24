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
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.bookapp.data.api.GoogleLoginRequest

class LoginActivity : AppCompatActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Log.w("GoogleSignIn", "Google sign in failed", e)
                Toast.makeText(this, "Đăng nhập Google thất bại", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Kiểm tra phiên đăng nhập hiện tại
        val prefs = getSharedPreferences("BookAppPrefs", MODE_PRIVATE)
        val savedUserId = prefs.getString("userId", null)
        if (savedUserId != null) {
            // Đã đăng nhập, vào thẳng trang chủ (Hỗ trợ offline)
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_login)

        val username = findViewById<EditText>(R.id.edtUsername)
        val password = findViewById<EditText>(R.id.edtPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvGoToRegister = findViewById<TextView>(R.id.tvGoToRegister)
        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)

        val btnGoogleLogin = findViewById<Button>(R.id.btnGoogleLogin)

        auth = FirebaseAuth.getInstance()
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // Requires web client ID in strings.xml or google-services.json
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        tvGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        btnGoogleLogin.setOnClickListener {
            // Đăng xuất khỏi phiên Google cũ trước để ép hiển thị bảng chọn tài khoản
            googleSignInClient.signOut().addOnCompleteListener {
                val signInIntent = googleSignInClient.signInIntent
                googleSignInLauncher.launch(signInIntent)
            }
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
                            val token = body?.token
                            val role = body?.role
                            val user = body?.user

                            // Lưu thông tin user vào SharedPreferences
                            if (user != null && token != null) {
                                val prefs = getSharedPreferences("BookAppPrefs", MODE_PRIVATE)
                                prefs.edit().apply {
                                    putString("token", token)
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

                            startActivity(Intent(this@LoginActivity, HomeActivity::class.java))
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

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.getIdToken(true)?.addOnCompleteListener { tokenTask ->
                        if (tokenTask.isSuccessful) {
                            val firebaseToken = tokenTask.result?.token
                            if (firebaseToken != null) {
                                sendGoogleTokenToBackend(firebaseToken)
                            }
                        } else {
                            Toast.makeText(this, "Lỗi lấy token Firebase", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(this, "Xác thực Firebase thất bại", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun sendGoogleTokenToBackend(firebaseToken: String) {
        val request = GoogleLoginRequest(firebaseToken)
        RetrofitClient.instance.googleLogin(request)
            .enqueue(object : Callback<LoginResponse> {
                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                    if (response.isSuccessful) {
                        val body = response.body()
                        val token = body?.token
                        val user = body?.user

                        if (user != null && token != null) {
                            val prefs = getSharedPreferences("BookAppPrefs", MODE_PRIVATE)
                            prefs.edit().apply {
                                putString("token", token)
                                putString("userId", user.id)
                                putString("username", user.username)
                                putString("email", user.email)
                                putString("fullName", user.fullName ?: "")
                                putString("role", user.role)
                                apply()
                            }
                        }

                        startActivity(Intent(this@LoginActivity, HomeActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this@LoginActivity, "Đăng nhập qua Backend thất bại", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    Toast.makeText(this@LoginActivity, "Lỗi kết nối máy chủ", Toast.LENGTH_SHORT).show()
                }
            })
    }
}