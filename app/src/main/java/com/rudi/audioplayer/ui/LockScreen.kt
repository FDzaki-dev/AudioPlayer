package com.rudi.audioplayer.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.rudi.audioplayer.data.AppLockStore
import com.rudi.audioplayer.ui.theme.isSkeuTheme
import com.rudi.audioplayer.ui.theme.isTactileTheme
import com.rudi.audioplayer.ui.theme.skeuEmboss
import com.rudi.audioplayer.ui.theme.tactileEmboss
import kotlinx.coroutines.delay

@Composable
fun LockScreen(
    biometricEnabled: Boolean,
    onVerifyPin: (String) -> AppLockStore.PinResult,
    onUnlocked: () -> Unit,
    onRequestBiometric: () -> Unit,
    // Restores an in-progress lockout countdown after the activity/process is recreated,
    // so leaving the app and coming back doesn't reset the clock on a brute-force attempt.
    initialLockedOutUntil: Long? = null
) {
    var entered by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var lockedOutUntil by remember { mutableStateOf(initialLockedOutUntil) }
    var remainingSeconds by remember { mutableStateOf(0) }
    val haptic = LocalHapticFeedback.current
    // Sebelumnya layar paling "tactile" di app ini (dipencet berkali-kali tiap buka app)
    // justru satu-satunya yang nol haptic — termasuk saat PIN salah, yang cuma keliatan dari
    // teks merah tanpa getaran atau gerakan sama sekali.
    val shakeOffset = remember { Animatable(0f) }
    // Polish — this is the single most-tapped screen in the app (every cold open when App Lock
    // is on) but its number pad was still the plain flat CircleShape from before Tactile/Skeu
    // existed (Batch 79+) — every other frequently-tapped control (mini player, Now Playing
    // transport, Settings) already got the tactile/skeu identity. Apple theme is untouched below
    // (falls through to the original flat circle), so this only changes something for the two
    // themes that are supposed to look "pressable" everywhere.
    val isTactile = isTactileTheme()
    val isSkeu = isSkeuTheme()

    LaunchedEffect(error) {
        if (error) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            shakeOffset.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 300
                    0f at 0
                    -10f at 50
                    10f at 100
                    -8f at 150
                    8f at 200
                    -4f at 250
                    0f at 300
                }
            )
        }
    }

    LaunchedEffect(lockedOutUntil) {
        val until = lockedOutUntil ?: return@LaunchedEffect
        while (true) {
            val remainingMs = until - System.currentTimeMillis()
            if (remainingMs <= 0) {
                remainingSeconds = 0
                lockedOutUntil = null
                break
            }
            remainingSeconds = ((remainingMs + 999) / 1000).toInt()
            delay(1000)
        }
    }

    val locked = lockedOutUntil != null

    fun handleDigit(digit: String) {
        if (locked) return
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        error = false
        if (entered.length < 6) entered += digit
        if (entered.length == 6) {
            when (val result = onVerifyPin(entered)) {
                is AppLockStore.PinResult.Success -> onUnlocked()
                is AppLockStore.PinResult.Wrong -> {
                    error = true
                    entered = ""
                }
                is AppLockStore.PinResult.LockedOut -> {
                    error = true
                    entered = ""
                    lockedOutUntil = result.untilMillis
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            // Batch 111 — LockScreen render di luar Scaffold (MainActivity.kt), tidak dapat
            // contentWindowInsets. Sama root cause dengan WelcomeScreen/PermissionRationale.
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Lock,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("Masukkan PIN", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(24.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.offset(x = shakeOffset.value.dp)
        ) {
            repeat(6) { index ->
                val filled = index < entered.length
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(
                            if (filled) {
                                if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        if (locked) {
            Text(
                "Terlalu banyak percobaan salah. Coba lagi dalam ${formatLockoutTime(remainingSeconds)}.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        } else if (error) {
            Text("PIN salah", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(modifier = Modifier.height(32.dp))

        val rows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9")
        )
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                row.forEach { digit ->
                    PinKey(label = digit, enabled = !locked, isTactile = isTactile, isSkeu = isSkeu) { handleDigit(digit) }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            if (biometricEnabled) {
                RoundGlyphButton(
                    enabled = !locked,
                    isTactile = isTactile,
                    isSkeu = isSkeu,
                    onClick = onRequestBiometric
                ) {
                    Icon(Icons.Default.Fingerprint, contentDescription = "Buka dengan sidik jari", modifier = Modifier.size(28.dp))
                }
            } else {
                Spacer(modifier = Modifier.size(64.dp))
            }
            PinKey(label = "0", enabled = !locked, isTactile = isTactile, isSkeu = isSkeu) { handleDigit("0") }
            RoundGlyphButton(
                enabled = !locked,
                isTactile = isTactile,
                isSkeu = isSkeu,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    error = false
                    if (entered.isNotEmpty()) entered = entered.dropLast(1)
                }
            ) {
                Icon(Icons.Default.Backspace, contentDescription = "Hapus", modifier = Modifier.size(22.dp))
            }
        }
    }
}

private fun formatLockoutTime(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return if (minutes > 0) "${minutes}m ${seconds}d" else "${seconds}d"
}

@Composable
private fun PinKey(
    label: String,
    enabled: Boolean = true,
    isTactile: Boolean = false,
    isSkeu: Boolean = false,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    Box(
        modifier = Modifier
            .size(64.dp)
            .then(
                when {
                    isTactile -> Modifier.tactileEmboss(shape = CircleShape, elevation = 6.dp, pressed = isPressed)
                    isSkeu -> Modifier.skeuEmboss(shape = CircleShape, elevation = 6.dp, pressed = isPressed)
                    else -> Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.surface)
                }
            )
            .alpha(if (enabled) 1f else 0.4f)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            )
            .bouncyPress(interactionSource, pressedScale = 0.9f),
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = MaterialTheme.typography.titleLarge)
    }
}

/** Fingerprint/backspace: same round tactile/skeu treatment as [PinKey], smaller glyph instead of a digit. */
@Composable
private fun RoundGlyphButton(
    enabled: Boolean,
    isTactile: Boolean,
    isSkeu: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    Box(
        modifier = Modifier
            .size(64.dp)
            .then(
                when {
                    isTactile -> Modifier.tactileEmboss(shape = CircleShape, elevation = 6.dp, pressed = isPressed)
                    isSkeu -> Modifier.skeuEmboss(shape = CircleShape, elevation = 6.dp, pressed = isPressed)
                    else -> Modifier.clip(CircleShape)
                }
            )
            .alpha(if (enabled) 1f else 0.4f)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            )
            .bouncyPress(interactionSource, pressedScale = 0.9f),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
