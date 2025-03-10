package fr.uge.android.watchoid.ui.components

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.uge.android.watchoid.DAO.ServiceTestDao
import fr.uge.android.watchoid.entity.report.TestReport
import fr.uge.android.watchoid.utils.DatePickerField
import fr.uge.android.watchoid.utils.convertEpochToDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TestReportListScreen(coroutineScope: CoroutineScope, dao: ServiceTestDao) {
    var testsReports by remember { mutableStateOf<List<TestReport>>(emptyList()) }
    var startDate by remember { mutableStateOf<LocalDate?>(null) }
    var endDate by remember { mutableStateOf<LocalDate?>(null) }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            testsReports = dao.getAllTestReports().reversed()
        }
    }

    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault())

    val filteredReports = testsReports.filter { report ->
        val reportDate = Instant.ofEpochMilli(report.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
        val start = startDate ?: LocalDate.MIN
        val end = endDate ?: LocalDate.MAX
        reportDate in start..end
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        Row {
            DatePickerField(
                label = "Start Date: ",
                selectedDate = startDate,
                formatter = formatter,
                onDateSelected = { startDate = it }
            )
            DatePickerField(
                label = "End Date: ",
                selectedDate = endDate,
                formatter = formatter,
                onDateSelected = { endDate = it }
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(filteredReports) { testReport ->
                TestReportItem(testReport, coroutineScope, dao)
            }
        }
    }
}

@Composable
fun TestReportItem(testReport: TestReport, coroutineScope: CoroutineScope, dao: ServiceTestDao) {
    var serviceTestName by remember { mutableStateOf("${testReport.testId}") }

    LaunchedEffect(testReport) {
        coroutineScope.launch {
            val test = dao.getTestById(testReport.testId)
            serviceTestName = test?.name ?: "Unknown"
        }
    }

    val backgroundColor = if (testReport.isTestOk) {
        Color(0xFFDFF0D8) // Light green
    } else {
        Color(0xFFF2DEDE) // Light red
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp)
            .background(backgroundColor, RoundedCornerShape(8.dp))
    ) {
        Column (modifier = Modifier.padding(16.dp)) {
            Text(
                text = serviceTestName,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Execution Time: ${convertEpochToDate(testReport.timestamp)}",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Response Time: ${testReport.responseTime} ms",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Info: ${testReport.info}",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}