package com.localguard.gallery.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.localguard.gallery.data.GuardSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: GuardSettings,
    onChange: ((GuardSettings) -> GuardSettings) -> Unit,
    onRescan: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Sensitivity", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Strict blurs more (more false positives); Relaxed only blurs clear-cut content.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                SensitivityPicker(settings.sensitivity) { s -> onChange { it.copy(sensitivity = s) } }
            }
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("Blur suggestive content") },
                supportingContent = { Text("Also blur revealing but non-explicit photos (swimwear, lingerie…)") },
                trailingContent = {
                    Switch(settings.blurSuggestive, { v -> onChange { it.copy(blurSuggestive = v) } })
                },
            )
            ListItem(
                headlineContent = { Text("Blur until scanned") },
                supportingContent = { Text("Keep new items blurred until the on-device check finishes") },
                trailingContent = {
                    Switch(settings.blurUntilScanned, { v -> onChange { it.copy(blurUntilScanned = v) } })
                },
            )
            HorizontalDivider()
            Column(Modifier.padding(16.dp)) {
                OutlinedButton(onClick = onRescan) { Text("Clear results & rescan everything") }
                Spacer(Modifier.height(16.dp))
                Text(
                    "Detection runs 100% offline with a MobileNetV4 model bundled in the app. " +
                        "Guard Gallery requests no internet permission. Automatic detection is not perfect — " +
                        "some content may be missed or wrongly blurred.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
