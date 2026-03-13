package edu.nd.pmcburne.hwapp.one

import android.app.DatePickerDialog
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import edu.nd.pmcburne.hwapp.one.ui.theme.HWStarterRepoTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import androidx.compose.ui.platform.LocalContext
import android.util.Log
import androidx.compose.runtime.rememberCoroutineScope
import edu.nd.pmcburne.hwapp.one.network.ScoreApiClient
import kotlinx.coroutines.launch
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import androidx.compose.runtime.LaunchedEffect
import edu.nd.pmcburne.hwapp.one.gamedata.GameScoreDatabase
import edu.nd.pmcburne.hwapp.one.gamedata.SavedGame

// Data class - stores the game info shown on each card in the UI
data class GameCardData(
    val awayTeam: String,
    val homeTeam: String,
    val awayScore: String,
    val homeScore: String,
    val statusText: String
)

// Function - converts UI game card data into SavedGame objects for Room
fun convertGameCardDataListToSavedGameList(
    gameCardDataList: List<GameCardData>,
    selectedDate: String,
    gender: String
): List<SavedGame> {
    return gameCardDataList.map { gameCard ->
        SavedGame(
            selectedDate = selectedDate,
            gender = gender,
            awayTeam = gameCard.awayTeam,
            homeTeam = gameCard.homeTeam,
            awayScore = gameCard.awayScore,
            homeScore = gameCard.homeScore,
            statusText = gameCard.statusText
        )
    }
}

