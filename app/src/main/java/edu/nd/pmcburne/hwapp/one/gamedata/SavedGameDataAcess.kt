package edu.nd.pmcburne.hwapp.one.gamedata

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

// This file will tell Room how to save, load, and delete saved games
@Dao
interface SavedGameDataAccess {

    @Query("SELECT * FROM saved_games WHERE selectedDate = :selectedDate AND gender = :gender")
    suspend fun getSavedGames(selectedDate: String, gender: String): List<SavedGame>

    @Query("DELETE FROM saved_games WHERE selectedDate = :selectedDate AND gender = :gender")
    suspend fun deleteSavedGames(selectedDate: String, gender: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedGames(games: List<SavedGame>)
}