package fr.uge.android.watchoid

import fr.uge.android.watchoid.DAO.ServiceTestDao
import fr.uge.android.watchoid.entity.test.ServiceTest

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ServiceTest::class], version = 1)
abstract class WatchoidDatabase : RoomDatabase() {
    abstract fun serviceTestDao(): ServiceTestDao
}