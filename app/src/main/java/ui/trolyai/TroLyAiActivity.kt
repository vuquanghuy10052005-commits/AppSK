package com.example.appsk.ui.trolyai

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appsk.BuildConfig
import com.example.appsk.R
import com.example.appsk.data.csdl.AppDatabase
import com.example.appsk.data.csdl.TinNhanAi
import com.example.appsk.databinding.ActivityTroLyAiBinding
import com.google.ai.client.generativeai.Chat
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.launch

class TroLyAiActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTroLyAiBinding
    private lateinit var adapter: TinNhanAdapter
    private lateinit var db: AppDatabase
    private val danhSachTinNhan = mutableListOf<TinNhan>()
    private var phienChat: Chat? = null

    private val model by lazy {
        GenerativeModel(
            modelName = "gemini-flash-latest",
            apiKey = BuildConfig.GEMINI_API_KEY
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTroLyAiBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }

        db = AppDatabase.layInstance(this)

        adapter = TinNhanAdapter(danhSachTinNhan)
        binding.rvTinNhan.layoutManager = LinearLayoutManager(this)
        binding.rvTinNhan.adapter = adapter

        binding.btnGui.setOnClickListener {
            guiCauHoi()
        }

        taiLaiLichSuChat()
    }

    /**
     * Nạp lại toàn bộ lịch sử chat đã lưu từ Room, hiển thị lên RecyclerView,
     * và tái tạo lại phiên chat với Gemini kèm lịch sử cũ -> AI vẫn "nhớ"
     * được ngữ cảnh cuộc trò chuyện trước, không bị mất trí nhớ sau khi đóng app.
     */
    private fun taiLaiLichSuChat() {
        lifecycleScope.launch {
            val lichSuDaLuu = db.tinNhanAiDao().layTatCaTinNhan()
            val lichSuGuiChoGemini = mutableListOf<Content>()

            lichSuDaLuu.forEach { tinNhan ->
                danhSachTinNhan.add(TinNhan(tinNhan.noiDung, tinNhan.laCuaNguoiDung))
                lichSuGuiChoGemini.add(
                    content(role = if (tinNhan.laCuaNguoiDung) "user" else "model") {
                        text(tinNhan.noiDung)
                    }
                )
            }

            adapter.notifyDataSetChanged()
            cuonXuongCuoi()

            phienChat = model.startChat(history = lichSuGuiChoGemini)
        }
    }

    private fun guiCauHoi() {
        val cauHoi = binding.etCauHoi.text.toString().trim()
        if (cauHoi.isEmpty()) return

        themVaoGiaoDienVaLuu(cauHoi, laCuaNguoiDung = true)
        binding.etCauHoi.setText("")
        hienThiDangTai(true)

        lifecycleScope.launch {
            val chat = phienChat ?: model.startChat().also { phienChat = it }
            var soLanThu = 0
            var thanhCong = false

            while (soLanThu < 2 && !thanhCong) {
                try {
                    val phanHoi = chat.sendMessage(cauHoi)
                    val noiDungTraLoi = phanHoi.text ?: getString(R.string.loi_goi_ai)
                    themVaoGiaoDienVaLuu(noiDungTraLoi, laCuaNguoiDung = false)
                    thanhCong = true
                } catch (e: Exception) {
                    soLanThu++
                    if (soLanThu >= 2) {
                        val thongBaoLoi = if (e.message?.contains("UNAVAILABLE") == true || e.message?.contains("503") == true) {
                            "Máy chủ AI đang quá tải, vui lòng thử lại sau ít phút."
                        } else {
                            "Lỗi thật: ${e.message}"
                        }
                        themVaoGiaoDienVaLuu(thongBaoLoi, laCuaNguoiDung = false)
                    } else {
                        kotlinx.coroutines.delay(2000)
                    }
                }
            }
            hienThiDangTai(false)
        }
    }

    /**
     * Vừa hiển thị tin nhắn lên giao diện ngay, vừa lưu xuống Room
     * để lần sau mở app vẫn còn nguyên - áp dụng cho cả câu hỏi người dùng
     * lẫn câu trả lời/phân tích của AI.
     */
    private fun themVaoGiaoDienVaLuu(noiDung: String, laCuaNguoiDung: Boolean) {
        adapter.themTinNhan(TinNhan(noiDung, laCuaNguoiDung))
        cuonXuongCuoi()

        lifecycleScope.launch {
            db.tinNhanAiDao().themTinNhan(
                TinNhanAi(
                    noiDung = noiDung,
                    laCuaNguoiDung = laCuaNguoiDung,
                    thoiGianGuiTin = System.currentTimeMillis()
                )
            )
        }
    }

    private fun hienThiDangTai(dangTai: Boolean) {
        binding.progressBar.visibility = if (dangTai) View.VISIBLE else View.GONE
        binding.btnGui.isEnabled = !dangTai
    }

    private fun cuonXuongCuoi() {
        if (danhSachTinNhan.isNotEmpty()) {
            binding.rvTinNhan.scrollToPosition(danhSachTinNhan.size - 1)
        }
    }
}