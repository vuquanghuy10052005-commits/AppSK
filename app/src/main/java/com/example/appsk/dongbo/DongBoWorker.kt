package com.example.appsk.dongbo

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.CancellationException

class DongBoWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!CaiDatDongBo.dangBat(applicationContext)) return Result.success()

        return try {
            val ketQua = DongBoDuLieu.dongBo(applicationContext)
            // Lỗi tạm thời (mất mạng...) thì thử lại vài lần; lỗi khác để lần sau xử lý
            if (!ketQua.thanhCong && ketQua.canThuLai && runAttemptCount < 5) {
                Result.retry()
            } else {
                Result.success()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (runAttemptCount < 5) Result.retry() else Result.success()
        }
    }
}