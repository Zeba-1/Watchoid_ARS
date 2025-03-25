package fr.uge.android.watchoid.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import fr.uge.android.watchoid.DAO.ServiceTestDao
import fr.uge.android.watchoid.entity.test.PaternType
import fr.uge.android.watchoid.entity.test.ServiceTest
import fr.uge.android.watchoid.entity.test.TestStatus
import fr.uge.android.watchoid.entity.test.TestType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun ServiceTestForm(
    dao: ServiceTestDao,
    coroutineScope: CoroutineScope,
    onSubmit: (ServiceTest) -> Unit,
) {
    var showMiniGame by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(TestType.PING) }
    var target by remember { mutableStateOf("") }
    var periodicity by remember { mutableLongStateOf(0L) }
    var expandedType by remember { mutableStateOf(false) }
    var expandedPatern by remember { mutableStateOf(false) }
    var patern by remember { mutableStateOf("") }
    var paternType by remember { mutableStateOf(PaternType.CONTAINS) }
    var port by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf("") }


    if (showMiniGame) {
        GameScreen()
        return
        //RockPaperScisor()
    }

    Column (
        modifier = Modifier
            .padding(16.dp)
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        TextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        TextField(
            value = target,
            onValueChange = { target = it },
            label = { Text("Target") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        TextField(
            value = periodicity.toString(),
            onValueChange = { periodicity = it.toLongOrNull() ?: 0L },
            label = { Text("Periodicity") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expandedType = true }
                    .padding(8.dp)
            ) {
                Text(
                    text = "Type: ${type.name}",
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expandedType) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            DropdownMenu(
                expanded = expandedType,
                onDismissRequest = { expandedType = false }
            ) {
                TestType.entries.forEach { testType ->
                    DropdownMenuItem(
                        text = { Text(testType.name) },
                        onClick = {
                            type = testType
                            expandedType = false
                        }
                    )
                }
            }
        }

        if (type == TestType.UDP || type == TestType.TCP) {
            Spacer(modifier = Modifier.height(16.dp))
            TextField(
                value = port.toString(),
                onValueChange = { port = it.toIntOrNull() ?: 0 },
                label = { Text("Port") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
            TextField(
                value = message,
                onValueChange = { message = it },
                label = { Text("Message to send") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (type != TestType.PING) {
            Spacer(modifier = Modifier.height(16.dp))

            TextField(
                value = patern,
                onValueChange = { patern = it },
                label = { Text("Patern") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedPatern = true }
                        .padding(8.dp)
                ) {
                    Text(
                        text = "Patern type: ${paternType.name}",
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = if (expandedPatern) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                DropdownMenu(
                    expanded = expandedPatern,
                    onDismissRequest = { expandedPatern = false }
                ) {
                    PaternType.entries.forEach { _paternType ->
                        DropdownMenuItem(
                            text = { Text(_paternType.name) },
                            onClick = {
                                paternType = _paternType
                                expandedPatern = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
                val serviceTest = ServiceTest(
                    name = name,
                    type = type,
                    target = target,
                    periodicity = periodicity,
                    status = TestStatus.PENDING,
                    port = port,
                    patern = patern,
                    paternType = paternType,
                    message = message
                )
                coroutineScope.launch {
                    dao.insert(serviceTest)
                    onSubmit(serviceTest)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create")
        }
    }
    Spacer(modifier = Modifier.height(32.dp))

    // Invisible Button for Easter Egg
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(40.dp)
                .clickable {
                    showMiniGame = true
                }
        ) {}
    }
}
/*
@Composable
fun RockPaperScisor() {
    val options = listOf("Pierre", "Feuille", "Ciseaux")
    var playerChoice by remember { mutableStateOf<String?>(null) }
    var result by remember { mutableStateOf("") }
    var playerScore by remember { mutableIntStateOf(0) } // Score du joueur
    var computerScore by remember { mutableIntStateOf(0) } // Score de l'ordinateur

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(1.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Félicitations vous avez trouvé l'Easter Egg !\nPierre, Feuille, Ciseaux!", style = MaterialTheme.typography.titleMedium)

        // Affichage des scores
        Spacer(modifier = Modifier.height(8.dp))
        Text("Score - Joueur: $playerScore | Ordinateur: $computerScore", style = MaterialTheme.typography.bodyLarge)

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            options.forEach { choice ->
                Button(onClick = {
                    playerChoice = choice
                    val computerChoice = options[Random.nextInt(3)]
                    result = when {
                        playerChoice == computerChoice -> "Égalité !"
                        playerChoice == "Pierre" && computerChoice == "Ciseaux" -> {
                            playerScore++ // Le joueur gagne
                            "Gagné !"
                        }
                        playerChoice == "Feuille" && computerChoice == "Pierre" -> {
                            playerScore++ // Le joueur gagne
                            "Gagné !"
                        }
                        playerChoice == "Ciseaux" && computerChoice == "Feuille" -> {
                            playerScore++ // Le joueur gagne
                            "Gagné !"
                        }
                        else -> {
                            computerScore++ // L'ordinateur gagne
                            "Perdu : $computerChoice"
                        }
                    }
                }) {
                    Text(choice)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(result, style = MaterialTheme.typography.bodyLarge)
    }
}
*/


@Composable
fun CatchFallingObjectsGame() {

    // Variables du jeu
    var playerX by remember { mutableFloatStateOf(300f) }
    var score by remember { mutableIntStateOf(0) }
    var gameOver by remember { mutableStateOf(false) }
    var fallingObjects by remember { mutableStateOf(listOf<Pair<Float, Float>>()) }
    var fallingSpeed by remember { mutableFloatStateOf(5f) }
    var speedIncreaseTime by remember { mutableIntStateOf(0) }

    LaunchedEffect(key1 = true) {
        while (!gameOver) {
            delay(1000)
        }
        gameOver = true
    }

    LaunchedEffect(key1 = true) {
        while (!gameOver) {

            if (Random.nextInt(100) < 4) {
                val randomX = Random.nextInt(50, 550).toFloat()
                fallingObjects = fallingObjects + Pair(randomX, 0f)
            }

            fallingObjects = fallingObjects.map { (x, y) ->
                if (y < 1100f) Pair(x, y + fallingSpeed) else Pair(x, y)
            }.filter { (_, y) -> y < 1100f }


            if (score > speedIncreaseTime) {
                fallingSpeed += 0.3f
                speedIncreaseTime += 50
            }

            delay(50)
        }
    }

    LaunchedEffect(key1 = fallingObjects) {
        if (!gameOver) {
            fallingObjects.forEach { (x, y) ->
                if (y > 1030f && y < 1050f && x in (playerX - 50)..(playerX + 50)) {
                    score++
                    fallingObjects = fallingObjects.filterNot { it == Pair(x, y) }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                val maxWidth = size.width.toFloat()
                detectDragGestures { _, dragAmount ->
                    playerX += dragAmount.x
                    if (playerX < 50f) playerX = 50f
                    if (playerX > maxWidth) playerX = maxWidth
                }
            }
    ) {

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(Color.Black)

            drawRect(
                Color.Blue,
                size = androidx.compose.ui.geometry.Size(100f, 20f),
                topLeft = androidx.compose.ui.geometry.Offset(playerX - 50f, 1050f)
            )

            fallingObjects.forEach { (x, y) ->
                drawCircle(Color.Red, radius = 20f, center = androidx.compose.ui.geometry.Offset(x, y))
            }

            drawContext.canvas.nativeCanvas.apply {
                drawText("Score: $score", 20f, 1500f, android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 50f
                })
            }

            if (gameOver) {
                drawContext.canvas.nativeCanvas.apply {
                    drawText("Game Over! Final Score: $score", 20f, 700f, android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 75f
                    })
                }
            }
        }
    }

    LaunchedEffect(key1 = fallingObjects) {
        if (!gameOver) {
            fallingObjects.forEach { (_, y) ->
                if (y >= 1100f) {
                    gameOver = true
                }
            }
        }
    }
}

@Composable
fun GameScreen() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        CatchFallingObjectsGame()
        return@Surface
    }
}