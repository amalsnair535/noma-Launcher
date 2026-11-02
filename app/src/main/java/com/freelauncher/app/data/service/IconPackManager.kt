package com.freelauncher.app.data.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Process
import android.util.LruCache
import androidx.core.content.res.ResourcesCompat
import org.xmlpull.v1.XmlPullParser
import java.util.Locale

data class IconPackInfo(
    val packageName: String,
    val name: String,
    val icon: Drawable? = null
)

class IconPackManager(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager
    private val iconCache = LruCache<String, Drawable>(150) // LruCache is internally synchronized
    private val appFilterCache = java.util.concurrent.ConcurrentHashMap<String, Map<String, String>>()

    companion object {
        private val ICON_PACK_ACTIONS = listOf(
            "com.novalauncher.THEME",
            "org.adw.launcher.THEMES",
            "com.gau.go.launcherex.theme",
            "com.dlto.atom.launcher.THEME",
            "com.fede.launcher.THEME_ICONPACK",
            "com.anddoes.launcher.THEME"
        )
    }

    /**
     * Finds all installed third-party icon packs on the device.
     */
    fun getInstalledIconPacks(): List<IconPackInfo> {
        val list = mutableListOf<IconPackInfo>()
        val seenPackages = mutableSetOf<String>()

        for (action in ICON_PACK_ACTIONS) {
            val intent = Intent(action)
            val flags = PackageManager.MATCH_DEFAULT_ONLY
            val resolveInfos: List<ResolveInfo> = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    packageManager.queryIntentActivities(
                        intent,
                        PackageManager.ResolveInfoFlags.of(flags.toLong())
                    )
                } else {
                    packageManager.queryIntentActivities(intent, flags)
                }
            } catch (_: Exception) {
                emptyList()
            }

            for (resolveInfo in resolveInfos) {
                val pkgName = resolveInfo.activityInfo?.packageName ?: continue
                if (seenPackages.contains(pkgName) || pkgName == context.packageName) continue

                seenPackages.add(pkgName)
                val label = try {
                    resolveInfo.loadLabel(packageManager).toString()
                } catch (_: Exception) {
                    pkgName
                }

                val icon = try {
                    resolveInfo.loadIcon(packageManager)
                } catch (_: Exception) {
                    null
                }

                list.add(
                    IconPackInfo(
                        packageName = pkgName,
                        name = label,
                        icon = icon
                    )
                )
            }
        }

        return list.sortedBy { it.name.lowercase(Locale.ROOT) }
    }

    /**
     * Loads the icon for a specific app from a selected icon pack.
     */
    fun getIconForApp(appPackageName: String, activityName: String = "", iconPackPackage: String = ""): Drawable? {
        if (appPackageName.isBlank()) return null
        if (iconPackPackage.isBlank()) return getOriginalAppIcon(appPackageName, activityName)

        val cacheKey = "$iconPackPackage:$appPackageName:$activityName"
        iconCache.get(cacheKey)?.let { return it }

        val appFilterMap = getOrParseAppFilter(iconPackPackage)
        val drawableName = appFilterMap[appPackageName] ?: appPackageName.replace(".", "_")

        val drawable = loadDrawableFromIconPack(iconPackPackage, drawableName)
            ?: loadDrawableFromIconPack(iconPackPackage, appPackageName)
            ?: getOriginalAppIcon(appPackageName, activityName)

        if (drawable != null) {
            iconCache.put(cacheKey, drawable)
        }

        return drawable
    }

    /**
     * Multi-stage robust lookup for standard Android app icons (Camera, Settings, Chrome, etc.).
     */
    fun getOriginalAppIcon(appPackageName: String, activityName: String = ""): Drawable? {
        if (appPackageName.isBlank()) return null

        val cacheKey = "orig:$appPackageName:$activityName"
        iconCache.get(cacheKey)?.let { return it }

        var icon: Drawable? = null

        // Stage 1: Try ComponentName via PackageManager
        if (activityName.isNotBlank()) {
            try {
                val component = ComponentName(appPackageName, activityName)
                icon = packageManager.getActivityIcon(component)
            } catch (_: Exception) {}
        }

        // Stage 2: Try LauncherApps service (handles Camera, Settings, Work profiles, Dual Apps)
        if (icon == null) {
            try {
                val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
                if (launcherApps != null) {
                    val user = Process.myUserHandle()
                    val activities = launcherApps.getActivityList(appPackageName, user)
                    val matched = if (activityName.isNotBlank()) {
                        activities.find { it.name == activityName } ?: activities.firstOrNull()
                    } else {
                        activities.firstOrNull()
                    }
                    if (matched != null) {
                        icon = matched.getIcon(0)
                    }
                }
            } catch (_: Exception) {}
        }

        // Stage 3: Try ApplicationInfo / getApplicationIcon
        if (icon == null) {
            try {
                icon = packageManager.getApplicationIcon(appPackageName)
            } catch (_: Exception) {}
        }

        // Stage 4: Try launch intent fallback
        if (icon == null) {
            try {
                val intent = packageManager.getLaunchIntentForPackage(appPackageName)
                if (intent != null) {
                    icon = packageManager.getActivityIcon(intent)
                }
            } catch (_: Exception) {}
        }

        if (icon != null) {
            iconCache.put(cacheKey, icon)
        }

        return icon
    }

    /**
     * Parses `appfilter.xml` resource inside the icon pack APK.
     */
    private fun getOrParseAppFilter(iconPackPackage: String): Map<String, String> {
        if (appFilterCache.containsKey(iconPackPackage)) {
            return appFilterCache[iconPackPackage] ?: emptyMap()
        }

        val map = mutableMapOf<String, String>()
        try {
            val iconPackRes = packageManager.getResourcesForApplication(iconPackPackage)
            @Suppress("DiscouragedApi")
            val resId = iconPackRes.getIdentifier("appfilter", "xml", iconPackPackage)
            if (resId != 0) {
                val xpp = iconPackRes.getXml(resId)
                var eventType = xpp.eventType
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG && xpp.name == "item") {
                        val component = xpp.getAttributeValue(null, "component")
                        val drawable = xpp.getAttributeValue(null, "drawable")

                        if (!component.isNullOrBlank() && !drawable.isNullOrBlank()) {
                            val pkg = extractPackageFromComponent(component)
                            if (pkg.isNotBlank()) {
                                map[pkg] = drawable
                            }
                        }
                    }
                    eventType = xpp.next()
                }
            }
        } catch (_: Exception) {
            // Fallback if appfilter cannot be parsed
        }

        appFilterCache[iconPackPackage] = map
        return map
    }

    private fun extractPackageFromComponent(componentStr: String): String {
        val start = componentStr.indexOf('{')
        val end = componentStr.indexOf('/')
        return if (start != -1 && end != -1 && end > start) {
            componentStr.substring(start + 1, end).trim()
        } else if (start != -1) {
            val spaceOrEnd = componentStr.indexOf(' ', start)
            val limit = if (spaceOrEnd != -1) spaceOrEnd else componentStr.length - 1
            componentStr.substring(start + 1, limit).trim()
        } else {
            ""
        }
    }

    private fun loadDrawableFromIconPack(iconPackPackage: String, drawableName: String): Drawable? {
        return try {
            val iconPackRes = packageManager.getResourcesForApplication(iconPackPackage)
            @Suppress("DiscouragedApi")
            val resId = iconPackRes.getIdentifier(drawableName, "drawable", iconPackPackage)
            if (resId != 0) {
                ResourcesCompat.getDrawable(iconPackRes, resId, null)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }
}
