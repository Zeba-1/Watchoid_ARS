package fr.uge.space_invader

import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class Player(val x: Dp, val y: Dp, var score: Int = 0, var lives: Int = 3)
data class Invader(var x: Dp, var y: Dp, var health: Int = 3)
data class Projectile(val x: Dp, var y: Dp, val isPlayer: Boolean)
data class Wall(val x: Dp, val y: Dp, var health: Int = 3)

@Composable
fun rememberGameState(): GameState {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val screenHeight = configuration.screenHeightDp

    val player = remember { mutableStateOf(Player((screenWidth.dp/2), screenHeight.dp - (screenHeight.dp/4),
        0,3)) }
    val invaders = remember { mutableStateListOf<Invader>() }
    val projectiles = remember { mutableStateListOf<Projectile>() }
    val walls = remember { mutableStateListOf<Wall>() }
    return GameState(player, invaders, projectiles, walls, screenWidth.dp, screenHeight.dp)
}

data class GameState(
    val player: MutableState<Player>,
    val invaders: SnapshotStateList<Invader>,
    val projectiles: SnapshotStateList<Projectile>,
    val walls: SnapshotStateList<Wall>,
    val screenWidth: Dp = 0.dp,
    val screenHeight: Dp = 0.dp,
    var invaderDirection: Int = -1
)

fun GameState.checkCollisions() {
    val hitInvaders = mutableSetOf<Invader>()
    val hitProjectiles = mutableSetOf<Projectile>()

    projectiles.forEach { projectile ->
        walls.forEach { wall ->
            if (projectile.x in wall.x..(wall.x + 50.dp) && projectile.y in wall.y..(wall.y + 50.dp) &&
                !hitProjectiles.contains(projectile)) {
                wall.health--
                hitProjectiles.add(projectile)
            }
        }

        invaders.forEach { invader ->
            if ((projectile.x in invader.x..(invader.x + 50.dp) && projectile.y in invader.y..(invader.y + 50.dp)) &&
                !hitProjectiles.contains(projectile)) {
                hitInvaders.add(invader)
                invader.health--
                hitProjectiles.add(projectile)
            }
        }

        if (!projectile.isPlayer && projectile.x in player.value.x..(player.value.x + 50.dp) && projectile.y in player.value.y..(player.value.y + 50.dp)) {
            player.value.lives--
            hitProjectiles.add(projectile)
        }
    }

    invaders.removeAll { it.health <= 0 }
    projectiles.removeAll(hitProjectiles)
    walls.removeAll { it.health <= 0 }
}

fun DestroyUselessProjectiles(gameState: GameState, width: Dp, height: Dp) {
    gameState.projectiles.removeAll { projectile ->
        projectile.y < 0.dp || projectile.y > height || projectile.x < 0.dp || projectile.x > width
    }
}

fun GameState.update() {
    invaders.forEach { invader ->
        invader.x += 5.dp * invaderDirection
    }

    if (invaders.any { it.x < 0.dp || it.x > screenWidth - 50.dp }) {
        invaderDirection *= -1
        invaders.forEach { invader ->
            invader.y += 8.dp
        }
    }

    projectiles.forEach { projectile ->
        if (projectile.isPlayer) {
            projectile.y -= 25.dp
        } else {
            projectile.y += 10.dp
        }
    }

    DestroyUselessProjectiles(this, screenWidth, screenHeight)
    checkCollisions()
}