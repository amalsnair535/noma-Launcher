package com.freelauncher.app.data.models

enum class IconShape(val label: String) {
    CIRCLE("Circle"),
    SQUIRCLE("Squircle"),
    ROUNDED_SQUARE("Rounded Square"),
    HEXAGON("Hexagon"),
    OUTLINE_ONLY("Outline")
}

enum class IconContainerStyle(val label: String) {
    SOLID("Solid Surface"),
    GLASSMORPHIC("Glassmorphic"),
    OUTLINE("Minimal Outline"),
    TRANSPARENT("Transparent")
}

enum class IconColorTheme(val label: String, val defaultHex: String) {
    MONOCHROME_WHITE("Monochrome White", "#FFFFFF"),
    ACCENT_PRIMARY("Material Accent", "#E2D3FA"),
    PASTEL_NEON("Soft Pastel", "#B3E5FC"),
    DARK_SLATE("Dark Slate", "#2A2D34"),
    CUSTOM("Custom Color", "#E2D3FA")
}

enum class IconDisplayMode(val label: String, val subtitle: String) {
    TEXT_ONLY("Text Only", "Minima Default - Clean text list without icons or badges"),
    MONOGRAM("Letter Monograms", "Minima Mono - Minimalist 2-letter badges beside app labels"),
    ORIGINAL("Original System Icons", "Standard Android app icons from installed apps"),
    ICON_PACK("Installed Icon Pack", "Icons loaded from installed third-party icon packs"),
    MINIMAL_CREATOR("Custom Minimal Icon Pack", "Cohesive minimal vector icons created with Icon Creator");

    companion object {
        fun fromName(name: String): IconDisplayMode {
            return entries.find { it.name.equals(name, ignoreCase = true) } ?: TEXT_ONLY
        }
    }
}

data class MinimalIconPackConfig(
    val shape: IconShape = IconShape.SQUIRCLE,
    val containerStyle: IconContainerStyle = IconContainerStyle.SOLID,
    val colorTheme: IconColorTheme = IconColorTheme.MONOCHROME_WHITE,
    val customColorHex: String = "#E2D3FA",
    val strokeWidthDp: Float = 1.5f,
    val useMonogramGlyph: Boolean = true,
    val iconScale: Float = 1.0f
)
