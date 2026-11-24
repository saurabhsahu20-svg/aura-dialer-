package com.scall.phone.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** User-initiated chat shortcuts; no automatic messaging account lookup. */
@Composable
internal fun DirectChatActions(
    number: String,
    onLaunch: (String, ExternalChatApp) -> Unit,
    modifier: Modifier = Modifier
) {
    val valid = remember(number) { chatPhoneDigits(number) != null }
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { onLaunch(number, ExternalChatApp.WHATSAPP) },
            enabled = valid, modifier = Modifier.weight(1f).heightIn(min = 40.dp),
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = 7.dp, vertical = 0.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF16824C))) {
            Icon(Icons.Default.Chat, null, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(4.dp))
            Text("WhatsApp", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
        OutlinedButton(onClick = { onLaunch(number, ExternalChatApp.TELEGRAM) },
            enabled = valid, modifier = Modifier.weight(1f).heightIn(min = 40.dp),
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = 7.dp, vertical = 0.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF128CC2))) {
            Icon(Icons.Default.Send, null, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(4.dp))
            Text("Telegram", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}
