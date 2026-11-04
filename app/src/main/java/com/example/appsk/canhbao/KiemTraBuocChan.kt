package com.example.appsk.canhbao

import android.content.Context
import com.example.appsk.data.csdl.AppDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * So sánh số bước trung bình 6 ngày gần nhất (không tính hôm nay) với mức thông thường
 * của chính người dùng trong 4 tuần trước đó.
 */
object KiemTraBuocChan {

    private const val MOT_NGAY = 24L * 60 * 60 * 1000

    suspend fun kiemTra(
        context: Context,
        dauHomNay: Long,
        canhBao: MutableList<CanhBaoTamThoi>,
        thieuDuLieu: MutableList<String>
    ) {
        val db = AppDatabase.layInstance(context.applicationContext)
        val dinhDang = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val ngayHomNay = dinhDang.format(Date(dauHomNay))
        val ngayDauGanDay = dinhDang.format(Date(dauHomNay - 6 * MOT_NGAY))
        val ngayDauNenTang = dinhDang.format(Date(dauHomNay - 34 * MOT_NGAY))

        // Chỉ tính những ngày có dữ liệu thật (từ 500 bước), bỏ hôm nay vì chưa đi hết ngày
        val cacNgay = db.buocChanNgayDao().layTuNgay(ngayDauNenTang)
            .filter { it.soBuoc >= 500 && it.ngay != ngayHomNay }
        val ganDay = cacNgay.filter { it.ngay >= ngayDauGanDay }
        val nenTang = cacNgay.filter { it.ngay < ngayDauGanDay }

        if (nenTang.size < 10) {
            thieuDuLieu.add("Bước chân: cần ít nhất 10 ngày có dữ liệu bước chân trong 4 tuần trước đó để biết mức thông thường của bạn")
            return
        }
        if (ganDay.size < 3) {
            thieuDuLieu.add("Bước chân: cần dữ liệu ít nhất 3 ngày gần đây (không tính hôm nay)")
            return
        }

        val trungBinhThuong = nenTang.map { it.soBuoc }.average()
        val trungBinhGanDay = ganDay.map { it.soBuoc }.average()
        if (trungBinhThuong < 2000) return

        val tyLe = trungBinhGanDay / trungBinhThuong
        val mucDo = when {
            tyLe < 0.4 -> MucDoCanhBao.VUA
            tyLe < 0.6 -> MucDoCanhBao.NHE
            else -> 0
        }
        if (mucDo > 0) {
            canhBao.add(
                CanhBaoTamThoi(
                    khoa = "BUOC_CHAN_GIAM",
                    loai = "Vận động",
                    mucDo = mucDo,
                    tieuDe = "Số bước chân giảm so với thường ngày",
                    noiDung = "Trung bình ${trungBinhGanDay.roundToInt()} bước/ngày trong ${ganDay.size} ngày gần đây, " +
                            "chỉ bằng ${(tyLe * 100).roundToInt()}% mức thông thường của bạn " +
                            "(≈ ${trungBinhThuong.roundToInt()} bước/ngày trong 4 tuần trước đó). " +
                            "Nếu bạn để điện thoại ở nhà hoặc app bị hạn chế chạy nền thì số liệu có thể thấp hơn thực tế."
                )
            )
        }
    }
}