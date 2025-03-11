package fr.uge.android.watchoid.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import fr.uge.android.watchoid.entity.test.ServiceTest
import androidx.compose.runtime.Composable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.uge.android.watchoid.Action.ExecuteTest
import fr.uge.android.watchoid.DAO.ServiceTestDao
import fr.uge.android.watchoid.entity.test.TestStatus
import fr.uge.android.watchoid.entity.test.TestType
import fr.uge.android.watchoid.utils.deviceFunc
import fr.uge.android.watchoid.utils.convertEpochToDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


@Composable
fun ServiceTestList(serviceTests: List<ServiceTest>, onServiceTestClick: (ServiceTest) -> Unit = {}) {
    LazyColumn {
        items(serviceTests) { serviceTest ->
            ServiceTestCard(serviceTest, onServiceTestClick)
        }
    }
}

@Composable
fun ServiceTestCard(serviceTest: ServiceTest, onClick: (ServiceTest) -> Unit = {}) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp)
            .background(Color(0x25485C91), RoundedCornerShape(8.dp))
            .clickable(onClick = { onClick(serviceTest) })
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row {
                    val (icon, color) = GetStatusIcon(serviceTest)
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = serviceTest.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = serviceTest.type.name,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Target: ${serviceTest.target}",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Last Execution: ${convertEpochToDate(serviceTest.lastTest)}",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun ServiceTestDetails(serviceTestId: Int, dao: ServiceTestDao, coroutineScope: CoroutineScope) {
    var isLoading by remember { mutableStateOf(true) }
    var serviceTest by remember { mutableStateOf(ServiceTest()) }
    val context = LocalContext.current

    LaunchedEffect(serviceTestId, serviceTest) {
        coroutineScope.launch {
            serviceTest = dao.getTestById(serviceTestId)!!
        }
        isLoading = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = serviceTest.name,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(8.dp))
                val (icon, color) = GetStatusIcon(serviceTest)
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row {
                Text(
                    text = "Type: ",
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = serviceTest.type.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row {
                Text(
                    text = "Target: ",
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = serviceTest.target,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row {
                Text(
                    text = "Periodicity: ",
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = serviceTest.periodicity.toString(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row {
                Text(
                    text = "Last execution: ",
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = convertEpochToDate(serviceTest.lastTest),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row {
                Text(
                    text = "Min battery level: ",
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = serviceTest.minBatteryLevel.toString(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row {
                Text(
                    text = "Connection required: ",
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = serviceTest.connectionType.toString(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row {
                Text(
                    text = "Notification: ",
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = serviceTest.isNotification.toString(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (serviceTest.type == TestType.UDP || serviceTest.type == TestType.TCP) {
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    Text(
                        text = "Port: ",
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = serviceTest.port.toString(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    Text(
                        text = "Message to send: ",
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = serviceTest.message,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (serviceTest.type != TestType.PING) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Patern (${serviceTest.paternType}): ",
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                TextField(
                    value = serviceTest.patern,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 100.dp)
                )
            }


            Spacer(modifier = Modifier.height(16.dp))
            Spacer(modifier = Modifier.weight(1f))

            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            isLoading = true
                            val batteryLevel = deviceFunc().getBatteryLevel(context)
                            val connectionDevice = deviceFunc().getConnectionStatus(context)
                            ExecuteTest(serviceTest, coroutineScope, dao, batteryLevel,connectionDevice ,true) {
                                serviceTest = ServiceTest()
                                isLoading = false

                                Toast.makeText(
                                    context,
                                    if (it) "Test pass" else "Test fail",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    },
                ) {
                    Text("Execute")
                }
                Button(
                    onClick = {
                        coroutineScope.launch {
                            dao.delete(serviceTest)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))
                ) {
                    Text("Delete")
                }
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x80000000))
                    .clickable(enabled = false) {}
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

fun GetStatusIcon(serviceTest: ServiceTest): Pair<ImageVector, Color> {
    return when (serviceTest.status) {
        TestStatus.PENDING -> Pair(Icons.Default.Info, Color(0xFF2196F3))
        TestStatus.SUCCESS -> Pair(Icons.Default.CheckCircle, Color(0xFF4CAF50))
        TestStatus.FAILURE -> Pair(Icons.Default.Warning, Color(0xFFF44336))
    }
}
