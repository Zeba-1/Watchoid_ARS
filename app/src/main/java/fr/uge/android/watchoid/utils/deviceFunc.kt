package fr.uge.android.watchoid.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings.Global.getString
import android.util.Log
import androidx.compose.material3.DatePickerDialog
import androidx.core.content.ContextCompat.getSystemService
import fr.uge.android.watchoid.R

class deviceFunc {

    fun getBatteryLevel(context: Context): Int {
        var batteryStatus = getBatteryStatus(context)
        val batteryPct: Float? = batteryStatus?.let { intent ->
            val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            level * 100 / scale.toFloat()
        }
        return batteryPct?.toInt() ?: 0
    }

    fun getBatteryStatus(context: Context): Intent? {
        return context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    fun getConnectionStatus(context: Context): Pair<Boolean,Boolean> {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return Pair(false,false)
        return Pair(capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI), capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
    }
}