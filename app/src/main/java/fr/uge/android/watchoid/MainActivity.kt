package fr.uge.android.watchoid

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.room.Room
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import fr.uge.android.watchoid.DAO.ServiceTestDao
import fr.uge.android.watchoid.entity.test.ServiceTest
import fr.uge.android.watchoid.games.seb.ChessGameScreen
import fr.uge.android.watchoid.space_invader.MenuScreen
import fr.uge.android.watchoid.ui.ActiveScreen
import fr.uge.android.watchoid.ui.components.GameScreen
import fr.uge.android.watchoid.ui.components.ServiceTestDetails
import fr.uge.android.watchoid.ui.components.ServiceTestForm
import fr.uge.android.watchoid.ui.components.ServiceTestList
import fr.uge.android.watchoid.ui.components.TestReportListScreen
import fr.uge.android.watchoid.ui.theme.WatchoidTheme
import fr.uge.android.watchoid.utils.deviceFunc
import fr.uge.android.watchoid.worker.BlueWorker
import fr.uge.space_invader.GameControls
import fr.uge.space_invader.GameLoop
import fr.uge.space_invader.GameScreen
import fr.uge.space_invader.initializeLevel
import fr.uge.space_invader.loadLevels
import fr.uge.space_invader.rememberGameState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    lateinit var watchoidDatabase: WatchoidDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // connect or create the database
        watchoidDatabase = Room.databaseBuilder(
            applicationContext,
            WatchoidDatabase::class.java,
            "watchoid_database"
        ).fallbackToDestructiveMigration().build()

        Log.i("TEST", "${deviceFunc().getBatteryLevel(applicationContext)}")
        Log.i("TEST", "${deviceFunc().getConnectionStatus(applicationContext)}")

        Log.i("TEST", "Notification channel created")
        createNotificationChannel(applicationContext, "Watchoid", NotificationManager.IMPORTANCE_LOW)
        createNotificationChannel(applicationContext, "Watchoid2", NotificationManager.IMPORTANCE_MAX)

        setContent {
            WatchoidTheme {
                Scaffold(modifier = Modifier
                    .fillMaxSize()
                    .padding(WindowInsets.statusBars.asPaddingValues())
                ) { innerPadding ->
                    MainView(
                        modifier = Modifier.padding(innerPadding),
                        watchoidDatabase.serviceTestDao()
                    )
                }
            }
        }
    }
}

fun createNotificationChannel(context: Context, channelId: String = "Watchoid", importanceNotif: Int) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val name = context.getString(R.string.channel_name)
        val descriptionText = context.getString(R.string.channel_description)
        val importance = importanceNotif
        val channel = NotificationChannel(channelId, name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}

// This is for testing database implementation
@Composable
fun MainView(modifier: Modifier = Modifier, dao: ServiceTestDao) {
    var reloadTrigger by remember { mutableStateOf(false) }
    var activeScreen by remember { mutableStateOf(ActiveScreen.SERVICE_TESTS_LIST) }
    var selectedServiceTest by remember { mutableStateOf<ServiceTest?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    var gameResult by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    Column {
        TopBar(activeScreen) { activeScreen = it }

        when (activeScreen) {
            ActiveScreen.SERVICE_TESTS_LIST -> {
                reloadTrigger = !reloadTrigger // Reload tests when entering the list screen
                ServiceTestListScreen(coroutineScope, reloadTrigger, dao) {
                    selectedServiceTest = it
                    activeScreen = ActiveScreen.SERVICE_TEST_DETAILS
                }
            }
            ActiveScreen.SERVICE_TEST_DETAILS -> ServiceTestDetails(selectedServiceTest!!.id, dao, coroutineScope)
            ActiveScreen.SERVICE_TEST_CREATION -> ServiceTestForm(dao, coroutineScope) { st ->
                Log.i("INFO", "ServiceTest added: $st")
                if (st.target == "nasa.com") {
                    activeScreen = ActiveScreen.SPACE_INVADER
                } else {
                    activeScreen = ActiveScreen.SERVICE_TESTS_LIST
                }
            }
            ActiveScreen.SERVICE_TEST_HISTORY_ALL -> {
                TestReportListScreen(coroutineScope, dao)
            }
            ActiveScreen.SERVICE_TEST_HISTORY_DETAILS -> TODO()
            ActiveScreen.JEU_SEB -> {
                ChessGameScreen()
            }
            ActiveScreen.SPACE_INVADER -> {
                if (showMenu) {
                    MenuScreen(gameResult = gameResult) {
                        showMenu = false
                    }
                } else {
                    val gameState = rememberGameState()
                    val levels = loadLevels(LocalContext.current)
                    initializeLevel(gameState, levels[0])
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        Box(modifier = Modifier.padding(innerPadding)) {
                            GameScreen(gameState)
                            GameControls(gameState)
                            GameLoop(gameState) { result ->
                                gameResult = result
                                showMenu = true
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TopBar(activeScreen: ActiveScreen, onScreenChange : (ActiveScreen) -> Unit) {
    var nbClick by remember { mutableStateOf(0) }

    if (nbClick > 5) {
        nbClick = 0
        onScreenChange(ActiveScreen.JEU_SEB)
    }

    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .padding(16.dp)
            .clickable { nbClick++ }
    ) {
        Text(
            text = when (activeScreen) {
                ActiveScreen.SERVICE_TESTS_LIST -> "Service tests"
                ActiveScreen.SERVICE_TEST_DETAILS -> "Service test details"
                ActiveScreen.SERVICE_TEST_CREATION -> "Add a new test"
                ActiveScreen.SERVICE_TEST_HISTORY_ALL -> "Test report"
                ActiveScreen.SERVICE_TEST_HISTORY_DETAILS -> "Test report"
                ActiveScreen.JEU_SEB -> "Chess Game"
                ActiveScreen.SPACE_INVADER -> "Space Invader"
            },
            color = Color.White,
            fontSize = 20.sp,
            modifier = Modifier.weight(1f)
        )

        if (activeScreen == ActiveScreen.SERVICE_TESTS_LIST) {
            Row {
                IconButton(onClick = { onScreenChange(ActiveScreen.SERVICE_TEST_HISTORY_ALL) }) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Test history",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                IconButton(onClick = { onScreenChange(ActiveScreen.SERVICE_TEST_CREATION) }) {
                    Icon(
                        imageVector = Icons.Default.AddCircle,
                        contentDescription = "Add Service Test",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        } else {
            IconButton(onClick = { onScreenChange(ActiveScreen.SERVICE_TESTS_LIST) }) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}


@Composable
fun ServiceTestListScreen(coroutineScope: CoroutineScope, trigger: Boolean = false, dao: ServiceTestDao, onClickOnServiceTest: (ServiceTest) -> Unit = {}) {
    var serviceTests by remember { mutableStateOf<List<ServiceTest>>(emptyList()) }

    LaunchedEffect(trigger) { // Reload tests when reloadTrigger changes
        coroutineScope.launch {
            serviceTests = dao.getAllTests()
        }
    }

    ServiceTestList(serviceTests) {
        onClickOnServiceTest(it)
    }


}
