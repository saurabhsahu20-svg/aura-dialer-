package com.scall.phone.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.telecom.PhoneAccountHandle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import com.scall.phone.data.SimAccount
import com.scall.phone.data.SimPreferences
import com.scall.phone.data.SmsSubscription
import com.scall.phone.telecom.CallBridge
import com.scall.phone.telecom.CallUiActivity
import com.scall.phone.ui.theme.Blue
import com.scall.phone.ui.theme.SCallTheme
import kotlinx.coroutines.Dispatchers
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
    var theme by remember { mutableStateOf(prefs.getString("theme", "System") ?: "System") }
    var enableSwipes by remember { mutableStateOf(prefs.getBoolean("swipe_call", true)) }
    var page by rememberSaveable { mutableStateOf("Recents") }
    var query by rememberSaveable { mutableStateOf("") }
    var dialNumber by rememberSaveable { mutableStateOf("") }
    var activeContact by remember { mutableStateOf<ContactEntry?>(null) }
    var photoEditor by remember { mutableStateOf<ContactEntry?>(null) }
    var newPhoto by remember { mutableStateOf<Uri?>(null) }
    var localRefresh by remember { mutableIntStateOf(0) }
    var contacts by remember { mutableStateOf<List<ContactEntry>>(emptyList()) }
    var recents by remember { mutableStateOf<List<RecentEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var pendingNumber by remember { mutableStateOf<String?>(null) }
    var pendingSims by remember { mutableStateOf<List<SimAccount>>(emptyList()) }
    var pendingSmsNumber by remember { mutableStateOf<String?>(null) }
    var pendingSmsSims by remember { mutableStateOf<List<SmsSubscription>>(emptyList()) }
    val currentCall by CallBridge.current.collectAsState()

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
            page = "Keypad"
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
        if (!repository.isDefaultDialer()) {
            Toast.makeText(context, "Set S call as your default phone app first", Toast.LENGTH_LONG).show()
            requestDialerRole()
            return
        }
        if (!repository.hasCallPermission()) {
            requestPermissions()
            return
        }
        val accounts = repository.availableSimAccounts()
        // Match the exact Telecom PhoneAccountHandle, never assume SIM 1 == list index 0.
        val preferred = simPrefs.voiceChoice().takeIf { it != SimPreferences.ASK }
        val selected = accounts.firstOrNull { it.key == preferred }
        when {
            selected != null -> dial(number, selected.handle)
            accounts.size == 1 -> dial(number, accounts.first().handle)
            accounts.size > 1 -> { pendingSims = accounts; pendingNumber = number }
            else -> dial(number) // Let Android choose when it exposes no phone accounts.
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

        Scaffold(
            containerColor = scheme.background,
            topBar = {
                if (photoEditor != null || activeContact != null || page == "SIM Manager") {
                    TopAppBar(
                        title = { Text(when {
                            photoEditor != null -> "Caller photo"
                            activeContact != null -> "Contact details"
                            else -> "SIM & network"
                        }) },
                        navigationIcon = {
                            IconButton(onClick = {
                                when {
                                    photoEditor != null -> { photoEditor = null; newPhoto = null }
                                    activeContact != null -> activeContact = null
                                    else -> page = "Settings"
                                }
                            }) { Icon(Icons.Default.ArrowBack, "Back") }
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
                            IconButton(onClick = { page = "SIM Manager" }) {
                                Icon(Icons.Default.SimCard, contentDescription = "SIM and network preferences")
                            }
                            IconButton(onClick = { page = if (page == "Settings") "Recents" else "Settings" }) {
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
                                onClick = { page = tab.label; query = "" },
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
                        onSimSettings = { page = "SIM Manager" }
                    )
                    page == "SIM Manager" -> SimSettingsScreen(repository, simPrefs)
                    page == "Keypad" -> KeypadScreen(
                        value = dialNumber,
                        onValueChange = { dialNumber = it },
                        contacts = contacts,
                        onCall = { requestCall(dialNumber) }
                    )
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
                                calls = recents.filter { query.isBlank() || it.name.contains(query, true) || it.number.contains(query) },
                                photos = photos, loading = isLoading,
                                swipesEnabled = enableSwipes,
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
    calls: List<RecentEntry>, photos: PhotoStore, loading: Boolean,
    swipesEnabled: Boolean, onCall: (String) -> Unit, onMessage: (String) -> Unit,
    onDetails: (RecentEntry) -> Unit
) {
    if (calls.isEmpty()) {
        EmptyState(if (loading) "Loading calls…" else "No recent calls", "Your recent calls will appear here.")
        return
    }
    LazyColumn(contentPadding = PaddingValues(bottom = 22.dp)) {
        item { SectionHeading("Recent calls", "${calls.size} entries") }
        items(calls, key = { "${it.timestamp}:${it.number}:${it.type}" }) { item ->
            val direction = when (item.type) {
                android.provider.CallLog.Calls.MISSED_TYPE -> "Missed · "
                android.provider.CallLog.Calls.OUTGOING_TYPE -> "Outgoing · "
                android.provider.CallLog.Calls.INCOMING_TYPE -> "Incoming · "
                else -> "Call · "
            }
            val time = remember(item.timestamp) {
                java.text.SimpleDateFormat("dd MMM, h:mm a", java.util.Locale.getDefault())
                    .format(java.util.Date(item.timestamp))
            }
            SwipeContactRow(
                name = item.name, number = item.number,
                photoUri = photos.photoFor(item.number) ?: item.photo,
                subtitle = direction + time,
                missed = item.type == android.provider.CallLog.Calls.MISSED_TYPE,
                swipesEnabled = swipesEnabled,
                onCall = { onCall(item.number) },
                onMessage = { onMessage(item.number) },
                onClick = { onDetails(item) }
            )
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
    onSimSettings: () -> Unit
) {
    var themeMenu by remember { mutableStateOf(false) }
    LazyColumn(contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Settings", style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 5.dp, vertical = 8.dp)) }
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
                    supportingContent = { Text("Choose calling line, SMS suggestion & data settings") },
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
            Text("S call · v1.2.0\nBeautiful calls. Simple experience.",
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
