package com.scall.phone.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import coil.compose.AsyncImage
import com.scall.phone.data.ContactEntry
import com.scall.phone.ui.theme.Blue
import com.scall.phone.ui.theme.Cyan
import kotlin.math.roundToInt

@Composable
internal fun ContactAvatar(name: String, photo: String?, size: Dp = 52.dp) {
    Box(
        Modifier.size(size).clip(CircleShape).background(
            Brush.linearGradient(listOf(Color(0xFF1352B9), Color(0xFF41B7F7)))
        ), contentAlignment = Alignment.Center
    ) {
        if (!photo.isNullOrBlank()) {
            AsyncImage(
                model = photo, contentDescription = "Photo of $name",
                modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
            )
        } else {
            Text(name.take(1).uppercase(), color = Color.White,
                fontSize = (size.value * 0.40f).sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** Right swipe calls, left swipe messages. A separate call button is always available. */
@Composable
internal fun SwipeContactRow(
    name: String,
    number: String,
    photoUri: String?,
    subtitle: String,
    swipesEnabled: Boolean,
    missed: Boolean = false,
    onCall: () -> Unit,
    onMessage: () -> Unit,
    onClick: () -> Unit,
    onPhoto: (() -> Unit)? = null
) {
    var offset by remember(name, number) { mutableFloatStateOf(0f) }
    val animated by animateFloatAsState(offset, label = "Swipe contact")
    val density = LocalDensity.current
    val threshold = with(density) { 86.dp.toPx() }
    val maximum = with(density) { 116.dp.toPx() }
    val haptics = LocalHapticFeedback.current
    val surface = MaterialTheme.colorScheme.surface
    val primary = MaterialTheme.colorScheme.primary
    Box(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(17.dp))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(72.dp).background(if (animated > 0) Color(0xFFD9F8E9)
                else Color(0xFFDFECFF)).padding(horizontal = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (animated >= 0) Arrangement.Start else Arrangement.End
        ) {
            Icon(if (animated >= 0) Icons.Default.Call else Icons.Default.Message,
                contentDescription = if (animated >= 0) "Slide to call" else "Slide to message",
                tint = if (animated >= 0) Color(0xFF15995B) else Blue)
        }
        Row(
            modifier = Modifier.fillMaxWidth().height(72.dp)
                .graphicsLayer { translationX = animated }
                .clip(RoundedCornerShape(17.dp))
                .background(surface)
                .pointerInput(swipesEnabled, number) {
                    if (swipesEnabled) detectHorizontalDragGestures(
                        onDragEnd = {
                            val action = offset
                            offset = 0f
                            if (action > threshold) {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onCall()
                            } else if (action < -threshold) {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onMessage()
                            }
                        },
                        onDragCancel = { offset = 0f },
                        onHorizontalDrag = { change, drag ->
                            change.consume()
                            offset = (offset + drag).coerceIn(-maximum, maximum)
                        }
                    )
                }
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ContactAvatar(name, photoUri, 47.dp)
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.bodyLarge,
                    color = if (missed) Color(0xFFE05F65) else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            if (onPhoto != null) {
                IconButton(onClick = onPhoto, modifier = Modifier.size(38.dp)) {
                    Icon(Icons.Default.AddPhotoAlternate, "Set caller photo for $name",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(19.dp))
                }
            }
            IconButton(onClick = onCall, modifier = Modifier.size(42.dp)) {
                Icon(Icons.Default.Call, "Call $name", tint = primary, modifier = Modifier.size(20.dp))
            }
        }
    }
}

private val keypad = listOf(
    Triple("1", "", ""), Triple("2", "ABC", ""), Triple("3", "DEF", ""),
    Triple("4", "GHI", ""), Triple("5", "JKL", ""), Triple("6", "MNO", ""),
    Triple("7", "PQRS", ""), Triple("8", "TUV", ""), Triple("9", "WXYZ", ""),
    Triple("*", "", ""), Triple("0", "+", ""), Triple("#", "", "")
)

private fun t9(value: String): String = buildString {
    for (character in value.uppercase()) {
        val digit = when (character) {
            in 'A'..'C' -> '2'; in 'D'..'F' -> '3'; in 'G'..'I' -> '4'
            in 'J'..'L' -> '5'; in 'M'..'O' -> '6'; in 'P'..'S' -> '7'
            in 'T'..'V' -> '8'; in 'W'..'Z' -> '9'; else -> null
        }
        if (digit != null) append(digit)
    }
}

@Composable
internal fun KeypadScreen(
    value: String,
    onValueChange: (String) -> Unit,
    contacts: List<ContactEntry>,
    onCall: () -> Unit
) {
    val matches = remember(value, contacts) {
        if (value.length < 2) emptyList() else contacts.filter {
            it.number.filter(Char::isDigit).contains(value.filter(Char::isDigit)) || t9(it.name).startsWith(value)
        }.take(2)
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(10.dp))
        Text("Keypad", style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
        Spacer(Modifier.height(18.dp))
        BasicTextField(
            value = value,
            onValueChange = { onValueChange(it.filter { char -> char.isDigit() || char in "+*#(),; -" }) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
            textStyle = TextStyle(
                textAlign = TextAlign.Center,
                fontSize = 30.sp,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            decorationBox = { inner ->
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    if (value.isBlank()) Text("Enter phone number", fontSize = 19.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    inner()
                }
            }
        )
        Spacer(Modifier.height(6.dp))
        Column(Modifier.fillMaxWidth().height(70.dp)) {
            matches.forEach { item ->
                Row(
                    Modifier.fillMaxWidth().clickable { onValueChange(item.number) }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ContactAvatar(item.name, item.photo, 24.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(item.name, modifier = Modifier.weight(1f), maxLines = 1,
                        style = MaterialTheme.typography.bodySmall)
                    Text(item.number, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.weight(1f))
        for (row in 0 until 4) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                for (col in 0 until 3) {
                    val item = keypad[row * 3 + col]
                    Column(
                        modifier = Modifier.weight(1f).height(74.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .clickable { onValueChange(value + item.first) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(item.first, fontSize = 29.sp, fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface)
                        Text(item.second, fontSize = 10.sp, letterSpacing = 1.3.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.weight(1f))
            Button(
                onClick = onCall, enabled = value.isNotBlank(),
                modifier = Modifier.width(160.dp).height(58.dp),
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Blue)
            ) {
                Icon(Icons.Default.Call, "Call", modifier = Modifier.size(23.dp))
                Spacer(Modifier.width(10.dp))
                Text("Call", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                if (value.isNotBlank()) IconButton(onClick = { onValueChange(value.dropLast(1)) }) {
                    Icon(Icons.Default.Backspace, "Delete digit", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
    }
}
