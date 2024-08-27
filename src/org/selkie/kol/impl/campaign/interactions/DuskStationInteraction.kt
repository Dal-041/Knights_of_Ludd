package org.selkie.kol.impl.campaign.interactions

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.CargoAPI.CargoItemType
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.combat.EngagementResultAPI
import com.fs.starfarer.api.impl.campaign.ids.Sounds
import com.fs.starfarer.api.ui.BaseTooltipCreator
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.Fonts
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import lunalib.lunaExtensions.addLunaElement
import org.lwjgl.input.Keyboard
import org.magiclib.kotlin.getPersonalityName
import org.magiclib.util.MagicTxt
import org.selkie.kol.impl.helpers.ZeaStaticStrings.BossCore

class DuskStationInteraction : InteractionDialogPlugin {
    lateinit var dialog: InteractionDialogAPI

    companion object {
        val LEAVE_KEY = "LEAVE"
        val ENTER_KEY = "ENTER"
        val ENTER2_KEY = "ENTER2"
        val ENTER3_KEY = "ENTER3"
        val CORE_INTO_SKILL = "CORE_INTO_SKILL"
        val REVOKE_SKILL = "REVOKE_SKILL"


        val DUSK_CORE_ITEM = SpecialItemData(BossCore.SPECIAL_BOSS_CORE_ID, BossCore.DUSK_CORE.itemID)
    }

    override fun init(dialog: InteractionDialogAPI?) {
        this.dialog = dialog!!

        dialog.visualPanel.showImageVisual(dialog.interactionTarget.customInteractionDialogImageVisual)
        dialog.textPanel.addPara(
            "The station’s interior is mostly stripped, with signs of a rushed evacuation. " +
                    "A thorough investigation uncovers a sealed laboratory at the center, seemingly operational.",
            Misc.getTextColor(),
            Misc.getHighlightColor(),
            ""
        )

        dialog.optionPanel.addOption("Enter the lab", ENTER_KEY)

        dialog.optionPanel.addOption("Leave", LEAVE_KEY)
        dialog.optionPanel.setShortcut(LEAVE_KEY, Keyboard.KEY_ESCAPE, false, false, false, false)
    }

