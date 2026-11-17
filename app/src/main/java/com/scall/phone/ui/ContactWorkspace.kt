package com.scall.phone.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import com.scall.phone.data.ContactAccount
import com.scall.phone.data.ContactManager
import com.scall.phone.data.ContactValue
import com.scall.phone.data.ManagedContact
import com.scall.phone.data.RecentEntry
import com.scall.phone.data.normalizeNumber
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** A real Android ContactsContract-backed profile, not a visual placeholder. */
@Composable
internal fun ContactWorkspace(
    manager: ContactManager,
    contactId: Long,
    editing: Boolean,
    onEdit: () -> Unit,
    onSaved: (Long) -> Unit,
    onClose: () -> Unit,
    onDeleted: () -> Unit,
    onCall: (String) -> Unit,
    onMessage: (String) -> Unit,
    onPermissions: () -> Unit,
    history: List<RecentEntry>,
    backSignal: Int = 0
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var version by remember { mutableIntStateOf(0) }
    var loading by remember(contactId) { mutableStateOf(contactId > 0) }
    var person by remember(contactId) { mutableStateOf(ManagedContact()) }
    var numbers by remember(contactId) { mutableStateOf<List<ContactValue>>(emptyList()) }
    var accounts by remember { mutableStateOf<List<ContactAccount>>(emptyList()) }
    var selectedAccount by remember { mutableStateOf<ContactAccount?>(null) }
    var moreDetails by rememberSaveable(contactId) { mutableStateOf(false) }
    var pendingSave by remember { mutableStateOf(false) }
    var dirty by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var duplicateNames by remember { mutableStateOf<List<String>>(emptyList()) }
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmExit by remember { mutableStateOf(false) }
    var chooseNumberAction by remember { mutableStateOf<String?>(null) }

    fun change(updated: ManagedContact) {
        person = updated
        dirty = true
    }
    LaunchedEffect(contactId, version) {
        if (contactId > 0) {
            loading = true
            try {
                val loaded = manager.load(contactId)
                if (loaded != null) person = loaded
                numbers = manager.numbers(contactId)
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) {
                Toast.makeText(context, "Could not load contact", Toast.LENGTH_SHORT).show()
            } finally { loading = false }
        } else {
            person = ManagedContact()
            numbers = emptyList()
            loading = false
        }
        dirty = false
    }
    LaunchedEffect(Unit) {
        try { accounts = manager.accounts() } catch (e: CancellationException) { throw e }
        catch (_: Exception) { accounts = emptyList() }
    }

    fun saveNow() {
        if (saving) return
        saving = true
        scope.launch {
            try {
                val id = manager.save(person, selectedAccount)
                if (id > 0L) {
                    dirty = false
                    Toast.makeText(context, "Contact saved", Toast.LENGTH_SHORT).show()
                    onSaved(id)
                } else Toast.makeText(context, "Contact saved. Refresh contacts to view it.",
                    Toast.LENGTH_LONG).show()
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                Toast.makeText(context, e.message?.take(100) ?: "Could not save contact",
                    Toast.LENGTH_LONG).show()
            } finally { saving = false }
        }
    }
    fun startSave() {
        if (!manager.canWrite()) { onPermissions(); return }
        if (person.fullName.isBlank()) {
            Toast.makeText(context, "Enter a contact name", Toast.LENGTH_SHORT).show()
            return
        }
        val number = person.phones.firstOrNull { it.value.isNotBlank() }?.value.orEmpty()
        if (number.count(Char::isDigit) < 3) {
            Toast.makeText(context, "Enter a phone number", Toast.LENGTH_SHORT).show()
            return
        }
        scope.launch {
            try {
                duplicateNames = if (contactId == 0L) manager.duplicates(number) else emptyList()
                if (duplicateNames.isEmpty()) saveNow()
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { saveNow() }
        }
    }
    fun leaveEditor() { if (dirty) confirmExit = true else onClose() }
    LaunchedEffect(backSignal) {
        if (backSignal > 0) {
            if (editing && dirty) confirmExit = true else onClose()
        }
    }

    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text("Discard unsaved changes?") },
            text = { Text("Your edits haven't been saved to your contacts.") },
            confirmButton = { TextButton(onClick = {
                confirmExit = false
                onClose()
            }) { Text("Discard") } },
            dismissButton = { TextButton(onClick = { confirmExit = false }) { Text("Keep editing") } }
        )
    }
    if (duplicateNames.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { duplicateNames = emptyList() },
            title = { Text("A similar contact exists") },
            text = { Text("This number is already saved for: ${duplicateNames.take(3).joinToString()}. Save a separate contact anyway?") },
            confirmButton = { TextButton(onClick = {
                duplicateNames = emptyList()
                saveNow()
            }) { Text("Save separately") } },
            dismissButton = { TextButton(onClick = { duplicateNames = emptyList() }) { Text("Cancel") } }
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete contact?") },
            text = { Text("This removes the contact from its Android account and may sync the deletion. This cannot be undone.") },
            confirmButton = { TextButton(onClick = {
                confirmDelete = false
                scope.launch {
                    try {
                        if (manager.delete(contactId)) { onDeleted(); Toast.makeText(context, "Contact deleted", Toast.LENGTH_SHORT).show() }
                        else Toast.makeText(context, "Delete not permitted", Toast.LENGTH_SHORT).show()
                    } catch (e: CancellationException) { throw e }
                    catch (_: Exception) { Toast.makeText(context, "Could not delete contact", Toast.LENGTH_SHORT).show() }
                }
            }) { Text("Delete", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }
    if (chooseNumberAction != null) {
        val which = chooseNumberAction
        AlertDialog(
            onDismissRequest = { chooseNumberAction = null },
            title = { Text(if (which == "call") "Choose number to call" else "Choose number for message") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    (numbers.ifEmpty { person.phones }).filter { it.value.isNotBlank() }.forEach { item ->
                        FilledTonalButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                chooseNumberAction = null
                                if (which == "call") onCall(item.value) else onMessage(item.value)
                            }
                        ) { Text("${item.label}: ${item.value}") }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { chooseNumberAction = null }) { Text("Cancel") } }
        )
    }

    if (loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val palette = MaterialTheme.colorScheme
    if (editing) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = ::leaveEditor) { Icon(Icons.Default.Close, "Cancel contact editor") }
                Text(if (contactId == 0L) "New contact" else "Edit contact",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold)
                Button(onClick = ::startSave, enabled = !saving) {
                    Text(if (saving) "Saving…" else "Save")
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 30.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    ContactAvatar(person.fullName.ifBlank { "New" }, person.photo, 90.dp)
                    Text("Profile photo can be changed using Android Contacts",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.onSurfaceVariant, textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 6.dp))
                }
                if (contactId == 0L && accounts.isNotEmpty()) {
                    item {
                        var accountsOpen by remember { mutableStateOf(false) }
                        Box(Modifier.fillMaxWidth()) {
                            OutlinedButton(onClick = { accountsOpen = true }, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.AccountCircle, null)
                                Spacer(Modifier.width(8.dp))
                                Text(selectedAccount?.label ?: "Save to: Device (local)")
                                Icon(Icons.Default.ArrowDropDown, null)
                            }
                            DropdownMenu(expanded = accountsOpen, onDismissRequest = { accountsOpen = false }) {
                                accounts.forEach { account ->
                                    DropdownMenuItem(text = { Text(account.label) }, onClick = {
                                        selectedAccount = account; accountsOpen = false
                                    })
                                }
                            }
                        }
                    }
                }
                item { ContactInput("First name", person.first, { change(person.copy(first = it)) }) }
                item { ContactInput("Last name", person.last, { change(person.copy(last = it)) }) }
                items(person.phones.indices.toList()) { index ->
                    val number = person.phones[index]
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ContactInput(number.label.ifBlank { "Phone" }, number.value,
                            { v -> change(person.copy(phones = person.phones.toMutableList().also {
                                it[index] = number.copy(value = v)
                            })) }, Modifier.weight(1f), KeyboardType.Phone)
                        if (person.phones.size > 1) IconButton(onClick = {
                            change(person.copy(phones = person.phones.filterIndexed { i, _ -> i != index }))
                        }) { Icon(Icons.Default.RemoveCircleOutline, "Remove number") }
                    }
                }
                item {
                    TextButton(onClick = { moreDetails = !moreDetails }, modifier = Modifier.fillMaxWidth()) {
                        Icon(if (moreDetails) Icons.Default.ExpandLess else Icons.Default.AddCircleOutline, null)
                        Spacer(Modifier.width(7.dp))
                        Text(if (moreDetails) "Fewer details" else "+ Add more details")
                    }
                }
                if (moreDetails) {
                    item {
                        Text("PHONE NUMBERS & EMAIL", style = MaterialTheme.typography.titleSmall,
                            color = palette.primary, modifier = Modifier.fillMaxWidth())
                        TextButton(onClick = {
                            change(person.copy(phones = person.phones + ContactValue(label = "Other")))
                        }) { Text("+ Add another number") }
                    }
                    items(person.phones.indices.toList()) { i ->
                        val v = person.phones[i]
                        var expanded by remember { mutableStateOf(false) }
                        Row {
                            TextButton(onClick = { expanded = true }) { Text(v.label) }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                listOf("Mobile", "Home", "Work", "Other", "WhatsApp").forEach { label ->
                                    DropdownMenuItem(text = { Text(label) }, onClick = {
                                        expanded = false
                                        change(person.copy(phones = person.phones.toMutableList().also {
                                            it[i] = v.copy(label = label)
                                        }))
                                    })
                                }
                            }
                        }
                    }
                    items(person.emails.indices.toList()) { i ->
                        val v = person.emails[i]
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ContactInput(v.label + " email", v.value, { value ->
                                change(person.copy(emails = person.emails.toMutableList().also {
                                    it[i] = v.copy(value = value)
                                }))
                            }, Modifier.weight(1f), KeyboardType.Email)
                            IconButton(onClick = {
                                change(person.copy(emails = person.emails.filterIndexed { index, _ -> index != i }))
                            }) { Icon(Icons.Default.RemoveCircleOutline, "Remove email") }
                        }
                    }
                    item {
                        TextButton(onClick = { change(person.copy(emails = person.emails + ContactValue(label = "Personal"))) }) {
                            Text("+ Add another email")
                        }
                    }
                    item {
                        Text("PERSONAL DETAILS", style = MaterialTheme.typography.titleSmall,
                            color = palette.primary, modifier = Modifier.fillMaxWidth())
                    }
                    item { ContactInput("Middle name", person.middle, { change(person.copy(middle = it)) }) }
                    item { ContactInput("Nickname", person.nickname, { change(person.copy(nickname = it)) }) }
                    item { ContactInput("Birthday (YYYY-MM-DD)", person.birthday, { change(person.copy(birthday = it)) }) }
                    item { ContactInput("Anniversary (YYYY-MM-DD)", person.anniversary, { change(person.copy(anniversary = it)) }) }
                    item { ContactInput("Relationship", person.relationship, { change(person.copy(relationship = it)) }) }
                    item {
                        Text("PROFESSIONAL", style = MaterialTheme.typography.titleSmall,
                            color = palette.primary, modifier = Modifier.fillMaxWidth())
                    }
                    item { ContactInput("Company", person.company, { change(person.copy(company = it)) }) }
                    item { ContactInput("Job title", person.jobTitle, { change(person.copy(jobTitle = it)) }) }
                    item { ContactInput("Department", person.department, { change(person.copy(department = it)) }) }
                    item { ContactInput("Website", person.website, { change(person.copy(website = it)) }) }
                    item {
                        Text("ADDRESS", style = MaterialTheme.typography.titleSmall,
                            color = palette.primary, modifier = Modifier.fillMaxWidth())
                    }
                    item { ContactInput("Street", person.street, { change(person.copy(street = it)) }) }
                    item { ContactInput("City / District", person.city, { change(person.copy(city = it)) }) }
                    item { ContactInput("State", person.state, { change(person.copy(state = it)) }) }
                    item { ContactInput("PIN / ZIP", person.postcode, { change(person.copy(postcode = it)) }) }
                    item { ContactInput("Country", person.country, { change(person.copy(country = it)) }) }
                    item { ContactInput("Notes", person.notes, { change(person.copy(notes = it)) }, lines = 3) }
                }
                item {
                    if (!manager.canWrite()) {
                        Text("Editing requires Contacts permission.", color = palette.error)
                        TextButton(onClick = onPermissions) { Text("Grant contact access") }
                    }
                    Text("Existing unrelated contact information is preserved.",
                        style = MaterialTheme.typography.labelSmall, color = palette.onSurfaceVariant)
                }
            }
        }
        return
    }

    val allNumbers = numbers.ifEmpty { person.phones }.filter { it.value.isNotBlank() }
    val clipboard = LocalClipboardManager.current
    Column(Modifier.fillMaxSize()) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onClose) { Icon(Icons.Default.ArrowBack, "Back") }
        Text("Contact profile", style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ContactAvatar(person.fullName.ifBlank { "Contact" }, person.photo, 115.dp)
            Spacer(Modifier.height(9.dp))
            Text(person.fullName.ifBlank { "Saved contact" },
                style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center)
            Text(allNumbers.firstOrNull()?.value.orEmpty(), color = palette.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    scope.launch {
                        try {
                            if (manager.favorite(contactId, !person.favorite)) {
                                person = person.copy(favorite = !person.favorite)
                                version++
                            } else onPermissions()
                        } catch (_: Exception) { Toast.makeText(context, "Cannot change favourite", Toast.LENGTH_SHORT).show() }
                    }
                }) {
                    Icon(if (person.favorite) Icons.Default.Star else Icons.Default.StarBorder,
                        "Toggle favourite", tint = palette.primary)
                }
                IconButton(onClick = {
                    try {
                        val uri = manager.vcardUri(contactId) ?: return@IconButton
                        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                            type = "text/x-vcard"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }, "Share contact"))
                    } catch (_: Exception) { Toast.makeText(context, "Contact sharing unavailable", Toast.LENGTH_SHORT).show() }
                }) { Icon(Icons.Default.Share, "Share contact") }
                IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Default.DeleteOutline, "Delete contact") }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                ContactActionButton("CALL", Icons.Default.Call, Modifier.weight(1f)) {
                    if (allNumbers.size == 1) onCall(allNumbers.first().value)
                    else chooseNumberAction = "call"
                }
                ContactActionButton("MESSAGE", Icons.Default.Message, Modifier.weight(1f)) {
                    if (allNumbers.size == 1) onMessage(allNumbers.first().value)
                    else chooseNumberAction = "message"
                }
                ContactActionButton("EDIT", Icons.Default.Edit, Modifier.weight(1f), onEdit)
            }
        }
        item {
            ContactInfoCard("Phone numbers") {
                allNumbers.forEach { item ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(item.label, style = MaterialTheme.typography.labelSmall, color = palette.onSurfaceVariant)
                            Text(item.value, style = MaterialTheme.typography.bodyLarge)
                        }
                        IconButton(onClick = { clipboard.setText(AnnotatedString(item.value)) }) {
                            Icon(Icons.Default.ContentCopy, "Copy phone number")
                        }
                        IconButton(onClick = { onCall(item.value) }) { Icon(Icons.Default.Call, "Call number") }
                    }
                }
            }
        }
        if (person.nickname.isNotBlank() || person.birthday.isNotBlank() ||
            person.anniversary.isNotBlank() || person.relationship.isNotBlank()) item {
            ContactInfoCard("Personal information") {
                listOf("Nickname" to person.nickname, "Birthday" to person.birthday,
                    "Anniversary" to person.anniversary, "Relationship" to person.relationship)
                    .filter { it.second.isNotBlank() }.forEach { (label, value) ->
                        ContactInfoLine(label, value)
                    }
            }
        }
        if (person.company.isNotBlank() || person.jobTitle.isNotBlank() ||
            person.department.isNotBlank() || person.emails.isNotEmpty()) item {
            ContactInfoCard("Work & email") {
                listOf("Company" to person.company, "Job title" to person.jobTitle,
                    "Department" to person.department).filter { it.second.isNotBlank() }
                    .forEach { (label, value) -> ContactInfoLine(label, value) }
                person.emails.filter { it.value.isNotBlank() }.forEach {
                    ContactInfoLine(it.label + " email", it.value)
                }
            }
        }
        if (person.street.isNotBlank() || person.city.isNotBlank() || person.state.isNotBlank()) item {
            ContactInfoCard("Address") {
                Text(listOf(person.street, person.city, person.state, person.postcode,
                    person.country).filter { it.isNotBlank() }.joinToString(", "))
            }
        }
        if (person.notes.isNotBlank()) item {
            ContactInfoCard("Notes") { Text(person.notes) }
        }
        item {
            val numbersKey = allNumbers.map { normalizeNumber(it.value).takeLast(10) }.toSet()
            val matched = history.filter {
                normalizeNumber(it.number).takeLast(10) in numbersKey
            }.take(12)
            ContactInfoCard("Recent calls on this device") {
                if (matched.isEmpty()) Text("No accessible recent calls for this contact.",
                    style = MaterialTheme.typography.bodySmall, color = palette.onSurfaceVariant)
                matched.forEach { row ->
                    Text("${row.name} · ${java.text.SimpleDateFormat("dd MMM, h:mm a",
                        java.util.Locale.getDefault()).format(java.util.Date(row.timestamp))}",
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
    }
}

@Composable
private fun ContactInput(label: String, value: String, onValue: (String) -> Unit,
                         modifier: Modifier = Modifier.fillMaxWidth(),
                         keyboard: KeyboardType = KeyboardType.Text, lines: Int = 1) {
    OutlinedTextField(
        value = value, onValueChange = onValue, label = { Text(label) },
        modifier = modifier, shape = RoundedCornerShape(16.dp),
        singleLine = lines == 1, maxLines = lines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard)
    )
}

@Composable
private fun ContactActionButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector,
                                modifier: Modifier = Modifier, action: () -> Unit) {
    FilledTonalButton(
        onClick = action, modifier = modifier.heightIn(min = 64.dp),
        shape = RoundedCornerShape(18.dp),
        contentPadding = PaddingValues(horizontal = 3.dp, vertical = 10.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(3.dp))
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ContactInfoCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(title, color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun ContactInfoLine(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
