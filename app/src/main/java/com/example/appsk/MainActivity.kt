package com.example.appsk

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.appsk.databinding.ActivityMainBinding
import com.example.appsk.dongbo.CaiDatDongBo
import com.example.appsk.dongbo.DongBo
import com.example.appsk.nhacnho.QuanLyNhacNho
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        binding.bottomNav.setupWithNavController(navHostFragment.navController)

        // Đưa các việc nhắc nhở về đúng theo cài đặt của tài khoản đang đăng nhập
        QuanLyNhacNho(this).kichHoatTheoCaiDat()

        // Đồng bộ đám mây: bật lịch nếu đã bật, lần đầu thì hỏi người dùng
        DongBo.kichHoat(this)
        if (!CaiDatDongBo.daHoi(this)) hoiBatDongBo()
    }

    override fun onStop() {
        super.onStop()
        // Rời app thì gửi số liệu mới nhất (ví dụ số bước chân hôm nay)
        DongBo.yeuCau(this, 10)
    }

    private fun hoiBatDongBo() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dongbo_hoi_tieu_de)
            .setMessage(R.string.dongbo_hoi_noi_dung)
            .setCancelable(false)
            .setPositiveButton(R.string.dongbo_bat) { _, _ ->
                CaiDatDongBo.datBat(this, true)
                DongBo.kichHoat(this)
            }
            .setNegativeButton(R.string.dongbo_de_sau) { _, _ ->
                CaiDatDongBo.datBat(this, false)
            }
            .show()
    }
}