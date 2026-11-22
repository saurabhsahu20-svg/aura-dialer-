package com.scall.phone.ui

import android.telecom.Call
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import coil.compose.AsyncImage
import com.scall.phone.data.PhoneRepository
import com.scall.phone.data.PhotoStore
import com.scall.phone.data.CallerPhotoStyle
import com.scall.phone.telecom.EndCallProgress
import com.scall.phone.telecom.CallBridge
import com.scall.phone.telecom.CallSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import kotlin.math.abs

private val BackBlue = Color(0xFF082A1C)
private val Accept = Color(0xFF2CD987)
private val Decline = Color(0xFFFF6376)

@Composable
internal fun CallerPhotoPreview(name: String, photo: String?, style: CallerPhotoStyle = CallerPhotoStyle()) {
    Box(Modifier.fillMaxSize().clip(RoundedCornerShape(28.dp))) {
        CallPhotoBackground(photo, style)
        Column(
            Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("INCOMING CALL", color = Color.White.copy(alpha = 0.8f),
                fontSize = 11.sp, letterSpacing = 2.sp)
            Spacer(Modifier.height(15.dp))
            Text(name, color = Color.White, fontWeight = FontWeight.Bold,
                fontSize = 25.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(25.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CallEnd, "Decline", tint = Color.White,
                    modifier = Modifier.size(40.dp).background(Decline, CircleShape).padding(8.dp))
                Icon(Icons.Default.Call, "Answer", tint = Color.White,
                    modifier = Modifier.size(40.dp).background(Accept, CircleShape).padding(8.dp))
            }
        }
    }
}

@Composable
private fun CallPhotoBackground(photo: String?, style: CallerPhotoStyle = CallerPhotoStyle()) {
    Box(
        Modifier.fillMaxSize().background(
            Brush.linearGradient(listOf(Color(0xFF051A11), Color(0xFF0D5738), Color(0xFF071B13)))
        )
    ) {
        if (!photo.isNullOrBlank()) {
            AsyncImage(
                model = photo, contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    scaleX = style.zoom
                    scaleY = style.zoom
                    translationX = size.width * style.horizontal
                    translationY = size.height * style.vertical
                }
            )
        } else {
            Canvas(Modifier.fillMaxSize()) {
                val center = Offset(size.width * 0.73f, size.height * 0.42f)
                repeat(4) { index ->
                    drawCircle(
                        color = Color(0xFF56E3A4).copy(alpha = 0.10f - index * 0.016f),
                        radius = size.minDimension * (0.23f + index * 0.14f),
                        center = center, style = Stroke(width = 4.dp.toPx())
                    )
                }
                drawCircle(Color(0xFF26CB88).copy(alpha = 0.14f),
                    radius = size.minDimension * 0.24f,
                    center = center)
            }
        }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
            listOf(Color.Black.copy(alpha = (style.dim * 0.7f).coerceAtMost(0.5f)), Color.Transparent,
                Color(0xFF03140D).copy(alpha = (style.dim + 0.30f).coerceAtMost(0.85f)),
                Color(0xFF021009).copy(alpha = (style.dim + 0.50f).coerceAtMost(0.94f)))
        )))
    }
}

