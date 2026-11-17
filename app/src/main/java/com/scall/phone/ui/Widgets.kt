package com.scall.phone.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import android.content.ClipboardManager
import android.widget.Toast
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import coil.compose.AsyncImage
import com.scall.phone.data.ContactEntry
import com.scall.phone.ui.theme.Blue
import com.scall.phone.ui.theme.Cyan
import kotlin.math.roundToInt
import kotlin.math.abs

@Composable
internal fun ContactAvatar(name: String, photo: String?, size: Dp = 52.dp) {
    Box(
        Modifier.size(size).clip(CircleShape).background(
            Brush.linearGradient(listOf(Color(0xFF065434), Color(0xFF2DCB8B)))
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

/**
 * Direction-specific call symbol, shown on the caller avatar and next to
 * the label. Symbol + label ensures color is never the only distinction.
 */
@Composable
internal fun CallDirection.callTint(): Color =
    strongColor(MaterialTheme.colorScheme.background.luminance() < 0.45f)

internal fun CallDirection.icon() = when (this) {
    CallDirection.INCOMING -> Icons.Default.CallReceived
    CallDirection.OUTGOING -> Icons.Default.CallMade
    CallDirection.MISSED -> Icons.Default.CallMissed
    CallDirection.OTHER -> Icons.Default.Phone
}

/**
 * Thicker, rounded 3.4dp-style direction arrows, drawn from the same shape
 * geometry in Light and Dark themes. This is intentionally more visible
 * than Material's small 15dp default icon at avatar size.
 */
@Composable
internal fun CallDirectionSymbol(
    direction: CallDirection,
    modifier: Modifier = Modifier,
    description: String? = null
) {
    val tint = direction.callTint()
    if (direction == CallDirection.OTHER) {
        Icon(Icons.Default.Phone, contentDescription = description, tint = tint, modifier = modifier)
    } else {
        val semanticModifier = if (description == null) modifier
            else modifier.semantics { contentDescription = description }
        Canvas(semanticModifier) {
            val w = size.width
            val h = size.height
            // Stroke scales with icon size; the rounded ends remain distinct at 18dp.
            val stroke = size.minDimension * 0.165f
            fun segment(ax: Float, ay: Float, bx: Float, by: Float) {
                drawLine(
                    color = tint,
                    start = Offset(w * ax, h * ay),
                    end = Offset(w * bx, h * by),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
            }
            when (direction) {
                CallDirection.INCOMING -> {
                    // Received: bold descending arrow down-left.
                    segment(.74f, .24f, .28f, .70f)
                    segment(.28f, .70f, .28f, .40f)
                    segment(.28f, .70f, .58f, .70f)
                }
                CallDirection.OUTGOING -> {
                    // Made: bold rising arrow up-right.
                    segment(.26f, .74f, .72f, .28f)
                    segment(.72f, .28f, .42f, .28f)
                    segment(.72f, .28f, .72f, .58f)
                }
                CallDirection.MISSED -> {
                    // Missed: broken/kinked path with down-right arrowhead.
                    segment(.17f, .29f, .38f, .50f)
                    segment(.38f, .50f, .54f, .34f)
                    segment(.54f, .34f, .82f, .62f)
                    segment(.82f, .62f, .82f, .33f)
                    segment(.82f, .62f, .53f, .62f)
                }
                CallDirection.OTHER -> Unit
            }
        }
    }
}

@Composable
internal fun CallDirectionBadge(direction: CallDirection, modifier: Modifier = Modifier) {
    val tint = direction.callTint()
    Surface(
        modifier = modifier.size(30.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, tint.copy(alpha = 0.7f)),
        shadowElevation = 2.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(2.dp)
                .background(tint.copy(alpha = 0.17f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            CallDirectionSymbol(
                direction = direction,
                description = "${direction.label} call",
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

/** Gesture follows the finger without animation lag, springs back on release, haptics once. */
@Composable
internal fun SwipeContactRow(
    name: String,
    number: String,
    photoUri: String?,
    subtitle: String,
    swipesEnabled: Boolean,
    missed: Boolean = false,
    callDirection: CallDirection? = null,
    onCall: () -> Unit,
    onMessage: () -> Unit,
    onClick: () -> Unit,
    onPhoto: (() -> Unit)? = null
) {
    var offset by remember(name, number) { mutableFloatStateOf(0f) }
    var dragging by remember(name, number) { mutableStateOf(false) }
    var notified by remember(name, number) { mutableStateOf(false) }
    val callAction by rememberUpdatedState(onCall)
    val messageAction by rememberUpdatedState(onMessage)
    val springOffset by animateFloatAsState(
        targetValue = offset,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow),
        label = "Contact row spring"
    )
    val shown = if (dragging) offset else springOffset
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val surface = MaterialTheme.colorScheme.surface
    val primary = MaterialTheme.colorScheme.primary
    val revealingCall = shown >= 0f

    BoxWithConstraints(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(20.dp))
    ) {
        // Track geometry follows screen width: a short flick can never place a call.
        val rowTravel = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
        val threshold = requiredSlideDistance(rowTravel, CONTACT_SLIDE_FRACTION)
        val maximum = rowTravel * 0.86f
        val reached = abs(shown) >= threshold
        Row(
            modifier = Modifier.fillMaxWidth().height(76.dp).background(
                if (revealingCall) Color(0xFFDEF7E9) else Color(0xFFE2EDFF)
            ).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (revealingCall) Arrangement.Start else Arrangement.End
        ) {
            Icon(if (revealingCall) Icons.Default.Call else Icons.Default.Message,
                contentDescription = null, tint = if (revealingCall) Color(0xFF087A45) else Blue,
                modifier = Modifier.size(22.dp))
            if (abs(shown) >= rowTravel * 0.15f) {
                Spacer(Modifier.width(5.dp))
                Text(if (reached) "Release now" else "Slide 68%",
                    color = if (revealingCall) Color(0xFF087A45) else Blue,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().height(76.dp)
                .graphicsLayer { translationX = shown }
                .clip(RoundedCornerShape(20.dp))
                .background(surface)
                .pointerInput(swipesEnabled, number, maximum, threshold) {
                    if (swipesEnabled) detectHorizontalDragGestures(
                        onDragStart = {
                            dragging = true
                            notified = false
                        },
                        onDragEnd = {
                            val result = chooseSwipeAction(offset, threshold)
                            dragging = false
                            offset = 0f
                            notified = false
                            when (result) {
                                SwipeAction.POSITIVE -> callAction()
                                SwipeAction.NEGATIVE -> messageAction()
                                SwipeAction.NONE -> Unit
                            }
                        },
                        onDragCancel = {
                            dragging = false
                            offset = 0f
                            notified = false
                        },
                        onHorizontalDrag = { change, drag ->
                            change.consume()
                            offset = (offset + drag).coerceIn(-maximum, maximum)
                            if (abs(offset) >= threshold && !notified) {
                                notified = true
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        }
                    )
                }
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(54.dp)) {
                ContactAvatar(name, photoUri, 47.dp)
                if (callDirection != null) {
                    CallDirectionBadge(
                        direction = callDirection,
                        modifier = Modifier.align(Alignment.BottomEnd).offset(x = 2.dp, y = 2.dp)
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.bodyLarge,
                    color = if (missed) CallDirection.MISSED.callTint() else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                if (callDirection != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CallDirectionSymbol(
                            direction = callDirection,
                            modifier = Modifier.size(20.dp),
                            description = "${callDirection.label} direction"
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            callDirection.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = callDirection.callTint(),
                            maxLines = 1
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                } else {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }
            if (onPhoto != null) {
                IconButton(onClick = onPhoto, modifier = Modifier.size(42.dp)) {
                    Icon(Icons.Default.AddPhotoAlternate, "Set caller photo for $name",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(19.dp))
                }
            }
            IconButton(onClick = onCall, modifier = Modifier.size(46.dp)) {
                Icon(Icons.Default.Call, "Call $name", tint = primary, modifier = Modifier.size(20.dp))
            }
        }
    }
}

private val keypad = listOf(
    "1" to "", "2" to "ABC", "3" to "DEF",
    "4" to "GHI", "5" to "JKL", "6" to "MNO",
    "7" to "PQRS", "8" to "TUV", "9" to "WXYZ",
    "*" to "", "0" to "+", "#" to ""
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DialKey(
    digit: String,
    letters: String,
    modifier: Modifier = Modifier,
    onTap: () -> Unit,
    onLongTap: (() -> Unit)? = null
) {
    val touch = remember { MutableInteractionSource() }
    val pressed by touch.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.91f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "Dial key pressed"
    )
    val haptic = LocalHapticFeedback.current
    val bg = if (pressed) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerHigh
    val clickDescription = if (digit == "0") "0, long press for plus"
        else if (letters.isBlank()) digit else "$digit, $letters"
    Box(
        modifier = modifier.fillMaxHeight().graphicsLayer {
            scaleX = scale
            scaleY = scale
        }.clip(RoundedCornerShape(23.dp))
            .background(bg)
            .combinedClickable(
                interactionSource = touch,
                indication = null,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onTap()
                },
                onLongClick = onLongTap?.let { callback ->
                    {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        callback()
                    }
                }
            )
            .semantics { contentDescription = clickDescription },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                digit,
                fontSize = 29.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                letters.ifBlank { " " },
                fontSize = 10.sp,
                letterSpacing = 1.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Responsive tactile keypad; keeps the Call and Backspace actions visible on small screens. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun KeypadScreen(
    value: String,
    onValueChange: (String) -> Unit,
    contacts: List<ContactEntry>,
    onCall: () -> Unit,
    onMessage: () -> Unit,
    onSimSettings: () -> Unit,
    onBack: () -> Unit,
    simLabel: String
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val hasNumber = value.any { it.isDigit() }
    val matches = remember(value, contacts) {
        val digits = value.filter(Char::isDigit)
        if (digits.length < 2) emptyList() else contacts.filter {
            it.number.filter(Char::isDigit).contains(digits) ||
                t9(it.name).startsWith(digits)
        }.take(3)
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 17.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(46.dp)) {
                Icon(Icons.Default.ArrowBack, "Return to previous screen")
            }
            Text(
                "Keypad",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = onSimSettings) {
                Icon(Icons.Default.SimCard, contentDescription = null, Modifier.size(18.dp))
                Spacer(Modifier.width(5.dp))
                Text("SIM")
                Icon(Icons.Default.ChevronRight, null, Modifier.size(16.dp))
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(22.dp)
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 11.dp)) {
                Text(
                    if (value.isBlank()) "Enter a number" else "Phone number",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium
                )
                Row(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 55.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(Modifier.width(6.dp))
                    BasicTextField(
                        value = value,
                        onValueChange = {},
                        readOnly = true,
                        singleLine = true,
                        modifier = Modifier.weight(1f).semantics {
                            contentDescription = if (value.isEmpty()) "No number dialed"
                                else "Dialed phone number $value"
                        },
                        textStyle = TextStyle(
                            fontSize = 29.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface,
                            letterSpacing = 0.7.sp
                        ),
                        decorationBox = { inner ->
                            Box(
                                Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                if (value.isEmpty()) {
                                    Text("● ● ●", fontSize = 22.sp,
                                        color = MaterialTheme.colorScheme.outlineVariant)
                                }
                                inner()
                            }
                        }
                    )
                    TextButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            if (value.isNotBlank()) {
                                onValueChange("")
                            } else {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE)
                                    as? ClipboardManager
                                val clip = clipboard?.primaryClip
                                val copied = if (clip != null && clip.itemCount > 0) {
                                    clip.getItemAt(0).coerceToText(context).toString()
                                } else ""
                                val pasted = cleanDialNumber(copied)
                                if (pasted.isNotBlank()) onValueChange(pasted)
                                else Toast.makeText(context, "No phone number to paste",
                                    Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) { Text(if (value.isNotBlank()) "Clear" else "Paste") }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 7.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Call,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "Calling SIM: $simLabel",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
                maxLines = 2
            )
        }
        BoxWithConstraints(
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            // Use the available space instead of the old fixed 75dp keypad rows.
            // On compact screens, retain 72dp touch targets and scroll if needed.
            val rowHeight = adaptiveDialRowHeight(maxHeight.value, matches.size).dp
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                verticalArrangement = if (matches.isEmpty()) Arrangement.SpaceEvenly
                    else Arrangement.Top
            ) {
                if (matches.isNotEmpty()) {
                Text(
                    "Matching contacts",
                    modifier = Modifier.padding(start = 9.dp, bottom = 6.dp, top = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                matches.forEach { item ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 5.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(15.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onValueChange(cleanDialNumber(item.number))
                            }.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ContactAvatar(item.name, item.photo, 30.dp)
                            Spacer(Modifier.width(9.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.name, style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold, maxLines = 1)
                                Text(item.number, style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                            }
                            Icon(Icons.Default.ChevronRight, null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
                if (matches.isNotEmpty()) Spacer(Modifier.height(7.dp))
                for (row in 0 until 4) {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(rowHeight)
                            .padding(vertical = 5.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                    for (col in 0 until 3) {
                        val item = keypad[row * 3 + col]
                        DialKey(
                            digit = item.first,
                            letters = item.second,
                            modifier = Modifier.weight(1f),
                            onTap = {
                                onValueChange(appendDialKey(value, item.first))
                            },
                            onLongTap = if (item.first == "0") {
                                { onValueChange(appendDialKey(value, "+")) }
                            } else null
                        )
                    }
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FilledTonalIconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onMessage()
                },
                enabled = hasNumber,
                modifier = Modifier.size(58.dp)
            ) {
                Icon(Icons.Default.Message, "Message this number", Modifier.size(23.dp))
            }
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onCall()
                },
                enabled = hasNumber,
                modifier = Modifier.weight(1f).height(60.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF118458),
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.Call, null, Modifier.size(23.dp))
                Spacer(Modifier.width(9.dp))
                Text("Call", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            val deleteTouch = remember { MutableInteractionSource() }
            val isDeletePressed by deleteTouch.collectIsPressedAsState()
            val deleteScale by animateFloatAsState(
                targetValue = if (isDeletePressed) 0.92f else 1f,
                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                label = "Backspace press"
            )
            Box(
                modifier = Modifier.size(58.dp).graphicsLayer {
                    scaleX = deleteScale
                    scaleY = deleteScale
                }.clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .combinedClickable(
                        interactionSource = deleteTouch,
                        indication = null,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            if (value.isNotEmpty()) onValueChange(value.dropLast(1))
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onValueChange("")
                        }
                    ).semantics {
                        contentDescription = "Delete last digit, long press to clear all"
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Backspace, null, Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
