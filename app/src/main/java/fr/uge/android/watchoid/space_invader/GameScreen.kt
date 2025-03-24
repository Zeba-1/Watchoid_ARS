package fr.uge.space_invader

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GameScreen(gameState: GameState) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Box(
            modifier = Modifier
                .absoluteOffset(x = gameState.player.value.x, y = gameState.player.value.y)
                .size(20.dp)
                .background(Color.Blue)
        )

        gameState.invaders.forEach { invader ->
            Box(
                modifier = Modifier
                    .absoluteOffset(x = invader.x, y = invader.y)
                    .size(20.dp)
                    .background(
                        color = when (invader.health) {
                            3 -> Color.Red
                            2 -> Color.Yellow
                            else -> Color.Green
                        },
                        shape = CircleShape
                    )
            )
        }

        gameState.walls.forEach { wall ->
            Box(
                modifier = Modifier
                    .absoluteOffset(x = wall.x, y = wall.y)
                    .size(20.dp)
                    .background(
                        color = when (wall.health) {
                            3 -> Color.Red
                            2 -> Color.Yellow
                            else -> Color.Green
                        }
                    )
            )
        }

        gameState.projectiles.forEach { projectile ->
            Box(
                modifier = Modifier
                    .absoluteOffset(x = projectile.x, y = projectile.y)
                    .size(5.dp, 10.dp)
                    .background(if (projectile.isPlayer) Color.Green else Color.Yellow)
            )
        }

        Text(
            text = "Lives: ${gameState.player.value.lives}",
            color = Color.White,
            fontSize = 20.sp,
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp)
        )
    }
}