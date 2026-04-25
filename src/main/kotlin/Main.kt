import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.github.shiroedev2024.leaf.desktop.LeafException
import com.github.shiroedev2024.leaf.desktop.LeafWrapper
import com.github.shiroedev2024.leaf.desktop.delegate.AssetsCallback
import com.github.shiroedev2024.leaf.desktop.delegate.CoreListener
import com.github.shiroedev2024.leaf.desktop.delegate.LeafListener
import com.github.shiroedev2024.leaf.desktop.delegate.SubscriptionCallback
import com.github.shiroedev2024.leaf.desktop.model.LogLevel
import com.github.shiroedev2024.leaf.desktop.model.UpdateLeafPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.*

@Composable
@Preview
fun App() {
    var isCoreLoading by remember { mutableStateOf(false) }
    var isLeafLoading by remember { mutableStateOf(false) }

    var isCoreRunning by remember { mutableStateOf(LeafWrapper.getInstance().isCoreRunning) }
    var isLeafRunning by remember {
        mutableStateOf(
            if (LeafWrapper.getInstance().isCoreRunning) {
                LeafWrapper.getInstance().isLeafRunning
            } else {
                false
            }
        )
    }

    var showDialog by remember { mutableStateOf(false) }
    var lastError by remember { mutableStateOf<String?>(null) }

    var clientId by remember { mutableStateOf(loadClientId()) }

    LaunchedEffect(Unit) {
        val version = LeafWrapper.getInstance().version
        println("Core Version: $version")

        LeafWrapper.getInstance().initLogger(LogLevel.DEBUG, 1, "", false)

        val packageVersionStr = loadPackageVersion()
        val (major, minor, patch) = parseVersionParts(packageVersionStr) ?: Triple(1, 1, 1)

        LeafWrapper.getInstance().updateAssets(major, minor, patch, object : AssetsCallback {
            override fun onAssetsUpdated() {
                println("Assets updated successfully")
            }

            override fun onAssetsError(p0: LeafException?) {
                println("Assets error: $p0")
            }
        })

        if (System.getProperty("os.name").lowercase().contains("win")) {
            val programPath = LeafWrapper.getInstance().leafProgramPath
            println("Program Path: $programPath")
            // wintun path is program path parent + "wintun.dll"
            val wintunPath = File(programPath).parentFile.resolve("wintun.dll").absolutePath
            println("Wintun Path: $wintunPath")
            LeafWrapper.getInstance().setupWintun(wintunPath)
        }
    }

    LaunchedEffect(clientId) {
        launch(Dispatchers.IO) {
            saveClientId(clientId)
        }
    }

    LaunchedEffect(isCoreRunning) {
        if (isCoreRunning) {
            LeafWrapper.getInstance().setLeafPreferences(UpdateLeafPreferences().apply {
                logLevel = LogLevel.INFO
                isMemoryLogger = true
            })
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Information") },
            text = { Text(lastError ?: "Unknown error occurred") },
            confirmButton = {
                Button(onClick = { showDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    LeafWrapper.getInstance().addLeafListener(object : LeafListener {
        override fun onLeafStarting() {
            println("Leaf Starting")
            isLeafLoading = true
        }

        override fun onLeafStarted() {
            println("Leaf Started")
            isLeafRunning = true
            isLeafLoading = false
        }

        override fun onLeafStopped() {
            println("Leaf Stopped")
            isLeafRunning = false
            isLeafLoading = false
        }

        override fun onLeafReloaded() {
            println("Leaf Reloaded")
            isLeafLoading = false
        }

        override fun onLeafError(error: String?) {
            println("Leaf Error: $error")
            lastError = error
            showDialog = true
            isLeafRunning = false
            isLeafLoading = false
        }
    })

    LeafWrapper.getInstance().addCoreListener(object : CoreListener {
        override fun onCoreStarting() {
            println("Core Starting")
            isCoreLoading = true
        }

        override fun onCoreStarted() {
            println("Core Started")
            isCoreRunning = true
            isCoreLoading = false
        }

        override fun onCoreStopped() {
            println("Core Stopped")
            isCoreRunning = false
            isCoreLoading = false
            isLeafRunning = false
        }

        override fun onCoreError(error: String?) {
            println("Core Error: $error")
            isCoreLoading = false
            lastError = error
            showDialog = true
        }
    })

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            color = MaterialTheme.colors.background
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Surf Shield Desktop Client", style = MaterialTheme.typography.h5)

                Spacer(modifier = Modifier.height(24.dp))

                TextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = clientId,
                    onValueChange = { clientId = it },
                    label = { Text("Client ID (UUID)") },
                    singleLine = true,
                    enabled = !isLeafRunning && !isLeafLoading
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                        onClick = {
                            if (isCoreRunning) {
                                LeafWrapper.getInstance().stopCore(true)
                            } else {
                                LeafWrapper.getInstance().startCore(true)
                            }
                        },
                        enabled = !isCoreLoading && !isLeafRunning
                    ) {
                        if (isCoreLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text(if (isCoreRunning) "Stop Core" else "Start Core")
                        }
                    }

                    Button(
                        onClick = {
                            if (clientId.isBlank()) {
                                lastError = "Please enter a Client ID."
                                showDialog = true
                                return@Button
                            }

                            isLeafLoading = true
                            LeafWrapper.getInstance().updateSubscription(
                                -1, -1, clientId, 1, 1,
                                object : SubscriptionCallback {
                                    override fun onSubscriptionUpdating() {
                                        println("Subscription is updating...")
                                    }

                                    override fun onSubscriptionSuccess() {
                                        try {
                                            LeafWrapper.getInstance().verifyFileIntegrity()
                                            LeafWrapper.getInstance().startLeaf()
                                        } catch (e: LeafException) {
                                            lastError = e.message
                                            showDialog = true
                                            isLeafLoading = false
                                        }
                                    }

                                    override fun onSubscriptionError(error: String?) {
                                        lastError = "Subscription failed: $error"
                                        showDialog = true
                                        isLeafLoading = false
                                    }
                                }
                            )
                        },
                        enabled = isCoreRunning && !isLeafRunning && !isLeafLoading
                    ) {
                        if (isLeafLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Fetch & Connect")
                        }
                    }

                    Button(
                        onClick = {
                            LeafWrapper.getInstance().stopLeaf()
                        },
                        enabled = isLeafRunning
                    ) {
                        Text("Disconnect")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isLeafRunning) {
                    Text("Status: Connected", color = MaterialTheme.colors.primary)
                } else if (isCoreRunning) {
                    Text("Status: Core Ready", color = MaterialTheme.colors.secondary)
                } else {
                    Text("Status: Offline", color = MaterialTheme.colors.error)
                }
            }
        }
    }
}

fun getSavedClientIdPath(): String {
    return "${System.getProperty("user.home")}/.surfshield_client_id.txt"
}

fun loadClientId(): String {
    val file = File(getSavedClientIdPath())
    if (file.exists()) {
        return file.readText().trim()
    }
    return ""
}

fun saveClientId(id: String) {
    val file = File(getSavedClientIdPath())
    file.writeText(id.trim())
}

fun loadPackageVersion(): String {
    val props = Properties()
    val inputStream = object {}.javaClass.classLoader.getResourceAsStream("version.properties")
    if (inputStream != null) {
        props.load(inputStream)
        return props.getProperty("packageVersion") ?: ""
    }
    return ""
}

fun parseVersionParts(version: String): Triple<Int, Int, Int>? {
    val parts = version.split('.').map { it.toIntOrNull() }
    if (parts.size != 3 || parts.any { it == null }) {
        return null
    }
    return Triple(parts[0]!!, parts[1]!!, parts[2]!!)
}

fun main() = application {
    Window(
        onCloseRequest = {
            println("Exit requested")
            if (LeafWrapper.getInstance().isCoreRunning) {
                if (LeafWrapper.getInstance().isLeafRunning) {
                    LeafWrapper.getInstance().stopLeaf()
                }
                LeafWrapper.getInstance().stopCore(true)
            }
            exitApplication()
        },
        title = "Surf Shield Desktop"
    ) {
        App()
    }
}
