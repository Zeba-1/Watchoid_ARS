package fr.uge.android.watchoid.Action

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import fr.uge.android.watchoid.DAO.ServiceTestDao
import fr.uge.android.watchoid.R
import fr.uge.android.watchoid.entity.test.PaternType
import fr.uge.android.watchoid.entity.test.ServiceTest
import fr.uge.android.watchoid.entity.report.TestReport
import fr.uge.android.watchoid.entity.test.ConnectionType
import fr.uge.android.watchoid.entity.test.TestStatus
import fr.uge.android.watchoid.entity.test.TestType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.net.URL

var notificationId = 0

fun ExecuteTest(serviceTest: ServiceTest, coroutineScope: CoroutineScope, dao: ServiceTestDao, batteryLevel: Int = 100, connectionDevice:Pair<Boolean,Boolean>, userExecuted: Boolean = false, userAction: (Boolean) -> Unit = {}) {
    if (batteryLevel < serviceTest.minBatteryLevel) {
        Log.i("BlueWorker", "Battery level is too low to execute the test")
        AddReport(serviceTest, dao, false, -1, "Battery level is too low to execute the test", coroutineScope)
        userAction(false)
        return
    }

    if ((serviceTest.connectionType == ConnectionType.WIFI && !connectionDevice.first) ||
        (serviceTest.connectionType == ConnectionType.CELLULAR && !connectionDevice.second) ||
        (serviceTest.connectionType == ConnectionType.ALL && !connectionDevice.first && !connectionDevice.second)) {
        Log.i("ServiceTest", "No WIFI OR CELLULAR connection available")
        AddReport(serviceTest, dao, false, -1, "No WIFI OR CELLULAR connection available", coroutineScope)
        userAction(false)
        return
    }


    when(serviceTest.type) {
        TestType.HTTP -> {
            ExecuteHttpTest(serviceTest, coroutineScope, dao, userExecuted, userAction)
        }
        TestType.PING -> {
            ExecutePingTest(serviceTest, coroutineScope, dao, userExecuted, userAction)
        }
        TestType.UDP -> {
            ExecuteUdpTest(serviceTest, coroutineScope, dao, userExecuted, userAction)
        }
        TestType.TCP -> {
            ExecuteTcpTest(serviceTest, coroutineScope, dao, userExecuted, userAction)
        }
    }

}

fun AddReport(serviceTest: ServiceTest, dao: ServiceTestDao, isTestOk: Boolean, responseTime: Long, info: String, coroutineScope: CoroutineScope) {
    coroutineScope.launch {
        val report = TestReport(testId = serviceTest.id,
            isTestOk = isTestOk,
            responseTime = responseTime,
            info = info)
        dao.insertTestReport(report)
    }
}

fun ExecuteTests(tests: List<ServiceTest>, coroutineScope: CoroutineScope, dao: ServiceTestDao) {
//    for (test in tests) {
//        ExecuteTest(test, coroutineScope, dao)
//    }
}

suspend fun noficationGestion(serviceTest: ServiceTest, dao: ServiceTestDao, context: Context) {
    Log.i("ServiceTest", "isNotification : ${serviceTest.isNotification}, testResult : ${serviceTest.status}")
    if (serviceTest.isNotification && serviceTest.status == TestStatus.FAILURE) {
        val nbTestFail = dao.getTestReportCountByName(serviceTest.id, false)
        Log.i("ServiceTest", "nbTestFail : $nbTestFail")
        if ((nbTestFail % serviceTest.nBTestFailBeforeNotification) == 0) {
            Log.i("ServiceTest", "Send notification")
            if(serviceTest.notifcationImportance == NotificationManager.IMPORTANCE_LOW) {
                buildNotification("Test failed", "Test ${serviceTest.name} failed", "Watchoid", context, NotificationCompat.PRIORITY_LOW)

            }else {
                buildNotification("Test failed", "Test ${serviceTest.name} failed", "Watchoid2", context, NotificationCompat.PRIORITY_HIGH)
            }
        }
    }
}

