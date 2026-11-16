package com.scall.phone.ui

import android.content.Intent
import android.media.MediaPlayer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.scall.phone.data.RecordingStore
import com.scall.phone.data.PhoneRepository
import com.scall.phone.data.normalizeNumber
import com.scall.phone.data.SavedRecording
import com.scall.phone.data.canAutomaticallyRecordCellularCalls
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.text.DateFormat
import java.util.Date

/**
 * A transparent recording library, not a SIM call recorder.
 * User-controlled import, playback, rename, local deletion, sharing and cleanup.
 */
@Composable
internal fun RecordingSettingsScreen() {
    val context = LocalContext.current
    val store = remember { RecordingStore(context.applicationContext) }
    val repository = remember { PhoneRepository(context.applicationContext) }
    var contactsByNumber by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    val scope = rememberCoroutineScope()
    var recordings by remember { mutableStateOf<List<SavedRecording>>(emptyList()) }
    var search by remember { mutableStateOf("") }
    var isImporting by remember { mutableStateOf(false) }
    var refresh by remember { mutableIntStateOf(0) }
    var selectedForEdit by remember { mutableStateOf<SavedRecording?>(null) }
    var selectedForDelete by remember { mutableStateOf<SavedRecording?>(null) }
    var editTitle by remember { mutableStateOf("") }
    var editPhone by remember { mutableStateOf("") }
    var retention by remember { mutableIntStateOf(store.retentionDays()) }
    var retentionMenu by remember { mutableStateOf(false) }
    var cleanupConfirm by remember { mutableStateOf(false) }
    var playingId by remember { mutableStateOf<String?>(null) }
    var paused by remember { mutableStateOf(false) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }

    fun stopPlayback() {
        val active = player
        player = null
        playingId = null
        paused = false
        runCatching { active?.stop() }
        runCatching { active?.release() }
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { player?.release() }
            player = null
        }
    }

    LaunchedEffect(refresh) {
        recordings = store.all()
        contactsByNumber = repository.contacts()
            .associate { normalizeNumber(it.number).takeLast(10) to it.name }
    }

    val importPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null && !isImporting) {
            isImporting = true
            scope.launch {
                try {
                    val saved = store.importAudio(uri)
                    Toast.makeText(context, "Imported: ${saved.title}", Toast.LENGTH_SHORT).show()
                    refresh++
                } catch (error: Exception) {
                    Toast.makeText(
                        context, error.message ?: "Cannot import this recording",
                        Toast.LENGTH_LONG
                    ).show()
                } finally {
                    isImporting = false
                }
            }
        }
    }

    fun playOrPause(recording: SavedRecording) {
        if (playingId == recording.id) {
            try {
                if (paused) player?.start() else player?.pause()
                paused = !paused
            } catch (_: RuntimeException) {
                stopPlayback()
            }
            return
        }
        stopPlayback()
        val audio = store.playableFile(recording)
        if (audio == null) {
            Toast.makeText(context, "Recording file unavailable", Toast.LENGTH_SHORT).show()
            return
        }
        val mp = MediaPlayer()
        try {
            FileInputStream(audio).use { input ->
                mp.setDataSource(input.fd)
                mp.prepare()
            }
            mp.setOnCompletionListener {
                playingId = null
                paused = false
                player = null
                runCatching { mp.release() }
            }
            mp.start()
            player = mp
            playingId = recording.id
            paused = false
        } catch (_: Exception) {
            runCatching { mp.release() }
            Toast.makeText(context, "Audio playback failed", Toast.LENGTH_LONG).show()
        }
    }

    fun share(recording: SavedRecording) {
        val file = store.playableFile(recording) ?: return
        try {
            val uri = FileProvider.getUriForFile(
                context, "${context.packageName}.recordings", file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "audio/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share recording"))
        } catch (_: Exception) {
            Toast.makeText(context, "Cannot share this recording", Toast.LENGTH_LONG).show()
        }
    }

    if (selectedForEdit != null) {
        val selected = selectedForEdit!!
        AlertDialog(
            onDismissRequest = { selectedForEdit = null },
            title = { Text("Edit recording") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editTitle, onValueChange = { editTitle = it.take(90) },
                        label = { Text("Recording name") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                    OutlinedTextField(
                        value = editPhone, onValueChange = { editPhone = it.take(24) },
                        label = { Text("Phone number (optional)") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                    Text(
                        "Only the recording's private library details change. " +
                            "The original audio is kept unchanged.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        if (store.rename(selected.id, editTitle, editPhone)) refresh++
                        selectedForEdit = null
                    }
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { selectedForEdit = null }) { Text("Cancel") }
            }
        )
    }

    if (selectedForDelete != null) {
        val selected = selectedForDelete!!
        AlertDialog(
            onDismissRequest = { selectedForDelete = null },
            title = { Text("Delete recording?") },
            text = {
                Text(if (selected.source == "manual")
                    "Permanently delete ${selected.title} and its private audio file?"
                else "Remove ${selected.title} from S Call's library? The original imported file is unchanged.")
            },
            confirmButton = {
                TextButton(onClick = {
                    if (playingId == selected.id) stopPlayback()
                    scope.launch {
                        if (store.delete(selected.id)) refresh++
                        selectedForDelete = null
                    }
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { selectedForDelete = null }) { Text("Cancel") }
            }
        )
    }

    if (cleanupConfirm) {
        AlertDialog(
            onDismissRequest = { cleanupConfirm = false },
            title = { Text("Delete older imported recordings?") },
            text = {
                Text("This permanently removes S call's private copies older than " +
                    "$retention days. Original recordings in other apps are unaffected.")
            },
            confirmButton = {
                TextButton(onClick = {
                    cleanupConfirm = false
                    stopPlayback()
                    scope.launch {
                        val count = store.cleanupExpired(retention)
                        refresh++
                        Toast.makeText(context, "Removed $count old imported files", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("Clean up") }
            },
            dismissButton = {
                TextButton(onClick = { cleanupConfirm = false }) { Text("Cancel") }
            }
        )
    }

    val matches = recordings.filter {
        search.isBlank() ||
            it.title.contains(search, ignoreCase = true) ||
            it.phoneNumber.contains(search)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        item {
            Text(
                "S Caller · Recordings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold
            )
        }

        item {
            Card(colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )) {
                Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null)
                        Spacer(Modifier.width(9.dp))
                        Text("Cellular recording: Not available",
                            fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "Manual recording is available only on Android builds with authorized " +
                            "two-way cellular audio capture. Standard S Caller APKs do not have " +
                            "that privilege. Automatic incoming/outgoing recording remains OFF.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "Use your phone's built-in recorder only when supported and after " +
                            "informing everyone involved. You can import permitted audio below.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Recording Library · ${recordings.size}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    enabled = !isImporting,
                    onClick = { importPicker.launch(arrayOf("audio/*")) }
                ) {
                    Icon(Icons.Default.FileUpload, null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (isImporting) "Importing" else "Import")
                }
            }
        }
        item {
            OutlinedTextField(
                value = search, onValueChange = { search = it.take(100) },
                label = { Text("Search recordings") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
        }
        if (matches.isEmpty()) {
            item {
                Card {
                    Column(
                        Modifier.fillMaxWidth().padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.GraphicEq, null,
                            modifier = Modifier.size(34.dp))
                        Spacer(Modifier.height(10.dp))
                        Text(
                            if (search.isBlank()) "No saved recordings yet"
                            else "No matching recordings",
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Import an audio file created by a supported recorder " +
                                "with everyone’s permission.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
        items(matches, key = { it.id }) { item ->
            ElevatedCard {
                Column(
                    Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    val matchedName = contactsByNumber[
                        normalizeNumber(item.phoneNumber).takeLast(10)
                    ]?.takeIf { item.phoneNumber.isNotBlank() }
                    Text(matchedName ?: item.title, fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium)
                    if (matchedName != null) {
                        Text(item.title, style = MaterialTheme.typography.bodySmall)
                    }
                    Text(
                        listOfNotNull(
                            item.phoneNumber.ifBlank { null },
                            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                                .format(Date(item.importedAtMillis)),
                            "${item.durationMillis / 1000}s",
                            "${item.bytes / 1024} KB",
                            if (item.source == "manual") "Manually recorded" else "Imported"
                        ).joinToString("  •  "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = { playOrPause(item) }) {
                            Icon(
                                if (playingId == item.id && !paused) Icons.Default.Pause
                                else Icons.Default.PlayArrow,
                                contentDescription = when {
                                    playingId == item.id && !paused -> "Pause recording"
                                    playingId == item.id && paused -> "Resume recording"
                                    else -> "Play recording"
                                }
                            )
                        }
                        IconButton(onClick = {
                            selectedForEdit = item
                            editTitle = item.title
                            editPhone = item.phoneNumber
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = "Rename recording")
                        }
                        IconButton(onClick = { share(item) }) {
                            Icon(Icons.Default.Share, contentDescription = "Share recording")
                        }
                        IconButton(onClick = { selectedForDelete = item }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete recording",
                                tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
        item {
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Recording preferences", fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium)
                    ListItem(
                        headlineContent = { Text("Automatic incoming calls") },
                        supportingContent = { Text("Unavailable for standard SIM calls") },
                        trailingContent = {
                            Switch(checked = canAutomaticallyRecordCellularCalls(),
                                onCheckedChange = null, enabled = false)
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Automatic outgoing calls") },
                        supportingContent = { Text("Unavailable for standard SIM calls") },
                        trailingContent = {
                            Switch(checked = false, onCheckedChange = null, enabled = false)
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Contact-specific recording") },
                        supportingContent = {
                            Text("Requires a supported, consented audio recording pipeline")
                        }
                    )
                    Text(
                        "When authorized native recording is supported, S Caller saves AAC audio " +
                            "in M4A format after manual Stop or call end. Other imported audio " +
                            "retains its original quality. No unsupported HD claim is made.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        item {
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("Privacy & storage", fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium)
                    Text("Private on-device folder • no cloud upload • no background microphone access",
                        style = MaterialTheme.typography.bodySmall)
                    Text(
                        "Imported audio is stored in S call's private app storage. " +
                            "Share only with everyone’s permission.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Older than:", modifier = Modifier.weight(1f))
                        Box {
                            OutlinedButton(onClick = { retentionMenu = true }) {
                                Text(if (retention == 0) "Keep all" else "$retention days")
                            }
                            DropdownMenu(
                                expanded = retentionMenu,
                                onDismissRequest = { retentionMenu = false }
                            ) {
                                listOf(0, 30, 90).forEach { days ->
                                    DropdownMenuItem(
                                        text = { Text(if (days == 0) "Keep all" else "$days days") },
                                        onClick = {
                                            store.setRetentionDays(days)
                                            retention = days
                                            retentionMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        "Files are never removed automatically. Choose a period and tap " +
                            "Clean up to delete private copies; keep all is the default.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (retention > 0) {
                        OutlinedButton(onClick = { cleanupConfirm = true }) {
                            Icon(Icons.Default.CleaningServices, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Clean up older files")
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(22.dp)) }
    }
}
