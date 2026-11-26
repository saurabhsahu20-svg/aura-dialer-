package com.scall.phone.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.telecom.PhoneAccountHandle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scall.phone.R
import com.scall.phone.data.CallerPhotoStyle
import com.scall.phone.data.ContactEntry
import com.scall.phone.data.CallStartAccess
import com.scall.phone.data.callStartAccess
import com.scall.phone.data.PhoneRepository
import com.scall.phone.data.PhotoStore
import com.scall.phone.data.RecentEntry
import com.scall.phone.data.MissedCallCursor
import com.scall.phone.data.MissedCallBadgeStore
import com.scall.phone.data.countNewMissedCalls
import com.scall.phone.data.CallHistoryStore
import com.scall.phone.data.SimAccount
import com.scall.phone.data.SimPreferences
import com.scall.phone.data.chooseCallRoute
import com.scall.phone.data.CallRoute
import com.scall.phone.data.SmsSubscription
import com.scall.phone.telecom.CallBridge
import com.scall.phone.telecom.CallUiActivity
import com.scall.phone.telecom.IncomingCallAccess
import com.scall.phone.telecom.IncomingCallVisibility
import com.scall.phone.telecom.AudioCapabilityInspector
import com.scall.phone.telecom.cellularAudioProcessingLabel
import com.scall.phone.ui.theme.Blue
import com.scall.phone.ui.theme.SCallTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class NavTab(val label: String, val icon: ImageVector)
private val tabs = listOf(
    NavTab("Recents", Icons.Default.History),
    NavTab("Contacts", Icons.Default.Contacts),
    NavTab("Keypad", Icons.Default.Dialpad),
    NavTab("Favorites", Icons.Default.Star)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SCallApp(
    repository: PhoneRepository,
    externalDialNumber: String,
    externalDialRequestId: Int,
    refreshVersion: Int,
    callLogRefreshVersion: Int,
    requestDialerRole: () -> Unit,
    requestPermissions: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("s_call_settings", android.content.Context.MODE_PRIVATE) }
    val photos = remember { PhotoStore(context.applicationContext) }
    val simPrefs = remember { SimPreferences(context.applicationContext) }
    val historyStore = remember { CallHistoryStore(context.applicationContext) }
    val missedBadgeStore = remember { MissedCallBadgeStore(context.applicationContext) }
    val actionScope = rememberCoroutineScope()
    var theme by remember { mutableStateOf(prefs.getString("theme", "System") ?: "System") }
    var enableSwipes by remember { mutableStateOf(prefs.getBoolean("swipe_call", true)) }
    var flipToSilence by remember { mutableStateOf(prefs.getBoolean("flip_silence", false)) }
    var announceCaller by remember { mutableStateOf(prefs.getBoolean("announce_caller", false)) }
    var oneHandMode by remember { mutableStateOf(prefs.getString("one_hand_mode", "Off") ?: "Off") }
    var page by rememberSaveable { mutableStateOf("Recents") }
    var pageHistory by rememberSaveable { mutableStateOf(listOf<String>()) }
    var query by rememberSaveable { mutableStateOf("") }
    var dialNumber by rememberSaveable { mutableStateOf("") }
    var activeContact by remember { mutableStateOf<ContactEntry?>(null) }
    var photoEditor by remember { mutableStateOf<ContactEntry?>(null) }
    var newPhoto by remember { mutableStateOf<Uri?>(null) }
    var localRefresh by remember { mutableIntStateOf(0) }
    var contacts by remember { mutableStateOf<List<ContactEntry>>(emptyList()) }
    var recents by remember { mutableStateOf<List<RecentEntry>>(emptyList()) }
    var missedSeenThrough by remember { mutableStateOf<MissedCallCursor?>(null) }
    // Large call books must never sort/scan missed-call events on the UI thread.
    // Cancel older calculations when the provider delivers a newer snapshot.
    var newMissedCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(recents, missedSeenThrough) {
        val marker = missedSeenThrough
        newMissedCount = if (marker == null) 0 else
            withContext(Dispatchers.Default) { countNewMissedCalls(recents, marker) }
    }
    var editingHistory by remember { mutableStateOf<RecentEntry?>(null) }
    var historyTitle by rememberSaveable { mutableStateOf("") }
    var historyNote by rememberSaveable { mutableStateOf("") }
    var historySelectionMode by remember { mutableStateOf(false) }
    var selectedHistoryIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var pendingHistoryDeletion by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var isDeletingHistory by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var loadedOnce by remember { mutableStateOf(false) }
    var pendingNumber by remember { mutableStateOf<String?>(null) }
    var pendingSims by remember { mutableStateOf<List<SimAccount>>(emptyList()) }
    var pendingChatNumber by remember { mutableStateOf<String?>(null) }
    var pendingSmsNumber by remember { mutableStateOf<String?>(null) }
    var pendingSmsSims by remember { mutableStateOf<List<SmsSubscription>>(emptyList()) }
    val currentCall by CallBridge.current.collectAsState()
    var waitingForOutgoingCallUi by remember { mutableStateOf(false) }

    // Cache search results: otherwise every keypress, call-state update and UI animation
    // walks the entire call log and reads private history notes again.
    // Keep searches off the Compose UI thread to avoid jank on big contact books.
    var matchedContacts by remember { mutableStateOf<List<ContactEntry>>(emptyList()) }
    var matchedFavorites by remember { mutableStateOf<List<ContactEntry>>(emptyList()) }
    LaunchedEffect(contacts, query) {
        val term = query.trim()
        if (term.isNotEmpty()) kotlinx.coroutines.delay(75)
        val results = withContext(Dispatchers.Default) {
            val visible = if (term.isEmpty()) contacts
                else contacts.filter { matchesContactSearch(it.name, it.number, term) }
            visible to visible.filter { it.favorite }
        }
        matchedContacts = results.first
        matchedFavorites = results.second
    }
    // Expensive call-note searches run off the UI thread. Fast consecutive
    // keystrokes cancel older searches, keeping scrolling and dialpad responsive.
    var matchedRecents by remember { mutableStateOf<List<RecentEntry>>(emptyList()) }
    LaunchedEffect(recents, query, localRefresh) {
        if (query.isBlank()) {
            matchedRecents = recents
        } else {
            kotlinx.coroutines.delay(120)
            matchedRecents = withContext(Dispatchers.Default) {
                recents.filter {
                    it.name.contains(query, true) || it.number.contains(query) ||
                        (historyStore.title(it)?.contains(query, true) == true) ||
                        historyStore.note(it).contains(query, true)
                }
            }
        }
    }

    // Users upgrading from a prior release may have the removed page in saved
    // navigation state. Return to Recents rather than showing an orphan screen.
    LaunchedEffect(Unit) {
        if (page == "Recording Manager") page = "Recents"
        pageHistory = pageHistory.filterNot { it == "Recording Manager" }
    }

    fun acknowledgeMissedCalls() {
        if (!loadedOnce) return
        // Only rows actually loaded at the moment of viewing are acknowledged.
        // New calls arriving afterwards will still appear in the badge.
        val previouslySeen = missedSeenThrough
        val newlyViewedIds = if (previouslySeen == null) emptySet() else recents
            .asSequence()
            .filter {
                it.type == android.provider.CallLog.Calls.MISSED_TYPE &&
                    (it.timestamp > previouslySeen.timestamp ||
                        (it.timestamp == previouslySeen.timestamp &&
                            it.id > previouslySeen.id))
            }
            .map { it.id }.toSet()
        missedSeenThrough = missedBadgeStore.markViewed(recents)
        if (newlyViewedIds.isNotEmpty()) {
            // Run the provider update in IO; scrolling and taps stay responsive.
            actionScope.launch {
                repository.markMissedCallsRead(newlyViewedIds)
            }
        }
    }

    // A live Call Log observer updates the missed-call badge without reloading
    // the entire contacts database on every provider notification.
    LaunchedEffect(callLogRefreshVersion) {
        if (callLogRefreshVersion > 0 && loadedOnce && repository.hasLogsPermission()) {
            try {
                val latestCalls = repository.recentCalls(contacts)
                missedSeenThrough = missedBadgeStore.initialize(latestCalls)
                recents = latestCalls
            } catch (error: kotlinx.coroutines.CancellationException) {
                throw error
            } catch (error: Exception) {
                android.util.Log.w("SCall", "Call history background refresh unavailable", error)
            }
        }
    }

    // Opening the Recents page counts as viewing missed calls. Keep the new
    // badge briefly readable, then acknowledge only the currently loaded calls.
    LaunchedEffect(page, newMissedCount, loadedOnce) {
        if (page == "Recents" && loadedOnce && newMissedCount > 0) {
            kotlinx.coroutines.delay(850)
            if (page == "Recents") acknowledgeMissedCalls()
        }
    }

    fun navigateTo(destination: String) {
        if (destination == page) return
        pageHistory = pushPageHistory(pageHistory, page, destination)
        page = destination
    }

    fun navigateBack() {
        when {
            photoEditor != null -> {
                photoEditor = null
                newPhoto = null
            }
            activeContact != null -> activeContact = null
            historySelectionMode -> {
                historySelectionMode = false
                selectedHistoryIds = emptySet()
            }
            query.isNotBlank() -> query = ""
            else -> {
                val (previous, remaining) = popPageHistory(pageHistory)
                pageHistory = remaining
                page = previous
                query = ""
            }
        }
    }

    // Android system Back and gesture navigation mirror the in-app Back arrow.
    // Only the root Recents page can exit S call; the keypad never closes the app.
    BackHandler(
        enabled = photoEditor != null || activeContact != null ||
            historySelectionMode || query.isNotBlank() ||
            page != "Recents" || pageHistory.isNotEmpty()
    ) {
        navigateBack()
    }

    LaunchedEffect(refreshVersion, localRefresh) {
        // Keep the last successful list visible while Android reloads content providers.
        // This prevents the Recents/Contacts screens flashing a loading state
        // whenever returning from a call or system permission dialog.
        if (!loadedOnce) isLoading = true
        try {
            val newContacts = repository.contacts()
            val newRecents = repository.recentCalls(newContacts)
            contacts = newContacts
            // Initialize once from existing call history so years-old missed
            // entries do not reappear as a fresh notification after an update.
            // Do not set the first-run baseline to zero when Android denies
            // Call Log permission: that would turn all old calls into "new"
            // the moment permission is later granted.
            missedSeenThrough = if (repository.hasLogsPermission())
                missedBadgeStore.initialize(newRecents) else null
            recents = newRecents
            loadedOnce = true
        } catch (error: kotlinx.coroutines.CancellationException) {
            throw error
        } catch (error: Exception) {
            android.util.Log.e("SCall", "Unable to refresh phone records", error)
        } finally {
            isLoading = false
        }
    }
    LaunchedEffect(externalDialRequestId) {
        if (externalDialRequestId > 0 && externalDialNumber.isNotBlank()) {
            dialNumber = cleanDialNumber(externalDialNumber)
            navigateTo("Keypad")
        }
    }

    // Do not open the in-call Activity before Android Telecom delivers a real call.
    // On some devices that races InCallService binding and shows a dead call surface.
    LaunchedEffect(waitingForOutgoingCallUi, currentCall) {
        if (waitingForOutgoingCallUi && currentCall != null) {
            waitingForOutgoingCallUi = false
            try {
                context.startActivity(CallUiActivity.createIntent(context))
            } catch (error: RuntimeException) {
                android.util.Log.e("SCall", "Unable to show outgoing call UI", error)
            }
        }
    }
    LaunchedEffect(waitingForOutgoingCallUi) {
        if (waitingForOutgoingCallUi) {
            kotlinx.coroutines.delay(10_000)
            waitingForOutgoingCallUi = false
        }
    }

    fun dial(number: String, sim: PhoneAccountHandle? = null) {
        if (waitingForOutgoingCallUi) return // Ignore accidental double taps.
        try {
            repository.placeCall(number, sim)
            waitingForOutgoingCallUi = true
        } catch (error: SecurityException) {
            android.util.Log.e("SCall", "Phone permission denied while placing call", error)
            Toast.makeText(context, "Phone permission required", Toast.LENGTH_SHORT).show()
        } catch (error: Exception) {
            android.util.Log.e("SCall", "Android Telecom could not place call", error)
            Toast.makeText(context,
                if (error is IllegalStateException) "Set S Caller as your default Phone app and retry"
                else "Unable to start call. Check your SIM and phone settings",
                Toast.LENGTH_LONG).show()
        }
    }

    fun requestCall(number: String) {
        if (number.isBlank()) return
        if (!repository.hasCellularTelephony()) {
            Toast.makeText(context,
                "This device does not support cellular phone calls",
                Toast.LENGTH_LONG).show()
            return
        }
        when (callStartAccess(
            repository.isDefaultDialer(),
            repository.hasCallPermission(),
            repository.hasPhoneStatePermission()
        )) {
            CallStartAccess.NEED_DEFAULT_DIALER -> {
                Toast.makeText(context, "Set S Caller as your default Phone app first",
                    Toast.LENGTH_LONG).show()
                requestDialerRole()
                return
            }
            CallStartAccess.NEED_CALL_PERMISSION -> {
                Toast.makeText(context, "Allow Phone permission before making a call",
                    Toast.LENGTH_LONG).show()
                requestPermissions()
                return
            }
            CallStartAccess.USE_DEFAULT_SIM -> {
                // READ_PHONE_STATE is not required to place a call.
                Toast.makeText(context, "Using the system default SIM", Toast.LENGTH_SHORT).show()
                dial(number)
                return
            }
            CallStartAccess.SIM_SELECTION_AVAILABLE -> Unit
        }
        val accounts = repository.availableSimAccounts()
        val activeSims = repository.availableSmsSubscriptions()
        val preference = simPrefs.voiceChoice()
        val selected = accounts.firstOrNull { it.key == preference }
        // An exact Telecom account handle is required. SIM list ordering is not a safe mapping.
        when (chooseCallRoute(preference, accounts.map { it.key }, activeSims.size)) {
            CallRoute.SPECIFIC_ACCOUNT -> dial(number, (selected ?: accounts.single()).handle)
            CallRoute.ASK_USER -> {
                pendingSims = accounts
                pendingNumber = number
            }
            CallRoute.ANDROID_DEFAULT -> dial(number)
            CallRoute.SETUP_REQUIRED -> {
                Toast.makeText(context,
                    "SIM selection is unavailable. Check your Phone and SIM settings.",
                    Toast.LENGTH_LONG).show()
                navigateTo("SIM Manager")
            }
        }
    }

    fun openMessage(number: String, subscriptionId: Int? = null) {
        try {
            repository.openSms(number, subscriptionId)
            if (subscriptionId != null) {
                Toast.makeText(context, "Confirm the sending SIM in your Messages app", Toast.LENGTH_LONG).show()
            }
        } catch (_: Exception) {
            Toast.makeText(context, "No messaging app available", Toast.LENGTH_SHORT).show()
        }
    }

    fun requestSms(number: String) {
        if (number.isBlank()) return
        val subs = repository.availableSmsSubscriptions()
        val preference = simPrefs.smsChoice()
        val selected = subs.firstOrNull { it.key == preference }
        when {
            preference == SimPreferences.SYSTEM -> openMessage(number)
            selected != null -> openMessage(number, selected.subscriptionId)
            subs.size > 1 -> { pendingSmsSims = subs; pendingSmsNumber = number }
            else -> openMessage(number, subs.firstOrNull()?.subscriptionId)
        }
    }

    fun requestMessage(number: String) {
        if (number.isNotBlank()) pendingChatNumber = number
    }

    fun openExternalChat(number: String, app: ExternalChatApp) {
        val url = externalChatLink(number, app)
        if (url == null) {
            Toast.makeText(context, "Enter a valid phone number with country code.", Toast.LENGTH_LONG).show()
            return
        }
        val packages = when (app) {
            ExternalChatApp.WHATSAPP -> listOf("com.whatsapp", "com.whatsapp.w4b")
            ExternalChatApp.TELEGRAM -> listOf("org.telegram.messenger", "org.thunderdog.challegram")
        }
        val appUri = externalChatDirectUri(number, app) ?: return
        val intents = packages.map { pkg ->
            Intent(Intent.ACTION_VIEW, Uri.parse(appUri)).apply { setPackage(pkg) }
        } + Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
        }
        for (intent in intents) {
            try {
                context.startActivity(intent)
                return
            } catch (_: android.content.ActivityNotFoundException) {
            } catch (error: SecurityException) {
                android.util.Log.w("SCall", "Chat app launch blocked", error)
            } catch (error: RuntimeException) {
                android.util.Log.w("SCall", "Chat app launch failed", error)
            }
        }
        Toast.makeText(context, "No compatible messaging app or browser found", Toast.LENGTH_LONG).show()
    }

    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) newPhoto = uri // Preview first; do not change saved image yet.
    }

    fun openPhotoEditor(contact: ContactEntry) {
        newPhoto = null
        photoEditor = contact
    }

    fun savePhotoStyle(contact: ContactEntry, style: CallerPhotoStyle) {
        val picked = newPhoto
        if (picked != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    picked, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                Toast.makeText(context, "This photo cannot be saved. Choose another image.", Toast.LENGTH_LONG).show()
                return
            }
        }
        try {
            photos.save(contact.number, picked, style)
            newPhoto = null
            photoEditor = null
            localRefresh++
            Toast.makeText(context, "Caller photo saved", Toast.LENGTH_SHORT).show()
        } catch (error: Exception) {
            android.util.Log.e("SCall", "Unable to save caller photo settings", error)
            Toast.makeText(context, "Could not save photo. Check device storage.",
                Toast.LENGTH_LONG).show()
        }
    }

    SCallTheme(theme) {
        val scheme = MaterialTheme.colorScheme
        if (pendingNumber != null) {
            AlertDialog(
                onDismissRequest = { pendingNumber = null },
                shape = RoundedCornerShape(26.dp),
                title = { Text("Call using") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("Select a phone line for this call", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        pendingSims.forEach { sim ->
                            FilledTonalButton(onClick = {
                                val number = pendingNumber.orEmpty()
                                pendingNumber = null
                                dial(number, sim.handle)
                            }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                                Icon(Icons.Default.SimCard, null)
                                Spacer(Modifier.width(12.dp))
                                Text(sim.label, modifier = Modifier.weight(1f))
                                Icon(Icons.Default.ChevronRight, null)
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { pendingNumber = null }) { Text("Cancel") } }
            )
        }
        if (pendingChatNumber != null) {
            val number = pendingChatNumber.orEmpty()
            AlertDialog(
                onDismissRequest = { pendingChatNumber = null },
                title = { Text("Message this number") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(number, fontWeight = FontWeight.Bold)
                        Text("The selected service checks whether this number has an account.",
                            style = MaterialTheme.typography.bodySmall)
                        FilledTonalButton(onClick = {
                            pendingChatNumber = null
                            requestSms(number)
                        }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Sms, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("SMS")
                        }
                        FilledTonalButton(onClick = {
                            pendingChatNumber = null
                            openExternalChat(number, ExternalChatApp.WHATSAPP)
                        }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Chat, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("WhatsApp")
                        }
                        FilledTonalButton(onClick = {
                            pendingChatNumber = null
                            openExternalChat(number, ExternalChatApp.TELEGRAM)
                        }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Send, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Telegram")
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { pendingChatNumber = null }) { Text("Close") } }
            )
        }
        if (pendingSmsNumber != null) {
            AlertDialog(
                onDismissRequest = { pendingSmsNumber = null },
                shape = RoundedCornerShape(26.dp),
                title = { Text("Message using") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("The messaging app controls the actual SIM. Confirm before sending.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        pendingSmsSims.forEach { sim ->
                            FilledTonalButton(onClick = {
                                val number = pendingSmsNumber.orEmpty()
                                pendingSmsNumber = null
                                openMessage(number, sim.subscriptionId)
                            }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                                Icon(Icons.Default.SimCard, null)
                                Spacer(Modifier.width(10.dp))
                                Text(sim.label, modifier = Modifier.weight(1f))
                                Icon(Icons.Default.ChevronRight, null)
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { pendingSmsNumber = null }) { Text("Cancel") } }
            )
        }


        if (editingHistory != null) {
            val row = editingHistory!!
            AlertDialog(
                onDismissRequest = { editingHistory = null },
                title = { Text("Edit call history") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Only this entry's display name and private note will change in S call.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedTextField(
                            value = historyTitle,
                            onValueChange = { historyTitle = it.take(70) },
                            label = { Text("Display name (optional)") },
                            placeholder = { Text(row.name) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = historyNote,
                            onValueChange = { historyNote = it.take(250) },
                            label = { Text("Personal note (optional)") },
                            minLines = 2, maxLines = 4,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("Original number, date, call type and duration stay unchanged.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        try {
                            historyStore.save(row, historyTitle, historyNote)
                            editingHistory = null
                            localRefresh++
                        } catch (error: Exception) {
                            android.util.Log.e("SCall", "Unable to save call note", error)
                            Toast.makeText(context, "Could not save call note",
                                Toast.LENGTH_SHORT).show()
                        }
                    }) { Text("Save") }
                },
                dismissButton = {
                    TextButton(onClick = { editingHistory = null }) { Text("Cancel") }
                }
            )
        }
        if (pendingHistoryDeletion.isNotEmpty()) {
            val count = pendingHistoryDeletion.size
            AlertDialog(
                onDismissRequest = { if (!isDeletingHistory) pendingHistoryDeletion = emptySet() },
                icon = { Icon(Icons.Default.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
                title = { Text(if (count == 1) "Delete this call?" else "Delete $count calls?") },
                text = { Text(
                    "This permanently removes the selected records from Android call history. " +
                        "Other phone apps may also stop showing them. This cannot be undone."
                ) },
                confirmButton = {
                    Button(
                        enabled = !isDeletingHistory,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        onClick = {
                            if (isDeletingHistory) return@Button
                            isDeletingHistory = true
                            val ids = pendingHistoryDeletion
                            actionScope.launch {
                                val deleted = repository.deleteRecentCalls(ids)
                                isDeletingHistory = false
                                pendingHistoryDeletion = emptySet()
                                if (deleted >= 0) {
                                    // Do not discard notes for call rows not confirmed deleted.
                                    if (deleted == ids.size) {
                                        recents.filter { it.id in ids }.forEach(historyStore::remove)
                                    }
                                    selectedHistoryIds = emptySet()
                                    historySelectionMode = false
                                    localRefresh++
                                    val resultMessage = when {
                                        deleted == 0 -> "No matching calls to delete"
                                        deleted < ids.size -> "Deleted $deleted of ${ids.size} call(s); others kept"
                                        else -> "Deleted $deleted call(s)"
                                    }
                                    Toast.makeText(context, resultMessage, Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context,
                                        "Delete blocked. Set S call as default phone app and allow Call log access.",
                                        Toast.LENGTH_LONG).show()
                                    if (!repository.isDefaultDialer()) requestDialerRole() else requestPermissions()
                                }
                            }
                        }
                    ) { Text(if (isDeletingHistory) "Deleting…" else "Delete permanently") }
                },
                dismissButton = {
                    TextButton(enabled = !isDeletingHistory, onClick = {
                        pendingHistoryDeletion = emptySet()
                    }) { Text("Cancel") }
                }
            )
        }

        Scaffold(
            containerColor = scheme.background,
            topBar = {
                if (photoEditor != null || activeContact != null || page == "SIM Manager" || page == "Settings" ) {
                    TopAppBar(
                        title = { Text(when {
                            photoEditor != null -> "Caller photo"
                            activeContact != null -> "Contact details"
                            page == "SIM Manager" -> "SIM & network"
                            else -> "Settings"
                        }) },
                        navigationIcon = {
                            IconButton(onClick = { navigateBack() }) {
                                Icon(Icons.Default.ArrowBack, "Back")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = scheme.background)
                    )
                } else {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                androidx.compose.foundation.Image(
                                    painter = painterResource(R.drawable.scall_bottle_green_call_icon),
                                    contentDescription = "S call logo",
                                    modifier = Modifier.size(34.dp)
                                )
                                Spacer(Modifier.width(11.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                    Text("S Call", fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleLarge,
                                        letterSpacing = (-0.45).sp)
                                    Text("PHONE • CONTACTS", fontSize = 10.sp,
                                        letterSpacing = 1.15.sp,
                                        color = scheme.onSurfaceVariant)
                                }
                            }
                        },
                        actions = {
                            // A discreet warning on every main tab, without moving
                            // the keypad or disturbing the main-screen layout.
                            // The user can correct notifications/FSI in Settings.
                            val callAlertState = remember(refreshVersion, localRefresh, page) {
                                IncomingCallVisibility.access(context, repository.isDefaultDialer())
                            }
                            if (callAlertState != IncomingCallAccess.READY) {
                                IconButton(onClick = { navigateTo("Settings") }) {
                                    Icon(
                                        Icons.Default.NotificationsOff,
                                        contentDescription = "Incoming call alerts need setup",
                                        tint = scheme.error
                                    )
                                }
                            }
                            IconButton(onClick = { localRefresh++ }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                            }
                            IconButton(onClick = { navigateTo("SIM Manager") }) {
                                Icon(Icons.Default.SimCard, contentDescription = "SIM and network preferences")
                            }
                            IconButton(onClick = { navigateTo("Settings") }) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = scheme.background)
                    )
                }
            },
            bottomBar = {
                if (activeContact == null && photoEditor == null && page != "Settings" && page != "SIM Manager") {
                    NavigationBar(containerColor = scheme.surface, tonalElevation = 2.dp) {
                        tabs.forEach { tab ->
                            NavigationBarItem(
                                selected = page == tab.label,
                                onClick = {
                                    if (tab.label == "Recents") acknowledgeMissedCalls()
                                    navigateTo(tab.label)
                                    query = ""
                                },
                                icon = {
                                    if (tab.label == "Recents" && newMissedCount > 0) {
                                        BadgedBox(badge = {
                                            Badge {
                                                Text(if (newMissedCount > 99) "99+" else "$newMissedCount")
                                            }
                                        }) { Icon(tab.icon, contentDescription = tab.label) }
                                    } else Icon(tab.icon, contentDescription = tab.label)
                                },
                                label = { Text(tab.label, fontSize = 11.sp,
                                    fontWeight = if (page == tab.label) FontWeight.SemiBold else FontWeight.Normal) },
                                alwaysShowLabel = true,
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = scheme.primary,
                                    selectedTextColor = scheme.primary,
                                    indicatorColor = scheme.primaryContainer,
                                    unselectedIconColor = scheme.onSurfaceVariant,
                                    unselectedTextColor = scheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
            ) {
                when {
                    photoEditor != null -> ProfessionalPhotoEditor(
                        contact = photoEditor!!,
                        currentPhoto = photos.photoFor(photoEditor!!.number) ?: photoEditor!!.photo,
                        candidatePhoto = newPhoto?.toString(),
                        originalStyle = photos.styleFor(photoEditor!!.number),
                        onPick = { pickPhoto.launch(arrayOf("image/*")) },
                        onSave = { style -> savePhotoStyle(photoEditor!!, style) },
                        onRemove = {
                            photos.removePhoto(photoEditor!!.number)
                            newPhoto = null
                            photoEditor = null
                            localRefresh++
                            Toast.makeText(context, "Custom photo removed", Toast.LENGTH_SHORT).show()
                        }
                    )
                    activeContact != null -> ContactDetailsScreen(
                        contact = activeContact!!,
                        photo = photos.photoFor(activeContact!!.number) ?: activeContact!!.photo,
                        onCall = { requestCall(activeContact!!.number) },
                        onMessage = { requestMessage(activeContact!!.number) },
                        onChat = ::openExternalChat,
                        onEditPhoto = { activeContact?.let(::openPhotoEditor) }
                    )
                    page == "Settings" -> SettingsScreen(
                        theme = theme,
                        onThemeChanged = { theme = it; prefs.edit().putString("theme", it).apply() },
                        swipeEnabled = enableSwipes,
                        onSwipeEnabledChanged = { enableSwipes = it; prefs.edit().putBoolean("swipe_call", it).apply() },
                        flipToSilence = flipToSilence,
                        onFlipToSilenceChanged = {
                            flipToSilence = it
                            prefs.edit().putBoolean("flip_silence", it).apply()
                        },
                        announceCaller = announceCaller,
                        onAnnounceCallerChanged = {
                            announceCaller = it
                            prefs.edit().putBoolean("announce_caller", it).apply()
                        },
                        oneHandMode = oneHandMode,
                        onOneHandModeChanged = {
                            oneHandMode = it
                            prefs.edit().putString("one_hand_mode", it).apply()
                        },
                        defaultDialer = repository.isDefaultDialer(),
                        contactsGranted = repository.hasContactsPermission(),
                        logsGranted = repository.hasLogsPermission(),
                        requestDialerRole = requestDialerRole,
                        requestPermissions = requestPermissions,
                        onSimSettings = { navigateTo("SIM Manager") },
                        incomingAccess = IncomingCallVisibility.access(
                            context, repository.isDefaultDialer()
                        ),
                        onFixIncomingCallDisplay = {
                            val state = IncomingCallVisibility.access(
                                context, repository.isDefaultDialer()
                            )
                            when (state) {
                                IncomingCallAccess.NEED_DEFAULT_DIALER -> requestDialerRole()
                                IncomingCallAccess.NEED_NOTIFICATION_PERMISSION ->
                                    IncomingCallVisibility.openSettings(context, state)
                                else -> IncomingCallVisibility.openSettings(context, state)
                            }
                        }
                    )
                    page == "SIM Manager" -> SimSettingsScreen(
                        repository = repository,
                        preferences = simPrefs,
                        refreshVersion = refreshVersion,
                        requestDialerRole = requestDialerRole,
                        requestPermissions = requestPermissions
                    )
                    page == "Keypad" -> {
                        // Avoid repeatedly querying Telecom over Binder on each keypad digit.
                        val choice = remember(page, refreshVersion, localRefresh) {
                            simPrefs.voiceChoice()
                        }
                        val label = remember(page, choice, refreshVersion, localRefresh) {
                            when (choice) {
                                SimPreferences.ASK -> "Ask every time"
                                SimPreferences.SYSTEM -> "Android default"
                                else -> runCatching {
                                    repository.availableSimAccounts()
                                        .firstOrNull { it.key == choice }?.label
                                }.getOrNull() ?: "Line unavailable"
                            }
                        }
                        // A Column measures an unweighted child with an unbounded
                        // height. Give KeypadScreen a bounded full-height slot so
                        // its weighted digit area and measured row gaps can work.
                        Box(Modifier.fillMaxWidth().weight(1f)) {
                            KeypadScreen(
                                value = dialNumber,
                                onValueChange = { dialNumber = it },
                                contacts = contacts,
                                onCall = { requestCall(dialNumber) },
                                onMessage = { requestMessage(dialNumber) },
                                onChat = ::openExternalChat,
                                onSimSettings = { navigateTo("SIM Manager") },
                                onBack = { navigateBack() },
                                simLabel = label,
                                oneHandMode = oneHandMode
                            )
                        }
                    }
                    else -> {
                        if (currentCall != null) {
                            Surface(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 5.dp)
                                    .clickable { context.startActivity(CallUiActivity.createIntent(context)) },
                                color = scheme.primaryContainer,
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Call, null, tint = scheme.primary)
                                    Spacer(Modifier.width(10.dp))
                                    Text("Return to ongoing call", color = scheme.onPrimaryContainer, modifier = Modifier.weight(1f))
                                    Icon(Icons.Default.ChevronRight, null, tint = scheme.primary)
                                }
                            }
                        }
                        if (!repository.isDefaultDialer() || !repository.hasContactsPermission() || !repository.hasLogsPermission()) {
                            SetupBanner(
                                defaultDialer = repository.isDefaultDialer(),
                                onRole = requestDialerRole,
                                onPermission = requestPermissions
                            )
                        }
                        if (page == "Recents" && repository.isDefaultDialer()) {
                            val callAlerts = IncomingCallVisibility.access(
                                context, defaultDialer = true
                            )
                            if (callAlerts != IncomingCallAccess.READY) {
                                Surface(
                                    color = scheme.errorContainer,
                                    shape = RoundedCornerShape(15.dp),
                                    modifier = Modifier.fillMaxWidth()
                                        .padding(horizontal = 18.dp, vertical = 6.dp)
                                        .clickable { navigateTo("Settings") }
                                ) {
                                    Row(
                                        Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.NotificationsActive, null,
                                            tint = scheme.onErrorContainer)
                                        Spacer(Modifier.width(8.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text("Incoming call display needs setup",
                                                color = scheme.onErrorContainer,
                                                fontWeight = FontWeight.Bold)
                                            Text("Tap to fix call alerts in Settings",
                                                color = scheme.onErrorContainer,
                                                style = MaterialTheme.typography.bodySmall)
                                        }
                                        Icon(Icons.Default.ChevronRight, null,
                                            tint = scheme.onErrorContainer)
                                    }
                                }
                            }
                        }
                        if (page == "Recents" && query.isBlank()) {
                            QuickDialCard(onOpenKeypad = { navigateTo("Keypad") })
                        }
                        SearchField(value = query, onChange = { query = it })
                        when (page) {
                            "Contacts" -> ContactsScreen(
                                contacts = matchedContacts,
                                photos = photos, loading = isLoading,
                                swipesEnabled = enableSwipes,
                                onCall = ::requestCall,
                                onMessage = ::requestMessage,
                                onChat = ::openExternalChat,
                                onContact = { activeContact = it },
                                onPhoto = ::openPhotoEditor
                            )
                            "Favorites" -> ContactsScreen(
                                contacts = matchedFavorites,
                                photos = photos, loading = isLoading,
                                swipesEnabled = enableSwipes,
                                onCall = ::requestCall,
                                onMessage = ::requestMessage,
                                onChat = ::openExternalChat,
                                onContact = { activeContact = it },
                                onPhoto = ::openPhotoEditor,
                                emptyTitle = "No favorites yet"
                            )
                            else -> RecentsScreen(
                                calls = matchedRecents,
                                // The fresh badge is calculated from the full call log,
                                // not a filtered/search subset.
                                newMissedCount = newMissedCount,
                                onMissedViewed = ::acknowledgeMissedCalls,
                                photos = photos, historyStore = historyStore, loading = isLoading,
                                swipesEnabled = enableSwipes,
                                selectionMode = historySelectionMode,
                                selectedIds = selectedHistoryIds,
                                onSelectionModeChange = {
                                    historySelectionMode = it
                                    if (!it) selectedHistoryIds = emptySet()
                                },
                                onToggleSelected = { row ->
                                    selectedHistoryIds = if (row.id in selectedHistoryIds) {
                                        selectedHistoryIds - row.id
                                    } else selectedHistoryIds + row.id
                                },
                                onSelectAll = { visible ->
                                    selectedHistoryIds = visible.map { it.id }.toSet()
                                },
                                onDeleteSelected = { pendingHistoryDeletion = selectedHistoryIds },
                                onEdit = { entry ->
                                    editingHistory = entry
                                    historyTitle = historyStore.title(entry).orEmpty()
                                    historyNote = historyStore.note(entry)
                                },
                                onDeleteOne = { pendingHistoryDeletion = setOf(it.id) },
                                onCall = ::requestCall,
                                onMessage = ::requestMessage,
                                onChat = ::openExternalChat,
                                onDetails = { entry ->
                                    activeContact = contacts.firstOrNull { it.number == entry.number }
                                        ?: ContactEntry(0, entry.name, entry.number, entry.photo, false)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Small, real shortcut on Recents: a professional landing card without adding
 * fake calling, recording, spam-detection or VOIP capabilities.
 */
@Composable
private fun QuickDialCard(onOpenKeypad: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 5.dp),
        shape = RoundedCornerShape(22.dp),
        color = scheme.primaryContainer,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenKeypad)
                .padding(horizontal = 17.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Ready to connect?", color = scheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(2.dp))
                Text("Open keypad for a new call", color = scheme.onPrimaryContainer.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodySmall)
            }
            Box(
                Modifier.size(44.dp).background(scheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Call, contentDescription = "Open keypad",
                    tint = scheme.onPrimary, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
private fun SetupBanner(defaultDialer: Boolean, onRole: () -> Unit, onPermission: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.48f),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 5.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.VerifiedUser, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Finish phone setup", fontWeight = FontWeight.SemiBold)
                Text(if (defaultDialer) "Allow contacts, calls and notifications" else "Make S call your default phone app",
                    style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = if (defaultDialer) onPermission else onRole) {
                Text("Set up")
            }
        }
    }
}

@Composable
private fun SearchField(value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(20.dp)) },
        trailingIcon = {
            if (value.isNotEmpty()) IconButton(onClick = { onChange("") }) {
                Icon(Icons.Default.Close, "Clear search")
            }
        },
        placeholder = { Text("Search calls, contacts or numbers", style = MaterialTheme.typography.bodyMedium) },
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
internal fun EmptyState(title: String, subtitle: String) {
    Box(Modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Phone, null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                modifier = Modifier.size(50.dp))
            Spacer(Modifier.height(14.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun ContactsScreen(
    contacts: List<ContactEntry>, photos: PhotoStore, loading: Boolean,
    swipesEnabled: Boolean, onCall: (String) -> Unit, onMessage: (String) -> Unit,
    onChat: (String, ExternalChatApp) -> Unit,
    onContact: (ContactEntry) -> Unit,
    onPhoto: (ContactEntry) -> Unit,
    emptyTitle: String = "No contacts found"
) {
    if (contacts.isEmpty()) {
        EmptyState(if (loading) "Loading contacts…" else emptyTitle,
            "Allow contacts access or add contacts to your phone.")
        return
    }
    LazyColumn(contentPadding = PaddingValues(bottom = 22.dp)) {
        item { SectionHeading("Contacts", "${contacts.size} numbers") }
        items(contacts, key = { "${it.id}:${it.number}" }) { contact ->
            SwipeContactRow(
                name = contact.name, number = contact.number,
                photoUri = photos.photoFor(contact.number) ?: contact.photo,
                subtitle = contact.number, swipesEnabled = swipesEnabled,
                onCall = { onCall(contact.number) },
                onMessage = { onMessage(contact.number) },
                onClick = { onContact(contact) },
                onPhoto = { onPhoto(contact) }
            )
            DirectChatActions(number = contact.number, onLaunch = onChat,
                modifier = Modifier.padding(start = 79.dp, end = 18.dp, bottom = 9.dp))
        }
    }
}

@Composable
private fun RecentsScreen(
    calls: List<RecentEntry>,
    newMissedCount: Int,
    onMissedViewed: () -> Unit,
    photos: PhotoStore,
    historyStore: CallHistoryStore,
    loading: Boolean,
    swipesEnabled: Boolean,
    selectionMode: Boolean,
    selectedIds: Set<Long>,
    onSelectionModeChange: (Boolean) -> Unit,
    onToggleSelected: (RecentEntry) -> Unit,
    onSelectAll: (List<RecentEntry>) -> Unit,
    onDeleteSelected: () -> Unit,
    onEdit: (RecentEntry) -> Unit,
    onDeleteOne: (RecentEntry) -> Unit,
    onCall: (String) -> Unit,
    onMessage: (String) -> Unit,
    onChat: (String, ExternalChatApp) -> Unit,
    onDetails: (RecentEntry) -> Unit
) {
    var filter by remember { mutableStateOf<CallDirection?>(null) }
    // Only one expanded row at a time: constant-size state even for long call logs.
    var expandedCallId by rememberSaveable { mutableStateOf<Long?>(null) }
    val filteredCalls = remember(calls, filter) {
        calls.filter { filter == null || classifyCallDirection(it.type) == filter }
    }
    val (incomingCount, outgoingCount, missedCount) = remember(calls) {
        Triple(
            calls.count { classifyCallDirection(it.type) == CallDirection.INCOMING },
            calls.count { classifyCallDirection(it.type) == CallDirection.OUTGOING },
            calls.count { classifyCallDirection(it.type) == CallDirection.MISSED }
        )
    }

    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Call history", style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold)
                    Text(
                        if (newMissedCount > 0) "${calls.size} calls · $newMissedCount new missed"
                        else "${calls.size} calls",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium
                    )
                    if (newMissedCount > 0) {
                        TextButton(onClick = onMissedViewed) {
                            Text("Mark missed as viewed")
                        }
                    }
                }
                TextButton(onClick = { onSelectionModeChange(!selectionMode) }) {
                    Icon(if (selectionMode) Icons.Default.Close else Icons.Default.Checklist, null,
                        modifier = Modifier.size(19.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(if (selectionMode) "Cancel" else "Select")
                }
            }
        }
        item {
            // A compact scrollable segment bar keeps call-type filtering visible on small phones.
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                    .padding(start = 17.dp, end = 17.dp, bottom = 9.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = filter == null,
                    onClick = { filter = null },
                    label = { Text("All · ${calls.size}") },
                    leadingIcon = {
                        Icon(Icons.Default.History, null, Modifier.size(17.dp))
                    }
                )
                listOf(
                    CallDirection.INCOMING to incomingCount,
                    CallDirection.OUTGOING to outgoingCount,
                    CallDirection.MISSED to missedCount
                ).forEach { (direction, count) ->
                    FilterChip(
                        selected = filter == direction,
                        onClick = {
                            filter = direction
                            if (direction == CallDirection.MISSED) onMissedViewed()
                        },
                        label = {
                            Text(
                                if (direction == CallDirection.MISSED)
                                    "Missed history · $count" else "${direction.label} · $count",
                                fontWeight = FontWeight.ExtraBold,
                                color = direction.callTint()
                            )
                        },
                        leadingIcon = {
                            CallDirectionSymbol(
                                direction = direction,
                                modifier = Modifier.size(21.dp),
                                description = "${direction.label} filter"
                            )
                        }
                    )
                }
            }
        }
        if (selectionMode) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(17.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${selectedIds.size} selected", modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelLarge)
                        TextButton(
                            onClick = { onSelectAll(filteredCalls) },
                            enabled = filteredCalls.isNotEmpty()
                        ) { Text("Select visible") }
                        Button(
                            onClick = onDeleteSelected,
                            enabled = selectedIds.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) { Text("Delete") }
                    }
                }
            }
        }
        if (filteredCalls.isEmpty()) {
            item {
                EmptyState(
                    if (loading) "Loading calls…" else if (filter == null) "No recent calls"
                        else "No ${filter!!.label.lowercase()} calls",
                    if (filter == null) "Your recent calls will appear here."
                        else "Try All calls to view other call types."
                )
            }
        }
        items(filteredCalls, key = { it.id }) { item ->
            val expanded = !selectionMode && expandedCallId == item.id
            val direction = classifyCallDirection(item.type)
            val time = remember(item.timestamp) {
                java.text.SimpleDateFormat("dd MMM, h:mm a", java.util.Locale.getDefault())
                    .format(java.util.Date(item.timestamp))
            }
            val note = historyStore.note(item)
            val duration = if (item.duration > 0 &&
                direction != CallDirection.MISSED) {
                " · ${formatCallDuration(item.duration)}"
            } else ""
            Column {
                SwipeContactRow(
                    name = historyStore.title(item) ?: item.name,
                    number = item.number,
                    photoUri = photos.photoFor(item.number) ?: item.photo,
                    subtitle = time + duration + if (note.isBlank()) "" else " · ${note.take(30)}",
                    missed = direction == CallDirection.MISSED,
                    callDirection = direction,
                    swipesEnabled = swipesEnabled && !selectionMode,
                    onCall = { onCall(item.number) },
                    onMessage = { onMessage(item.number) },
                    showMoreIndicator = !selectionMode,
                    expanded = expanded,
                    onClick = {
                        if (selectionMode) onToggleSelected(item)
                        else expandedCallId = if (expanded) null else item.id
                    }
                )
                if (selectionMode) {
                    Row(
                        Modifier.fillMaxWidth().padding(start = 22.dp, end = 18.dp, bottom = 7.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = item.id in selectedIds,
                            onCheckedChange = { onToggleSelected(item) })
                        Text("Select", style = MaterialTheme.typography.labelSmall)
                    }
                }
                AnimatedVisibility(visible = expanded) {
                    Column(Modifier.fillMaxWidth().padding(bottom = 5.dp)) {
                        DirectChatActions(
                            number = item.number,
                            onLaunch = onChat,
                            modifier = Modifier.padding(start = 22.dp, end = 18.dp, bottom = 4.dp)
                        )
                        Row(
                            Modifier.fillMaxWidth().padding(start = 22.dp, end = 18.dp, bottom = 3.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { onDetails(item) }) {
                                Icon(Icons.Default.Info, "Contact details", Modifier.size(17.dp))
                                Spacer(Modifier.width(3.dp))
                                Text("Details")
                            }
                            TextButton(onClick = { onEdit(item) }) {
                                Icon(Icons.Default.Edit, "Edit this call", Modifier.size(17.dp))
                                Spacer(Modifier.width(3.dp))
                                Text("Edit")
                            }
                            TextButton(onClick = { onDeleteOne(item) }) {
                                Icon(Icons.Default.DeleteOutline, "Delete this call", Modifier.size(17.dp),
                                    tint = MaterialTheme.colorScheme.error)
                                Spacer(Modifier.width(3.dp))
                                Text("Delete", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f),
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
        }
    }
}

@Composable
private fun SectionHeading(title: String, trailing: String) {
    Row(
        Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 8.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(trailing, style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ContactDetailsScreen(
    contact: ContactEntry, photo: String?, onCall: () -> Unit,
    onMessage: () -> Unit,
    onChat: (String, ExternalChatApp) -> Unit,
    onEditPhoto: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(30.dp))
        ContactAvatar(contact.name, photo, size = 114.dp)
        Spacer(Modifier.height(22.dp))
        Text(contact.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(contact.number, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(30.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            FilledTonalIconButton(onClick = onCall, modifier = Modifier.size(64.dp)) {
                Icon(Icons.Default.Call, "Call", modifier = Modifier.size(27.dp))
            }
            FilledTonalIconButton(onClick = onMessage, modifier = Modifier.size(64.dp)) {
                Icon(Icons.Default.Message, "Message", modifier = Modifier.size(27.dp))
            }
            FilledTonalIconButton(onClick = onEditPhoto, modifier = Modifier.size(64.dp)) {
                Icon(Icons.Default.PhotoCamera, "Caller photo", modifier = Modifier.size(27.dp))
            }
        }
        Spacer(Modifier.height(16.dp))
        DirectChatActions(number = contact.number, onLaunch = onChat)
        Spacer(Modifier.height(20.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        ListItem(
            headlineContent = { Text("Full-screen caller photo") },
            supportingContent = { Text("Customize the picture shown during calls") },
            leadingContent = { Icon(Icons.Default.Wallpaper, null) },
            trailingContent = { Icon(Icons.Default.ChevronRight, null) },
            modifier = Modifier.clickable(onClick = onEditPhoto)
        )
    }
}

@Composable
private fun PhotoEditorScreen(contact: ContactEntry, photo: String?, onPick: () -> Unit, onRemove: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Full-screen photo preview", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(16.dp))
        Box(
            Modifier.fillMaxWidth().weight(1f).background(
                MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(28.dp)
            ),
            contentAlignment = Alignment.Center
        ) {
            CallerPhotoPreview(name = contact.name, photo = photo)
        }
        Spacer(Modifier.height(18.dp))
        Button(onClick = onPick, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(18.dp)) {
            Icon(Icons.Default.AddPhotoAlternate, null)
            Spacer(Modifier.width(9.dp))
            Text("Choose photo")
        }
        TextButton(onClick = onRemove) { Text("Remove custom photo") }
        Text("Images stay on your device", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SettingsScreen(
    theme: String, onThemeChanged: (String) -> Unit,
    swipeEnabled: Boolean, onSwipeEnabledChanged: (Boolean) -> Unit,
    flipToSilence: Boolean, onFlipToSilenceChanged: (Boolean) -> Unit,
    announceCaller: Boolean, onAnnounceCallerChanged: (Boolean) -> Unit,
    oneHandMode: String, onOneHandModeChanged: (String) -> Unit,
    defaultDialer: Boolean, contactsGranted: Boolean, logsGranted: Boolean,
    requestDialerRole: () -> Unit, requestPermissions: () -> Unit,
    onSimSettings: () -> Unit,
    incomingAccess: IncomingCallAccess,
    onFixIncomingCallDisplay: () -> Unit
) {
    var themeMenu by remember { mutableStateOf(false) }
    var oneHandMenu by remember { mutableStateOf(false) }
    val audioCapabilities = remember { AudioCapabilityInspector.inspect() }
    LazyColumn(contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Settings", style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 5.dp, vertical = 8.dp)) }
        item {
            SettingsCard {
                ListItem(
                    leadingContent = {
                        Icon(Icons.Default.PhoneInTalk, null,
                            tint = if (incomingAccess == IncomingCallAccess.READY)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error)
                    },
                    headlineContent = {
                        Text("Incoming call display", fontWeight = FontWeight.Bold)
                    },
                    supportingContent = {
                        Text(when (incomingAccess) {
                            IncomingCallAccess.NEED_DEFAULT_DIALER ->
                                "Make S call your default Phone app to display incoming calls"
                            IncomingCallAccess.NEED_NOTIFICATION_PERMISSION ->
                                "Allow S call notifications, so ringing calls appear on screen"
                            IncomingCallAccess.NEED_NOTIFICATION_CHANNEL ->
                                "Set Phone calls notifications to HIGH / Pop on screen. Low or Default priority may vibrate without showing who is calling"
                            IncomingCallAccess.NEED_FULL_SCREEN_ACCESS ->
                                "Allow full-screen alerts in Android settings for locked screen calls"
                            IncomingCallAccess.READY ->
                                "Ready: incoming calls appear as heads-up alerts or lock-screen call UI"
                        })
                    },
                    trailingContent = {
                        if (incomingAccess == IncomingCallAccess.READY) {
                            Icon(Icons.Default.CheckCircle, "Ready",
                                tint = MaterialTheme.colorScheme.primary)
                        } else TextButton(onClick = onFixIncomingCallDisplay) {
                            Text("Fix")
                        }
                    }
                )
            }
        }
        item {
            SettingsCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.GraphicEq, null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Voice quality & audio capabilities",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        cellularAudioProcessingLabel(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "SIM calls use your device's modem and voice processing. S call " +
                            "cannot directly activate AI noise cancellation, echo cancellation " +
                            "or microphone gain controls on protected carrier audio.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HorizontalDivider()
                    Text(
                        "Capabilities available for app-owned VoIP microphone sessions " +
                            "(not active during SIM calls):",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        "Noise suppression: ${if (audioCapabilities.noiseSuppressor) "Available" else "Not reported"}" +
                            "   •   Echo cancellation: ${if (audioCapabilities.echoCanceler) "Available" else "Not reported"}" +
                            "   •   Auto gain: ${if (audioCapabilities.autoGainControl) "Available" else "Not reported"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Audio output can be selected during a call using the Audio button. " +
                            "No microphone recording or voice data is collected by this check.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        item {
            SettingsCard {
                ListItem(
                    leadingContent = { Icon(Icons.Default.Palette, null) },
                    headlineContent = { Text("Appearance") },
                    supportingContent = { Text("$theme theme") },
                    trailingContent = {
                        Box {
                            TextButton(onClick = { themeMenu = true }) { Text("Change") }
                            DropdownMenu(expanded = themeMenu, onDismissRequest = { themeMenu = false }) {
                                listOf("System", "Light", "Dark", "AMOLED").forEach { option ->
                                    DropdownMenuItem(text = { Text(option) }, onClick = {
                                        themeMenu = false
                                        onThemeChanged(option)
                                    })
                                }
                            }
                        }
                    }
                )
            }
        }
        item {
            SettingsCard {
                ListItem(
                    leadingContent = { Icon(Icons.Default.Swipe, null) },
                    headlineContent = { Text("Swipe on name to call") },
                    supportingContent = { Text("Right: call · Left: message") },
                    trailingContent = { Switch(checked = swipeEnabled, onCheckedChange = onSwipeEnabledChanged) }
                )
            }
        }
        item {
            SettingsCard {
                ListItem(
                    leadingContent = { Icon(Icons.Default.VolumeOff, null) },
                    headlineContent = { Text("Flip to silence incoming calls") },
                    supportingContent = {
                        Text("Turn the screen face down for a moment to silence only the ringtone. The call stays incoming.")
                    },
                    trailingContent = {
                        Switch(checked = flipToSilence, onCheckedChange = onFlipToSilenceChanged)
                    }
                )
            }
        }
        item {
            SettingsCard {
                ListItem(
                    leadingContent = { Icon(Icons.Default.RecordVoiceOver, null) },
                    headlineContent = { Text("Announce saved caller name") },
                    supportingContent = {
                        Text("Optional voice announcement using the phone's Text-to-Speech engine. Only saved names; off by default.")
                    },
                    trailingContent = {
                        Switch(checked = announceCaller, onCheckedChange = onAnnounceCallerChanged)
                    }
                )
            }
        }
        item {
            SettingsCard {
                ListItem(
                    leadingContent = { Icon(Icons.Default.Dialpad, null) },
                    headlineContent = { Text("One-handed keypad") },
                    supportingContent = { Text("$oneHandMode • Compact number keys for easier reach") },
                    trailingContent = {
                        Box {
                            TextButton(onClick = { oneHandMenu = true }) { Text("Change") }
                            DropdownMenu(expanded = oneHandMenu,
                                onDismissRequest = { oneHandMenu = false }) {
                                listOf("Off", "Left", "Right").forEach { option ->
                                    DropdownMenuItem(text = { Text(option) }, onClick = {
                                        oneHandMenu = false
                                        onOneHandModeChanged(option)
                                    })
                                }
                            }
                        }
                    }
                )
            }
        }
        item {
            SettingsCard {
                ListItem(
                    leadingContent = { Icon(Icons.Default.SimCard, null) },
                    headlineContent = { Text("SIM & network preferences") },
                    supportingContent = { Text("SIM 1 / SIM 2 · Calls · SMS · Mobile data") },
                    trailingContent = { Icon(Icons.Default.ChevronRight, "Open") },
                    modifier = Modifier.clickable(onClick = onSimSettings)
                )
            }
        }
        item {
            SettingsCard {
                ListItem(
                    leadingContent = { Icon(Icons.Default.PhoneAndroid, null) },
                    headlineContent = { Text("Default phone application") },
                    supportingContent = { Text(if (defaultDialer) "S call is your default phone app" else "Required for call controls") },
                    trailingContent = {
                        if (!defaultDialer) TextButton(onClick = requestDialerRole) { Text("Enable") }
                        else Icon(Icons.Default.CheckCircle, "Enabled", tint = MaterialTheme.colorScheme.primary)
                    }
                )
            }
        }
        item {
            SettingsCard {
                ListItem(
                    leadingContent = { Icon(Icons.Default.Security, null) },
                    headlineContent = { Text("Permissions") },
                    supportingContent = { Text(if (contactsGranted && logsGranted) "Contacts and call history allowed"
                        else "Allow contacts, call history and phone") },
                    trailingContent = { TextButton(onClick = requestPermissions) { Text("Manage") } }
                )
            }
        }
        item {
            Text("S call · v1.9.35\nBeautiful calls. Simple experience.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(22.dp)
            )
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(), content = content)
}
