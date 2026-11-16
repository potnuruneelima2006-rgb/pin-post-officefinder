package com.example.pinpostofficefinder

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PinDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePin(pin: SavedPin)

    @Query("SELECT * FROM saved_pins")
    suspend fun getAllPins(): List<SavedPin>

    @Query("DELETE FROM saved_pins WHERE pin = :pin")
    suspend fun deletePin(pin: String)
}
