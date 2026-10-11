package org.selkie.kol.campaign.story.core

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI

/** Dialog helpers shared by the story's scenes. */
object KolDialog {
    /** Shows a story person (any id `KolPeople.byId` resolves) in the dialog's visual panel; false if unknown. */
    fun showPerson(dialog: InteractionDialogAPI, who: String): Boolean {
        val person = KolPeople.byId(who) ?: return false
        dialog.visualPanel.showPersonInfo(person, true)
        return true
    }

    /**
     * Plays the best rule on [trigger] as a continuation of the text above it (rule text, options, script), for
     * dossier-style readings. With `readingAppends` on, its text replaces the last paragraph, which is the echo of the
     * option just chosen, so the reading runs on without the "Continue" lines between items.
     */
    fun readOn(trigger: String, ruleId: String?, dialog: InteractionDialogAPI, map: MutableMap<String, MemoryAPI>): Boolean {
        val rules = Global.getSector().rules
        val rule = rules.getBestMatching(ruleId, trigger, dialog, map) ?: return false
        val text = rule.pickText()?.takeIf { it.isNotBlank() }?.let { rules.performTokenReplacement(rule.id, it, dialog.interactionTarget, map) }
        if (text != null) {
            if (KolStorySettings.readingAppends) dialog.textPanel.replaceLastParagraph(text)
            else dialog.textPanel.addPara(text)
        }
        if (rule.options.isNotEmpty()) {
            dialog.optionPanel.clearOptions()
            for (option in rule.options.sortedBy { it.order }) {
                dialog.optionPanel.addOption(rules.performTokenReplacement(rule.id, option.text, dialog.interactionTarget, map), option.id)
            }
        }
        rule.runScript(dialog, map)
        return true
    }
}
