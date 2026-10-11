package org.selkie.kol.campaign.story.systems

/**
 * A story feature's registration. Each feature declares what it adds to the shared systems here, so no central file
 * changes when a feature is added: only its module joins [KolStoryModules.all].
 */
interface KolStoryModule {
    /** The feature's name, as used in its log lines (`KOL: [id]: ...`). */
    val id: String

    /** At each load, after every module's cues, modifiers and requirements are registered: scripts, listeners, ensure() calls. */
    fun onLoad() {}

    /** Requirements for chapter assemblies. */
    fun requirements(): List<Requirement> = emptyList()

    /** Story cues by key (a person marked important and a notice, while wanted). */
    fun cues(): Map<String, org.selkie.kol.campaign.story.systems.cue.KolStoryCue.Cue> = emptyMap()

    /** Fleet-spawn modifiers by key. */
    fun spawnModifiers(): Map<String, org.selkie.kol.campaign.story.systems.spawn.KolFleetSpawnHook.Modifier> = emptyMap()

    /** The story heartbeat (twice a day), in module order. Each module gates itself (chapter, situation stage). */
    fun pulse() {}

    /** Memory-key prefixes severance wipes (sector, market, entity and person memory), and exceptions it keeps. */
    fun severedKeys(): List<String> = emptyList()
    fun keptKeys(): List<String> = emptyList()

    /** Persistent-data keys severance removes. Other story data survives severance. */
    fun severedData(): List<String> = emptyList()

    /** A chapter requirement: counts when [valid], met when [done]. */
    class Requirement(val chapter: Int, val key: String, val valid: () -> Boolean, val done: () -> Boolean)
}
