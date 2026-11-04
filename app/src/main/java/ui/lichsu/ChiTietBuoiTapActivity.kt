package com.example.appsk.ui.lichsu

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.withTransaction
import com.example.appsk.R
import com.example.appsk.bando.CauHinhBanDo
import com.example.appsk.data.DinhDang
import com.example.appsk.data.LoaiHoatDong
import com.example.appsk.data.csdl.AppDatabase
import com.example.appsk.data.csdl.DiemTuyenDuong
import com.example.appsk.data.csdl.LichSuChayBo
import com.example.appsk.databinding.ActivityChiTietBuoiTapBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChiTietBuoiTapActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PHIEN_ID = "phien_id"
    }

    private lateinit var binding: ActivityChiTietBuoiTapBinding
    private lateinit var db: AppDatabase
    private val dinhDangNgayGio = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        CauHinhBanDo.khoiTao(this)

        super.onCreate(savedInstanceState)
        binding = ActivityChiTietBuoiTapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val phienId = intent.getLongExtra(EXTRA_PHIEN_ID, -1L)
        if (phienId < 0) {
            finish()
            return
        }

        db = AppDatabase.layInstance(applicationContext)

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.btnXoa.setOnClickListener { hoiXacNhanXoa(phienId) }

        thietLapBanDo()
        napDuLieu(phienId)
    }

    private fun thietLapBanDo() {
        binding.mapView.setTileSource(TileSourceFactory.OpenTopo)
        binding.mapView.setMultiTouchControls(true)
        binding.mapView.setUseDataConnection(true)
        binding.mapView.minZoomLevel = 4.0
        binding.mapView.maxZoomLevel = 20.0
        binding.mapView.controller.setZoom(15.0)
        binding.mapView.controller.setCenter(GeoPoint(21.0278, 105.8342))
    }

    private fun napDuLieu(phienId: Long) {
        lifecycleScope.launch {
            val phien = db.lichSuChayBoDao().layTheoId(phienId)
            if (phien == null) {
                Toast.makeText(
                    this@ChiTietBuoiTapActivity,
                    getString(R.string.bt_khong_tim_thay),
                    Toast.LENGTH_SHORT
                ).show()
                finish()
                return@launch
            }

            val loai = db.chiTietBuoiTapDao().layLoai(phienId)
            val diem = db.diemTuyenDuongDao().layTheoPhien(phienId)

            hienThiThongTin(phien, loai)
            hienThiTuyenDuong(diem)
        }
    }

    private fun hienThiThongTin(phien: LichSuChayBo, loai: String?) {
        binding.tvTenLoai.text = "${LoaiHoatDong.bieuTuong(loai)} ${LoaiHoatDong.ten(loai)}"
        binding.tvThoiDiem.text = dinhDangNgayGio.format(Date(phien.thoiGianGhiNhan))
        binding.tvQuangDuong.text = String.format(Locale.getDefault(), "%.2f km", phien.quangDuongKm)
        binding.tvThoiGian.text = DinhDang.thoiLuong(phien.thoiGianGiay)
        binding.tvTocDo.text = String.format(Locale.getDefault(), "%.1f km/h", phien.tocDoTrungBinh)

        // Nhịp độ (phút/km) có ý nghĩa với đi bộ và chạy bộ, không dùng cho đạp xe
        val hienNhipDo = loai != LoaiHoatDong.DAP_XE && phien.quangDuongKm > 0f
        if (hienNhipDo) {
            val phutMoiKm = phien.thoiGianGiay / 60.0 / phien.quangDuongKm
            binding.tvNhipDo.text = getString(R.string.bt_nhip_do) + ": " + DinhDang.nhipDo(phutMoiKm)
            binding.tvNhipDo.visibility = View.VISIBLE
        } else {
            binding.tvNhipDo.visibility = View.GONE
        }
    }

    private fun hienThiTuyenDuong(danhSachDiem: List<DiemTuyenDuong>) {
        if (danhSachDiem.size < 2) {
            binding.tvKhongCoTuyen.visibility = View.VISIBLE
            return
        }
        binding.tvKhongCoTuyen.visibility = View.GONE

        val geoPoints = danhSachDiem.map { GeoPoint(it.vido, it.kinhdo) }

        val duong = Polyline()
        duong.outlinePaint.color = Color.parseColor("#2E7D32")
        duong.outlinePaint.strokeWidth = 10f
        duong.setPoints(geoPoints)
        binding.mapView.overlays.add(duong)

        val diemBatDau = Marker(binding.mapView).apply {
            position = geoPoints.first()
            title = getString(R.string.bt_diem_bat_dau)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
        val diemKetThuc = Marker(binding.mapView).apply {
            position = geoPoints.last()
            title = getString(R.string.bt_diem_ket_thuc)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
        binding.mapView.overlays.add(diemBatDau)
        binding.mapView.overlays.add(diemKetThuc)

        // Phóng bản đồ vừa khít tuyến đường (phải chờ MapView có kích thước)
        val khung = BoundingBox.fromGeoPoints(geoPoints)
        binding.mapView.post {
            if (khung.latitudeSpan < 0.0002 && khung.longitudeSpan < 0.0002) {
                binding.mapView.controller.setCenter(geoPoints.first())
                binding.mapView.controller.setZoom(18.0)
            } else {
                binding.mapView.zoomToBoundingBox(khung, false, 100)
            }
            binding.mapView.invalidate()
        }
    }

    private fun hoiXacNhanXoa(phienId: Long) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.bt_xoa_tieu_de)
            .setMessage(R.string.bt_xoa_noi_dung)
            .setPositiveButton(R.string.bt_xoa_dong_y) { _, _ -> xoaBuoiTap(phienId) }
            .setNegativeButton(R.string.bt_huy, null)
            .show()
    }

    private fun xoaBuoiTap(phienId: Long) {
        lifecycleScope.launch {
            db.withTransaction {
                db.diemTuyenDuongDao().xoaTheoPhien(phienId)
                db.chiTietBuoiTapDao().xoa(phienId)
                db.lichSuChayBoDao().xoaTheoId(phienId)
            }
            Toast.makeText(
                this@ChiTietBuoiTapActivity,
                getString(R.string.bt_da_xoa),
                Toast.LENGTH_SHORT
            ).show()
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.mapView.onDetach()
    }
}