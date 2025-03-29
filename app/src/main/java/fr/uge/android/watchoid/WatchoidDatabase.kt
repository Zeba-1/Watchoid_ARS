package fr.uge.android.watchoid

import android.content.Context
import fr.uge.android.watchoid.DAO.ServiceTestDao
import fr.uge.android.watchoid.entity.test.ServiceTest

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import fr.uge.android.watchoid.entity.report.TestReport

@Database(entities = [ServiceTest::class, TestReport::class], version = 13)
abstract class WatchoidDatabase : RoomDatabase() {
    abstract fun serviceTestDao(): ServiceTestDao

    companion object {
        @Volatile
        private var INSTANCE: WatchoidDatabase? = null

        fun getInstance(context: Context): WatchoidDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WatchoidDatabase::class.java,
                    "watchoid_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}