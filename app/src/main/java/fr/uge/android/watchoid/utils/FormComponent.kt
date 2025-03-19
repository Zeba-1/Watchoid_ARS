package fr.uge.android.watchoid.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import fr.uge.android.watchoid.entity.test.ServiceTest
import fr.uge.android.watchoid.entity.test.TestType

@Composable
fun <T> DropDown(label: String, items: List<T>, selected: T?, onSelected: (T?) -> Unit) {
    var expended by remember { mutableStateOf(false) }

    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expended = true }
                .background(Color(0x25485C91), RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            Text(
                text = "${label}: ${selected?.toString() ?: "ALL"}",
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (expended) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
        DropdownMenu(
            expanded = expended,
            onDismissRequest = { expended = false }
        ) {
            // Option "ALL" pour tout afficher
            DropdownMenuItem(
                text = { Text("ALL") },
                onClick = {
                    onSelected(null)
                    expended = false
                }
            )
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item.toString()) },
                    onClick = {
                        onSelected(item)
                        expended = false
                    }
                )
            }
        }
    }
}
