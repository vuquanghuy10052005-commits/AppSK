package com.example.appsk

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.appsk.databinding.ActivityDangNhapBinding
import com.google.firebase.auth.FirebaseAuth

class DangNhapActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDangNhapBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        // Phải gọi trước super.onCreate để hiện màn hình khởi động
        installSplashScreen()
        super.onCreate(savedInstanceState)
        binding = ActivityDangNhapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        binding.btnDangNhap.setOnClickListener {
            dangNhap()
        }

        binding.tvChuyenDangKy.setOnClickListener {
            startActivity(Intent(this, DangKyActivity::class.java))
        }
    }

    override fun onStart() {
        super.onStart()
        if (auth.currentUser != null) {
            moToMain()
        }
    }

    private fun dangNhap() {
        val email = binding.etEmail.text.toString().trim()
        val matKhau = binding.etMatKhau.text.toString().trim()

        if (email.isEmpty() || matKhau.isEmpty()) {
            binding.tilEmail.error = getString(R.string.loi_thieu_thong_tin)
            return
        }
        binding.tilEmail.error = null

        hienThiDangTai(true)
        auth.signInWithEmailAndPassword(email, matKhau)
            .addOnCompleteListener { task ->
                hienThiDangTai(false)
                if (task.isSuccessful) {
                    moToMain()
                } else {
                    binding.tilMatKhau.error = task.exception?.localizedMessage
                }
            }
    }

    private fun hienThiDangTai(dangTai: Boolean) {
        binding.progressBar.visibility = if (dangTai) View.VISIBLE else View.GONE
        binding.btnDangNhap.isEnabled = !dangTai
    }

    private fun moToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}