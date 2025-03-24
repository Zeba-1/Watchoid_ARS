package fr.uge.android.watchoid.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import fr.uge.android.watchoid.WatchoidDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.i("INFO", "Device rebooted, rescheduling BlueWorkers")
            val database = WatchoidDatabase.getInstance(context)
            val dao = database.serviceTestDao()
            val coroutineScope = CoroutineScope(Dispatchers.IO)

            coroutineScope.launch {
                val allTests = dao.getAllTests()
                for (test in allTests) {
                    if (test.periodicity >= 15 * 60) {
                        val periodicWorkRequest = PeriodicWorkRequestBuilder<BlueWorker>(test.periodicity, TimeUnit.SECONDS)
                            .setInputData(workDataOf("testName" to test.name))
                            .build()
                        WorkManager.getInstance(context).enqueue(periodicWorkRequest)
                    }
                }
            }
        }
    }
}