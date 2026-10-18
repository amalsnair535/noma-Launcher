package com.freelauncher.app.data.models

data class AppCategoryInfo(
    val id: String,
    val title: String,
    val isCustom: Boolean = false,
    val isHidden: Boolean = false,
    val order: Int = 0
)

data class AppItem(
    val id: String = "",
    val packageName: String,
    val activityName: String = "",
    val label: String,
    val category: AppCategory = AppCategory.TOOLS,
    val categoryId: String = category.name,
    val categoryTitle: String = category.title,
    val monogram: String = "",
    val isPinned: Boolean = false,
    val pinIndex: Int = -1,
    val userSerialNumber: Long = 0L,
    val isDualApp: Boolean = false
)

enum class AppCategory(val title: String) {
    COMMUNICATION("COMMUNICATION"),
    SOCIAL("SOCIAL"),
    PRODUCTIVITY("PRODUCTIVITY"),
    WORK("WORK"),
    FINANCE("FINANCE"),
    SHOPPING("SHOPPING"),
    MEDIA("MEDIA"),
    PHOTOGRAPHY("PHOTOGRAPHY"),
    TRAVEL("TRAVEL"),
    FOOD("FOOD"),
    UTILITIES("UTILITIES"),
    TOOLS("TOOLS"),
    NEWS("NEWS"),
    GAMES("GAMES"),
    HEALTH_FITNESS("HEALTH & FITNESS"),
    EDUCATION("EDUCATION"),
    ENTERTAINMENT("ENTERTAINMENT"),
    SYSTEM("SYSTEM");

    companion object {
        fun fromId(id: String): AppCategory {
            return entries.find { it.name.equals(id, ignoreCase = true) } ?: TOOLS
        }
    }
}

enum class AppDrawerStyle(val id: String, val displayName: String, val subtitle: String) {
    CLASSIC("classic", "Classic", "Original text list with top category tabs"),
    NEO_MINIMAL("neo_minimal", "Neo Minimal", "Centered text list with swipe categories & floating search");

    companion object {
        fun fromName(name: String): AppDrawerStyle {
            return entries.find { it.name.equals(name, ignoreCase = true) || it.id.equals(name, ignoreCase = true) } ?: CLASSIC
        }
    }
}

enum class SixAppsAlignment(val id: String, val displayName: String) {
    LEFT("left", "Left"),
    CENTER("center", "Center"),
    RIGHT("right", "Right");

    companion object {
        fun fromName(name: String): SixAppsAlignment {
            return entries.find { it.name.equals(name, ignoreCase = true) || it.id.equals(name, ignoreCase = true) } ?: LEFT
        }
    }
}

enum class AllAppsAlignment(val id: String, val displayName: String) {
    LEFT("left", "Left"),
    CENTER("center", "Center"),
    RIGHT("right", "Right");

    companion object {
        fun fromName(name: String, default: AllAppsAlignment = CENTER): AllAppsAlignment {
            return entries.find { it.name.equals(name, ignoreCase = true) || it.id.equals(name, ignoreCase = true) } ?: default
        }
    }
}
