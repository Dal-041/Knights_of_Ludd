package org.selkie.kol.campaign.story.systems.assembly

/** Assemblies at Lyra and attending them: data, reckoning, triggers, the attendance mission. */
object AssemblyKeys {
    // Assemblies (at Star Keep Lyra)
    const val ASSEMBLY_DATA_KEY = "kol_assemblyData"
    const val ASSEMBLY_PREFIX = "\$kolAssembly_" // + report key (published at an assembly)
    const val ASSEMBLY_MISSED = "\$kolAssembly_missed" // int
    const val ASSEMBLY_HELD = "\$kolAssembly_held" // int
    const val TRIGGER_ASSEMBLY = "KolAssembly"
    const val TRIGGER_ASSEMBLY_ITEMS = "KolAssemblyItems" // FireAll: one line rule per report key
    const val TRIGGER_ASSEMBLY_CHAPTER = "KolAssemblyChapter" // + the chapter being entered
    const val TRIGGER_ASSEMBLY_NOTICE = "KolAssemblyNotice" // the blurb at Lyra while an assembly sits
    // Attending an important assembly (convocation or chapter assembly)
    const val ATTEND_ID = "kolAssemblyAttend"
    const val ATTEND_REF = "\$kolAssemblyAttend_ref"
    const val ATTEND_ACTIVE = "\$kolAssemblyAttend_active"
    const val ATTEND_SITTING = "\$kolAssemblyAttend_sitting" // stage flags, cleared by the mission
    const val ATTEND_DONE = "\$kolAssemblyAttend_done"
    const val ATTEND_MISSED = "\$kolAssemblyAttend_missed"
}
