package org.selkie.kol.impl.campaign.interactions

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.CargoAPI.CargoItemType
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.combat.EngagementResultAPI
import com.fs.starfarer.api.impl.campaign.ids.Sounds
import com.fs.starfarer.api.loading.Description
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
    companion object{
        val skillspec = Global.getSettings().getSkillSpec("zea_dusk_boss_core_skill")
        val LEAVE_KEY = "LEAVE"
        val ENTER_KEY = "ENTER"
        val CORE_INTO_SKILL = "CORE_INTO_SKILL"
        val SKILL_INTO_CORE = "SKILL_INTO_CORE"
        val FORCE_REVOKE = "FORCE_REVOKE"
    }
    override fun init(dialog: InteractionDialogAPI?) {
        this.dialog = dialog!!

        dialog.visualPanel.showImageVisual(dialog.interactionTarget.customInteractionDialogImageVisual)
        val text = Global.getSettings().getDescription(dialog.interactionTarget.customDescriptionId, Description.Type.CUSTOM).text1
        dialog.textPanel.addPara(text)
        dialog.textPanel.addPara("Text 1", Misc.getTextColor(), Misc.getHighlightColor(), "")

        dialog.optionPanel.addOption("Enter", ENTER_KEY)

        dialog.optionPanel.addOption("Leave", LEAVE_KEY)
        dialog.optionPanel.setShortcut(LEAVE_KEY, Keyboard.KEY_ESCAPE, false, false, false, false)
    }

    override fun optionSelected(optionText: String?, optionData: Any?) {
        if (optionData == LEAVE_KEY) {
            dialog.dismiss()
        }
        if (optionData == ENTER_KEY) {
            dialog.textPanel.addPara("Text 2")

            if (Global.getSector().playerFleet.cargo.getQuantity(CargoItemType.SPECIAL, SpecialItemData(BossCore.BOSS_CORE_ID, BossCore.DUSK_CORE_SKILL_ID,)) > 0){
                val tooltip = dialog.textPanel.beginTooltip()

                tooltip.setParaFont(Fonts.ORBITRON_12)
                tooltip.addPara("(Hover over the icon for a detailed description)", 0f, Misc.getGrayColor(), Misc.getGrayColor())
                val fake = Global.getFactory().createPerson()
                fake.stats.setSkillLevel(skillspec.id, 1f)
                tooltip.addSkillPanel(fake, 0f)

                dialog.textPanel.addTooltip()

                dialog.optionPanel.clearOptions()

                dialog.textPanel.addPara("Any officer can be selected for the procedure. This change is permanent and can only applied to a single person.")
                dialog.optionPanel.addOption("Select an officer", CORE_INTO_SKILL)

                dialog.optionPanel.addOption("Leave", LEAVE_KEY)
                dialog.optionPanel.setShortcut(LEAVE_KEY, Keyboard.KEY_ESCAPE, false, false, false, false)
            } else{
                dialog.textPanel.addPara("Text 3")

                dialog.optionPanel.clearOptions()

                dialog.optionPanel.addOption("Leave", LEAVE_KEY)
                dialog.optionPanel.setShortcut(LEAVE_KEY, Keyboard.KEY_ESCAPE, false, false, false, false)
            }


        }
        if (optionData == CORE_INTO_SKILL) {
            dialog.showCustomDialog(320f, 440f, object: BaseCustomDialogDelegate(){
                var selected: PersonAPI? = null

                override fun createCustomDialog(panel: CustomPanelAPI?, callback: CustomDialogDelegate.CustomDialogCallback?) {

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
                            img.addPara("Name: ${officer.nameString}", 0f, Misc.getTextColor(), Misc.getHighlightColor(), "Name:")
                            img.addPara("Personality: ${officer.getPersonalityName()}", 0f, Misc.getTextColor(), Misc.getHighlightColor(), "Personality:")
                            img.addSpacer(5f)
                            innerElement.addImageWithText(0f)
                        }

                        element.addTooltipToPrevious( object : BaseTooltipCreator() {
                            override fun createTooltip(tooltip: TooltipMakerAPI?, expanded: Boolean, tooltipParam: Any?) {
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

                    Global.getSoundPlayer().playUISound(Sounds.STORY_POINT_SPEND, 1f, 1f)

                    dialog.optionPanel.clearOptions()


                    dialog.textPanel.addPara("Choosen ${selected!!.nameString}", Misc.getBasePlayerColor(), Misc.getBasePlayerColor())

                    dialog.textPanel.addPara("You've chosen ${selected!!.nameString} as the participant of the procedure, and hours later, they awaken as something new.")

                    dialog.textPanel.addPara("> ${selected!!.nameString} acquired the ${skillspec.name} skill", Misc.getPositiveHighlightColor(), Misc.getPositiveHighlightColor())

                    selected!!.stats.setSkillLevel(skillspec.id, 1f)
                    Global.getSector().playerFleet.cargo.removeItems(CargoItemType.SPECIAL, SpecialItemData("zea_boss_core_special", "zea_dusk_boss_core"), 1f)

                    dialog.optionPanel.addOption("Leave", LEAVE_KEY)
                    dialog.optionPanel.setShortcut(LEAVE_KEY, Keyboard.KEY_ESCAPE, false, false, false, false)
                }
            })
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