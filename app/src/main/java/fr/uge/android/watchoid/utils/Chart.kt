package fr.uge.android.watchoid.utils

import android.text.TextPaint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        val textPaint = TextPaint().apply {
            color = Color.Black.toArgb()
            textSize = 12.sp.toPx()
        }

        // Draw horizontal lines and labels
        for (i in 0..4) {
            val y = i * maxBarHeight / 4
            drawLine(
                color = Color.Gray,
                start = androidx.compose.ui.geometry.Offset(0f, y),
                end = androidx.compose.ui.geometry.Offset(size.width, y)
            )
        }

        // Draw bars
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

        for (i in 0..4) {
            val y = i * maxBarHeight / 4
            drawContext.canvas.nativeCanvas.drawText(
                "${maxResponseTime * (4 - i) / 4}",
                0f,
                y + textPaint.textSize / 2,
                textPaint
            )
        }
    }
}