package com.freelauncher.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

data class AtmospherePreset(
    val name: String,
    val topColor: Color,
    val middleColor: Color,
    val bottomColor: Color,
    val isDark: Boolean = true
)

@Composable
fun AtmosphericThemeCreatorDialog(
    onSave: (String, List<Long>, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var themeName by remember { mutableStateOf("My Atmosphere") }
    var color1 by remember { mutableStateOf(Color(0xFF1E293B)) }
    var color2 by remember { mutableStateOf(Color(0xFF0F172A)) }
    var color3 by remember { mutableStateOf(Color(0xFF020617)) }
    var isDark by remember { mutableStateOf(true) }

    val gradientPresets = remember {
        listOf(
            AtmospherePreset("Midnight Slate", Color(0xFF1E293B), Color(0xFF0F172A), Color(0xFF020617), true),
            AtmospherePreset("Cyber Violet", Color(0xFF2E1065), Color(0xFF1E1B4B), Color(0xFF020617), true),
            AtmospherePreset("Emerald Forest", Color(0xFF064E3B), Color(0xFF022C22), Color(0xFF000000), true),
            AtmospherePreset("Oceanic Deep", Color(0xFF164E63), Color(0xFF083344), Color(0xFF020617), true),
            AtmospherePreset("Sunset Gold", Color(0xFF451A03), Color(0xFF78350F), Color(0xFF0F172A), true),
            AtmospherePreset("Deep Crimson", Color(0xFF4C0519), Color(0xFF881337), Color(0xFF000000), true),
            AtmospherePreset("Nordic Slate", Color(0xFF334155), Color(0xFF1E293B), Color(0xFF0F172A), true),
            AtmospherePreset("OLED Stealth", Color(0xFF0F0F0F), Color(0xFF050505), Color(0xFF000000), true),
            AtmospherePreset("Pastel Dawn", Color(0xFFF1F5F9), Color(0xFFE2E8F0), Color(0xFFCBD5E1), false)
        )
    }

    val presetSwatches = remember {
        listOf(
            Color(0xFF000000), Color(0xFF020617), Color(0xFF0F172A), Color(0xFF1E293B),
            Color(0xFF1E1B4B), Color(0xFF2E1065), Color(0xFF064E3B), Color(0xFF164E63),
            Color(0xFF451A03), Color(0xFF4C0519), Color(0xFF334155), Color(0xFF64748B),
            Color(0xFF94A3B8), Color(0xFFCBD5E1), Color(0xFFF1F5F9), Color(0xFFFFFFFF)
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(28.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            LazyColumn(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                item(key = "header") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Create Atmosphere",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                }

                // Live Gradient Preview Card
                item(key = "preview") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brush.verticalGradient(listOf(color1, color2, color3)))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Live Gradient Preview",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = if (isDark) Color.White.copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.85f)
                        )
                    }
                }

                // Name Input
                item(key = "name_input") {
                    OutlinedTextField(
                        value = themeName,
                        onValueChange = { themeName = it },
                        label = { Text("Atmosphere Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                // 1-Tap Curated Gradient Presets
                item(key = "gradient_presets") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Curated Gradient Presets (1-Tap)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            gradientPresets.forEach { preset ->
                                Surface(
                                    onClick = {
                                        color1 = preset.topColor
                                        color2 = preset.middleColor
                                        color3 = preset.bottomColor
                                        isDark = preset.isDark
                                        if (themeName == "My Atmosphere" || themeName.isBlank()) {
                                            themeName = preset.name
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        0.75.dp,
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    Brush.verticalGradient(
                                                        listOf(preset.topColor, preset.middleColor, preset.bottomColor)
                                                    )
                                                )
                                        )
                                        Text(
                                            text = preset.name,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Individual Node Customization Header
                item(key = "colors_header") {
                    Text(
                        text = "Customize Colors (Top, Middle, Bottom)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                item(key = "color1_selector") {
                    ColorNodeSelector(label = "Top Color", selectedColor = color1, swatches = presetSwatches) { color1 = it }
                }

                item(key = "color2_selector") {
                    ColorNodeSelector(label = "Middle Color", selectedColor = color2, swatches = presetSwatches) { color2 = it }
                }

                item(key = "color3_selector") {
                    ColorNodeSelector(label = "Bottom Color", selectedColor = color3, swatches = presetSwatches) { color3 = it }
                }

                item(key = "dark_switch") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Dark Atmosphere (OLED Friendly)",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                        )
                        Switch(checked = isDark, onCheckedChange = { isDark = it })
                    }
                }

                item(key = "save_button") {
                    Button(
                        onClick = {
                            val colorLongs = listOf(
                                color1.toArgb().toLong() and 0xFFFFFFFFL,
                                color2.toArgb().toLong() and 0xFFFFFFFFL,
                                color3.toArgb().toLong() and 0xFFFFFFFFL
                            )
                            onSave(themeName.ifBlank { "My Atmosphere" }, colorLongs, isDark)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(14.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save & Apply Atmosphere", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }
}

@Composable
fun ColorNodeSelector(
    label: String,
    selectedColor: Color,
    swatches: List<Color>,
    onColorSelect: (Color) -> Unit
) {
    var isSpectrumMode by remember { mutableStateOf(false) }

    val hsv = remember(selectedColor) {
        val array = FloatArray(3)
        android.graphics.Color.colorToHSV(selectedColor.toArgb(), array)
        array
    }
    var hue by remember(selectedColor) { mutableFloatStateOf(hsv[0]) }
    var brightness by remember(selectedColor) { mutableFloatStateOf(hsv[2]) }

    val rainbowBrush = remember {
        Brush.horizontalGradient(
            listOf(
                Color.Red, Color.Yellow, Color.Green,
                Color.Cyan, Color.Blue, Color.Magenta, Color.Red
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(selectedColor)
                        .border(1.dp, Color.Gray.copy(alpha = 0.4f), CircleShape)
                )
                Text(text = label, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
            }
            TextButton(
                onClick = { isSpectrumMode = !isSpectrumMode },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Icon(
                    imageVector = if (isSpectrumMode) Icons.Default.Palette else Icons.Default.ColorLens,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isSpectrumMode) "Palette" else "Hue Spectrum", fontSize = 11.sp)
            }
        }

        if (isSpectrumMode) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // Rainbow Hue Spectrum Slider
                Text(
                    text = "Color Hue Spectrum",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(rainbowBrush)
                )
                Slider(
                    value = hue,
                    onValueChange = {
                        hue = it
                        val newArgb = android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.85f, brightness.coerceAtLeast(0.15f)))
                        onColorSelect(Color(newArgb))
                    },
                    valueRange = 0f..360f,
                    modifier = Modifier.fillMaxWidth()
                )

                // Brightness / Shade Slider
                Text(
                    text = "Shade / Brightness",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Slider(
                    value = brightness,
                    onValueChange = {
                        brightness = it
                        val newArgb = android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.85f, brightness))
                        onColorSelect(Color(newArgb))
                    },
                    valueRange = 0.05f..1.0f,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                swatches.forEach { swatch ->
                    val isSelected = swatch == selectedColor
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(swatch)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f),
                                shape = CircleShape
                            )
                            .clickable {
                                onColorSelect(swatch)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = if (swatch.luminance() > 0.5f) Color.Black else Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun Color.luminance(): Float {
    return 0.299f * red + 0.587f * green + 0.114f * blue
}
