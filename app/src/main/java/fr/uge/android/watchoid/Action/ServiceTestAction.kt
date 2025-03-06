package fr.uge.android.watchoid.Action

import android.util.Log
import fr.uge.android.watchoid.DAO.ServiceTestDao
import fr.uge.android.watchoid.entity.test.PaternType
import fr.uge.android.watchoid.entity.test.ServiceTest
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

fun ExecuteTest(serviceTest: ServiceTest, coroutineScope: CoroutineScope, dao: ServiceTestDao, userExecuted: Boolean = false, userAction: (Boolean) -> Unit = {}) {
    var testOk = true

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

fun ExecuteTests(tests: List<ServiceTest>, coroutineScope: CoroutineScope, dao: ServiceTestDao) {
    for (test in tests) {
        ExecuteTest(test, coroutineScope, dao)
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

        dao.update(serviceTest)

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
        val (isReachable, response) = HttpGet(serviceTest.target)

        if (!isReachable) {
            Log.i("ServiceTest", "${serviceTest.target} is not reachable or error")
            return@launch
        }

        var error = response.isEmpty()

        error = error || !checkPattern(response, serviceTest.patern, serviceTest.paternType, serviceTest.target)

        if (error) {
            serviceTest.status = TestStatus.FAILURE
        } else {
            Log.i("ServiceTest", "${serviceTest.name} succeded !")
            serviceTest.status = TestStatus.SUCCESS
        }
        dao.update(serviceTest)

        if (userExecuted) {
            userAction(!error)
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

fun ExecuteUdpTest(serviceTest: ServiceTest, coroutineScope: CoroutineScope, dao: ServiceTestDao,userExecuted: Boolean = false, userAction: (Boolean) -> Unit = {}) {
    coroutineScope.launch {
        Log.i("ServiceTest", "Executing UDP test on ${serviceTest.target}")
        val (isReachable, response) = UdpSend(serviceTest.target, serviceTest.message, serviceTest.port, serviceTest.patern, serviceTest.paternType)
        if (isReachable) {
            serviceTest.status = TestStatus.SUCCESS
            Log.i("ServiceTest", "${serviceTest.target} returned $response")
        } else {
            serviceTest.status = TestStatus.FAILURE
            Log.i("ServiceTest", "${serviceTest.target} is not reachable or error")
        }
        dao.update(serviceTest)

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
                if (checkPattern(String(receivePacket.data, 0, receivePacket.length), patternMatching, patternType, target)) {
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
        Log.i("ServiceTest", "Executing TCP test on ${serviceTest.target}")
        val (isReachable, response) = TcpSend(serviceTest.target, serviceTest.message, serviceTest.port, serviceTest.patern, serviceTest.paternType)
        if (isReachable) {
            serviceTest.status = TestStatus.SUCCESS
            Log.i("ServiceTest", "${serviceTest.target} returned $response")
        } else {
            serviceTest.status = TestStatus.FAILURE
            Log.i("ServiceTest", "${serviceTest.target} is not reachable or error")
        }

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
                    if (checkPattern(inValue.readLine(), patternMatching, patternType, target)) {
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

fun checkPattern(response: String, patern: String, paternType:PaternType, target:String): Boolean {
    when(paternType) {
        PaternType.CONTAINS -> {
            if (!response.contains(patern)) {
                Log.e("ServiceTest" ,"${response} for ${target} returned $response but does not contain ${patern}")
                return false
            }
        }
        PaternType.REGEX -> {
            if (!response.matches(patern.toRegex())) {
                Log.e("ServiceTest", "${response} for ${target} response not match ${patern}")
                return false
            }
        }
        PaternType.NOT_CONTAINS -> {
            if (response.contains(patern)) {
                Log.e("ServiceTest", "${response} for ${target} response contains ${patern}")
                return false
            }
        }
        PaternType.EQUALS -> {
            if (response != patern) {
                Log.e("ServiceTest", "${response} for ${target} returned a different response: $response")
                return false
            }
        }
    }
    return true
}