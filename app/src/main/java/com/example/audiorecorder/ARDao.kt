package com.example.audiorecorder

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update


@Dao
interface ARDao {

    @Query("SELECT * FROM audioRecords")
    fun getAll(): List<AudioRecord>

    @Insert
    fun Insert(vararg audioRecord: AudioRecord)

    @Delete
    fun delete(audioRecord: AudioRecord)

    @Update
    fun update(audioRecord: AudioRecord)

}