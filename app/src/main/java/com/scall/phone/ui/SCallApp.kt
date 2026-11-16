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
import com.scall.phone.data.PhoneRepository
import com.scall.phone.data.PhotoStore
import com.scall.phone.data.RecentEntry
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
    refreshVersion: Int,
    requestDialerRole: () -> Unit,
    requestPermissions: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("s_call_settings", android.content.Context.MODE_PRIVATE) }
    val photos = remember { PhotoStore(context.applicationContext) }
    val simPrefs = remember { SimPreferences(context.applicationContext) }
    val historyStore = remember { CallHistoryStore(context.applicationContext) }
    val actionScope = rememberCoroutineScope()
    var theme by remember { mutableStateOf(prefs.getString("theme", "System") ?: "System") }
    var enableSwipes by remember { mutableStateOf(prefs.getBoolean("swipe_call", true)) }
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
    var editingHistory by remember { mutableStateOf<RecentEntry?>(null) }
    var historyTitle by rememberSaveable { mutableStateOf("") }
    var historyNote by rememberSaveable { mutableStateOf("") }
    var historySelectionMode by remember { mutableStateOf(false) }
    var selectedHistoryIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var pendingHistoryDeletion by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var isDeletingHistory by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var pendingNumber by remember { mutableStateOf<String?>(null) }
    var pendingSims by remember { mutableStateOf<List<SimAccount>>(emptyList()) }
    var pendingSmsNumber by remember { mutableStateOf<String?>(null) }
    var pendingSmsSims by remember { mutableStateOf<List<SmsSubscription>>(emptyList()) }
    val currentCall by CallBridge.current.collectAsState()

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
        isLoading = true
        val newContacts = repository.contacts()
        contacts = newContacts
        recents = repository.recentCalls(newContacts)
        isLoading = false
    }
    LaunchedEffect(externalDialNumber) {
        if (externalDialNumber.isNotEmpty()) {
            dialNumber = externalDialNumber
            navigateTo("Keypad")
        }
    }

    fun dial(number: String, sim: PhoneAccountHandle? = null) {
        try {
            repository.placeCall(number, sim)
            context.startActivity(CallUiActivity.createIntent(context))
        } catch (_: SecurityException) {
            Toast.makeText(context, "Phone permission required", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            Toast.makeText(context, "Unable to start call", Toast.LENGTH_SHORT).show()
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
        if (!repository.isDefaultDialer()) {
            Toast.makeText(context, "Set S call as your default phone app first", Toast.LENGTH_LONG).show()
            requestDialerRole()
            return
        }
        if (!repository.hasCallPermission() || !repository.hasPhoneStatePermission()) {
            Toast.makeText(context, "Allow Phone permission to choose a calling SIM", Toast.LENGTH_LONG).show()
            requestPermissions()
            return
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

    fun requestMessage(number: String) {
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
        photos.save(contact.number, picked, style)
        newPhoto = null
        photoEditor = null
        localRefresh++
        Toast.makeText(context, "Caller photo saved", Toast.LENGTH_SHORT).show()
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
                        historyStore.save(row, historyTitle, historyNote)
                        editingHistory = null
                        localRefresh++
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
                                    recents.filter { it.id in ids }.forEach(historyStore::remove)
                                    selectedHistoryIds = emptySet()
                                    historySelectionMode = false
                                    localRefresh++
                                    Toast.makeText(context,
                                        if (deleted == 0) "No matching calls to delete" else "Deleted $deleted call(s)",
                                        Toast.LENGTH_SHORT).show()
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
                if (photoEditor != null || activeContact != null || page == "SIM Manager" || page == "Settings") {
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
                                    painter = painterResource(R.drawable.scall_brand_icon),
                                    contentDescription = "S call logo",
                                    modifier = Modifier.size(34.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text("S call", fontWeight = FontWeight.Bold, letterSpacing = (-0.7).sp)
                            }
                        },
                        actions = {
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
                    NavigationBar(containerColor = scheme.surface, tonalElevation = 1.dp) {
                        tabs.forEach { tab ->
                            NavigationBarItem(
                                selected = page == tab.label,
                                onClick = { navigateTo(tab.label); query = "" },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label, fontSize = 11.sp) },
                                alwaysShowLabel = true
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
                        onEditPhoto = { activeContact?.let(::openPhotoEditor) }
                    )
                    page == "Settings" -> SettingsScreen(
                        theme = theme,
                        onThemeChanged = { theme = it; prefs.edit().putString("theme", it).apply() },
                        swipeEnabled = enableSwipes,
                        onSwipeEnabledChanged = { enableSwipes = it; prefs.edit().putBoolean("swipe_call", it).apply() },
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
                        val choice = simPrefs.voiceChoice()
                        val label = when (choice) {
                            SimPreferences.ASK -> "Ask every time"
                            SimPreferences.SYSTEM -> "Android default"
                            else -> repository.availableSimAccounts()
                                .firstOrNull { it.key == choice }?.label ?: "Line unavailable"
                        }
                        KeypadScreen(
                            value = dialNumber,
                            onValueChange = { dialNumber = it },
                            contacts = contacts,
                            onCall = { requestCall(dialNumber) },
                            onMessage = { requestMessage(dialNumber) },
                            onSimSettings = { navigateTo("SIM Manager") },
                            onBack = { navigateBack() },
                            simLabel = label
                        )
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
                        SearchField(value = query, onChange = { query = it })
                        when (page) {
                            "Contacts" -> ContactsScreen(
                                contacts = contacts.filter { query.isBlank() || it.name.contains(query, true) || it.number.contains(query) },
                                photos = photos, loading = isLoading,
                                swipesEnabled = enableSwipes,
                                onCall = ::requestCall,
                                onMessage = ::requestMessage,
                                onContact = { activeContact = it },
                                onPhoto = ::openPhotoEditor
                            )
                            "Favorites" -> ContactsScreen(
                                contacts = contacts.filter { it.favorite && (query.isBlank() || it.name.contains(query, true) || it.number.contains(query)) },
                                photos = photos, loading = isLoading,
                                swipesEnabled = enableSwipes,
                                onCall = ::requestCall,
                                onMessage = ::requestMessage,
                                onContact = { activeContact = it },
                                onPhoto = ::openPhotoEditor,
                                emptyTitle = "No favorites yet"
                            )
                            else -> RecentsScreen(
                                calls = recents.filter { query.isBlank() ||
                                    it.name.contains(query, true) || it.number.contains(query) ||
                                    (historyStore.title(it)?.contains(query, true) == true) ||
                                    historyStore.note(it).contains(query, true) },
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
        placeholder = { Text("Search contacts or numbers", style = MaterialTheme.typography.bodyMedium) },
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
            unfocusedBorderColor = Color.Transparent,
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
        }
    }
}

@Composable
private fun RecentsScreen(
    calls: List<RecentEntry>,
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
    onDetails: (RecentEntry) -> Unit
) {
    var filter by remember { mutableStateOf<CallDirection?>(null) }
    val filteredCalls = remember(calls, filter) {
        calls.filter { filter == null || classifyCallDirection(it.type) == filter }
    }
    val incomingCount = calls.count { classifyCallDirection(it.type) == CallDirection.INCOMING }
    val outgoingCount = calls.count { classifyCallDirection(it.type) == CallDirection.OUTGOING }
    val missedCount = calls.count { classifyCallDirection(it.type) == CallDirection.MISSED }

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
                    Text("${calls.size} calls · ${missedCount} missed",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium)
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
                        onClick = { filter = direction },
                        label = {
                            Text(
                                "${direction.label} · $count",
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
                    onClick = {
                        if (selectionMode) onToggleSelected(item) else onDetails(item)
                    }
                )
                Row(
                    Modifier.fillMaxWidth().padding(start = 22.dp, end = 18.dp, bottom = 7.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (selectionMode) {
                        Checkbox(
                            checked = item.id in selectedIds,
                            onCheckedChange = { onToggleSelected(item) }
                        )
                        Text("Select", style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.weight(1f))
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                    TextButton(onClick = { onEdit(item) }) {
                        Icon(Icons.Default.Edit, null, Modifier.size(17.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("Edit")
                    }
                    TextButton(onClick = { onDeleteOne(item) }) {
                        Icon(Icons.Default.DeleteOutline, null, Modifier.size(17.dp),
                            tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(3.dp))
                        Text("Delete", color = MaterialTheme.colorScheme.error)
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
    onMessage: () -> Unit, onEditPhoto: () -> Unit
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
        Spacer(Modifier.height(28.dp))
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
    defaultDialer: Boolean, contactsGranted: Boolean, logsGranted: Boolean,
    requestDialerRole: () -> Unit, requestPermissions: () -> Unit,
    onSimSettings: () -> Unit,
    incomingAccess: IncomingCallAccess,
    onFixIncomingCallDisplay: () -> Unit
) {
    var themeMenu by remember { mutableStateOf(false) }
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
                                "Enable the Phone calls notification channel"
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
            Text("S call · v1.7.7\nBeautiful calls. Simple experience.",
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
