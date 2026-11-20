package com.scall.phone.ui

import android.telecom.Call
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.scall.phone.data.PhoneRepository
import com.scall.phone.data.PhotoStore
import com.scall.phone.telecom.CallBridge
import com.scall.phone.telecom.CallSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Compact incoming-call card in the normal, Telecom-owned in-call Activity.
 * Hiding closes only the Activity; it never rejects or disconnects a call.
 * Android's CallStyle notification stays available for reopening the call.
 *
 * We do not use SYSTEM_ALERT_WINDOW overlays or force a launcher popup;
 * Android decides how/when a heads-up or full-screen call may appear.
 */
@Composable
internal fun IncomingCallerPopup(call: CallSnapshot, onHide: () -> Unit) {
    val context = LocalContext.current
    val number = call.number
    val repository = remember { PhoneRepository(context.applicationContext) }
    val photos = remember { PhotoStore(context.applicationContext) }
    var savedName by remember(number) { mutableStateOf<String?>(null) }
    var photo by remember(number) { mutableStateOf<String?>(null) }
    var buttonActionSent by remember(number) { mutableStateOf(false) }

    LaunchedEffect(number) {
        val found = withContext(Dispatchers.IO) {
            val contact = runCatching { repository.findContact(number) }.getOrNull()
            val image = runCatching { photos.photoFor(number) }.getOrNull() ?: contact?.photo
            contact?.name to image
        }
        savedName = found.first
        photo = found.second
    }
    val shownName = savedName?.takeIf { it.isNotBlank() }
        ?: call.displayName.takeIf { it.isNotBlank() }
        ?: number.ifBlank { "Unknown caller" }

    Box(
        Modifier.fillMaxSize()
            .background(Color(0xFF071810))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(top = 42.dp),
            shape = RoundedCornerShape(26.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF123425))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("INCOMING CALL", color = Color(0xFF9BE7BC),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f))
                    IconButton(onClick = onHide, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Close, "Hide incoming call popup", tint = Color.White)
                    }
                }
                Spacer(Modifier.height(12.dp))
                if (photo != null) {
                    AsyncImage(model = photo, contentDescription = "Caller photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(86.dp).clip(CircleShape))
                } else {
                    Box(Modifier.size(86.dp).background(Color(0xFF245C43), CircleShape),
                        contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Person, contentDescription = null,
                            tint = Color.White, modifier = Modifier.size(47.dp))
                    }
                }
                Spacer(Modifier.height(15.dp))
                Text(shownName, color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 2)
                if (number.isNotBlank() && number != shownName) {
                    Spacer(Modifier.height(4.dp))
                    Text(number, color = Color(0xFFD0E5D9),
                        style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(25.dp))
                Row(Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { if (!buttonActionSent) { buttonActionSent = true; CallBridge.reject() } },
                        enabled = !buttonActionSent,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD83D4D)),
                        modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.CallEnd, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Decline")
                    }
                    Button(onClick = { if (!buttonActionSent) { buttonActionSent = true; CallBridge.answer() } },
                        enabled = !buttonActionSent,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF198B55)),
                        modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Call, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Answer")
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("Or slide right to answer • left to decline",
                    color = Color(0xFFD0E5D9),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center)
                Spacer(Modifier.height(6.dp))
                IncomingSlideControl(
                    onAnswer = { if (!buttonActionSent) { buttonActionSent = true; CallBridge.answer() } },
                    onDecline = { if (!buttonActionSent) { buttonActionSent = true; CallBridge.reject() } }
                )
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onHide) {
                    Text("Hide • Keep ringing", color = Color(0xFFC7E8D6))
                }
            }
        }
        Text("Open the ongoing call notification to show this screen again.",
            color = Color(0xFFC4D6C9), textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp))
    }
}
