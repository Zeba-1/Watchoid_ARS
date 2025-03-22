package fr.uge.android.watchoid.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import fr.uge.android.watchoid.Action.ExecuteTest
import fr.uge.android.watchoid.WatchoidDatabase
import fr.uge.android.watchoid.utils.deviceFunc
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.absoluteValue


class BlueWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val database = WatchoidDatabase.getInstance(applicationContext)
        val dao = database.serviceTestDao()

        val tests = dao.getAllTests()

        tests.forEach { test ->
            if (test.periodicity != 0L && test.periodicity >= 15 * 60) {
                val currentTime = System.currentTimeMillis() / 1000
                val testInfoEntity = dao.getTestById(test.id)
                val elapsedTime = ((testInfoEntity?.lastTest ?: 0) - currentTime).absoluteValue
                if (elapsedTime >= test.periodicity) {
                    val batteryLevel = deviceFunc().getBatteryLevel(applicationContext)
                    val connectionDevice = deviceFunc().getConnectionStatus(applicationContext)
                    ExecuteTest(test, CoroutineScope(Dispatchers.IO), dao, batteryLevel,connectionDevice,false)
                    dao.update(testInfoEntity!!.apply { lastTest = currentTime })
                }
            }
        }
        Result.success()
    }
}
