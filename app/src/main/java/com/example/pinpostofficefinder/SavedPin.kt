package com.example.pinpostofficefinder

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_pins")
data class SavedPin(
    @PrimaryKey val pin: String,
    val name: String
)
