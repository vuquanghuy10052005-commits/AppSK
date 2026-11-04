package com.example.appsk

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import com.example.appsk.buocchan.LayMauBuocChanWorker
import com.example.appsk.taikhoan.TaiKhoan

class UngDungSucKhoe : Application() {

    override fun onCreate() {
        super.onCreate()
        LayMauBuocChanWorker.lenLich(this)
        chanManHinhKhiChuaDangNhap()
    }

    /**
     * Mọi màn hình (trừ Đăng nhập và Đăng ký) đều bị đưa về Đăng nhập nếu chưa đăng nhập,
     * kể cả khi mở bằng cách bấm vào một thông báo cũ.
     */
    private fun chanManHinhKhiChuaDangNhap() {
        registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                if (activity is DangNhapActivity || activity is DangKyActivity) return

                if (!TaiKhoan.daDangNhap()) {
                    val intent = Intent(activity, DangNhapActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    }
                    activity.startActivity(intent)
                    activity.finish()
                }
            }

            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }
}