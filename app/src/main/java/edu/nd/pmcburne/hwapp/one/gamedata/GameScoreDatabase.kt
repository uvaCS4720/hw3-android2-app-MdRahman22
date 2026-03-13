package edu.nd.pmcburne.hwapp.one.gamedata

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// This is the main Room database class
// It creates the database and gives the app access to SavedGameDataAccess

@Database(entities = [SavedGame::class], version = 1, exportSchema = false)
abstract class GameScoreDatabase : RoomDatabase() {

    abstract fun savedGameDataAccess(): SavedGameDataAccess

    companion object {
        @Volatile
        private var INSTANCE: GameScoreDatabase? = null

        fun getDatabase(context: Context): GameScoreDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GameScoreDatabase::class.java,
                    "game_score_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}