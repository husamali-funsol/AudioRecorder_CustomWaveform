package com.example.audiorecorder

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase


@Database (entities = [AudioRecord::class], version = 1)
abstract class ARDatabase: RoomDatabase() {

    abstract fun audioRecordDao(): ARDao

    companion object{

        @Volatile
        private var INSTANCE: ARDatabase? = null

        fun getDB(context: Context): ARDatabase {
            return INSTANCE ?: synchronized(this){
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ARDatabase::class.java,
                    "audioRecordsDB"
                ).build()
                INSTANCE = instance
                instance
            }
        }

    }

}