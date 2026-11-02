package com.freelauncher.app.data.repository

import android.content.Context
import com.freelauncher.app.data.models.AppCategory
import com.freelauncher.app.data.models.AppItem
import com.freelauncher.app.data.service.InstalledAppsService

/**
 * AppManager acts as a repository-level facade over [InstalledAppsService]
 * to retrieve, categorize, and query installed applications on the device.
 */
class AppManager(
    private val context: Context,
    private val installedAppsService: InstalledAppsService = InstalledAppsService(context)
) {
    private var cachedApps: List<AppItem>? = null

    /**
     * Retrieves all installed applications. Caches the result in memory for instant subsequent lookups.
     */
    suspend fun getInstalledApps(pinnedIds: List<String>, forceRefresh: Boolean = false): List<AppItem> {
        if (cachedApps == null || forceRefresh) {
            cachedApps = installedAppsService.getInstalledApps(emptyList())
        }
        
        // Return cached apps, applying pinnedIds locally if requested
        return cachedApps?.map { app ->
            val pinIdx = pinnedIds.indexOf(app.id)
            if (pinIdx != -1) {
                app.copy(isPinned = true, pinIndex = pinIdx)
            } else {
                app.copy(isPinned = false, pinIndex = -1)
            }
        } ?: emptyList()
    }

    fun invalidateCache() {
        cachedApps = null
    }

    /**
     * Retrieves all installed applications grouped into the 4 required categories.
     */
    suspend fun getCategorizedApps(pinnedIds: List<String>): Map<AppCategory, List<AppItem>> {
        return installedAppsService.getCategorizedApps(pinnedIds)
    }
}
