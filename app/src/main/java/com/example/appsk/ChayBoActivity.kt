package com.example.appsk

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.appsk.bando.CauHinhBanDo
import com.example.appsk.data.DinhDang
import com.example.appsk.data.LoaiHoatDong
import com.example.appsk.data.TrangThaiChayBo
import com.example.appsk.databinding.ActivityChayBoBinding
import com.example.appsk.ui.lichsu.LichSuBuoiTapActivity
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

class ChayBoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChayBoBinding
    private lateinit var lopHienThiViTri: MyLocationNewOverlay
    private lateinit var duongVe: Polyline

    private var service: TheoDoiChayBoService? = null
    private var daKetNoiService = false
    private var daCanGiuaLanDau = false

    private val ketNoiService = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as TheoDoiChayBoService.LocalBinder
            service = localBinder.layService()
            daKetNoiService = true
            service?.trangThai?.observe(this@ChayBoActivity) { trangThai ->
                capNhatGiaoDien(trangThai)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            daKetNoiService = false
            service = null
        }
    }

    private val xinQuyenViTri = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { duocCap ->
        if (duocCap) {
            lopHienThiViTri.enableMyLocation()
            layThoiViTriHienTai()
        }
    }

    private val xinQuyenThongBao = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        CauHinhBanDo.khoiTao(this)

        super.onCreate(savedInstanceState)
        binding = ActivityChayBoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        thietLapBanDo()

        binding.btnBatDauDung.setOnClickListener {
            val dangChay = service?.trangThai?.value?.dangChay ?: false
            if (dangChay) dungTheoDoi() else batDauTheoDoi()
        }

        binding.btnLichSu.setOnClickListener {
            startActivity(Intent(this, LichSuBuoiTapActivity::class.java))
        }

        if (coQuyenViTri()) {
            lopHienThiViTri.enableMyLocation()
            layThoiViTriHienTai()
        } else {
            xinQuyenViTri.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            xinQuyenThongBao.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onStart() {
        super.onStart()
        Intent(this, TheoDoiChayBoService::class.java).also { intent ->
            bindService(intent, ketNoiService, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        if (daKetNoiService) {
            unbindService(ketNoiService)
            daKetNoiService = false
        }
    }

    private fun thietLapBanDo() {
        binding.mapView.setTileSource(TileSourceFactory.OpenTopo)
        binding.mapView.setMultiTouchControls(true)
        binding.mapView.setUseDataConnection(true)
        binding.mapView.minZoomLevel = 4.0
        binding.mapView.maxZoomLevel = 20.0

        binding.mapView.controller.setZoom(18.0)
        binding.mapView.controller.setCenter(GeoPoint(21.0278, 105.8342))

        duongVe = Polyline()
        duongVe.outlinePaint.color = Color.parseColor("#2E7D32")
        duongVe.outlinePaint.strokeWidth = 10f
        binding.mapView.overlays.add(duongVe)

        lopHienThiViTri = MyLocationNewOverlay(GpsMyLocationProvider(this), binding.mapView)
        lopHienThiViTri.enableFollowLocation()
        binding.mapView.overlays.add(lopHienThiViTri)
    }

    private fun coQuyenViTri(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun layThoiViTriHienTai() {
        lopHienThiViTri.runOnFirstFix {
            runOnUiThread {
                if (!daCanGiuaLanDau) {
                    daCanGiuaLanDau = true
                    val diem = lopHienThiViTri.myLocation
                    if (diem != null) {
                        binding.mapView.controller.setCenter(diem)
                        binding.mapView.controller.setZoom(19.0)
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------ Loại hoạt động

    private fun loaiDangChon(): String = when (binding.nhomLoaiHoatDong.checkedButtonId) {
        R.id.btn_loai_di_bo -> LoaiHoatDong.DI_BO
        R.id.btn_loai_dap_xe -> LoaiHoatDong.DAP_XE
        else -> LoaiHoatDong.CHAY_BO
    }

    private fun chonLoai(loai: String) {
        val idNut = when (loai) {
            LoaiHoatDong.DI_BO -> R.id.btn_loai_di_bo
            LoaiHoatDong.DAP_XE -> R.id.btn_loai_dap_xe
            else -> R.id.btn_loai_chay_bo
        }
        binding.nhomLoaiHoatDong.check(idNut)
    }

    /** Đang theo dõi thì không cho đổi loại hoạt động giữa chừng. */
    private fun khoaChonLoai(dangChay: Boolean) {
        listOf(binding.btnLoaiDiBo, binding.btnLoaiChayBo, binding.btnLoaiDapXe)
            .forEach { it.isEnabled = !dangChay }
    }

    // ------------------------------------------------------------ Bắt đầu / dừng

    private fun batDauTheoDoi() {
        if (!coQuyenViTri()) {
            xinQuyenViTri.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            return
        }

        val dichVu = service
        if (dichVu == null) {
            Toast.makeText(
                this, "Chưa kết nối được dịch vụ theo dõi, hãy thử lại sau giây lát",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val loai = loaiDangChon()

        // Xóa tuyến đường của buổi trước trên bản đồ
        duongVe.setPoints(ArrayList<GeoPoint>())
        binding.mapView.invalidate()

        val intent = Intent(this, TheoDoiChayBoService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        binding.mapView.postDelayed({ dichVu.batDau(loai) }, 300)

        binding.btnBatDauDung.text = getString(R.string.btn_dung_lai)
    }

    private fun dungTheoDoi() {
        val ketQua = service?.dungLai()
        binding.btnBatDauDung.text = getString(R.string.btn_bat_dau)

        if (ketQua != null) {
            val tocDoTrungBinh = if (ketQua.thoiGianGiay > 0) {
                ketQua.quangDuongKm / (ketQua.thoiGianGiay / 3600f)
            } else 0f

            val thongBaoKetQua = getString(
                R.string.ket_qua_chay_bo,
                String.format("%.2f", ketQua.quangDuongKm),
                DinhDang.thoiLuong(ketQua.thoiGianGiay),
                String.format("%.1f", tocDoTrungBinh)
            )
            Toast.makeText(this, thongBaoKetQua, Toast.LENGTH_LONG).show()
        }
    }

    private fun capNhatGiaoDien(trangThai: TrangThaiChayBo) {
        binding.tvGiaTriQuangDuong.text = String.format("%.2f km", trangThai.quangDuongKm)
        binding.tvGiaTriThoiGian.text = DinhDang.thoiLuong(trangThai.thoiGianGiay)
        binding.btnBatDauDung.text = if (trangThai.dangChay) {
            getString(R.string.btn_dung_lai)
        } else {
            getString(R.string.btn_bat_dau)
        }

        khoaChonLoai(trangThai.dangChay)
        if (trangThai.dangChay) chonLoai(trangThai.loaiHoatDong)

        if (trangThai.duongDi.isNotEmpty()) {
            val danhSachGeoPoint = trangThai.duongDi.map { GeoPoint(it.vido, it.kinhdo) }
            duongVe.setPoints(danhSachGeoPoint)
            binding.mapView.invalidate()
        }
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
        lopHienThiViTri.enableMyLocation()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
        lopHienThiViTri.disableMyLocation()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.mapView.onDetach()
    }
}