@Composable
internal fun CallScreen(
    call: CallSnapshot?,
    onClose: () -> Unit,
    phoneServiceUnavailable: Boolean = false,
    onRetry: () -> Unit = {}
) {
    val context = LocalContext.current
    val store = remember { PhotoStore(context.applicationContext) }
    val repo = remember { PhoneRepository(context.applicationContext) }
    val number = call?.number.orEmpty()
    var resolvedName by remember(number) { mutableStateOf("") }
    var resolvedPhoto by remember(number) { mutableStateOf<String?>(null) }
    val photoStyle = remember(number) {
        // A corrupted or unreadable photo preference must never crash live call UI.
        runCatching { store.styleFor(number) }.getOrDefault(CallerPhotoStyle())
    }
    LaunchedEffect(number) {
        // Keep photo/contact provider delays and failures off live call controls.
        val (contact, photo) = withContext(Dispatchers.IO) {
            val matching = try {
                repo.findContact(number)
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                android.util.Log.w("SCall", "Caller contact lookup unavailable", error)
                null
            }
            val custom = try {
                store.photoFor(number)
            } catch (error: Exception) {
                android.util.Log.w("SCall", "Caller photo lookup unavailable", error)
                null
            }
            matching to (custom ?: matching?.photo)
        }
        resolvedName = contact?.name.orEmpty()
        resolvedPhoto = photo
    }
    val name = resolvedName.ifBlank { call?.displayName?.ifBlank { number.ifBlank { "Unknown caller" } } ?: "S call" }
    val incoming = call?.state == Call.STATE_RINGING
    var keypadOpen by remember { mutableStateOf(false) }
    val endProgress by CallBridge.endProgress.collectAsState()

    Box(Modifier.fillMaxSize().background(BackBlue)) {
        CallPhotoBackground(resolvedPhoto, photoStyle)
        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
                .padding(horizontal = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(34.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).background(if (incoming) Accept else Color(0xFF7FE0AC), CircleShape))
                Spacer(Modifier.width(8.dp))
                Text(
                    when (call?.state) {
                        Call.STATE_RINGING -> "INCOMING CALL"
                        Call.STATE_DIALING, Call.STATE_CONNECTING -> "CALLING"
                        Call.STATE_ACTIVE -> "CONNECTED"
                        Call.STATE_HOLDING -> "ON HOLD"
                        else -> "S CALL"
                    },
                    color = if (incoming) Color(0xFF78FFC3) else Color.White,
                    fontSize = if (incoming) 17.sp else 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = if (incoming) 1.4.sp else 1.7.sp
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(name, color = Color.White, fontWeight = FontWeight.Bold,
                fontSize = 33.sp, textAlign = TextAlign.Center, lineHeight = 39.sp,
                maxLines = 2)
            Spacer(Modifier.height(5.dp))
            Text(number, color = Color.White.copy(alpha = 0.75f), fontSize = 15.sp,
                textAlign = TextAlign.Center)
            if (!call?.accountLabel.isNullOrBlank()) {
                Spacer(Modifier.height(7.dp))
                Text("${call?.accountLabel}", color = Color.White.copy(alpha = 0.70f),
                    fontSize = 12.sp, textAlign = TextAlign.Center)
            }
            if (call != null && !incoming) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = Color(0xFF052B1C).copy(alpha = 0.72f),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.HeadsetMic, "Current audio output", tint = Color(0xFF9BEECB),
                            modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(7.dp))
                        Text("Audio: ${call.audioRouteLabel}",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold)
                    }
                }
                if (!call.audioRouteError.isNullOrBlank()) {
                    Spacer(Modifier.height(5.dp))
                    Text(
                        call.audioRouteError,
                        color = Color(0xFFFFC6C6),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
            Spacer(Modifier.height(11.dp))
            CallTimer(call)
            Spacer(Modifier.weight(1f))
            when {
                incoming -> {
                    Text("☎  PHONE IS RINGING", color = Color(0xFF78FFC3),
                        fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(7.dp))
                    Text("Slide to answer or reject, or tap a button",
                        color = Color.White.copy(alpha = 0.92f), fontSize = 13.sp)
                    Spacer(Modifier.height(19.dp))
                    IncomingSlideControl(
                        onAnswer = CallBridge::answer,
                        onDecline = CallBridge::reject
                    )
                    Spacer(Modifier.height(65.dp))
                }
                call != null -> {
                    if (keypadOpen) {
                        InCallKeypad(onDigit = CallBridge::dtmf)
                    } else {
                        ActiveCallControls(
                            call = call,
                            onKeypad = { keypadOpen = true }
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                    FilledIconButton(
                        onClick = CallBridge::disconnect,
                        enabled = endProgress != EndCallProgress.ENDING,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Decline,
                            disabledContainerColor = Decline.copy(alpha = 0.65f)
                        ),
                        modifier = Modifier.size(76.dp)
                    ) {
                        if (endProgress == EndCallProgress.ENDING) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(26.dp),
                                color = Color.White, strokeWidth = 3.dp
                            )
                        } else {
                            Icon(Icons.Default.CallEnd, "End call", Modifier.size(32.dp),
                                tint = Color.White)
                        }
                    }
                    if (endProgress != EndCallProgress.IDLE) {
                        Spacer(Modifier.height(7.dp))
                        Text(
                            if (endProgress == EndCallProgress.ENDING) "Ending call…"
                            else "Call is still active. Tap End again.",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                    if (keypadOpen) TextButton(onClick = { keypadOpen = false }) {
                        Text("Close keypad", color = Color.White)
                    } else Spacer(Modifier.height(12.dp))
                    Spacer(Modifier.height(25.dp))
                }
                else -> {
                    if (phoneServiceUnavailable) {
                        Icon(
                            Icons.Default.PhoneDisabled,
                            contentDescription = null,
                            tint = Color(0xFFFFB5B5),
                            modifier = Modifier.size(38.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Phone service not connected",
                            color = Color.White,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(9.dp))
                        Text(
                            "No live call was supplied by Android Telecom. Check that S call is your default Phone app.",
                            color = Color.White.copy(alpha = .85f),
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp
                        )
                        Spacer(Modifier.height(15.dp))
                        Button(onClick = onRetry) {
                            Icon(Icons.Default.Refresh, null)
                            Spacer(Modifier.width(9.dp))
                            Text("Retry call screen")
                        }
                    } else {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(Modifier.height(12.dp))
                        Text("Opening call screen…", color = Color.White)
                    }
                    Spacer(Modifier.height(16.dp))
                    TextButton(onClick = onClose) { Text("Close", color = Color.White) }
                    Spacer(Modifier.height(54.dp))
                }
            }
        }
    }
}

@Composable
private fun CallTimer(call: CallSnapshot?) {
    if (call == null) return
    if (call.state != Call.STATE_ACTIVE || call.connectedAtMillis <= 0) return
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(call.connectedAtMillis) {
        while (isActive) {
            currentTime = System.currentTimeMillis()
            delay(1000)
        }
    }
    val seconds = ((currentTime - call.connectedAtMillis) / 1000).coerceAtLeast(0)
    val display = "%02d:%02d".format(seconds / 60, seconds % 60)
    Text(display, color = Color.White.copy(alpha = 0.92f), fontSize = 15.sp)
}

@Composable
internal fun IncomingSlideControl(onAnswer: () -> Unit, onDecline: () -> Unit) {
    var drag by remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }
    var feedbackGiven by remember { mutableStateOf(false) }
    var actionSent by remember { mutableStateOf(false) }
    val answerNow by rememberUpdatedState(onAnswer)
    val declineNow by rememberUpdatedState(onDecline)
    val animated by animateFloatAsState(
        targetValue = drag,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow),
        label = "Answer decline spring"
    )
    val shown = if (dragging) drag else animated
    val haptics = LocalHapticFeedback.current
    val answerOnce = {
        if (!actionSent) {
            actionSent = true
            answerNow()
        }
    }
    val declineOnce = {
        if (!actionSent) {
            actionSent = true
            declineNow()
        }
    }

    BoxWithConstraints(Modifier.fillMaxWidth().height(98.dp)) {
        val density = LocalDensity.current
        val maximum = with(density) { ((maxWidth - 78.dp) / 2f).toPx() }.coerceAtLeast(48f)
        val threshold = requiredSlideDistance(maximum, INCOMING_SLIDE_FRACTION)
        val result = chooseSwipeAction(shown, threshold)
        val shape = RoundedCornerShape(48.dp)

        Box(Modifier.fillMaxSize().clip(shape)
            .background(Color(0xFF061B12).copy(alpha = 0.53f)),
            contentAlignment = Alignment.Center) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 15.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    Modifier.width(88.dp).clickable(enabled = !actionSent, onClick = declineOnce)
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.CallEnd, "Decline call", tint = Decline,
                        modifier = Modifier.size(29.dp))
                    Spacer(Modifier.height(5.dp))
                    Text(if (result == SwipeAction.NEGATIVE) "Release" else "Decline",
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
                Column(
                    Modifier.width(88.dp).clickable(enabled = !actionSent, onClick = answerOnce)
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Call, "Answer call", tint = Accept,
                        modifier = Modifier.size(29.dp))
                    Spacer(Modifier.height(5.dp))
                    Text(if (result == SwipeAction.POSITIVE) "Release" else "Answer",
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }
            Box(
                Modifier.graphicsLayer { translationX = shown }
                    .size(70.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .pointerInput(maximum, threshold, actionSent) {
                        if (actionSent) return@pointerInput
                        detectHorizontalDragGestures(
                            onDragStart = { dragging = true; feedbackGiven = false },
                            onDragEnd = {
                                val action = chooseSwipeAction(drag, threshold)
                                drag = 0f
                                dragging = false
                                feedbackGiven = false
                                when (action) {
                                    SwipeAction.POSITIVE -> answerOnce()
                                    SwipeAction.NEGATIVE -> declineOnce()
                                    SwipeAction.NONE -> Unit
                                }
                            },
                            onDragCancel = {
                                drag = 0f
                                dragging = false
                                feedbackGiven = false
                            },
                            onHorizontalDrag = { change, amount ->
                                change.consume()
                                drag = (drag + amount).coerceIn(-maximum, maximum)
                                if (abs(drag) >= threshold && !feedbackGiven) {
                                    feedbackGiven = true
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Swipe, "Slide left to decline or right to answer",
                    tint = BackBlue, modifier = Modifier.size(29.dp))
            }
        }
    }
}

@Composable
private fun ActiveCallControls(
    call: CallSnapshot,
    onKeypad: () -> Unit
) {
    var routePickerOpen by remember { mutableStateOf(false) }

    if (routePickerOpen) {
        AlertDialog(
            onDismissRequest = { routePickerOpen = false },
            icon = { Icon(Icons.Default.HeadsetMic, null) },
            title = { Text("Call audio output") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(
                        "Choose a device available through Android Telecom. " +
                            "Audio is managed by your phone and carrier.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (call.availableAudioRoutes.isEmpty()) {
                        Text("No audio routes reported yet. Try again when the call connects.")
                    } else {
                        call.availableAudioRoutes.forEach { route ->
                            val selected = route.title.equals(call.audioRouteLabel, ignoreCase = true) ||
                                (route.kind == "speaker" && call.speakerOn)
                            FilledTonalButton(
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                                onClick = {
                                    routePickerOpen = false
                                    CallBridge.changeAudioRoute(route.key)
                                }
                            ) {
                                Icon(
                                    when (route.kind) {
                                        "speaker" -> Icons.Default.VolumeUp
                                        "bluetooth" -> Icons.Default.Bluetooth
                                        "wired" -> Icons.Default.Headset
                                        else -> Icons.Default.PhoneAndroid
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(route.title, modifier = Modifier.weight(1f))
                                if (selected) Icon(Icons.Default.Check, "Selected")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { routePickerOpen = false }) { Text("Close") }
            }
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            ControlButton(Icons.Default.MicOff, if (call.isMuted) "Unmute" else "Mute",
                active = call.isMuted, onClick = CallBridge::toggleMute)
            ControlButton(Icons.Default.Dialpad, "Keypad", onClick = onKeypad)
            ControlButton(Icons.Default.VolumeUp, "Speaker", active = call.speakerOn,
                enabled = call.availableAudioRoutes.any { it.kind == "speaker" } || call.speakerOn,
                onClick = CallBridge::toggleSpeaker)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            ControlButton(Icons.Default.Pause, if (call.state == Call.STATE_HOLDING) "Resume" else "Hold",
                enabled = call.canHold || call.state == Call.STATE_HOLDING,
                onClick = CallBridge::holdOrUnhold)
            ControlButton(Icons.Default.Add, "Add call", enabled = call.canAddCall,
                onClick = CallBridge::addCall)
            ControlButton(
                if (call.canMerge) Icons.Default.CallMerge else Icons.Default.HeadsetMic,
                if (call.canMerge) "Merge" else "Audio",
                enabled = call.canMerge || call.availableAudioRoutes.isNotEmpty(),
                onClick = if (call.canMerge) CallBridge::merge
                    else { { routePickerOpen = true } }
            )
        }
    }
}

@Composable
private fun ControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Column(Modifier.width(88.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        FilledTonalIconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.size(63.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = if (active) Color(0xFF087347) else Color.White.copy(alpha = 0.17f),
                contentColor = Color.White,
                disabledContainerColor = Color.White.copy(alpha = 0.07f),
                disabledContentColor = Color.White.copy(alpha = 0.35f)
            )
        ) { Icon(icon, label, Modifier.size(25.dp)) }
        Spacer(Modifier.height(5.dp))
        Text(label, color = Color.White.copy(alpha = if (enabled) 0.95f else 0.55f),
            fontSize = 12.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun InCallKeypad(onDigit: (Char) -> Unit) {
    val digits = listOf("1","2","3","4","5","6","7","8","9","*","0","#")
    Column {
        repeat(4) { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                repeat(3) { col ->
                    val digit = digits[row * 3 + col]
                    Box(
                        Modifier.size(width = 85.dp, height = 62.dp)
                            .clickable { onDigit(digit[0]) },
                        contentAlignment = Alignment.Center
                    ) { Text(digit, color = Color.White, fontSize = 29.sp) }
                }
            }
        }
    }
}
