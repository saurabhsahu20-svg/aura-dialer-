package com.scall.phone.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.scall.phone.data.PhoneRepository
import com.scall.phone.data.SimPreferences
import com.scall.phone.data.openMobileNetworkSettings

/** Transparent per-feature SIM configuration: only voice calls can be routed by this dialer. */
@Composable
internal fun SimSettingsScreen(repository: PhoneRepository, preferences: SimPreferences) {
    val context = LocalContext.current
    var voiceChoice by remember { mutableStateOf(preferences.voiceChoice()) }
    var smsChoice by remember { mutableStateOf(preferences.smsChoice()) }
    var refresh by remember { mutableIntStateOf(0) }
    val voice = remember(refresh) { repository.availableSimAccounts() }
    val subscriptions = remember(refresh) { repository.availableSmsSubscriptions() }
    val dataId = remember(refresh) { repository.defaultDataSubscription() }
    val smsId = remember(refresh) { repository.defaultSmsSubscription() }
    val dataLabel = subscriptions.firstOrNull { it.subscriptionId == dataId }?.label ?: "Android system setting"
    val smsLabel = subscriptions.firstOrNull { it.subscriptionId == smsId }?.label ?: "Ask in Messages / Android default"

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("SIM & network", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Choose how each SIM is used", color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = { refresh++ }) { Icon(Icons.Default.Refresh, "Refresh SIM information") }
            }
        }
        item {
            SimSectionCard(Icons.Default.Call, "Voice calls", "Choose a line once or pick before every call") {
                SimChoiceRow("Ask each time", "Best for two active SIMs", voiceChoice == SimPreferences.ASK) {
                    voiceChoice = SimPreferences.ASK
                    preferences.setVoiceChoice(voiceChoice)
                }
                if (voice.isEmpty()) {
                    Text("No calling lines available. Enable S call as the default phone app and allow phone permissions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                }
                voice.forEach { account ->
                    SimChoiceRow(account.label, "Use this specific phone account", voiceChoice == account.key) {
                        voiceChoice = account.key
                        preferences.setVoiceChoice(voiceChoice)
                    }
                }
                Text("Stored by phone account, not list position, so SIMs cannot be silently swapped by reordering.",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp))
            }
        }
        item {
            SimSectionCard(Icons.Default.Message, "Text messages", "System SMS default: $smsLabel") {
                SimChoiceRow("Ask when opening a message", "Choose a SIM suggestion", smsChoice == SimPreferences.ASK) {
                    smsChoice = SimPreferences.ASK
                    preferences.setSmsChoice(smsChoice)
                }
                SimChoiceRow("Use messaging app default", "Let Messages decide", smsChoice == SimPreferences.SYSTEM) {
                    smsChoice = SimPreferences.SYSTEM
                    preferences.setSmsChoice(smsChoice)
                }
                subscriptions.forEach { sim ->
                    SimChoiceRow(sim.label, "Suggest this SIM to Messages", smsChoice == sim.key) {
                        smsChoice = sim.key
                        preferences.setSmsChoice(smsChoice)
                    }
                }
                Text("Important: S call opens your messaging app. That app controls the actual sending SIM. Verify it before sending.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp))
                TextButton(onClick = { context.openMobileNetworkSettings() }, modifier = Modifier.padding(start = 10.dp)) {
                    Icon(Icons.Default.Settings, null, Modifier.size(17.dp))
                    Spacer(Modifier.width(9.dp))
                    Text("Change SMS SIM in system settings")
                }
            }
        }
        item {
            SimSectionCard(Icons.Default.DataUsage, "Mobile data", "System data SIM: $dataLabel") {
                Text("Android controls mobile data SIM selection. S call cannot switch it directly without special carrier or system privileges.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                Button(onClick = { context.openMobileNetworkSettings() },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    shape = RoundedCornerShape(16.dp)) {
                    Icon(Icons.Default.SimCard, null, Modifier.size(19.dp))
                    Spacer(Modifier.width(9.dp))
                    Text("Choose data SIM in Android settings")
                }
            }
        }
        item {
            Text("SIM availability and controls depend on your phone, carrier, and Android version.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(12.dp))
        }
    }
}

@Composable
private fun SimSectionCard(icon: ImageVector, title: String, detail: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().padding(vertical = 9.dp)) {
            Row(Modifier.padding(horizontal = 18.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                    Text(detail, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
            content()
        }
    }
}

@Composable
private fun SimChoiceRow(title: String, subtitle: String, selected: Boolean, onSelect: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onSelect)
        .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall)
        }
        RadioButton(selected = selected, onClick = onSelect)
    }
}
