package de.ovgu.netsys.scion_ip_translator

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
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
    var active by remember { mutableStateOf(false) }
    var bindAddress by remember { mutableStateOf("192.168.200.25") }
    var endhostPort by remember { mutableStateOf("30041") }
    var bootstrapServer by remember { mutableStateOf("192.168.200.253:8041") }
    var connectToDaemon by remember { mutableStateOf(true) }
    var remoteDaemon by remember { mutableStateOf("192.168.200.253:30255") }
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding -> (
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
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
                    onValueChange = { bindAddress = it },
                    label = { Text(stringResource(R.string.bind_address)) }
                )
                TextField(
                    value = endhostPort,
                    onValueChange = { endhostPort = it },
                    label = { Text(stringResource(R.string.endhost_port)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                TextField(
                    value = bootstrapServer,
                    onValueChange = { bootstrapServer = it },
                    label = { Text(stringResource(R.string.bootstrap_server)) }
                )
                TextField(
                    value = remoteDaemon,
                    onValueChange = { remoteDaemon = it },
                    label = { Text(stringResource(R.string.remote_daemon)) }
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(15.dp),
                ) {
                    Switch(
                        checked = connectToDaemon,
                        onCheckedChange = {
                            connectToDaemon = it
                        }
                    )
                    Text(text=stringResource(R.string.connect_to_daemon))
                }
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    ConnectButton(active) {
                        active = !active
                        if (active) {
                            activity.enableTranslator()
                        } else {
                            activity.disableTranslator()
                        }
                    }
                }
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