// Function - converts saved Room data back into UI game card data
fun convertSavedGameListToGameCardDataList(
    savedGameList: List<SavedGame>
): List<GameCardData> {
    return savedGameList.map { savedGame ->
        GameCardData(
            awayTeam = savedGame.awayTeam,
            homeTeam = savedGame.homeTeam,
            awayScore = savedGame.awayScore,
            homeScore = savedGame.homeScore,
            statusText = savedGame.statusText
        )
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HWStarterRepoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    BasketballScoresScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

// Main screen - shows the date picker, gender buttons, refresh button, loading spinner, and game list
@Composable
fun BasketballScoresScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val calendar = remember { Calendar.getInstance() }
    val formatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val scope = rememberCoroutineScope()
    val database = remember { GameScoreDatabase.getDatabase(context) }
    val savedGameDataAccess = remember { database.savedGameDataAccess() }

    var selectedDateText by remember {
        mutableStateOf(formatter.format(calendar.time))
    }

    var gender by remember { mutableStateOf("Men") }

    var isLoading by remember { mutableStateOf(false) }

    var games by remember { mutableStateOf(listOf<GameCardData>()) }

    suspend fun loadScores() {
        isLoading = true

        val storedGender = gender.lowercase()

        try {
            val parts = selectedDateText.split("-")
            val year = parts[0]
            val month = parts[1]
            val day = parts[2]

            val response = ScoreApiClient.api.getScores(
                gender = storedGender,
                year = year,
                month = month,
                day = day
            )

            val freshGames = parseGames(response)
            games = freshGames

            savedGameDataAccess.deleteSavedGames(selectedDateText, storedGender)

            savedGameDataAccess.insertSavedGames(
                convertGameCardDataListToSavedGameList(
                    gameCardDataList = freshGames,
                    selectedDate = selectedDateText,
                    gender = storedGender
                )
            )

            Log.d("API_TEST", "Fetched from API and saved to Room")
        } catch (e: Exception) {
            Log.e("API_TEST", "API failed, loading from Room", e)

            val savedGames = savedGameDataAccess.getSavedGames(
                selectedDate = selectedDateText,
                gender = storedGender
            )

            games = convertSavedGameListToGameCardDataList(savedGames)
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(selectedDateText, gender) {
        loadScores()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Basketball Scores",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                DatePickerDialog(
                    context,
                    { _, year, month, dayOfMonth ->
                        calendar.set(year, month, dayOfMonth)
                        selectedDateText = formatter.format(calendar.time)
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                ).show()
            }
        ) {
            Text("Date: $selectedDateText")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { gender = "Men" },
                modifier = Modifier.weight(1f)
            ) {
                Text("Men")
            }

            Button(
                onClick = { gender = "Women" },
                modifier = Modifier.weight(1f)
            ) {
                Text("Women")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Showing: $gender",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(12.dp))


        Button(
            onClick = {
                scope.launch {
                    loadScores()
                }
            }
        ) {
            Text("Refresh")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(games) { game ->
                GameCard(game = game)
            }
        }
    }
}

// Shows one game's info in a card
@Composable
fun GameCard(game: GameCardData) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = "${game.awayTeam} at ${game.homeTeam}",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(text = "Away Score: ${game.awayScore}")
            Text(text = "Home Score: ${game.homeScore}")
            Text(text = "Status: ${game.statusText}")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BasketballScoresPreview() {
    HWStarterRepoTheme {
        BasketballScoresScreen()
    }
}

// Function - takes the API response and turns it into game cards for the UI
fun parseGames(apiResponse: JsonObject): List<GameCardData> {
    val gameCards = mutableListOf<GameCardData>()
    val gamesList = apiResponse.getAsJsonArray("games") ?: return emptyList()

    // Go through each game in the array
    for (gameItem in gamesList) {
        try {
            val gameWrapper = gameItem.asJsonObject
            val gameData = gameWrapper.getAsJsonObject("game") ?: continue

            // Get away and home team objects
            val awayTeamData = gameData.getAsJsonObject("away")
            val homeTeamData = gameData.getAsJsonObject("home")

            // Get nested team name objects
            val awayNameData = awayTeamData?.getAsJsonObject("names")
            val homeNameData = homeTeamData?.getAsJsonObject("names")

            val awayTeamName = getJsonString(awayNameData, "short").ifBlank {
                getJsonString(awayNameData, "full")
            }

            val homeTeamName = getJsonString(homeNameData, "short").ifBlank {
                getJsonString(homeNameData, "full")
            }

            val awayTeamScore = getJsonString(awayTeamData, "score").ifBlank { "-" }
            val homeTeamScore = getJsonString(homeTeamData, "score").ifBlank { "-" }

            // Get game status info
            val gameState = getJsonString(gameData, "gameState").lowercase()
            val startTime = getJsonString(gameData, "startTime")
            val period = getJsonString(gameData, "currentPeriod")
            val clock = getJsonString(gameData, "contestClock")

            // Check if either team is the winner
            val awayWon = getJsonBoolean(awayTeamData, "winner")
            val homeWon = getJsonBoolean(homeTeamData, "winner")

            // Build the status text shown in the card
            val statusLabel = when (gameState) {
                "final" -> "Final"
                "live" -> {
                    if (period.isNotBlank() && clock.isNotBlank()) {
                        "$period $clock"
                    } else if (period.isNotBlank()) {
                        period
                    } else {
                        "Live"
                    }
                }
                else -> {
                    if (startTime.isNotBlank()) "Starts at $startTime" else "Upcoming"
                }
            }

            val finalAwayName = if (gameState == "final" && awayWon) {
                "$awayTeamName (W)"
            } else {
                awayTeamName
            }

            val finalHomeName = if (gameState == "final" && homeWon) {
                "$homeTeamName (W)"
            } else {
                homeTeamName
            }

            // Add one finished card object to the list
            gameCards.add(
                GameCardData(
                    awayTeam = finalAwayName,
                    homeTeam = finalHomeName,
                    awayScore = awayTeamScore,
                    homeScore = homeTeamScore,
                    statusText = statusLabel
                )
            )
        } catch (e: Exception) {
            Log.e("PARSE_GAMES", "Failed to parse one game", e)
        }
    }

    return gameCards
}

// Helper function - safely reads a String value from a JsonObject
fun getJsonString(jsonObject: JsonObject?, key: String): String {
    if (jsonObject == null || !jsonObject.has(key) || jsonObject.get(key).isJsonNull) return ""

    return try {
        jsonObject.get(key).asString
    } catch (e: Exception) {
        ""
    }
}

// Helper function - safely reads a Boolean value from a JsonObject
fun getJsonBoolean(jsonObject: JsonObject?, key: String): Boolean {
    if (jsonObject == null || !jsonObject.has(key) || jsonObject.get(key).isJsonNull) return false

    return try {
        jsonObject.get(key).asBoolean
    } catch (e: Exception) {
        false
    }
}