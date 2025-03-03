package fr.uge.android.watchoid

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.room.Room
import fr.uge.android.watchoid.Action.ExecuteTest
import fr.uge.android.watchoid.DAO.ServiceTestDao
import fr.uge.android.watchoid.entity.test.ServiceTest
import fr.uge.android.watchoid.ui.components.ServiceTestForm
import fr.uge.android.watchoid.ui.components.ServiceTestList
import fr.uge.android.watchoid.ui.theme.WatchoidTheme
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
        ).build()

        setContent {
            WatchoidTheme {
                Scaffold(modifier = Modifier
                    .fillMaxSize()
                    .padding(WindowInsets.statusBars.asPaddingValues())
                ) { innerPadding ->
                    TEST(
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
fun TEST(modifier: Modifier = Modifier, dao: ServiceTestDao) {
    var reloadTrigger by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    var serviceTests by remember { mutableStateOf<List<ServiceTest>>(emptyList()) }

    LaunchedEffect(reloadTrigger) { // Reload tests when reloadTrigger changes
        coroutineScope.launch {
            serviceTests = dao.getAllTests()
        }
    }

    Column {
        ServiceTestForm (dao, coroutineScope) { st ->
            reloadTrigger = !reloadTrigger
            Log.i("INFO", "ServiceTest added: $st")
        }

        ServiceTestList(serviceTests) {
            Log.i("INFO", "ServiceTest clicked: $it")
            ExecuteTest(it, coroutineScope)
        }
    }
}