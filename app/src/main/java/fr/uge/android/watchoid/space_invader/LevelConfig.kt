package fr.uge.space_invader

import android.content.Context
import androidx.compose.ui.unit.dp
import org.json.JSONObject
import java.io.InputStream

data class LevelConfig(val level: Int, val enemyCount: Int, val wallCount:Int, val enemyLife: Int, val wallLife: Int)

fun parseLevelConfig(json: String): List<LevelConfig> {
    val jsonObject = JSONObject(json)
    val levelsArray = jsonObject.getJSONArray("levels")
    val levels = mutableListOf<LevelConfig>()

    for (i in 0 until levelsArray.length()) {
        val levelObject = levelsArray.getJSONObject(i)
        val level = levelObject.getInt("level")
        val enemyCount = levelObject.getInt("enemyCount")
        val wallCount = levelObject.getInt("wallCount")
        val enemyLife = levelObject.getInt("enemyLife")
        val wallLife = levelObject.getInt("wallLife")
        levels.add(LevelConfig(level, enemyCount, wallCount, enemyLife, wallLife))
    }

    return levels
}

fun loadLevels(context: Context): List<LevelConfig> {
    val inputStream: InputStream = context.assets.open("levels.json")
    val json = inputStream.bufferedReader().use { it.readText() }
    return parseLevelConfig(json)
}

fun initializeLevel(gameState: GameState, levelConfig: LevelConfig) {
    for (index in 0 until levelConfig.enemyCount) {
        val row = index / 6
        val col = index % 6
        val xOffset = (gameState.screenWidth / 10) * col
        val yOffset = (gameState.screenHeight / 20) * row

        val defaultEnemyX = gameState.screenWidth / (levelConfig.enemyCount - 1)
        val defaultEnemyY = 20.dp

        gameState.invaders.add(Invader(defaultEnemyX + xOffset, defaultEnemyY + yOffset, levelConfig.enemyLife))
    }

    for (index in 0 until levelConfig.wallCount) {
        val row = index / 8
        val col = index % 8
        val xOffset = (gameState.screenWidth / 10) * col
        val yOffset = (gameState.screenHeight / 2) * row

        val defaultWallX = gameState.screenWidth / (levelConfig.wallCount -1)
        val defaultWallY = gameState.screenHeight - (gameState.screenHeight / 3)

        gameState.walls.add(Wall(defaultWallX + xOffset, defaultWallY + yOffset, levelConfig.wallLife))
    }
}