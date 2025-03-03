package fr.uge.android.watchoid.DAO

import fr.uge.android.watchoid.entity.test.ServiceTest

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

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
}