package fr.uge.android.watchoid.utils

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp
import fr.uge.android.watchoid.entity.report.TestReport

@Composable
fun TestReportChart(reports: List<TestReport>) {
    val maxResponseTime = reports.maxOfOrNull { it.responseTime } ?: 0L

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        val barWidth = size.width / (reports.size * 2)
        val maxBarHeight = size.height

        reports.forEachIndexed { index, report ->
            val barHeight = (report.responseTime.toFloat() / maxResponseTime) * maxBarHeight
            val xOffset = index * 2 * barWidth

            translate(left = xOffset, top = maxBarHeight - barHeight) {
                drawRect(
                    color = Color.Blue,
                    size = Size(barWidth, barHeight)
                )
            }
        }
    }
}