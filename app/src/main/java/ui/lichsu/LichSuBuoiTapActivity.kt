package com.example.appsk.ui.lichsu

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appsk.data.csdl.AppDatabase
import com.example.appsk.databinding.ActivityLichSuBuoiTapBinding
import kotlinx.coroutines.launch

class LichSuBuoiTapActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLichSuBuoiTapBinding
    private lateinit var adapter: BuoiTapAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLichSuBuoiTapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        adapter = BuoiTapAdapter { phienId ->
            val intent = Intent(this, ChiTietBuoiTapActivity::class.java)
            intent.putExtra(ChiTietBuoiTapActivity.EXTRA_PHIEN_ID, phienId)
            startActivity(intent)
        }
        binding.rvBuoiTap.layoutManager = LinearLayoutManager(this)
        binding.rvBuoiTap.adapter = adapter
    }

    // Nạp lại mỗi lần quay về màn này, để buổi vừa xóa hoặc vừa lưu được cập nhật
    override fun onResume() {
        super.onResume()
        napDanhSach()
    }

    private fun napDanhSach() {
        lifecycleScope.launch {
            val db = AppDatabase.layInstance(applicationContext)
            val danhSach = db.lichSuChayBoDao().layTatCa()
            val loaiTheoPhien = db.chiTietBuoiTapDao().layTatCa()
                .associate { it.phienId to it.loaiHoatDong }

            adapter.capNhat(danhSach.map { BuoiTapHienThi(it, loaiTheoPhien[it.id]) })
            binding.tvTrong.visibility = if (danhSach.isEmpty()) View.VISIBLE else View.GONE
        }
    }
}