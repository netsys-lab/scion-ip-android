package de.ovgu.netsys.scion_ip_translator

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue;
import androidx.compose.runtime.setValue;
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import de.ovgu.netsys.scion_ip_translator.ui.theme.SCIONIPTranslatorTheme
import kotlinx.coroutines.delay
import java.net.InetAddress
import java.net.UnknownHostException
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.launch
import org.intellij.lang.annotations.JdkConstants.HorizontalAlignment
import kotlin.math.ln
import kotlin.math.pow
import kotlin.text.*


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SCIONIPTranslatorTheme {
                Content(this)
            }
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun Preview() {
        SCIONIPTranslatorTheme {
            Content(this)
        }
    }

    fun enableTranslator() {
        val intent = VpnService.prepare(this@MainActivity)
        if (intent != null) {
            startActivityForResult(intent, 0)
        } else {
            onActivityResult(0, RESULT_OK, null)
        }
    }

    fun disableTranslator() {
        startService(getServiceIntent().setAction(ScionTranslatorService.ACTION_DISCONNECT));
    }

    override fun onActivityResult(request: Int, result: Int, data: Intent?) {
        super.onActivityResult(request, result, data)
        if (result == RESULT_OK) {
            startService(getServiceIntent().setAction(ScionTranslatorService.ACTION_CONNECT))
        }
    }

    private fun getServiceIntent(): Intent {
        return Intent(this, ScionTranslatorService::class.java)
    }
}

@Composable
fun Content(activity: MainActivity) {
    val configuration = ConfigurationManager.configuration
    val status by StatusManager.statusFlow.collectAsState(initial = Status())


    var bindAddress by remember { mutableStateOf(configuration.bindAddress) }
    var endhostPort by remember { mutableStateOf(configuration.endhostPort) }
    var bootstrapServer by remember { mutableStateOf(configuration.bootstrapServer) }
    var connectToDaemon by remember { mutableStateOf(configuration.connectToDaemon) }
    var remoteDaemon by remember { mutableStateOf(configuration.remoteDaemon) }
    var active by remember { mutableStateOf(false) }

    var bindAddressError by remember { mutableStateOf<String?>(null) }
    var endhostPortError by remember { mutableStateOf<String?>(null) }
    var bootstrapServerError by remember { mutableStateOf<String?>(null) }
    var remoteDaemonError by remember { mutableStateOf<String?>(null) }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding -> (
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(innerPadding).fillMaxWidth(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.scion_logo_gradient_notagline),
                        contentDescription = "SCION Logo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.width(96.dp)
                    )
                    Box() {
                        Text(
                            text = "SCION-IP Translator",
                        )
                    }
                }
                TextField(
                    value = bindAddress,
                    onValueChange = {
                        bindAddress = it
                        bindAddressError = if (isValidHost(it)) null else "Invalid host"
                        configuration.bindAddress = it
                    },
                    label = { Text(stringResource(R.string.bind_address)) },
                    isError = bindAddressError != null
                )
                TextField(
                    value = endhostPort,
                    onValueChange = {
                        endhostPort = it
                        endhostPortError = if (isValidPort(it)) null else "Invalid port"
                        configuration.endhostPort = it
                    },
                    label = { Text(stringResource(R.string.endhost_port)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = endhostPortError != null
                )
                TextField(
                    value = bootstrapServer,
                    onValueChange = {
                        bootstrapServer = it
                        bootstrapServerError = if (isValidServerAddress(it)) null else "Invalid address"
                        configuration.bootstrapServer = it
                    },
                    label = { Text(stringResource(R.string.bootstrap_server)) },
                    isError = bootstrapServerError != null,
                    enabled = !connectToDaemon
                )
                TextField(
                    value = remoteDaemon,
                    onValueChange = {
                        remoteDaemon = it
                        remoteDaemonError = if (isValidServerAddress(it)) null else "Invalid address"
                        configuration.remoteDaemon = it
                    },
                    label = { Text(stringResource(R.string.remote_daemon)) },
                    isError = remoteDaemonError != null,
                    enabled = connectToDaemon
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(15.dp),
                ) {
                    Switch(
                        checked = connectToDaemon,
                        onCheckedChange = {
                            connectToDaemon = it
                            configuration.connectToDaemon = it
                        }
                    )
                    Text(text=stringResource(R.string.connect_to_daemon))
                }
                Row(
                    horizontalArrangement = Arrangement.Center,
                ) {
                    ConnectButton(active) {
                        val isValid = bindAddressError == null &&
                                endhostPortError == null &&
                                (bootstrapServerError == null || connectToDaemon) &&
                                (remoteDaemonError == null || !connectToDaemon)
                        if (isValid){
                            active = !active
                            if (active) {
                                StatusManager.reset()
                                activity.enableTranslator()
                            } else {
                                activity.disableTranslator()
                            }
                        }
                    }
                }
                StatusDisplay(status)
            })
    }
}

