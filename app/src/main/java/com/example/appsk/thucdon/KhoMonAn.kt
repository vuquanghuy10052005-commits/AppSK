package com.example.appsk.thucdon

import kotlin.math.abs

object KhoMonAn {

    private data class MonCoSan(val loai: String, val ten: String, val calo: Int)

    private val DANH_SACH = listOf(
        // Bữa sáng
        MonCoSan("Sáng", "Cháo thịt bằm (1 tô)", 260),
        MonCoSan("Sáng", "Cháo yến mạch sữa chua trái cây", 280),
        MonCoSan("Sáng", "2 trứng luộc và 1 lát bánh mì nguyên cám", 300),
        MonCoSan("Sáng", "Sữa chua Hy Lạp, yến mạch và chuối", 310),
        MonCoSan("Sáng", "Bánh cuốn nhân thịt (1 đĩa)", 320),
        MonCoSan("Sáng", "Xôi đậu xanh (1 phần nhỏ)", 330),
        MonCoSan("Sáng", "Bánh mì trứng ốp la", 350),
        MonCoSan("Sáng", "Phở gà (1 tô nhỏ)", 380),
        MonCoSan("Sáng", "Bún mọc (1 tô)", 380),
        MonCoSan("Sáng", "Bún riêu cua (1 tô)", 400),
        MonCoSan("Sáng", "Hủ tiếu (1 tô)", 430),
        MonCoSan("Sáng", "Phở bò (1 tô)", 450),
        MonCoSan("Sáng", "Bánh mì ốp la xúc xích", 480),
        MonCoSan("Sáng", "Xôi gà (1 phần)", 520),

        // Bữa trưa
        MonCoSan("Trưa", "Salad ức gà và 1 lát bánh mì", 400),
        MonCoSan("Trưa", "Miến gà (1 tô)", 430),
        MonCoSan("Trưa", "Cơm gạo lứt, ức gà luộc, rau luộc", 480),
        MonCoSan("Trưa", "Cơm trắng, đậu hũ sốt cà, rau xào", 520),
        MonCoSan("Trưa", "Phở bò tái (tô lớn)", 520),
        MonCoSan("Trưa", "Cơm trắng, tôm rim, canh bí", 540),
        MonCoSan("Trưa", "Cơm trắng, cá thu kho, canh rau", 560),
        MonCoSan("Trưa", "Bún bò Huế (1 tô)", 560),
        MonCoSan("Trưa", "Mì Quảng (1 tô)", 560),
        MonCoSan("Trưa", "Cơm trắng, trứng chiên, canh rau", 580),
        MonCoSan("Trưa", "Cơm gà luộc (1 phần)", 600),
        MonCoSan("Trưa", "Bún chả (1 phần)", 620),
        MonCoSan("Trưa", "Bún thịt nướng (1 phần)", 650),
        MonCoSan("Trưa", "Cơm trắng, thịt kho trứng, canh cải", 680),
        MonCoSan("Trưa", "Cơm rang dương châu (1 đĩa)", 700),
        MonCoSan("Trưa", "Cơm tấm sườn nướng", 730),

        // Bữa tối
        MonCoSan("Tối", "Súp gà nấm (1 tô)", 300),
        MonCoSan("Tối", "Cháo cá (1 tô)", 320),
        MonCoSan("Tối", "Phở cuốn (1 phần)", 350),
        MonCoSan("Tối", "Lẩu rau nấm (phần nhẹ)", 350),
        MonCoSan("Tối", "Gà luộc, rau luộc, nửa chén cơm", 380),
        MonCoSan("Tối", "Salad cá hồi áp chảo", 420),
        MonCoSan("Tối", "Bún cá (1 tô)", 430),
        MonCoSan("Tối", "Cơm trắng, cá hấp, canh bí đỏ", 450),
        MonCoSan("Tối", "Bún riêu (1 tô)", 450),
        MonCoSan("Tối", "Cơm trắng, đậu hũ sốt cà chua", 470),
        MonCoSan("Tối", "Cơm trắng, trứng hấp thịt, rau luộc", 500),
        MonCoSan("Tối", "Cơm trắng, thịt luộc, canh rau", 550),
        MonCoSan("Tối", "Cơm trắng, cá chép kho, rau muống", 560),
        MonCoSan("Tối", "Cơm trắng, thịt bò xào rau, canh", 620),

        // Bữa phụ
        MonCoSan("Phụ", "1 miếng dưa hấu (200g)", 60),
        MonCoSan("Phụ", "1 quả cam", 70),
        MonCoSan("Phụ", "1 quả trứng luộc", 75),
        MonCoSan("Phụ", "1 quả táo", 80),
        MonCoSan("Phụ", "1 quả chuối", 100),
        MonCoSan("Phụ", "1 ly sữa đậu nành không đường", 100),
        MonCoSan("Phụ", "1 hũ sữa chua (100g)", 110),
        MonCoSan("Phụ", "2 cái bánh quy yến mạch", 120),
        MonCoSan("Phụ", "1 ly sữa tươi không đường (200ml)", 130),
        MonCoSan("Phụ", "1 củ khoai lang luộc (vừa)", 130),
        MonCoSan("Phụ", "1 bắp ngô luộc", 140),
        MonCoSan("Phụ", "1 nắm hạt hạnh nhân (30g)", 170),
        MonCoSan("Phụ", "1 chén chè đậu xanh nhỏ", 180),
        MonCoSan("Phụ", "1 thanh granola", 180),
        MonCoSan("Phụ", "2 lát bánh mì nguyên cám và bơ đậu phộng", 200),
        MonCoSan("Phụ", "1 ly sinh tố bơ ít đường", 250)
    )

    /**
     * Chọn món cùng loại bữa, chênh lệch calo không quá dung sai, khác món hiện tại
     * và khác các món đã có trong ngày. Nếu không có món nào trong dung sai thì lấy món gần nhất.
     */
    fun goiYThay(
        loaiBua: String,
        caloMuc: Int,
        tenHienTai: String,
        tenDaCo: List<String>
    ): MonGoiY {
        val cungLoai = DANH_SACH.filter { it.loai == loaiBua }.ifEmpty { DANH_SACH }
        val hienTai = KeHoachMonAn.boDau(tenHienTai)
        val daDung = (tenDaCo + tenHienTai).map { KeHoachMonAn.boDau(it) }.toSet()

        val chuaDung = cungLoai
            .filter { KeHoachMonAn.boDau(it.ten) !in daDung }
            .ifEmpty { cungLoai.filter { KeHoachMonAn.boDau(it.ten) != hienTai } }
            .ifEmpty { cungLoai }

        val trongDungSai = chuaDung.filter { abs(it.calo - caloMuc) <= ThayMonAn.DUNG_SAI_CALO }
        val chon = if (trongDungSai.isNotEmpty()) {
            trongDungSai.random()
        } else {
            chuaDung.minByOrNull { abs(it.calo - caloMuc) } ?: cungLoai.first()
        }
        return MonGoiY(chon.ten, chon.calo)
    }
}