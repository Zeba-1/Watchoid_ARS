package fr.uge.android.watchoid

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.room.Room
import fr.uge.android.watchoid.Action.ExecuteTest
import fr.uge.android.watchoid.DAO.ServiceTestDao
import fr.uge.android.watchoid.entity.test.ServiceTest
import fr.uge.android.watchoid.ui.ActiveScreen
import fr.uge.android.watchoid.ui.components.ServiceTestDetails
import fr.uge.android.watchoid.ui.components.ServiceTestForm
import fr.uge.android.watchoid.ui.components.ServiceTestList
import fr.uge.android.watchoid.ui.theme.WatchoidTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

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

// This is for testing database implementation
@Composable
fun MainView(modifier: Modifier = Modifier, dao: ServiceTestDao) {
    var reloadTrigger by remember { mutableStateOf(false) }
    var activeScreen by remember { mutableStateOf(ActiveScreen.SERVICE_TESTS_LIST) }
    var selectedServiceTest by remember { mutableStateOf<ServiceTest?>(null) }

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
            ActiveScreen.SERVICE_TEST_CREATION -> ServiceTestForm (dao, coroutineScope) { st ->
                Log.i("INFO", "ServiceTest added: $st")
                activeScreen = ActiveScreen.SERVICE_TESTS_LIST
            }
        }
    }
}

@Composable
fun TopBar(activeScreen: ActiveScreen, onScreenChange : (ActiveScreen) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .padding(16.dp)
    ) {
        Text(
            text = when (activeScreen) {
                ActiveScreen.SERVICE_TESTS_LIST -> "Service tests"
                ActiveScreen.SERVICE_TEST_DETAILS -> "Service test details"
                ActiveScreen.SERVICE_TEST_CREATION -> "Add a new test"
            },
            color = Color.White,
            fontSize = 20.sp,
            modifier = Modifier.weight(1f)
        )

        if (activeScreen == ActiveScreen.SERVICE_TESTS_LIST) {
            IconButton(onClick = { onScreenChange(ActiveScreen.SERVICE_TEST_CREATION) }) {
                Icon(
                    imageVector = Icons.Default.AddCircle,
                    contentDescription = "Add Service Test",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
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