package com.example.appsk.dongbo

import com.example.appsk.data.csdl.AppDatabase
import java.security.MessageDigest
import java.util.Locale
import kotlin.math.abs
import kotlin.math.round

/** Một dòng dữ liệu trên máy: mã dòng và các trường sẽ được gửi lên đám mây. */
class BanGhiCucBo(val id: Long, val truong: Map<String, Any?>)

/** Mô tả cách đọc, thêm, sửa, xóa một loại dữ liệu để bộ máy đồng bộ dùng chung. */
abstract class LoaiDongBo(val ten: String, val choPhepCapNhat: Boolean = false) {
    abstract suspend fun layTatCa(db: AppDatabase): List<BanGhiCucBo>
    abstract suspend fun them(db: AppDatabase, truong: Map<String, Any?>): Long
    open suspend fun capNhat(db: AppDatabase, id: Long, truong: Map<String, Any?>) {}
    abstract suspend fun xoa(db: AppDatabase, id: Long)
}

/** Dấu vân nội dung: hai bản có cùng nội dung thì cho cùng chuỗi, bất kể máy nào tạo ra. */
object DauVan {

    fun tinh(truong: Map<String, Any?>): String {
        val bam = MessageDigest.getInstance("SHA-256")
            .digest(chuanHoa(truong).toByteArray(Charsets.UTF_8))
        return bam.take(12).joinToString("") { "%02x".format(it) }
    }

    private fun chuanHoa(giaTri: Any?): String = when (giaTri) {
        null -> "null"
        is Map<*, *> -> giaTri.entries
            .sortedBy { it.key.toString() }
            .joinToString(",", "{", "}") { "${it.key}=${chuanHoa(it.value)}" }
        is List<*> -> giaTri.joinToString(",", "[", "]") { chuanHoa(it) }
        is Number -> chuanHoaSo(giaTri.toDouble())
        is Boolean -> giaTri.toString()
        else -> "\"$giaTri\""
    }

    // 3 và 3.0 cho cùng kết quả, vì đám mây có thể trả số nguyên dưới dạng khác
    private fun chuanHoaSo(d: Double): String =
        if (d == round(d) && abs(d) < 1e15) d.toLong().toString()
        else String.format(Locale.US, "%.6f", d)
}