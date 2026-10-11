package org.selkie.kol.campaign.story.core

import com.fs.starfarer.api.Global

/** The one accessor for saved story data holders in the sector's persistent data, created on first use. */
object KolStoryData {
    inline fun <reified T : Any> get(key: String, create: () -> T): T {
        val data = Global.getSector().persistentData
        return data[key] as? T ?: create().also { data[key] = it }
    }

    fun remove(key: String) {
        Global.getSector().persistentData.remove(key)
    }
}
