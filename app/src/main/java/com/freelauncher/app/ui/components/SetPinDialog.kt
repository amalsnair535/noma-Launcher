package com.freelauncher.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.freelauncher.app.ui.util.LauncherHaptics

@Composable
fun SetPinDialog(
    existingPin: String = "",
    onDismiss: () -> Unit,
    onConfirmPin: (String) -> Unit
) {
    val context = LocalContext.current
    val requiresOldPin = existingPin.isNotBlank()
    var currentStep by remember { mutableIntStateOf(if (requiresOldPin) 0 else 1) }

    var oldPinInput by remember { mutableStateOf("") }
    var newPinInput by remember { mutableStateOf("") }
    var confirmPinInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val currentPin = when (currentStep) {
        0 -> oldPinInput
        1 -> newPinInput
        else -> confirmPinInput
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = when (currentStep) {
                                0 -> "Enter Old PIN"
                                1 -> "Set New 4-Digit PIN"
                                else -> "Confirm New PIN"
                            },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp).testTag("close_set_pin_dialog")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                Text(
                    text = when (currentStep) {
                        0 -> "Enter your current 4-digit PIN to authorize change"
                        1 -> "Create a new secret 4-digit PIN for Parent / Child lock"
                        else -> "Re-enter the new 4 digits to confirm PIN"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }

                // 4 PIN Dot Indicators
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    for (i in 0..3) {
                        val isFilled = i < currentPin.length
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isFilled) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .border(
                                    1.dp,
                                    if (isFilled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                    CircleShape
                                )
                        )
                    }
                }

                // Numpad 0-9 & Backspace
                PinNumpad(
                    onDigitClick = { digit ->
                        if (currentPin.length < 4) {
                            LauncherHaptics.playClick(context)
                            val updated = currentPin + digit
                            when (currentStep) {
                                0 -> {
                                    oldPinInput = updated
                                    if (oldPinInput.length == 4) {
                                        if (oldPinInput == existingPin) {
                                            errorMessage = null
                                            currentStep = 1
                                        } else {
                                            LauncherHaptics.playClick(context)
                                            errorMessage = "Incorrect old PIN. Try again."
                                            oldPinInput = ""
                                        }
                                    }
                                }
                                1 -> {
                                    newPinInput = updated
                                    if (newPinInput.length == 4) {
                                        errorMessage = null
                                        currentStep = 2
                                    }
                                }
                                2 -> {
                                    confirmPinInput = updated
                                    if (confirmPinInput.length == 4) {
                                        if (confirmPinInput == newPinInput) {
                                            onConfirmPin(confirmPinInput)
                                        } else {
                                            LauncherHaptics.playClick(context)
                                            errorMessage = "PINs did not match. Try again."
                                            newPinInput = ""
                                            confirmPinInput = ""
                                            currentStep = 1
                                        }
                                    }
                                }
                            }
                        }
                    },
                    onBackspaceClick = {
                        if (currentPin.isNotEmpty()) {
                            LauncherHaptics.playClick(context)
                            when (currentStep) {
                                0 -> oldPinInput = oldPinInput.dropLast(1)
                                1 -> newPinInput = newPinInput.dropLast(1)
                                2 -> confirmPinInput = confirmPinInput.dropLast(1)
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun PinNumpad(
    onDigitClick: (String) -> Unit,
    onBackspaceClick: () -> Unit
) {
    val digits = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "<")
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        digits.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                row.forEach { key ->
                    if (key.isEmpty()) {
                        Spacer(modifier = Modifier.size(64.dp, 48.dp))
                    } else if (key == "<") {
                        Surface(
                            onClick = onBackspaceClick,
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp, 48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.Backspace,
                                    contentDescription = "Backspace",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    } else {
                        Surface(
                            onClick = { onDigitClick(key) },
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(64.dp, 48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = key,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
