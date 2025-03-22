package fr.uge.space_invader

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Composable
fun GameControls(gameState: GameState) {
    val density = LocalDensity.current
    Box(modifier = Modifier
        .fillMaxSize()
        .pointerInput(Unit) {
            detectDragGestures { change, _ ->
                with(density) {
                    gameState.player.value = gameState.player.value.copy(
                        x = (change.position.x.toDp() - 10.dp),
                        y = (change.position.y.toDp() - 10.dp)
                    )
                }
            }
        }
    )
}