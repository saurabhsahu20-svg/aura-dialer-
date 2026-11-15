package com.scall.phone.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scall.phone.data.CallerPhotoStyle
import com.scall.phone.data.ContactEntry
import kotlin.math.roundToInt

/** The screen stores the image and its framing only when the user taps Save. */
@Composable
internal fun ProfessionalPhotoEditor(
    contact: ContactEntry,
    currentPhoto: String?,
    candidatePhoto: String?,
    originalStyle: CallerPhotoStyle,
    onPick: () -> Unit,
    onSave: (CallerPhotoStyle) -> Unit,
    onRemove: () -> Unit
) {
    var zoom by remember(contact.number) { mutableFloatStateOf(originalStyle.zoom) }
    var panX by remember(contact.number) { mutableFloatStateOf(originalStyle.horizontal) }
    var panY by remember(contact.number) { mutableFloatStateOf(originalStyle.vertical) }
    var dim by remember(contact.number) { mutableFloatStateOf(originalStyle.dim) }
    val photo = candidatePhoto ?: currentPhoto
    val scheme = MaterialTheme.colorScheme

    // A newly selected image is shown with safe, centered framing before adjustments.
    LaunchedEffect(candidatePhoto) {
        if (candidatePhoto != null) {
            zoom = 1f
            panX = 0f
            panY = 0f
        }
    }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 18.dp).padding(bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            Modifier.fillMaxWidth().padding(top = 3.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Design your call screen", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(3.dp))
                Text(
                    "${contact.name} · personal caller photo",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            Surface(color = scheme.primaryContainer, shape = RoundedCornerShape(12.dp)) {
                Text("LIVE", modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium, color = scheme.primary)
            }
        }

        Box(
            modifier = Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(30.dp))
                .background(Color(0xFF08152B))
                .pointerInput(photo) {
                    if (photo != null) detectTransformGestures { _, pan, gestureZoom, _ ->
                        zoom = (zoom * gestureZoom).coerceIn(1f, 2.4f)
                        val width = size.width.coerceAtLeast(1).toFloat()
                        val height = size.height.coerceAtLeast(1).toFloat()
                        panX = (panX + pan.x / width).coerceIn(-0.45f, 0.45f)
                        panY = (panY + pan.y / height).coerceIn(-0.45f, 0.45f)
                    }
                }
        ) {
            CallerPhotoPreview(
                name = contact.name,
                photo = photo,
                style = CallerPhotoStyle(zoom = zoom, horizontal = panX, vertical = panY, dim = dim)
            )
            if (photo == null) {
                Column(
                    Modifier.align(Alignment.Center).padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, null,
                        modifier = Modifier.size(56.dp), tint = Color.White.copy(alpha = 0.93f))
                    Spacer(Modifier.height(9.dp))
                    Text("Add a caller photo", color = Color.White,
                        style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text("Choose a picture from your gallery", color = Color.White.copy(alpha = 0.70f),
                        style = MaterialTheme.typography.bodySmall)
                }
            }
            Surface(
                modifier = Modifier.align(Alignment.TopStart).padding(14.dp),
                color = Color.Black.copy(alpha = 0.42f),
                shape = RoundedCornerShape(15.dp)
            ) {
                Row(
                    Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.TouchApp, null, tint = Color.White,
                        modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Pinch & drag photo", color = Color.White, fontSize = 11.sp)
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Photo adjustments", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = { zoom = 1f; panX = 0f; panY = 0f; dim = 0.34f }) {
                Icon(Icons.Default.RestartAlt, null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(3.dp))
                Text("Reset")
            }
        }
        Row(Modifier.fillMaxWidth().height(38.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.ZoomIn, "Zoom", tint = scheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(12.dp))
            Slider(value = zoom, onValueChange = { zoom = it }, valueRange = 1f..2.4f,
                modifier = Modifier.weight(1f), enabled = photo != null)
            Spacer(Modifier.width(9.dp))
            Text("${(zoom * 100).roundToInt()}%", style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.width(39.dp), color = scheme.onSurfaceVariant)
        }
        Row(Modifier.fillMaxWidth().height(38.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Contrast, "Photo contrast", tint = scheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(12.dp))
            Slider(value = dim, onValueChange = { dim = it }, valueRange = 0.14f..0.64f,
                modifier = Modifier.weight(1f), enabled = photo != null)
            Spacer(Modifier.width(9.dp))
            Text("Tone", style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.width(39.dp), color = scheme.onSurfaceVariant)
        }

        Spacer(Modifier.height(5.dp))
        Row(
            Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onPick,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(17.dp)
            ) {
                Icon(Icons.Default.PhotoLibrary, null, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (photo == null) "Choose photo" else "Change photo", maxLines = 1,
                    style = MaterialTheme.typography.labelMedium)
            }
            Button(
                onClick = { onSave(CallerPhotoStyle(zoom, panX, panY, dim)) },
                enabled = photo != null,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(17.dp)
            ) {
                Icon(Icons.Default.Check, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text("Save look", fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
        }
        Row(
            Modifier.fillMaxWidth().height(36.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Photos stay on your phone", style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant)
            TextButton(onClick = onRemove, enabled = currentPhoto != null || candidatePhoto != null,
                contentPadding = PaddingValues(horizontal = 4.dp)) {
                Text("Remove photo", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
