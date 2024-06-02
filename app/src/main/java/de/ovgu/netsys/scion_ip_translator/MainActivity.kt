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
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.ovgu.netsys.scion_ip_translator.ui.theme.SCIONIPTranslatorTheme


class MainActivity : ComponentActivity() {
    interface Prefs {
        companion object {
            const val NAME: String = "connection"
            const val BIND_ADDRESS: String = "bind_address";
            const val END_HOST_PORT: String = "end_host_port";
            const val DAEMON: String = "daemon"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SCIONIPTranslatorTheme {
                Content(this)
            }
        }

        // TODO: Edit preferences
        val prefs = getSharedPreferences(Prefs.NAME, MODE_PRIVATE)
        val bindAddress = prefs.getString(Prefs.BIND_ADDRESS, "10.0.2.15");
        val endHostPort = prefs.getInt(Prefs.END_HOST_PORT, 30041);
        val daemon = prefs.getString(Prefs.DAEMON, "10.0.2.2:30255");
    }

    @Preview(showBackground = true)
    @Composable
    fun GreetingPreview() {
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
    val active = remember { mutableStateOf(false) }
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding -> (
            Column(
                modifier = Modifier.padding(innerPadding)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(innerPadding)
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
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    ConnectButton(active.value) {
                        active.value = !active.value
                        if (active.value) {
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
