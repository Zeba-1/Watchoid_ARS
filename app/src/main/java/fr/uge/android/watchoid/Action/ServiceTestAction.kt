package fr.uge.android.watchoid.Action

import android.util.Log
import fr.uge.android.watchoid.entity.test.ServiceTest
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

fun ExecuteTest(serviceTest: ServiceTest, coroutineScope: CoroutineScope) {
    when(serviceTest.type) {
        TestType.HTTP -> {
            ExecuteHttpTest(serviceTest, coroutineScope)
        }
        TestType.PING -> {
            ExecutePingTest(serviceTest, coroutineScope)
        }
        TestType.UDP -> {
            ExecuteUdpTest(serviceTest, coroutineScope)
        }
        TestType.TCP -> {
            ExecuteTcpTest(serviceTest, coroutineScope)
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

fun ExecuteUdpTest(serviceTest: ServiceTest, coroutineScope: CoroutineScope) {
    coroutineScope.launch {
        Log.i("ServiceTest", "Executing UDP test on ${serviceTest.target}")
        val (isReachable, response) = UdpSend(serviceTest.target, "Get some UDP info", serviceTest.port)
        if (isReachable) {
            Log.i("ServiceTest", "${serviceTest.target} returned $response")
        } else {
            Log.i("ServiceTest", "${serviceTest.target} is not reachable or error")
        }
    }
}

suspend fun UdpSend(target: String, message: String, port: Int): Pair<Boolean, String> {
    return withContext(Dispatchers.IO) {
        try {
            val address = InetAddress.getByName(target)
            val socket = DatagramSocket()
            socket.soTimeout = 2000
            val buffer = message.toByteArray()
            val packet = DatagramPacket(buffer, buffer.size, address, port)
            println(socket.isConnected)
            socket.send(packet)

            val receiveData = ByteArray(1024)
            val receivePacket = DatagramPacket(receiveData, receiveData.size)

            try {
                socket.receive(receivePacket)
                val response = String(receivePacket.data, 0, receivePacket.length)
                socket.close()
                Pair(true, response)
            } catch (e: SocketTimeoutException) {
                socket.close()
                Pair(false, "Timeout")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(false, e.message ?: "Error")
        }
    }
}


fun ExecuteTcpTest(serviceTest: ServiceTest, coroutineScope: CoroutineScope) {
    coroutineScope.launch {
        Log.i("ServiceTest", "Executing TCP test on ${serviceTest.target}")
        val (isReachable, response) = TcpSend(serviceTest.target, "Get some TCP info", serviceTest.port)
        if (isReachable) {
            Log.i("ServiceTest", "${serviceTest.target} returned $response")
        } else {
            Log.i("ServiceTest", "${serviceTest.target} is not reachable or error")
        }
    }
}

suspend fun TcpSend(target: String,message:String ,port: Int): Pair<Boolean, String> {
    return withContext(Dispatchers.IO) {
        try {
            val socket = Socket()
            val socketAddress = InetSocketAddress(target, port)
            socket.connect(socketAddress, 3000)

            if (socket.isConnected) {
                val out = PrintWriter(socket.getOutputStream(), true)
                val inValue = BufferedReader(InputStreamReader(socket.getInputStream()))

                out.println(message)

                val response = try {
                    socket.soTimeout = 2000
                    inValue.readLine() ?: "Connection established, no data received"
                } catch (e: SocketTimeoutException) {
                    "Connection established, read timeout"
                }

                socket.close()
                Pair(true, response)
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
