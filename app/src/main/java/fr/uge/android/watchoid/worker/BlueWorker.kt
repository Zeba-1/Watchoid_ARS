package fr.uge.android.watchoid.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import fr.uge.android.watchoid.Action.ExecuteTest
import fr.uge.android.watchoid.Action.noficationGestion
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

        Log.i("BlueWorker", "Worker started")
        Log.i("BlueWorker", "Input data: $inputData")
        val test = dao.getTestByName(inputData.getString("testName") ?: "")

        if (test != null) {
            val batteryLevel = deviceFunc().getBatteryLevel(applicationContext)
            val connectionDevice = deviceFunc().getConnectionStatus(applicationContext)
            Log.i("BlueWorker", "Test found: ${test.name}")

            ExecuteTest(test, CoroutineScope(Dispatchers.IO), dao, batteryLevel,connectionDevice,true)
            noficationGestion(test, dao, applicationContext)
        } else {
            Log.e("BlueWorker", "Test not found")
        }
        Result.success()
    }
}
