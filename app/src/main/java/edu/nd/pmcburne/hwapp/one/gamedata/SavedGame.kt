package edu.nd.pmcburne.hwapp.one.gamedata

import androidx.room.Entity
import androidx.room.PrimaryKey

// This is one saved basketball game in the Room database
// Each object becomes one row in the "games" table
@Entity(tableName = "saved_games")
data class SavedGame(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val selectedDate: String,
    val gender: String,
    val awayTeam: String,
    val homeTeam: String,
    val awayScore: String,
    val homeScore: String,
    val statusText: String
)