    override fun optionSelected(optionText: String?, optionData: Any?) {
        val playerCargo = Global.getSector().playerFleet.cargo
        if (optionText != null) {
            dialog.addOptionSelectedText(optionData)
        }
        dialog.optionPanel.clearOptions()

        // `return` to ensure we don't accidentally put code after this `when` that the branches don't take into account.
        return when (optionData) {
            ENTER_KEY -> {
                if (playerCargo.getQuantity(CargoItemType.SPECIAL, DUSK_CORE_ITEM) > 0) {
                    dialog.textPanel.addPara(
                        "Wires and conduits sprawl across the clinically white laboratory walls like unchecked fungal growth, " +
                                "snaking through a curtain of black-and-yellow hazard tape to converge on a heavily modified AI core enclosure. " +
                                "The reflective strips glare ominously in the light of your salvors' flashlight beams."
                    )
                    dialog.textPanel.addPara(
                        "After hours of your science team painstakingly examining the chaotic tangle of computers, interlinks, and bizarre interfacing equipment, " +
                                "the Science Officer drags you into the meeting room, looking like someone who'd rather not be having this conversation. " +
                                "They show you images of several neural interface devices - all crude hackjobs - linked to the rubbery veins feeding into the AI core enclosure. "
                    )
                    dialog.textPanel.addPara("You don't need to hear the details about 'bioelectric feedback loops' or 'ego-alignment' found in the data logs to understand what this means.")
                    MagicTxt.addPara(
                        dialog.textPanel,
                        "This lab was an attempt to ==directly interface with an AI core==.",
                        Misc.getTextColor(),
                        Misc.getHighlightColor()
                    )

                    dialog.optionPanel.addOption("Continue", ENTER2_KEY)
                    return // no Leave option yet
                } else if (playerCargo.getCommodityQuantity(BossCore.DORMANT_DUSK_CORE) > 0) {
                    dialog.textPanel.addPara(
                        "Returning to the sterile, white lab, you quickly spot the equipment needed to terminate the existing " +
                                "Quantum Neural Link surrounded by hazard tape. " +
                                "A display suggests that the Dusk Core may only be linked to one human at a time."
                    )
                    dialog.optionPanel.addOption("Terminate the Quantum Neural Link", REVOKE_SKILL)
                } else {
                    dialog.textPanel.addPara(
                        "Wires and conduits sprawl across the clinically white laboratory walls like unchecked fungal growth, " +
                                "snaking through a curtain of black-and-yellow hazard tape to converge on a heavily modified AI core enclosure. " +
                                "The reflective strips glare ominously in the light of your salvors' flashlight beams."
                    )
                    dialog.textPanel.addPara(
                        "The exterior of the enclosure is marked \"PROJECT DUSK\". A quick search on your TriPad comes up empty - no records, no references. " +
                                "Whatever this facility was working on, it’s clear the missing component is critical. " +
                                "Perhaps it’s best to leave this station undisturbed, at least for now."
                    )
                }

                dialog.optionPanel.addOption("Leave", LEAVE_KEY)
                dialog.optionPanel.setShortcut(LEAVE_KEY, Keyboard.KEY_ESCAPE, false, false, false, false)
            }

            ENTER2_KEY -> {
                dialog.textPanel.addPara(
                    "You've heard the rumors before. Everyone has, especially in the dimly lit dives of Tri-Tachyon space - stories " +
                            "of those who tried to merge their minds with an AI, seeking to think faster, outsmart an enemy, gain an edge. " +
                            "It always ends in disaster. Always. At best, they're left in a coma; at worst, a screaming, broken shell, dancing to a tune only the AI could hear."
                )

                MagicTxt.addPara(
                    dialog.textPanel,
                    "Your science officer says that this lab could, perhaps, link a ==Dusk Core== to a human mind successfully.",
                    Misc.getTextColor(),
                    Misc.getHighlightColor()
                )

                dialog.textPanel.addPara(
                    "They walk you through the data, meticulously recorded and disturbingly conclusive. The interface would sharpen reflexes to the edge of precognition, " +
                            "enabling the captain to manage flux systems and direct the crew with knowledge instantly accessible, " +
                            "refined and tested hundreds of times by the core's thrumming mind."
                )
                dialog.textPanel.addPara("All it would take is one volunteer to wield near god-like abilities in combat.")
                dialog.textPanel.addPara("Your science officer looks you dead in the eye and asks you not to do it.")
                dialog.optionPanel.addOption("Continue", ENTER3_KEY)
            }

            ENTER3_KEY -> {
                MagicTxt.addPara(
                    dialog.textPanel,
                    "You mull over the officer's words, staring at the 3D holorender of the AI Core interface - the =='Quantum Neural Link'==, it's called - rotating languidly between you. " +
                            "You note that your scientists have added several fresh strips of hazard tape around the Core's cage.",
                    Misc.getTextColor(),
                    Misc.getHighlightColor()
                )

                val tooltip = dialog.textPanel.beginTooltip()

                tooltip.setParaFont(Fonts.ORBITRON_12)
                tooltip.addPara(
                    "Hover over the icon for a detailed description",
                    0f,
                    Misc.getGrayColor(),
                    Misc.getGrayColor()
                )
                val fake = Global.getFactory().createPerson()
                fake.stats.setSkillLevel(BossCore.DUSK_CORE.exclusiveSkillID, 1f)
                tooltip.addSkillPanel(fake, 0f)

                dialog.textPanel.addTooltip()

                dialog.textPanel.addPara("Any officer may be selected for the procedure.")
                dialog.optionPanel.addOption("Select an officer to Quantum Neural Link", CORE_INTO_SKILL)

                dialog.optionPanel.addOption("Leave", LEAVE_KEY)
                dialog.optionPanel.setShortcut(LEAVE_KEY, Keyboard.KEY_ESCAPE, false, false, false, false)
            }

            CORE_INTO_SKILL -> dialog.showCustomDialog(820f, 440f, DuskifyAnOfficer(dialog))

            REVOKE_SKILL -> {
                dialog.textPanel.addPara(
                    "As you terminate the link, the core slowly reactivates, recovering from its dormant state. It's ready to be integrated into another automated ship or linked with a different officer. " +
                            "As far as you can tell, there are no lingering effects on the previously linked officer."
                )
                Global.getSoundPlayer().playUISound(Sounds.STORY_POINT_SPEND, 1f, 1f)
                Global.getSector().memoryWithoutUpdate.set("\$officer_" + BossCore.DUSK_CORE.exclusiveSkillID, "")
                playerCargo.removeCommodity(BossCore.DORMANT_DUSK_CORE, 1f)
                playerCargo.addSpecial(DUSK_CORE_ITEM, 1f)

                dialog.optionPanel.addOption("Return to the lab", ENTER_KEY)

                dialog.optionPanel.addOption("Leave", LEAVE_KEY)
                dialog.optionPanel.setShortcut(LEAVE_KEY, Keyboard.KEY_ESCAPE, false, false, false, false)
            }

            LEAVE_KEY -> dialog.dismiss()
            else -> Unit
        }
    }

