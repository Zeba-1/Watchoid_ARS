package fr.uge.android.watchoid.DAO

import fr.uge.android.watchoid.entity.test.ServiceTest

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import fr.uge.android.watchoid.entity.report.TestReport
import fr.uge.android.watchoid.entity.test.TestStatus

@Dao
interface ServiceTestDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(serviceTest: ServiceTest)

    @Update
    suspend fun update(serviceTest: ServiceTest)

    @Delete
    suspend fun delete(serviceTest: ServiceTest)

    @Query("SELECT * FROM service_test WHERE id = :id")
    suspend fun getTestById(id: Int): ServiceTest?

    @Query("SELECT * FROM service_test")
    suspend fun getAllTests(): List<ServiceTest>

    // Test report
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTestReport(testReport: TestReport)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServiceTest(serviceTest: ServiceTest)


    @Query("SELECT * FROM test_report WHERE testId = :testId")
    suspend fun getTestReportByTestId(testId: Int): TestReport?

    @Query("SELECT * FROM test_report")
    suspend fun getAllTestReports(): List<TestReport>

    @Query("SELECT MAX(testId) FROM test_report")
    suspend fun getLastTestId(): Int?

    @Query("SELECT COUNT(*) FROM test_report WHERE testId = :testId AND isTestOk = :isTestOk")
    suspend fun getTestReportCountByName(testId: Int, isTestOk: Boolean =false): Int

    @Query("SELECT * FROM service_test WHERE name = :name")
    suspend fun getTestByName(name: String): ServiceTest?
}