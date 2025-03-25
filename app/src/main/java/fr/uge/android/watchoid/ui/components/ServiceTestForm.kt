package fr.uge.android.watchoid.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import fr.uge.android.watchoid.DAO.ServiceTestDao
import fr.uge.android.watchoid.entity.test.ConnectionType
import fr.uge.android.watchoid.entity.test.PaternType
import fr.uge.android.watchoid.entity.test.ServiceTest
import fr.uge.android.watchoid.entity.test.TestStatus
import fr.uge.android.watchoid.entity.test.TestType
import fr.uge.android.watchoid.entity.test.toNotificationPriority
import fr.uge.android.watchoid.utils.DropDown
import fr.uge.android.watchoid.utils.DropDownAll
import fr.uge.android.watchoid.worker.BlueWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.random.Random
import java.util.concurrent.TimeUnit

@Composable
fun ServiceTestForm(
    dao: ServiceTestDao,
    coroutineScope: CoroutineScope,
    onSubmit: (ServiceTest) -> Unit,
) {
    var showMiniGame by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(TestType.PING) }
    var target by remember { mutableStateOf("") }
    var periodicity by remember { mutableLongStateOf(0L) }
    var patern by remember { mutableStateOf("") }
    var paternType by remember { mutableStateOf(PaternType.CONTAINS) }
    var port by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf("")}
    var batteryLevel by remember { mutableIntStateOf(0) }
    var isNotification by remember { mutableStateOf(false)}
    var nBTestFailBeforeNotification by remember { mutableIntStateOf(0)}
    var connectionType by remember { mutableStateOf(ConnectionType.ALL)}
    var notifImportance by remember { mutableStateOf(NotificationManager.IMPORTANCE_DEFAULT) }

    var execCondExpended by remember { mutableStateOf(false) }
    var specificExpended by remember { mutableStateOf(false) }
    var notificationExpended by remember { mutableStateOf(false) }

    if (showMiniGame) {
        GameScreen()
        return
    }

    Column (
        modifier = Modifier
            .padding(16.dp)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = target,
            onValueChange = { target = it },
            label = { Text("Target") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = periodicity.toString(),
            onValueChange = { periodicity = it.toLongOrNull() ?: 0L },
            label = { Text("Periodicity") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        DropDown("Type", TestType.entries, type) {
            type = it
        }

        Spacer(modifier = Modifier.height(8.dp))

        //enable notification or not
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text(
                text = "Notification: ${if (isNotification) "Enabled" else "Disabled"}",
                modifier = Modifier.weight(1f)
            )
            Checkbox(
                modifier = Modifier.size(24.dp),
                checked = isNotification,
                onCheckedChange = { isNotification = it }
            )
        }

        
        // Notification details
        if (isNotification) {
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = nBTestFailBeforeNotification.toString(),
                onValueChange = { nBTestFailBeforeNotification = it.toIntOrNull() ?: 0
                    notificationExpended = !notificationExpended},
                label = { Text("Number test fail before notification") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            DropDown("Notification Importance", listOf("Low", "High"), notifImportance.toNotificationPriority()) {
                notifImportance = it.toNotificationPriority()
            }

        }

        // Test specific fields
        if (type != TestType.PING) {
            Spacer(modifier = Modifier.height(16.dp))
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { specificExpended = !specificExpended }
                ) {
                    Text(
                        text = "Test Specific Fields",
                        fontWeight = Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = if (specificExpended) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (specificExpended) {
            if (type == TestType.UDP || type == TestType.TCP) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = port.toString(),
                    onValueChange = { port = it.toIntOrNull() ?: 0 },
                    label = { Text("Port") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Message to send") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (type != TestType.PING) {
                Spacer(modifier = Modifier.height(8.dp))

                DropDown("Patern Type", PaternType.entries, paternType) {
                    paternType = it
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = patern,
                    onValueChange = { patern = it },
                    label = { Text("Patern") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Test execution conditions
        Spacer(modifier = Modifier.height(16.dp))
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { execCondExpended = !execCondExpended }
            ) {
                Text(
                    text = "Test Execution Conditions",
                    fontWeight = Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (execCondExpended) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (execCondExpended) {
            Spacer(modifier = Modifier.height(8.dp))

            Column {
                Slider(
                    value = batteryLevel.toFloat(),
                    onValueChange = { batteryLevel = it.toInt() },
                    valueRange = 0f..100f,
                    steps = 99,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Min Battery Level: $batteryLevel%",
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            DropDown("Connection Type", ConnectionType.entries, connectionType) {
                connectionType = it
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val serviceTest = ServiceTest(
                    name = name,
                    type = type,
                    target = target,
                    periodicity = periodicity,
                    status = TestStatus.PENDING,
                    port = port,
                    patern = patern,
                    paternType = paternType,
                    message = message,
                    minBatteryLevel = batteryLevel,
                    isNotification = isNotification,
                    connectionType = connectionType,
                    nBTestFailBeforeNotification = nBTestFailBeforeNotification,
                    notifcationImportance = notifImportance
                )
                coroutineScope.launch {
                    dao.insert(serviceTest)
                    onSubmit(serviceTest)
                }

                if(periodicity >= 15*60) {
                    val periodicWorkRequest = PeriodicWorkRequestBuilder<BlueWorker>(periodicity, TimeUnit.SECONDS)
                        .setInputData(workDataOf("testName" to name))
                        .build()
                    Log.i("INFO", "Scheduling periodic tests")

                    WorkManager.getInstance().enqueue(periodicWorkRequest)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create")
        }
    }
    Spacer(modifier = Modifier.height(32.dp))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(40.dp)
                .clickable {
                    showMiniGame = true
                }
        ) {}
    }
}

@Composable
fun CatchFallingObjectsGame() {

    var playerX by remember { mutableFloatStateOf(300f) }
    var score by remember { mutableIntStateOf(0) }
    var gameOver by remember { mutableStateOf(false) }
    var fallingObjects by remember { mutableStateOf(listOf<Pair<Float, Float>>()) }
    var fallingSpeed by remember { mutableFloatStateOf(5f) }
    var speedIncreaseTime by remember { mutableIntStateOf(0) }

    LaunchedEffect(key1 = true) {
        while (!gameOver) {
            delay(1000)
        }
        gameOver = true
    }

    LaunchedEffect(key1 = true) {
        while (!gameOver) {

            if (Random.nextInt(100) < 4) {
                val randomX = Random.nextInt(50, 550).toFloat()
                fallingObjects = fallingObjects + Pair(randomX, 0f)
            }

            fallingObjects = fallingObjects.map { (x, y) ->
                if (y < 1100f) Pair(x, y + fallingSpeed) else Pair(x, y)
            }.filter { (_, y) -> y < 1100f }


            if (score > speedIncreaseTime) {
                fallingSpeed += 0.3f
                speedIncreaseTime += 50
            }

            delay(50)
        }
    }

    LaunchedEffect(key1 = fallingObjects) {
        if (!gameOver) {
            fallingObjects.forEach { (x, y) ->
                if (y > 1030f && y < 1050f && x in (playerX - 50)..(playerX + 50)) {
                    score++
                    fallingObjects = fallingObjects.filterNot { it == Pair(x, y) }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                val maxWidth = size.width.toFloat()
                detectDragGestures { _, dragAmount ->
                    playerX += dragAmount.x
                    if (playerX < 50f) playerX = 50f
                    if (playerX > maxWidth) playerX = maxWidth
                }
            }
    ) {

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(Color.Black)

            drawRect(
                Color.Blue,
                size = androidx.compose.ui.geometry.Size(100f, 20f),
                topLeft = androidx.compose.ui.geometry.Offset(playerX - 50f, 1050f)
            )

            fallingObjects.forEach { (x, y) ->
                drawCircle(Color.Red, radius = 20f, center = androidx.compose.ui.geometry.Offset(x, y))
            }

            drawContext.canvas.nativeCanvas.apply {
                drawText("Score: $score", 20f, 1500f, android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 50f
                })
            }

            if (gameOver) {
                drawContext.canvas.nativeCanvas.apply {
                    drawText("Game Over! Final Score: $score", 20f, 700f, android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 75f
                    })
                }
            }
        }
    }

    LaunchedEffect(key1 = fallingObjects) {
        if (!gameOver) {
            fallingObjects.forEach { (_, y) ->
                if (y >= 1100f) {
                    gameOver = true
                }
            }
        }
    }
}

@Composable
fun GameScreen() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        CatchFallingObjectsGame()
        return@Surface
    }
}