fun buildNotification(textTitle: String, textContent: String, channelId: String, context: Context, priority: Int = NotificationCompat.PRIORITY_LOW) {
    val builder = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle(textTitle)
        .setContentText(textContent)
        .setPriority(priority)

    Log.i("ServiceTest", "Send notification $textTitle : $textContent")

    with(NotificationManagerCompat.from(context)) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.i("ServiceTest", "No permission to send notification")

            return@with
        }

        Log.i("ServiceTest", "notify")
        notify(notificationId++, builder.build())
    }
}

fun ExecutePingTest(serviceTest: ServiceTest, coroutineScope: CoroutineScope, dao : ServiceTestDao, userExecuted: Boolean = false, userAction: (Boolean) -> Unit = {}) {
    coroutineScope.launch {
        Log.i("ServiceTest", "Executing PING test on ${serviceTest.target}")
        val (isReachable, responseTime) = ping(serviceTest.target)
        if (isReachable) {
            Log.i("ServiceTest", "${serviceTest.target} is reachable in $responseTime ms")
            serviceTest.status = TestStatus.SUCCESS
        } else {
            Log.i("ServiceTest", "${serviceTest.target} is not reachable after $responseTime ms")
            serviceTest.status = TestStatus.FAILURE
        }
        serviceTest.lastTest = System.currentTimeMillis()
        dao.update(serviceTest)

        val report = TestReport(testId = serviceTest.id,
            isTestOk = isReachable,
            responseTime = responseTime,
            info = if (isReachable) "Ping success" else "Ping failed (timeout)")
        dao.insertTestReport(report)

        if (userExecuted) {
            userAction(isReachable)
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

fun ExecuteHttpTest(serviceTest: ServiceTest, coroutineScope: CoroutineScope, dao: ServiceTestDao, userExecuted: Boolean = false, userAction: (Boolean) -> Unit = {}) {
    coroutineScope.launch {
        Log.i("ServiceTest", "Executing HTTP test on ${serviceTest.target}")
        val startTime = System.currentTimeMillis()
        val (responseCode, response) = HttpGet(serviceTest.target)
        val endTime = System.currentTimeMillis()
        val responseTime = endTime - startTime

        if (responseCode != 200) {
            Log.i("ServiceTest", "${serviceTest.target} is not reachable or error")

            serviceTest.status = TestStatus.FAILURE
            dao.update(serviceTest)

            val report = TestReport(testId = serviceTest.id,
                isTestOk = false,
                responseTime = responseTime,
                info = if (responseCode == -1) "Connection error" else "Response code is $responseCode")
            dao.insertTestReport(report)

            return@launch
        }

        var testResult = checkResponse(response, serviceTest.patern, serviceTest.paternType)

        Log.i("ServiceTest", "${serviceTest.name} ${if (testResult) "success" else "failed"}")
        serviceTest.status = if (testResult) TestStatus.SUCCESS else TestStatus.FAILURE
        serviceTest.lastTest = System.currentTimeMillis()
        dao.update(serviceTest)

        val report = TestReport(testId = serviceTest.id,
            isTestOk = testResult,
            responseTime = responseTime,
            info = if (testResult) "Response verification succeed" else "Response verification failed")
        dao.insertTestReport(report)

        if (userExecuted) {
            userAction(testResult)
        }
    }
}

// return the response code and the response
suspend fun HttpGet(target: String): Pair<Int, String> {
    return withContext(Dispatchers.IO) {
        try {
            val url = URL(target)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connect()

            val responseCode = connection.responseCode
            val response = if (responseCode < 400) connection.inputStream.bufferedReader().use { it.readText() } else ""
            Pair(responseCode, response)
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(-1, "")
        }
    }
}

fun checkResponse(response: String, patern: String, paternType: PaternType): Boolean {
    Log.i("checkResponse", "response : $response, paternType : $paternType, patern : $patern" )
    return when(paternType) {
        PaternType.CONTAINS -> response.contains(patern)
        PaternType.EQUALS -> response == patern
        PaternType.NOT_CONTAINS -> !response.contains(patern)
        PaternType.REGEX -> response.matches(patern.toRegex())
    }
}

fun ExecuteUdpTest(serviceTest: ServiceTest, coroutineScope: CoroutineScope, dao: ServiceTestDao,userExecuted: Boolean = false, userAction: (Boolean) -> Unit = {}) {
    coroutineScope.launch {
        val startTime = System.currentTimeMillis()
        Log.i("ServiceTest", "Executing UDP test on ${serviceTest.target}")
        val (isReachable, response) = UdpSend(serviceTest.target, serviceTest.message, serviceTest.port, serviceTest.patern, serviceTest.paternType)
        if (isReachable) {
            serviceTest.status = TestStatus.SUCCESS
            Log.i("ServiceTest", "${serviceTest.target} returned $response")
        } else {
            serviceTest.status = TestStatus.FAILURE
            Log.i("ServiceTest", "${serviceTest.target} is not reachable or error")
        }
        val endTime = System.currentTimeMillis()
        dao.update(serviceTest)

        val responseTime = endTime - startTime
        val report = TestReport(testId = serviceTest.id,
            isTestOk = isReachable,
            responseTime = responseTime,
            info = if (isReachable) "UDP success" else response)
        dao.insertTestReport(report)

        if (userExecuted) {
            userAction(isReachable)
        }
    }
}

suspend fun UdpSend(target: String,message:String ,port: Int, patternMatching:String, patternType:PaternType): Pair<Boolean, String> {
    return withContext(Dispatchers.IO) {
        try {
            val address = InetAddress.getByName(target)
            val socket = DatagramSocket()
            socket.soTimeout = 3000
            val buffer = message.toByteArray()
            val packet = DatagramPacket(buffer, buffer.size, address, port)
            socket.send(packet)

            val receiveData = ByteArray(1024)
            val receivePacket = DatagramPacket(receiveData, receiveData.size)

            try {
                socket.receive(receivePacket)
                if (checkResponse(String(receivePacket.data, 0, receivePacket.length), patternMatching, patternType)) {
                    Pair(true, "Pattern found")
                } else {
                    Pair(false, "Pattern not found")
                }
            } catch (e: SocketTimeoutException) {
                Pair(false, "Timeout")
            } finally {
                socket.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(false, e.message ?: "Error")
        }
    }
}


fun ExecuteTcpTest(serviceTest: ServiceTest, coroutineScope: CoroutineScope, dao: ServiceTestDao, userExecuted: Boolean = false, userAction: (Boolean) -> Unit = {}) {
    coroutineScope.launch {
        val startTime = System.currentTimeMillis()
        Log.i("ServiceTest", "Executing TCP test on ${serviceTest.target}")
        val (isReachable, response) = TcpSend(serviceTest.target, serviceTest.message, serviceTest.port, serviceTest.patern, serviceTest.paternType)
        if (isReachable) {
            serviceTest.status = TestStatus.SUCCESS
            Log.i("ServiceTest", "${serviceTest.target} returned $response")
        } else {
            serviceTest.status = TestStatus.FAILURE
            Log.i("ServiceTest", "${serviceTest.target} is not reachable or error")
        }
        val endTime = System.currentTimeMillis()
        val responseTime = endTime - startTime
        val report = TestReport(testId = serviceTest.id,
            isTestOk = isReachable,
            responseTime = responseTime,
            info = if (isReachable) "TCP success" else response)

        dao.insertTestReport(report)

        dao.update(serviceTest)

        if (userExecuted) {
            userAction(isReachable)
        }
    }
}

suspend fun TcpSend(target: String,message:String ,port: Int, patternMatching:String, patternType:PaternType): Pair<Boolean, String> {
    return withContext(Dispatchers.IO) {
        try {
            val socket = Socket()
            val socketAddress = InetSocketAddress(target, port)
            socket.connect(socketAddress, 3000)

            if (socket.isConnected) {
                val out = PrintWriter(socket.getOutputStream(), true)
                val inValue = BufferedReader(InputStreamReader(socket.getInputStream()))

                out.println(message)

                try {
                    socket.soTimeout = 2000
                    if (checkResponse(inValue.readLine(), patternMatching, patternType)) {
                       Pair(true, "Pattern found")
                    } else {
                       Pair(false, "Pattern not found")
                    }
                } catch (e: SocketTimeoutException) {
                    Pair(false, e.message ?: "Connection established, read timeout")
                } finally {
                    socket.close()
                }
            } else {
                socket.close()
                Pair(false, "Could not establish connection")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(false, e.message ?: "Unknown error")
        }
    }
}