@Composable
fun ConnectButton(active: Boolean, onClick: ()->Unit) {
    Button(
        onClick = { onClick() },
        modifier = Modifier.fillMaxWidth(0.9F)
    ) {
        if (active) {
            Text("Disconnect")
        } else {
            Text("Connect")
        }
    }
}

@Composable
fun StatusDisplay(status: Status) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {
                        if (StatusManager.status.translationStatus == TranslationStatus.CONNECTED) {
                            coroutineScope.launch {
                                clipboardManager.setText(AnnotatedString(StatusManager.status.ipAddress))
                                Toast.makeText(context, "IP Address copied to clipboard", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Translation Status: ${status.translationStatus}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = when (status.translationStatus) {
                        TranslationStatus.CONNECTED -> Color.Green
                        TranslationStatus.CONNECTING -> Color.Yellow
                        TranslationStatus.DISCONNECTED -> Color.Red
                    }
                )
            }
            Divider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier.weight(0.5f)
                ) {
                    StatusItem(label = "Uploaded", value = formatBytes(status.uploadedBytes))
                    StatusItem(label = "Downloaded", value = formatBytes(status.downloadedBytes))
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.weight(1f)
                ) {
                    StatusItem(label = "Connection Time", value = formatConnectionTime(status.connectionTime), Alignment.End)
                    StatusItem(label = "IP Address", value = status.ipAddress, Alignment.End)
                }
            }
        }
    }
}

@Composable
fun StatusItem(label: String, value: String, horizontalAlignment: Alignment.Horizontal = Alignment.Start) {
    Column(horizontalAlignment = horizontalAlignment) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp, fontWeight = FontWeight.Normal)
        )
    }
}

fun formatConnectionTime(milliseconds: Long): String {
    val seconds = (milliseconds / 1000) % 60
    val minutes = (milliseconds / (1000 * 60)) % 60
    val hours = (milliseconds / (1000 * 60 * 60)) % 24
    return String.format("%02d:%02d:%02d", hours, minutes, seconds)
}

fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val exp = (ln(bytes.toDouble()) / ln(1024.0)).toInt()
    val prefix = "KMGTPE"[exp - 1]
    return String.format("%.1f %sB", bytes / 1024.0.pow(exp.toDouble()), prefix)
}

fun isValidHost(host: String): Boolean {
    return try {
        InetAddress.getByName(host)
        true
    } catch (e: Exception) {
        false
    }
}

fun isValidPort(port: String): Boolean {
    return port.toIntOrNull()?.let { it in 1..65535 } ?: false
}

fun splitAddress(address: String): Pair<String, String>? {
    // Check if the address contains brackets (indicating an IPv6 address)
    if (address.startsWith("[") && address.contains("]:")) {
        val host = address.substring(1, address.indexOf("]:"))
        val port = address.substring(address.indexOf("]:") + 2)
        return Pair(host, port)
    }
    // If no brackets, split normally (IPv4 or hostname)
    else if (address.contains(":")) {
        val parts = address.split(":")
        return Pair(parts[0], parts[1])
    }
    // If no colon, the address is not valid
    else {
        return null
    }
}

fun isValidServerAddress(address: String): Boolean {
    val parts = splitAddress(address) ?: return false
    val ipHost = parts.first
    val port = parts.second
    return isValidHost(ipHost) && isValidPort(port)
}