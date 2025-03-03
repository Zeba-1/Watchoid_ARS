package fr.uge.android.watchoid.Action

import android.util.Log
import androidx.compose.runtime.rememberCoroutineScope
import fr.uge.android.watchoid.entity.test.ServiceTest
import fr.uge.android.watchoid.entity.test.TestType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL

fun ExecuteTest(serviceTest: ServiceTest, coroutineScope: CoroutineScope) {
    when(serviceTest.type) {
        TestType.HTTP -> {
            ExecuteHttpTest(serviceTest, coroutineScope)
        }
        TestType.PING -> {
            ExecutePingTest(serviceTest, coroutineScope)
        }
        TestType.UDP -> {
            Log.i("ServiceTest", "Executing UDP test")
        }
        TestType.TCP -> {
            Log.i("ServiceTest", "Executing TCP test")
        }
    }
}

fun ExecuteTests(tests: List<ServiceTest>, coroutineScope: CoroutineScope) {
    for (test in tests) {
        ExecuteTest(test, coroutineScope)
    }
}

fun ExecutePingTest(serviceTest: ServiceTest, coroutineScope: CoroutineScope) {
    coroutineScope.launch {
        Log.i("ServiceTest", "Executing PING test on ${serviceTest.target}")
        val (isReachable, responseTime) = ping(serviceTest.target)
        if (isReachable) {
            Log.i("ServiceTest", "${serviceTest.target} is reachable in $responseTime ms")
        } else {
            Log.i("ServiceTest", "${serviceTest.target} is not reachable after $responseTime ms")
        }
    }
}


// ping via inet => ne semble pas fonctionner...
suspend fun ping(target: String): Pair<Boolean, Long> {
    return withContext(Dispatchers.IO) {
        try {
            val address = InetAddress.getByName(target)
            val startTime = System.currentTimeMillis()
            val isReachable = address.isReachable(1000)
            val endTime = System.currentTimeMillis()
            val responseTime = endTime - startTime
            Pair(isReachable, responseTime)
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(false, -1)
        }
    }
}

fun ExecuteHttpTest(serviceTest: ServiceTest, coroutineScope: CoroutineScope) {
    coroutineScope.launch {
        Log.i("ServiceTest", "Executing HTTP test on ${serviceTest.target}")
        val (isReachable, response) = HttpGet(serviceTest.target)
        if (isReachable) {
            Log.i("ServiceTest", "${serviceTest.target} returned $response")
        } else {
            Log.i("ServiceTest", "${serviceTest.target} is not reachable or error")
        }
    }
}

suspend fun HttpGet(target: String): Pair<Boolean, String> {
    return withContext(Dispatchers.IO) {
        try {
            val url = URL(target)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connect()

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                Pair(true, connection.inputStream.bufferedReader().use { it.readText() })
            } else {
                Pair(false, "")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(false, "")
        }
    }
}

