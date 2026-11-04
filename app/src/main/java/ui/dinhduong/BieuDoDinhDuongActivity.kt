package com.example.appsk.ui.dinhduong

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.appsk.R
import com.example.appsk.canhbao.caloMucTieu
import com.example.appsk.data.QuanLyHoSo
import com.example.appsk.data.csdl.AppDatabase
import com.example.appsk.databinding.ActivityBieuDoDinhDuongBinding
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

class BieuDoDinhDuongActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBieuDoDinhDuongBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBieuDoDinhDuongBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }
        napDuLieu()
    }

    private fun napDuLieu() {
        val appContext = applicationContext
        lifecycleScope.launch {
            val db = AppDatabase.layInstance(appContext)
            val dinhDangNhan = SimpleDateFormat("dd/MM", Locale.getDefault())

            // 7 ngày liên tiếp, kết thúc ở hôm nay
            val cal = Calendar.getInstance()
            chuanHoa(cal)
            cal.add(Calendar.DAY_OF_YEAR, -6)
            val bayNgay = (0..6).map {
                val dauNgay = cal.timeInMillis
                cal.add(Calendar.DAY_OF_YEAR, 1)
                dauNgay
            }
            val tu = bayNgay.first()
            val den = cal.timeInMillis // 00:00 của ngày mai

            val buaAn = db.buaAnDao().layBuaAnTrongNgay(tu, den)
            val nuoc = db.nuocUongDao().layTrongNgay(tu, den)

            val caloTheoNgay = buaAn.groupBy { dauNgay(it.thoiGianGhiNhan) }
                .mapValues { (_, ds) -> ds.sumOf { it.calo } }
            val nuocTheoNgay = nuoc.groupBy { dauNgay(it.thoiGianGhiNhan) }
                .mapValues { (_, ds) -> ds.sumOf { it.soMl } }

            val giaTriCalo = bayNgay.map { caloTheoNgay[it] ?: 0 }
            val giaTriNuoc = bayNgay.map { nuocTheoNgay[it] ?: 0 }
            val nhanTrucX = bayNgay.map { dinhDangNhan.format(it) }

            val quanLyHoSo = QuanLyHoSo(appContext)
            val hoSo = if (quanLyHoSo.daCoHoSo()) quanLyHoSo.docHoSo() else null
            val mucTieuCalo = hoSo?.caloMucTieu()?.roundToInt()
            val thamKhaoNuoc = if (hoSo != null && hoSo.canNangKg > 0f) {
                (hoSo.canNangKg * 30).roundToInt()
            } else 2000

            val coCalo = giaTriCalo.any { it > 0 }
            val coNuoc = giaTriNuoc.any { it > 0 }
            binding.tvKhongCoDuLieu.visibility =
                if (!coCalo && !coNuoc) View.VISIBLE else View.GONE

            veBieuDo(
                binding.bieuDoCalo, giaTriCalo, nhanTrucX, "Calo (kcal)", "#2E7D32",
                mucTieuCalo, "Mục tiêu"
            )
            veBieuDo(
                binding.bieuDoNuoc, giaTriNuoc, nhanTrucX, "Nước (ml)", "#00BCD4",
                thamKhaoNuoc, "Tham khảo"
            )

            // Trung bình chỉ tính các ngày có ghi nhận
            val caloCoGhi = giaTriCalo.filter { it > 0 }
            val nuocCoGhi = giaTriNuoc.filter { it > 0 }
            binding.tvCaloTb.text = if (caloCoGhi.isEmpty()) "" else
                getString(R.string.dd_calo_tb, caloCoGhi.average().roundToInt())
            binding.tvNuocTb.text = if (nuocCoGhi.isEmpty()) "" else
                getString(R.string.dd_nuoc_tb, nuocCoGhi.average().roundToInt())
        }
    }

    private fun veBieuDo(
        bieuDo: BarChart,
        giaTri: List<Int>,
        nhanTrucX: List<String>,
        tenTap: String,
        mauCot: String,
        gioiHan: Int?,
        nhanGioiHan: String
    ) {
        val danhSachCot = giaTri.mapIndexed { chiSo, v -> BarEntry(chiSo.toFloat(), v.toFloat()) }

        val boDuLieu = BarDataSet(danhSachCot, tenTap)
        boDuLieu.color = Color.parseColor(mauCot)
        boDuLieu.valueTextSize = 10f
        boDuLieu.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String =
                if (value <= 0f) "" else value.roundToInt().toString()
        }

        bieuDo.axisLeft.removeAllLimitLines()
        if (gioiHan != null) {
            val duong = LimitLine(gioiHan.toFloat(), "$nhanGioiHan $gioiHan")
            duong.lineColor = Color.parseColor("#EF6C00")
            duong.lineWidth = 1.5f
            duong.textSize = 10f
            duong.labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
            bieuDo.axisLeft.addLimitLine(duong)
        }

        val dinhTren = maxOf(giaTri.maxOrNull()?.toFloat() ?: 0f, gioiHan?.toFloat() ?: 0f)

        bieuDo.data = BarData(boDuLieu)
        bieuDo.xAxis.valueFormatter = IndexAxisValueFormatter(nhanTrucX)
        bieuDo.xAxis.position = XAxis.XAxisPosition.BOTTOM
        bieuDo.xAxis.granularity = 1f
        bieuDo.xAxis.setDrawGridLines(false)
        bieuDo.axisLeft.axisMinimum = 0f
        bieuDo.axisLeft.axisMaximum = if (dinhTren <= 0f) 1f else dinhTren * 1.2f
        bieuDo.axisRight.isEnabled = false
        bieuDo.legend.isEnabled = false
        bieuDo.description.isEnabled = false
        bieuDo.animateY(500)
        bieuDo.invalidate()
    }

    private fun dauNgay(thoiGian: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = thoiGian
        chuanHoa(cal)
        return cal.timeInMillis
    }

    private fun chuanHoa(cal: Calendar) {
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
    }
}