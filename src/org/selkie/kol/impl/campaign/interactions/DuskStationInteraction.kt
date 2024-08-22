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
import org.selkie.kol.impl.helpers.ZeaStaticStrings.BossCore

class DuskStationInteraction : InteractionDialogPlugin {
    lateinit var dialog: InteractionDialogAPI

    companion object {
        val LEAVE_KEY = "LEAVE"
        val ENTER_KEY = "ENTER"
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

        if (optionData == LEAVE_KEY) {
            dialog.dismiss()
        }

        if (optionData == ENTER_KEY) {
            dialog.optionPanel.clearOptions()
            dialog.textPanel.clear()
            if (playerCargo.getQuantity(CargoItemType.SPECIAL, DUSK_CORE_ITEM) > 0) {
                dialog.textPanel.addPara("The laboratory is expansive and messy.")
                dialog.textPanel.addPara(
                    "The laboratory is cluttered with wires and conduits, all converging on a heavily modified AI core enclosure. " +
                            "It appears to be designed to house the prototype Dusk Core currently sitting in your cargo hold."
                )
                dialog.textPanel.addPara(
                    "As your science team digs deeper, they find several neural interface devices connected to the enclosure and traces of bioelectric feedback loops in the remaining data logs. " +
                            "After hours of reverse engineering, they conclude that this lab was intended to link the Dusk Core to a human brain, granting unmatched combat abilities. " +
                            "Your scientists are confident they can activate what they're calling a 'Quantum Neural Link', and eagerly ask for a subject."
                )
                val tooltip = dialog.textPanel.beginTooltip()

                tooltip.setParaFont(Fonts.ORBITRON_12)
                tooltip.addPara(
                    "(Hover over the icon for a detailed description)",
                    0f,
                    Misc.getGrayColor(),
                    Misc.getGrayColor()
                )
                val fake = Global.getFactory().createPerson()
                fake.stats.setSkillLevel(BossCore.DUSK_CORE.exclusiveSkillID, 1f)
                tooltip.addSkillPanel(fake, 0f)

                dialog.textPanel.addTooltip()

                dialog.textPanel.addPara("Any officer can be selected for the procedure.")
                dialog.optionPanel.addOption("Select an officer to Quantum Neural Link", CORE_INTO_SKILL)
            } else if (playerCargo.getCommodityQuantity(BossCore.DORMANT_DUSK_CORE) > 0) {
                dialog.textPanel.addPara(
                    "Returning to the now familiar laboratory, you recognize the equipment needed to terminate the existing quantum neural link. " +
                            "It appears that the Dusk Core can only be linked to one officer at a time."
                )
                dialog.optionPanel.addOption("Terminate the Quantum Neural Link", REVOKE_SKILL)
            } else {
                dialog.textPanel.addPara("The laboratory is expansive and messy. ")
                dialog.textPanel.addPara(
                    "Looking around the room reveals a myriad of wires and conduits all connected to the center of the room, containing what appears to be a heavily modified AI core enclosure. " +
                            "Upon closer inspection, none of the enclosures ports seems to match with a standard Domain AI Core. "
                )
                dialog.textPanel.addPara(
                    "The outside of the enclosure reads \"PROJECT DUSK\", quickly querying a standard TriPad reveals no matches. " +
                            "Whatever this facility was trying to do, it won't happen without what is supposed to fit inside that enclosure. " +
                            "Perhaps its best to leave this station alone for now."
                )
            }

            dialog.optionPanel.addOption("Leave", LEAVE_KEY)
            dialog.optionPanel.setShortcut(LEAVE_KEY, Keyboard.KEY_ESCAPE, false, false, false, false)
        }

        if (optionData == CORE_INTO_SKILL) {
            dialog.showCustomDialog(820f, 440f, object : BaseCustomDialogDelegate() {
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

                    Global.getSoundPlayer().playUISound(Sounds.STORY_POINT_SPEND, 1f, 1f)

                    dialog.textPanel.addPara(
                        "Choosen ${selected!!.nameString}",
                        Misc.getBasePlayerColor(),
                        Misc.getBasePlayerColor()
                    )

                    dialog.textPanel.addPara(
                        "You've chosen ${selected!!.nameString} as the subject of the procedure, and hours later, everything seems stable. " +
                                "The Dusk core falls into an dormant state, and the officer appears no different in personality. "
                    )
                    dialog.textPanel.addPara(
                        "As the officer tests his new abilities in the simulator, you wonder why this research was abandoned at the last hurdle. " +
                                "You'll deal with the consequences whenever they show up later."
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
            })
        }

        if (optionData == REVOKE_SKILL) {
            dialog.optionPanel.clearOptions()
            dialog.textPanel.clear()

            dialog.textPanel.addPara(
                "As you terminate the link, the core recovers from its dormant state, ready to be fit into an automated ship or linked with somebody else. " +
                        "As far as you can tell, there has been no consequences for the previously linked officer."
            )
            Global.getSoundPlayer().playUISound(Sounds.STORY_POINT_SPEND, 1f, 1f)
            Global.getSector().memoryWithoutUpdate.set("\$officer_" + BossCore.DUSK_CORE.exclusiveSkillID, "")
            playerCargo.removeCommodity(BossCore.DORMANT_DUSK_CORE, 1f)
            playerCargo.addSpecial(DUSK_CORE_ITEM, 1f)

            dialog.optionPanel.addOption("Return to the lab", ENTER_KEY)

            dialog.optionPanel.addOption("Leave", LEAVE_KEY)
            dialog.optionPanel.setShortcut(LEAVE_KEY, Keyboard.KEY_ESCAPE, false, false, false, false)
        }
    }

    override fun optionMousedOver(optionText: String?, optionData: Any?) {
    }

    override fun advance(amount: Float) {
    }

    override fun backFromEngagement(battleResult: EngagementResultAPI?) {
    }

    override fun getContext(): Any? {
        return null
    }

    override fun getMemoryMap(): MutableMap<String, MemoryAPI>? {
        return null
    }
}