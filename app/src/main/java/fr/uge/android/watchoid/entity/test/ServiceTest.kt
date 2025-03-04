package fr.uge.android.watchoid.entity.test

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "service_test")
class ServiceTest (
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: TestType,
    val target: String,
    val periodicity: Long,
    val status: TestStatus = TestStatus.PENDING
)