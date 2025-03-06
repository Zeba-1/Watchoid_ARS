package fr.uge.android.watchoid.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.uge.android.watchoid.DAO.ServiceTestDao
import fr.uge.android.watchoid.entity.test.PaternType
import fr.uge.android.watchoid.entity.test.ServiceTest
import fr.uge.android.watchoid.entity.test.TestStatus
import fr.uge.android.watchoid.entity.test.TestType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

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
    var expandedType by remember { mutableStateOf(false) }
    var expandedPatern by remember { mutableStateOf(false) }
    var patern by remember { mutableStateOf("") }
    var paternType by remember { mutableStateOf(PaternType.CONTAINS) }
    var expanded by remember { mutableStateOf(false) }
    var port by remember { mutableStateOf(0) }
    var message by remember { mutableStateOf("") }

    Column (
        modifier = Modifier
            .padding(16.dp)
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        TextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        TextField(
            value = target,
            onValueChange = { target = it },
            label = { Text("Target") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        TextField(
            value = periodicity.toString(),
            onValueChange = { periodicity = it.toLongOrNull() ?: 0L },
            label = { Text("Periodicity") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expandedType = true }
                    .padding(8.dp)
            ) {
                Text(
                    text = "Type: ${type.name}",
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expandedType) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            DropdownMenu(
                expanded = expandedType,
                onDismissRequest = { expandedType = false }
            ) {
                TestType.entries.forEach { testType ->
                    DropdownMenuItem(
                        text = { Text(testType.name) },
                        onClick = {
                            type = testType
                            expandedType = false
                        }
                    )
                }
            }
        }

        if (type == TestType.UDP || type == TestType.TCP) {
            Spacer(modifier = Modifier.height(16.dp))
            TextField(
                value = port.toString(),
                onValueChange = { port = it.toIntOrNull() ?: 0 },
                label = { Text("Port") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
            TextField(
                value = message,
                onValueChange = { message = it },
                label = { Text("Message to send") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (type != TestType.PING) {
            Spacer(modifier = Modifier.height(16.dp))

            TextField(
                value = patern,
                onValueChange = { patern = it },
                label = { Text("Patern") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedPatern = true }
                        .padding(8.dp)
                ) {
                    Text(
                        text = "Patern type: ${paternType.name}",
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = if (expandedPatern) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                DropdownMenu(
                    expanded = expandedPatern,
                    onDismissRequest = { expandedPatern = false }
                ) {
                    PaternType.entries.forEach { _paternType ->
                        DropdownMenuItem(
                            text = { Text(_paternType.name) },
                            onClick = {
                                paternType = _paternType
                                expandedPatern = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
                val serviceTest = ServiceTest(
                    name = name,
                    type = type,
                    target = target,
                    periodicity = periodicity,
                    status = TestStatus.PENDING,
                    port = port,
                    patern = patern,
                    paternType = paternType,
                    message = message
                )
                coroutineScope.launch {
                    dao.insert(serviceTest)
                    onSubmit(serviceTest)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create")
        }
    }
}