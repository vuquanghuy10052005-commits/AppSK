package com.example.appsk.ui.thongke

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.appsk.data.QuanLyBuocChan
import com.example.appsk.data.csdl.AppDatabase
import com.example.appsk.databinding.FragmentThongKeBinding
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

class ThongKeFragment : Fragment() {

    private var _binding: FragmentThongKeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentThongKeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                hienThiTab(tab?.position ?: 0)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun hienThiTab(viTri: Int) {
        binding.khungCanNang.visibility = if (viTri == 0) View.VISIBLE else View.GONE
        binding.khungVanDong.visibility = if (viTri == 1) View.VISIBLE else View.GONE
        binding.khungBuocChan.visibility = if (viTri == 2) View.VISIBLE else View.GONE
    }

    override fun onResume() {
        super.onResume()
        taiBieuDoCanNang()
        taiBieuDoVanDong()
        taiBieuDoBuocChan()
    }

    private fun thoiGian30NgayTruoc(): Long {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -30)
        return cal.timeInMillis
    }

    private fun thoiGian7NgayTruoc(): Long {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -7)
        return cal.timeInMillis
    }

    private fun taiBieuDoCanNang() {
        val appContext = requireContext().applicationContext
        lifecycleScope.launch {
            val db = AppDatabase.layInstance(appContext)
            val danhSach = db.lichSuCanNangDao().layTheoKhoangThoiGian(thoiGian30NgayTruoc())

            if (_binding == null) return@launch

            if (danhSach.isEmpty()) {
                binding.bieuDoCanNang.visibility = View.GONE
                binding.tvKhongCoDuLieuCanNang.visibility = View.VISIBLE
                return@launch
            }

            binding.bieuDoCanNang.visibility = View.VISIBLE
            binding.tvKhongCoDuLieuCanNang.visibility = View.GONE

            val dinhDangNgay = SimpleDateFormat("dd/MM", Locale.getDefault())
            val danhSachDiem = danhSach.mapIndexed { chiSo, banGhi ->
                Entry(chiSo.toFloat(), banGhi.canNangKg)
            }
            val nhanTrucX = danhSach.map { dinhDangNgay.format(it.thoiGianGhiNhan) }

            val boDuLieu = LineDataSet(danhSachDiem, "Cân nặng (kg)")
            boDuLieu.color = Color.parseColor("#2E7D32")
            boDuLieu.setCircleColor(Color.parseColor("#2E7D32"))
            boDuLieu.lineWidth = 2f
            boDuLieu.circleRadius = 4f
            boDuLieu.valueTextSize = 10f

            binding.bieuDoCanNang.data = LineData(boDuLieu)
            binding.bieuDoCanNang.xAxis.valueFormatter = IndexAxisValueFormatter(nhanTrucX)
            binding.bieuDoCanNang.xAxis.position = XAxis.XAxisPosition.BOTTOM
            binding.bieuDoCanNang.xAxis.granularity = 1f
            binding.bieuDoCanNang.description.isEnabled = false
            binding.bieuDoCanNang.animateX(500)
            binding.bieuDoCanNang.invalidate()
        }
    }

    private fun taiBieuDoVanDong() {
        val appContext = requireContext().applicationContext
        lifecycleScope.launch {
            val db = AppDatabase.layInstance(appContext)
            val tuThoiGian = thoiGian7NgayTruoc()
            val danhSach = db.lichSuChayBoDao().layTheoKhoangThoiGian(tuThoiGian)
            val tongQuangDuong = db.lichSuChayBoDao().tinhTongQuangDuongTu(tuThoiGian) ?: 0f
            val soBuoi = db.lichSuChayBoDao().demSoBuoiTu(tuThoiGian)

            if (_binding == null) return@launch

            binding.tvGiaTriTongQuangDuong.text = String.format("%.2f km", tongQuangDuong)
            binding.tvGiaTriSoBuoi.text = soBuoi.toString()

            if (danhSach.isEmpty()) {
                binding.bieuDoVanDong.visibility = View.GONE
                binding.tvKhongCoDuLieuVanDong.visibility = View.VISIBLE
                return@launch
            }

            binding.bieuDoVanDong.visibility = View.VISIBLE
            binding.tvKhongCoDuLieuVanDong.visibility = View.GONE

            val dinhDangNgay = SimpleDateFormat("dd/MM", Locale.getDefault())
            val danhSachCot = danhSach.mapIndexed { chiSo, banGhi ->
                BarEntry(chiSo.toFloat(), banGhi.quangDuongKm)
            }
            val nhanTrucX = danhSach.map { dinhDangNgay.format(it.thoiGianGhiNhan) }

            val boDuLieu = BarDataSet(danhSachCot, "Quãng đường (km)")
            boDuLieu.color = Color.parseColor("#00BCD4")
            boDuLieu.valueTextSize = 10f

            binding.bieuDoVanDong.data = BarData(boDuLieu)
            binding.bieuDoVanDong.xAxis.valueFormatter = IndexAxisValueFormatter(nhanTrucX)
            binding.bieuDoVanDong.xAxis.position = XAxis.XAxisPosition.BOTTOM
            binding.bieuDoVanDong.xAxis.granularity = 1f
            binding.bieuDoVanDong.description.isEnabled = false
            binding.bieuDoVanDong.animateY(500)
            binding.bieuDoVanDong.invalidate()
        }
    }

    private fun taiBieuDoBuocChan() {
        val appContext = requireContext().applicationContext
        lifecycleScope.launch {
            val db = AppDatabase.layInstance(appContext)
            val dinhDangKhoa = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val dinhDangNhan = SimpleDateFormat("dd/MM", Locale.getDefault())

            // 7 ngày liên tiếp, kết thúc ở hôm nay
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -6)
            val bayNgay = (0..6).map {
                val ngay = cal.time
                cal.add(Calendar.DAY_OF_YEAR, 1)
                ngay
            }

            val duLieu = db.buocChanNgayDao()
                .layTuNgay(dinhDangKhoa.format(bayNgay.first()))
                .associate { it.ngay to it.soBuoc }
            val soBuocTheoNgay = bayNgay.map { duLieu[dinhDangKhoa.format(it)] ?: 0 }
            val mucTieu = QuanLyBuocChan(appContext).docMucTieuBuoc()

            if (_binding == null) return@launch

            val cacNgayCoDuLieu = soBuocTheoNgay.filter { it > 0 }
            val trungBinh = if (cacNgayCoDuLieu.isEmpty()) 0 else cacNgayCoDuLieu.sum() / cacNgayCoDuLieu.size
            binding.tvGiaTriHomNay.text = "${soBuocTheoNgay.last()} bước"
            binding.tvGiaTriTrungBinhBuoc.text = "$trungBinh bước"

            if (cacNgayCoDuLieu.isEmpty()) {
                binding.bieuDoBuocChan.visibility = View.GONE
                binding.tvKhongCoDuLieuBuocChan.visibility = View.VISIBLE
                return@launch
            }

            binding.bieuDoBuocChan.visibility = View.VISIBLE
            binding.tvKhongCoDuLieuBuocChan.visibility = View.GONE

            val danhSachCot = soBuocTheoNgay.mapIndexed { chiSo, soBuoc ->
                BarEntry(chiSo.toFloat(), soBuoc.toFloat())
            }
            val nhanTrucX = bayNgay.map { dinhDangNhan.format(it) }

            val boDuLieu = BarDataSet(danhSachCot, "Số bước")
            boDuLieu.color = Color.parseColor("#2E7D32")
            boDuLieu.valueTextSize = 10f
            boDuLieu.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String =
                    if (value <= 0f) "" else value.roundToInt().toString()
            }

            val gioiHan = LimitLine(mucTieu.toFloat(), "Mục tiêu $mucTieu")
            gioiHan.lineColor = Color.parseColor("#EF6C00")
            gioiHan.lineWidth = 1.5f
            gioiHan.textSize = 10f
            gioiHan.labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP

            val bieuDo = binding.bieuDoBuocChan
            bieuDo.data = BarData(boDuLieu)
            bieuDo.xAxis.valueFormatter = IndexAxisValueFormatter(nhanTrucX)
            bieuDo.xAxis.position = XAxis.XAxisPosition.BOTTOM
            bieuDo.xAxis.granularity = 1f
            bieuDo.xAxis.setDrawGridLines(false)
            bieuDo.axisLeft.removeAllLimitLines()
            bieuDo.axisLeft.addLimitLine(gioiHan)
            bieuDo.axisLeft.axisMinimum = 0f
            bieuDo.axisLeft.axisMaximum = max(mucTieu, soBuocTheoNgay.max()) * 1.2f
            bieuDo.axisRight.isEnabled = false
            bieuDo.legend.isEnabled = false
            bieuDo.description.isEnabled = false
            bieuDo.animateY(500)
            bieuDo.invalidate()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}