package fr.uge.android.watchoid.ui.components

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.uge.android.watchoid.Action.ExecuteTest
import fr.uge.android.watchoid.DAO.ServiceTestDao
import fr.uge.android.watchoid.entity.report.TestReport
import fr.uge.android.watchoid.entity.test.ServiceTest
import fr.uge.android.watchoid.utils.DatePickerField
import fr.uge.android.watchoid.utils.convertEpochToDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TestReportListScreen(coroutineScope: CoroutineScope, dao: ServiceTestDao) {
    var testsReports by remember { mutableStateOf<List<TestReport>>(emptyList()) }
    var startDate by remember { mutableStateOf<LocalDate?>(null) }
    var endDate by remember { mutableStateOf<LocalDate?>(null) }
    var searchText by remember { mutableStateOf("") }
    var testNames by remember { mutableStateOf<List<String>>(emptyList()) }
    var filterMode by remember { mutableStateOf("ALL") } // "ALL", "OK", "KO"
    var isImportCompleted by remember { mutableStateOf(false) }
    var latestID by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            testsReports = dao.getAllTestReports().reversed()
            testNames = testsReports.map { testReport ->
                val test = dao.getTestById(testReport.testId)
                test?.name ?: "Unknown"
            }
            latestID = dao.getLastTestId()!!
        }
    }

    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault())

    val filteredReports = remember(searchText, startDate, endDate, testsReports, testNames, filterMode) {
        testsReports.zip(testNames).filter { (report, name) ->
            val reportDate = Instant.ofEpochMilli(report.timestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()

            val start = startDate ?: LocalDate.MIN
            val end = endDate ?: LocalDate.MAX

            val matchesDate = reportDate in start..end
            val matchesName = name.contains(searchText, ignoreCase = true)

            val matchesFilter = when (filterMode) {
                "OK" -> report.isTestOk
                "KO" -> !report.isTestOk
                else -> true // "ALL", pas de filtre
            }

            matchesDate && matchesName && matchesFilter
        }.map { it.first }
    }

    val filteredReportsWithName = getReportWithNames(dao, filteredReports)
    val contextWrite = LocalContext.current
    // Déclarer le lanceur de fichier au niveau du composable (pas dans le onClick)
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                exportToFile(contextWrite, uri, filteredReportsWithName)
            }
        }
    }

    var reports = emptyList<TestReport>();
    val contextRead = LocalContext.current
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                reports = readFileAndProcessReports(contextRead, it, dao, coroutineScope)  // Lire et traiter le fichier sélectionné
                val reportsWithIds = reports.map { report ->
                    // Ajouter un testId unique en l'incrémentant à chaque nouveau rapport
                    val newTestReport = report.copy(testId = latestID)
                    latestID += 1  // Incrémente l'ID pour le prochain test
                    newTestReport
                }
                testsReports = reportsWithIds
                isImportCompleted = true;
            }
        }
    )

    LaunchedEffect(isImportCompleted) {
        if (isImportCompleted) {
            // Iterer à travers chaque TestReport dans newTestReport
            Log.i("Insert Base", "In insert")
            reports.forEach { testReport ->
                coroutineScope.launch {
                    dao.insertTestReport(testReport) // Utilise 'testReport' ici
                    testsReports = dao.getAllTestReports().reversed()
                    testNames = testsReports.map { testReport ->
                        val test = dao.getTestById(testReport.testId)
                        test?.name ?: "Unknown"
                    }
                    isImportCompleted = false
                }
            }

        }
    }

    Scaffold(
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = {
                    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TITLE, "rapport_exporté.txt")
                    }
                    exportLauncher.launch(intent)
                }) {
                    Text("Exporter")
                }
                Button(onClick = {
                    importLauncher.launch(arrayOf("text/plain"))
                }) {
                    Text("Importer")
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)) {
            Row {
                DatePickerField(
                    label = "Start Date: ",
                    selectedDate = startDate,
                    formatter = formatter,
                    onDateSelected = { startDate = it }
                )
                DatePickerField(
                    label = "End Date: ",
                    selectedDate = endDate,
                    formatter = formatter,
                    onDateSelected = { endDate = it }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                // Barre de recherche par nom
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    label = { Text("Rechercher par nom") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            // Boutons de filtrage
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = { filterMode = "ALL" },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (filterMode == "ALL") Color.Gray else Color.LightGray
                    )
                ) {
                    Text("Tous",
                        color = Color.Black)
                }
                Button(
                    onClick = { filterMode = "OK" },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (filterMode == "OK") Color(0xFFDFF0D8) else Color.LightGray
                    )
                ) {
                    Text(text = "Tests OK",
                        color = Color.Black)
                }
                Button(
                    onClick = { filterMode = "KO" },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (filterMode == "KO") Color(0xFFF2DEDE) else Color.LightGray
                    )
                ) {
                    Text("Tests KO",
                        color = Color.Black)
                }
            }
            // Liste des tests filtrés
            LazyColumn(
                modifier = Modifier.fillMaxSize()
                    .padding(paddingValues)
            ) {
                items(filteredReports) { testReport ->
                    TestReportItem(testReport, coroutineScope, dao)
                }
            }
        }
    }
}