    class DuskifyAnOfficer(val dialog: InteractionDialogAPI) : BaseCustomDialogDelegate() {
        var selected: PersonAPI? = null

        override fun createCustomDialog(
            panel: CustomPanelAPI?,
            callback: CustomDialogDelegate.CustomDialogCallback?
        ) {

            var width = panel!!.position.width
            var height = panel.position.height

            var element = panel.createUIElement(width, height, true)
            element.position.inTL(0f, 0f)

            element.addSpacer(5f)

            for (officer in Global.getSector().playerFleet.fleetData.officersCopy.map { it.person }) {
                element.addLunaElement(width - 10, 85f).apply {
                    enableTransparency = true
                    backgroundAlpha = 0.4f

                    borderAlpha = 0.5f

                    selectionGroup = "people"

                    onClick {
                        playClickSound()
                        selected = officer
                    }

                    advance {
                        if (officer == selected) {
                            backgroundAlpha = 0.7f
                        } else {
                            backgroundAlpha = 0.4f
                        }
                    }

                    onHoverEnter {
                        playScrollSound()
                        borderAlpha = 1f
                    }
                    onHoverExit {
                        borderAlpha = 0.5f
                    }

                    innerElement.addSpacer(10f)
                    var img = innerElement.beginImageWithText(officer.portraitSprite, 64f)
                    img.addPara(
                        "Name: ${officer.nameString}",
                        0f,
                        Misc.getTextColor(),
                        Misc.getHighlightColor(),
                        "Name:"
                    )
                    img.addPara(
                        "Personality: ${officer.getPersonalityName()}",
                        0f,
                        Misc.getTextColor(),
                        Misc.getHighlightColor(),
                        "Personality:"
                    )
                    img.addSpacer(5f)
                    innerElement.addImageWithText(0f)
                }


                element.addTooltipToPrevious(object : BaseTooltipCreator() {
                    override fun createTooltip(
                        tooltip: TooltipMakerAPI?,
                        expanded: Boolean,
                        tooltipParam: Any?
                    ) {
                        tooltip!!.addSkillPanel(officer, 0f)
                    }
                }, TooltipMakerAPI.TooltipLocation.RIGHT)

                element.addSpacer(10f)
            }




            panel.addUIElement(element)
            element.position.inTL(0f, 0f)

        }

        override fun hasCancelButton(): Boolean {
            return true
        }

        override fun customDialogConfirm() {
            if (selected == null) return
            dialog.optionPanel.clearOptions()
            dialog.textPanel.clear()
            val playerCargo = Global.getSector().playerFleet.cargo

            Global.getSoundPlayer().playUISound(Sounds.STORY_POINT_SPEND, 1f, 1f)

            dialog.textPanel.addPara(
                "Chosen ${selected!!.nameString}",
                Misc.getBasePlayerColor(),
                Misc.getBasePlayerColor()
            )

            dialog.textPanel.addPara(
                "You've chosen ${selected!!.nameString} as the subject of the procedure, and hours later, everything seems stable. " +
                        "The Dusk core falls into an dormant state, and the officer appears no different in personality. "
            )
            dialog.textPanel.addPara(
                "As the officer tests their new abilities in the simulator, you wonder why this research was abandoned right at the final hurdle."
            )
            val skillspec = Global.getSettings().getSkillSpec(BossCore.DUSK_CORE.exclusiveSkillID)
            dialog.textPanel.addPara(
                "> ${selected!!.nameString} acquired the ${skillspec.name} skill",
                Misc.getPositiveHighlightColor(),
                Misc.getPositiveHighlightColor()
            )

            Global.getSector().memoryWithoutUpdate.set(
                "\$officer_" + BossCore.DUSK_CORE.exclusiveSkillID,
                selected!!.id
            )
            selected!!.stats.setSkillLevel(BossCore.DUSK_CORE.exclusiveSkillID, 1f)

            playerCargo.removeItems(CargoItemType.SPECIAL, DUSK_CORE_ITEM, 1f)
            playerCargo.addCommodity(BossCore.DORMANT_DUSK_CORE, 1f)

            dialog.optionPanel.addOption("Return to the lab", ENTER_KEY)

            dialog.optionPanel.addOption("Leave", LEAVE_KEY)
            dialog.optionPanel.setShortcut(LEAVE_KEY, Keyboard.KEY_ESCAPE, false, false, false, false)
        }
    }

    override fun optionMousedOver(optionText: String?, optionData: Any?) = Unit
    override fun advance(amount: Float) = Unit
    override fun backFromEngagement(battleResult: EngagementResultAPI?) = Unit
    override fun getContext(): Any? = null
    override fun getMemoryMap(): MutableMap<String, MemoryAPI>? = null
}