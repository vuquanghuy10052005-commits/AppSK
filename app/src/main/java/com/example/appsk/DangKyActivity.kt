package com.example.appsk

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.appsk.databinding.ActivityDangKyBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest

class DangKyActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDangKyBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDangKyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        binding.btnDangKy.setOnClickListener {
            dangKy()
        }

        binding.tvChuyenDangNhap.setOnClickListener {
            finish()
        }
    }

    private fun dangKy() {
        val hoTen = binding.etHoTen.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val matKhau = binding.etMatKhau.text.toString().trim()

        if (hoTen.isEmpty() || email.isEmpty() || matKhau.isEmpty()) {
            binding.tilHoTen.error = getString(R.string.loi_thieu_thong_tin)
            return
        }
        binding.tilHoTen.error = null

        if (matKhau.length < 6) {
            binding.tilMatKhau.error = getString(R.string.loi_mat_khau_ngan)
            return
        }
        binding.tilMatKhau.error = null

        hienThiDangTai(true)
        auth.createUserWithEmailAndPassword(email, matKhau)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    capNhatTenHienThi(hoTen)
                } else {
                    hienThiDangTai(false)
                    binding.tilEmail.error = task.exception?.localizedMessage
                }
            }
    }

    private fun capNhatTenHienThi(hoTen: String) {
        val request = UserProfileChangeRequest.Builder()
            .setDisplayName(hoTen)
            .build()

        auth.currentUser?.updateProfile(request)
            ?.addOnCompleteListener {
                hienThiDangTai(false)
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
    }

    private fun hienThiDangTai(dangTai: Boolean) {
        binding.progressBar.visibility = if (dangTai) View.VISIBLE else View.GONE
        binding.btnDangKy.isEnabled = !dangTai
    }
}