@Composable
fun TestReportItem(testReport: TestReport, coroutineScope: CoroutineScope, dao: ServiceTestDao) {
    var serviceTestName by remember { mutableStateOf("${testReport.testId}") }

    LaunchedEffect(testReport) {
        coroutineScope.launch {
            val test = dao.getTestById(testReport.testId)
            serviceTestName = test?.name ?: "Unknown"
        }
    }

    val backgroundColor = if (testReport.isTestOk) {
        Color(0xFFDFF0D8) // Light green
    } else {
        Color(0xFFF2DEDE) // Light red
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp)
            .background(backgroundColor, RoundedCornerShape(8.dp))
    ) {
        Column (modifier = Modifier.padding(16.dp)) {
            Text(
                text = serviceTestName,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Execution Time: ${convertEpochToDate(testReport.timestamp)}",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Response Time: ${testReport.responseTime} ms",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Info: ${testReport.info}",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun getReportWithNames(dao: ServiceTestDao, testReports: List<TestReport>): List<Pair<TestReport, String>> {
    var reportWithNames by remember { mutableStateOf<List<Pair<TestReport, String>>>(emptyList()) }

    LaunchedEffect(testReports) {
        val reports = testReports.map { report ->
            val test = dao.getTestById(report.testId)
            val testName = test?.name ?: "Unknown"
            report to testName
        }
        reportWithNames = reports
    }

    return reportWithNames
}

fun exportToFile(context: Context, uri: Uri, testReportsWithNames : List<Pair<TestReport, String>>) {
    try {
        context.contentResolver.openOutputStream(uri)?.use { outputStream: OutputStream ->
            val reportString = "Test: import_1\n" +
                    "Test OK\n" +
                    "Execution Time: 0001010101010\n" +
                    "Response Time: 64\n" +
                    "Info: import_1 no test\n" +
                    "-".repeat(20) + "\n"

            outputStream.write("$reportString\n".toByteArray())
            testReportsWithNames.forEach { (report, name) ->
                val isTestOk = if (report.isTestOk) "Test OK" else "Test KO"
/*
                val reportString = "Test: $name \n" +
                                    isTestOk + "\n" +
                                    "Execution Time: ${report.timestamp} \n" +
                                    "Response Time: ${report.responseTime} \n" +
                                    "Info: ${report.info}\n" +
                                    "-".repeat(20) + "\n"
*/

            }
        }
        println("Fichier exporté avec succès !")
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

// Fonction pour lire le fichier et l'ajouter à la base de données
fun readFileAndProcessReports(context : Context, uri: Uri, dao: ServiceTestDao, coroutineScope : CoroutineScope): List<TestReport> {
    try {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        val fileContent = inputStream?.bufferedReader().use { it?.readText() }

        fileContent?.let {
            // Diviser le fichier en plusieurs rapports, séparés par la ligne "--------------------"
            val reports = it.split("\n".repeat(2))  // On suppose que chaque rapport est séparé par 20 tirets

            val newReports = reports.mapNotNull { reportBlock ->
                createTestReportFromBlock(reportBlock)
            }

            return newReports;
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return emptyList();
}

// Fonction pour analyser chaque bloc et créer un TestReport
fun createTestReportFromBlock(block: String): TestReport? {
    val lines = block.split("\n")
    if (lines.size < 5) return null // Si le bloc est incomplet, ignorer ce bloc

    try {
        // Extraire les valeurs des lignes
        val nameLine = lines[0]
        val isTestOkLine = if (lines[1] == "Test OK") true else false
        val executionTimeLine = lines[2]
        val responseTimeLine = lines[3]
        val infoLine = lines[4]

        Log.i("parsing", "line 0 : " + lines[0] + "\n" +
                "line 1 : " + lines[1] + "\n" +
                "line 2 : " + lines[2] + "\n" +
                "line 3 : " + lines[3] + "\n" +
                "line 4 : " + lines[4] + "\n")
        // Parser les informations
        //val testName = nameLine.removePrefix("Test: ").trim()
        val isTestOk = isTestOkLine // Convertir "true"/"false" en Boolean
        val executionTime = executionTimeLine.removePrefix("Execution Time: ").trim().toLong()
        val responseTime = responseTimeLine.removePrefix("Response Time: ").trim().toLong()
        val info = infoLine.removePrefix("Info: ").trim()

        // Créer et retourner un TestReport
        return TestReport(
            testId = 0, // Si tu as un ID unique, tu peux le générer ici
            timestamp = executionTime, // Assumer que timestamp est l'heure d'exécution
            isTestOk = isTestOk,
            responseTime = responseTime,
            info = info
        )
    } catch (e: Exception) {
        e.printStackTrace()
        return null // Si quelque chose échoue, ignorer ce rapport
    }
}