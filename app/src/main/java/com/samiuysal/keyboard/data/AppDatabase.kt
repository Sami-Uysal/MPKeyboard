package com.samiuysal.keyboard.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.samiuysal.keyboard.data.password.PasswordDao
import com.samiuysal.keyboard.data.password.PasswordEntity

@Database(entities = [PasswordEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun passwordDao(): PasswordDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE
                    ?: synchronized(this) {
                        val instance =
                                Room.databaseBuilder(
                                                context.applicationContext,
                                                AppDatabase::class.java,
                                                "keyboard_database"
                                        )
                                        .build()
                        INSTANCE = instance
                        instance
                    }
        }
    }
}
