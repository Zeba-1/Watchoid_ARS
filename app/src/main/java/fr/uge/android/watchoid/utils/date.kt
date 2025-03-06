package fr.uge.android.watchoid.utils

import android.app.DatePickerDialog
import android.app.Dialog
import androidx.fragment.app.DialogFragment
import android.content.Context
import android.content.DialogInterface
import android.icu.util.Calendar
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.DatePicker
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContentProviderCompat.requireContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun convertEpochToDate(epoch: Long): String {
     if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val instant = Instant.ofEpochMilli(epoch)
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault())
        return formatter.format(instant)
    } else {
        return epoch.toString()
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DatePickerField(label: String, selectedDate: LocalDate?, formatter: DateTimeFormatter, onDateSelected: (LocalDate) -> Unit) {
    var showModal by remember { mutableStateOf(false) }

    if (showModal) {
        DatePickerModal(
            onDateSelected = {
                if (it != null) {
                    onDateSelected(
                        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                    )
                }
            },
            onDismiss = {
                showModal = false
            }
        )
    }
    Button(
        onClick = {
            showModal = true
            Log.i("DatePickerField", "clickable")
        }
    ) {
        Text(text = (label + selectedDate?.format(formatter)))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerModal(
    onDateSelected: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val calendar = Calendar.getInstance()
            calendar.set(year, month, dayOfMonth)
            Log.i("DatePickerModal", "Date selected: ${calendar.time}")
            onDateSelected(calendar.timeInMillis)
            onDismiss()
        },
        Calendar.getInstance().get(Calendar.YEAR),
        Calendar.getInstance().get(Calendar.MONTH),
        Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
    )

    datePickerDialog.setButton(
        DialogInterface.BUTTON_NEGATIVE,
        "Cancel"
    ) { _, _ -> onDismiss() }

    datePickerDialog.show()
}