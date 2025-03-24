package fr.uge.android.watchoid.ui.components

import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import java.util.concurrent.TimeUnit

@Composable
fun ServiceTestForm(
    dao: ServiceTestDao,
    coroutineScope: CoroutineScope,
    onSubmit: (ServiceTest) -> Unit,
) {
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
}