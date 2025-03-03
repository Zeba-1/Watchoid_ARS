package fr.uge.android.watchoid.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.uge.android.watchoid.DAO.ServiceTestDao
import fr.uge.android.watchoid.entity.test.ServiceTest
import fr.uge.android.watchoid.entity.test.TestType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun ServiceTestForm(
    dao: ServiceTestDao,
    couroutineScope: CoroutineScope,
    onSubmit: (ServiceTest) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(TestType.PING) }
    var target by remember { mutableStateOf("") }
    var periodicity by remember { mutableLongStateOf(0L) }
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(16.dp)) {
        TextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") }
        )

        Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Text(
                text = "Type: ${type.name}",
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true }
                    .padding(8.dp)
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                TestType.values().forEach { testType ->
                    DropdownMenuItem(
                        text = { Text(testType.name) },
                        onClick = {
                            type = testType
                            expanded = false
                        }
                    )
                }
            }
        }


        TextField(
            value = target,
            onValueChange = { target = it },
            label = { Text("Target") }
        )
        TextField(
            value = periodicity.toString(),
            onValueChange = { periodicity = it.toLongOrNull() ?: 0L },
            label = { Text("Periodicity") }
        )

        Button(onClick = {
            val serviceTest = ServiceTest(
                name = name,
                type = type,
                target = target,
                periodicity = periodicity
            )
            couroutineScope.launch {
                dao.insert(serviceTest)
                onSubmit(serviceTest)
            }
        }) {
            Text("Create")
        }
    }
}