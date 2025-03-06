package fr.uge.android.watchoid

import fr.uge.android.watchoid.DAO.ServiceTestDao
import fr.uge.android.watchoid.entity.test.ServiceTest

import androidx.room.Database
import androidx.room.RoomDatabase
import fr.uge.android.watchoid.entity.test.TestReport

@Database(entities = [ServiceTest::class, TestReport::class], version = 6)
abstract class WatchoidDatabase : RoomDatabase() {
    abstract fun serviceTestDao(): ServiceTestDao
}

