package com.nearme.android.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nearme.android.data.IdentityStore
import com.nearme.android.service.ScanForegroundService
import com.nearme.core.ScanOutcome
import com.nearme.core.verdict.Verdict

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val identityStore = IdentityStore.get(applicationContext)

        setContent {
            MaterialTheme {
                NearMeScreen(
                    outcomes = identityStore.outcomes.collectAsState().value,
                    onRequestScanningStart = { startScanForegroundService() },
                    onRequestScanningStop = { stopScanForegroundService() },
                )
            }
        }
    }

    private fun startScanForegroundService() {
        val intent = Intent(this, ScanForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun stopScanForegroundService() {
        stopService(Intent(this, ScanForegroundService::class.java))
    }
}

private fun requiredPermissions(): Array<String> = buildList {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        add(Manifest.permission.BLUETOOTH_SCAN)
        add(Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        add(Manifest.permission.POST_NOTIFICATIONS)
    }
}.toTypedArray()

@Composable
private fun NearMeScreen(
    outcomes: List<ScanOutcome>,
    onRequestScanningStart: () -> Unit,
    onRequestScanningStop: () -> Unit,
) {
    var scanning by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        if (grants.values.all { it }) {
            scanning = true
            onRequestScanningStart()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("NEARME") }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = {
                    if (scanning) {
                        scanning = false
                        onRequestScanningStop()
                    } else {
                        permissionLauncher.launch(requiredPermissions())
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (scanning) "Stop scanning" else "Start scanning")
            }

            if (outcomes.isEmpty()) {
                Text("No devices tracked yet. Nothing leaves this device — all history stays local.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(outcomes.sortedByDescending { it.verdict.verdict.ordinal }) { outcome ->
                        DeviceCard(outcome)
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceCard(outcome: ScanOutcome) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = when (outcome.verdict.verdict) {
                Verdict.NORMAL -> Color(0xFFE6F4EA)
                Verdict.WORTH_NOTING -> Color(0xFFFFF4CE)
                Verdict.SUSPICIOUS -> Color(0xFFFCE4E4)
            },
        ),
    ) {
        Column(
            modifier = Modifier.padding(PaddingValues(16.dp)),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = outcome.verdict.verdict.name.replace('_', ' '),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(outcome.verdict.explanation, style = MaterialTheme.typography.bodyMedium)
            Text(
                "Device ${outcome.identity.id.take(8)} · ${outcome.identity.sightings.size} sighting(s)",
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}
