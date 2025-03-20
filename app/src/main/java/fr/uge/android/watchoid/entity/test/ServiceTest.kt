package fr.uge.android.watchoid.entity.test

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "service_test")
class ServiceTest (
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    var port: Int = 0,
    var name: String = "",
    var type: TestType = TestType.PING,
    var target: String = "",
    var periodicity: Long = 0,
    var patern: String = "", // use to check the response of http test
    var paternType: PaternType = PaternType.CONTAINS,
    var message: String = "test",
    var status: TestStatus = TestStatus.PENDING,
    var lastTest: Long = 0,
    var minBatteryLevel: Int = 0,
    var isNotification: Boolean = false,
    var connectionType: ConnectionType = ConnectionType.ALL,
)