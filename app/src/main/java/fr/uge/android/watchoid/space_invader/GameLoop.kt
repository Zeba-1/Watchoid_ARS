package fr.uge.space_invader

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay


@Composable
fun GameLoop(gameState: GameState, onGameEnd: (Boolean) -> Unit) {
    LaunchedEffect(Unit) {
        while (true) {
            gameState.update()
            delay(16L)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            gameState.projectiles.add(Projectile(gameState.player.value.x + 8.dp, gameState.player.value.y, true))
            gameState.invaders.forEach { invader ->
                if ((0..6).random() == 0) gameState.projectiles.add(Projectile(invader.x + 8.dp, invader.y + 50.dp, false))
            }
            delay(300L)
        }
    }


    if (gameState.invaders.isEmpty()) {
        onGameEnd(true)
    } else if (gameState.player.value.lives == 0) {
        onGameEnd(false)
    }
}