package fr.uge.android.watchoid.ui.components

import android.app.Activity
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
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
import fr.uge.android.watchoid.DAO.ServiceTestDao
import fr.uge.android.watchoid.entity.report.TestReport
import fr.uge.android.watchoid.entity.test.PaternType
import fr.uge.android.watchoid.entity.test.ServiceTest
import fr.uge.android.watchoid.entity.test.TestStatus
import fr.uge.android.watchoid.entity.test.TestType
import fr.uge.android.watchoid.utils.DatePickerField
import fr.uge.android.watchoid.utils.convertEpochToDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.io.InputStream
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
    var listTest by remember { mutableStateOf<List<ServiceTest>>(emptyList()) }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            testsReports = dao.getAllTestReports().reversed()
            testNames = testsReports.map { testReport ->
                val test = dao.getTestById(testReport.testId)
                test?.name ?: "Unknown"
            }
            latestID = dao.getLastTestId()!!
            listTest = dao.getAllTests();
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

    val filteredReportsWithAllInfo = getReportWithAllInfo(dao, filteredReports)
    val contextWrite = LocalContext.current
    // Déclarer le lanceur de fichier au niveau du composable (pas dans le onClick)
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                exportToFile(contextWrite, uri, filteredReportsWithAllInfo)
            }
        }
    }

    var reports = emptyList<TestReport>()
    val contextRead = LocalContext.current
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                reports = readFileAndProcessReports(contextRead, it, dao, coroutineScope, listTest)  // Lire et traiter le fichier sélectionné
                val reportsWithIds = reports.map { report ->
                    // Ajouter un testId unique en l'incrémentant à chaque nouveau rapport
                    val newTestReport = report.copy(testId = latestID)
                    latestID += 1  // Incrémente l'ID pour le prochain test
                    newTestReport
                }

                reports = reportsWithIds
                isImportCompleted = true
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
                    isImportCompleted = false
                }
            }
            val allReports = dao.getAllTestReports()
            allReports.toMutableList().addAll(reports)
            coroutineScope.launch {
                testsReports = allReports.reversed()
                Log.i("Get ALL ID", testsReports.toString())
                testNames = testsReports.map { testReport ->
                    val test = dao.getTestById(testReport.testId)
                    test?.name ?: "Unknown"
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
fun getReportWithAllInfo(dao: ServiceTestDao, testReports: List<TestReport>): Map<ServiceTest?, List<TestReport>> {
    var reportWithAllInfo by remember { mutableStateOf<Map<ServiceTest?, List<TestReport>>>(emptyMap()) }

    LaunchedEffect(testReports) {
        val allTests = dao.getAllTests().associateBy { it.id }

// Regroupe les TestReports par ServiceTest
        val reportsByTest: Map<ServiceTest?, List<TestReport>> = testReports.groupBy { report ->
            allTests[report.testId]
        }
        reportWithAllInfo = reportsByTest
    }
    return reportWithAllInfo
}

fun exportToFile(context: Context, uri: Uri, testReportsWithAllInfo : Map<ServiceTest?, List<TestReport>>) {
    val jsonBuilder = StringBuilder()
    jsonBuilder.append("[")

    testReportsWithAllInfo.entries.forEachIndexed { index, (test, reports) ->
        jsonBuilder.append("\n{")
        jsonBuilder.append("\"name\": ${test?.name}, \n")
        jsonBuilder.append("\"port\": ${test?.port},\n")
        jsonBuilder.append("\"message\": \"${test?.message?.takeIf { it.isNotEmpty() } ?: "unknown"}\",\n")
        jsonBuilder.append("\"type\": \"${test?.type}\",\n")
        jsonBuilder.append("\"target\": \"${test?.target}\",\n")
        jsonBuilder.append("\"periodicity\": ${test?.periodicity},\n")
        jsonBuilder.append("\"patern\": \"${test?.patern?.takeIf { it.isNotEmpty() } ?: "null"}\",\n")
        jsonBuilder.append("\"paternType\": \"${test?.paternType}\",\n")
        jsonBuilder.append("\"message\": \"${test?.message?.takeIf { it.isNotEmpty() } ?: "null"}\",\n")
        jsonBuilder.append("\"status\": \"${test?.status}\",\n")
        jsonBuilder.append("\"lastTest\": ${test?.lastTest},\n")

        jsonBuilder.append("\"reports\": [\n")
        reports.forEachIndexed { reportIndex, report ->
            jsonBuilder.append("\t{")
            jsonBuilder.append("\"reportId\": ${report.testId},\n")
            jsonBuilder.append("\t\"testOk\": \"${report.isTestOk}\",\n")
            jsonBuilder.append("\t\"responseTime\": ${report.responseTime},\n")
            jsonBuilder.append("\t\"info\": \"${report.info}\",\n")
            jsonBuilder.append("\t\"timestamp\": ${report.timestamp}")
            jsonBuilder.append("}")

            if (reportIndex != reports.size - 1) jsonBuilder.append(",")
            jsonBuilder.append("\n")
        }
        jsonBuilder.append("]")

        jsonBuilder.append("}")

        if (index != testReportsWithAllInfo.size - 1) jsonBuilder.append(",")
    }

    jsonBuilder.append("]")
    // Écriture dans le fichier
    try {
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            outputStream.write(jsonBuilder.toString().toByteArray())
        }
        println("Fichier exporté avec succès ! 🚀")
    } catch (e: Exception) {
        e.printStackTrace()
        println("Erreur lors de l'export : ${e.message}")
    }
}

// Fonction pour lire le fichier et l'ajouter à la base de données
fun readFileAndProcessReports(context : Context, uri: Uri, dao: ServiceTestDao, coroutineScope : CoroutineScope, listTest: List<ServiceTest>): List<TestReport> {
    /*
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
    */

    val mapImportFound: MutableMap<ServiceTest, MutableList<TestReport>> = mutableMapOf()

    try {

        // Lire le contenu du fichier
        val inputStream = context.contentResolver.openInputStream(uri)
        val jsonContent = inputStream?.bufferedReader().use { it?.readText() } ?: return emptyList()

        // Parser le JSON manuellement
        val jsonArray = JSONArray(jsonContent)

        for (i in 0 until jsonArray.length()) {
            val testJson = jsonArray.getJSONObject(i)

            // Extraction des infos du test
            val name = testJson.optString("name", "Unknown")
            val port = testJson.optInt("port", 0)
            val type = testJson.optString("type", "N/A")
            val target = testJson.optString("target", "N/A")
            val periodicity = testJson.optInt("periodicity", 0)
            val pattern = testJson.optString("patern", "")
            val patternType = testJson.optString("paternType", "")
            val message = testJson.optString("message", "")
            val status = testJson.optString("status", "")
            val lastTest = testJson.optLong("lastTest", 0)

            val serviceTest = ServiceTest(
                id = 0,  // Laisser la base générer l'ID
                name = name,
                port = port,
                type = parsingType(type),
                target = target,
                periodicity = periodicity.toLong(),
                patern = pattern,
                paternType = parsingPaternType(patternType),
                message = message,
                status = parsingStatus(status),
                lastTest = lastTest
            )
            // Vérifier si le test existe déjà
            val existingTest = listTest.find { it.name == serviceTest.name || it.lastTest == serviceTest.lastTest  }
            val testKey = existingTest ?: serviceTest

            Log.i("Import parsing testExists ?", existingTest.toString())

            if (existingTest == null) {
                Log.i("Import parsing", "On ne devrait jamais être la pour l'instant")
                // Créer un nouveau ServiceTest si inexistant
                // ON SAUVEGARDE ON INSERA PLUS TARD
                // dao.insertServiceTest(existingTest)
                // ON DOIT RÉCUPÉRER L'ID pour les RAPPORTS
                // existingTest = dao.getTestByName(name)  // Récupérer l'ID après insertion
            }

            // Ajouter les rapports liés au test
            val reportsArray = testJson.optJSONArray("reports") ?: JSONArray()
            val reports = mutableListOf<TestReport>()

            for (j in 0 until reportsArray.length()) {
                val reportJson = reportsArray.getJSONObject(j)

                val isTestOk = reportJson.optBoolean("testOk", false)
                val responseTime = reportJson.optLong("responseTime", 0)
                val info = reportJson.optString("info", "")
                val timestamp = reportJson.optLong("timestamp", 0)

                val testReport = TestReport(
                    testId = testKey.id,
                    isTestOk = isTestOk,
                    responseTime = responseTime,
                    info = info,
                    timestamp = timestamp
                )
                // ON SAUVEGARDE ON AJOUTE PLUS TARD
                // dao.insertTestReport(testReport)
            }
            mapImportFound.merge(testKey, reports) { existingReports, newReports ->
                existingReports.apply { addAll(newReports) }
            }
        }

        Log.i("Fin Import", "Import terminé avec succès ! 🚀")
        Log.i("Fin Import", mapImportFound.toString())
    } catch (e: Exception) {
        e.printStackTrace()
        println("Erreur lors de l'import : ${e.message}")
    }
    return emptyList()
}

fun parsingType(elementToParse: String): TestType {
    return when (elementToParse) {
        "HTTP" -> TestType.HTTP
        "PING" -> TestType.PING
        "UDP" -> TestType.UDP
        "TCP" -> TestType.TCP
        else -> throw IllegalArgumentException("type inconnu: $elementToParse")
    }
}

fun parsingPaternType(elementToParse: String): PaternType {
    return when(elementToParse) {
        "CONTAINS" -> PaternType.CONTAINS
        "NOT_CONTAINS" -> PaternType.NOT_CONTAINS
        "EQUALS" -> PaternType.EQUALS
        "REGEX" -> PaternType.REGEX
        else -> throw IllegalArgumentException("paternType inconnu: $elementToParse")
    }
}

fun parsingStatus(elementToParse: String): TestStatus {
    return when(elementToParse) {
        "PENDING" -> TestStatus.PENDING
        "SUCCESS" -> TestStatus.SUCCESS
        "FAILURE" -> TestStatus.FAILURE
        else -> throw IllegalArgumentException("status inconnu: $elementToParse")
    }
}

// Fonction pour analyser chaque bloc et créer un TestReport
fun createTestReportFromBlock(block: String): TestReport? {
    val lines = block.split("\n")
    if (lines.size < 5) return null // Si le bloc est incomplet, ignorer ce bloc

    try {
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