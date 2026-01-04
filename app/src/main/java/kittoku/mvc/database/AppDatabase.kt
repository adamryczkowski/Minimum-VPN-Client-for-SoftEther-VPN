package kittoku.mvc.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import kittoku.mvc.model.StringListConverter
import kittoku.mvc.model.VpnProfile
import kittoku.mvc.model.VpnProfileDao

/**
 * Room database for the SoftEther Connect application.
 *
 * Contains the VPN profiles table and provides access to the DAO.
 */
@Database(
    entities = [VpnProfile::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(StringListConverter::class)
abstract class AppDatabase : RoomDatabase() {
    /**
     * Get the VPN profile DAO for database operations.
     */
    abstract fun vpnProfileDao(): VpnProfileDao

    companion object {
        private const val DATABASE_NAME = "softether_connect.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Get the singleton database instance.
         *
         * @param context Application context
         * @return The database instance
         */
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME,
            )
                .fallbackToDestructiveMigration(dropAllTables = false)
                .build()
        }

        /**
         * Create an in-memory database for testing.
         *
         * @param context Application context
         * @return An in-memory database instance
         */
        fun createInMemoryDatabase(context: Context): AppDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
            )
                .allowMainThreadQueries()
                .build()
        }
    }
}
