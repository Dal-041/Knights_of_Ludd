package org.selkie.kol.campaign.story.situations.tech

/** The Technology situation's keys: settings, data, stage flags, rule triggers. */
object TechKeys {
    // Situations (placeholder event intels)
    const val SITUATION_KEY = "\$kolTech_situationRef"
    const val SETTINGS_KEY = "kol_tech" // JSON object in settings.json
    const val DATA_KEY = "kol_techData" // sector persistent data
    // Permanent flags other content reads (set on stage reached)
    const val STAGE = "\$kolTech_stage" // 0 START .. 4 CONSECRATED, 5 at the bar maximum
    @JvmField val STAGE_FLAGS = arrayOf("\$kolTech_start", "\$kolTech_requisitions", "\$kolTech_trusted", "\$kolTech_armory", "\$kolTech_consecrated", "\$kolTech_max")
    // Rule triggers fired by KolTechCMD
    const val TRIGGER_AFTER_HANDOVER = "KolTechAfterHandover"
    const val TRIGGER_SHROUDED = "KolTechShrouded"
    const val TRIGGER_UNIQUE_BOUGHT = "KolTechUniqueBought" // + slot number
    const val TRIGGER_AFTER_PURCHASE = "KolTechAfterPurchase" // $kolTech_purchaseType set
    const val TRIGGER_REQ_MENU = "KolTechReqMenu"
}
