package fr.uge.android.watchoid.entity.report

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import fr.uge.android.watchoid.entity.test.ServiceTest

@Entity(tableName = "test_report", foreignKeys = [ForeignKey(entity = ServiceTest::class, parentColumns = ["id"], childColumns = ["testId"], onDelete = ForeignKey.CASCADE)])
data class TestReport(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val testId: Int,
    val isTestOk: Boolean,
    val responseTime: Long,
    val info: String,
    val timestamp: Long = System.currentTimeMillis()
)
