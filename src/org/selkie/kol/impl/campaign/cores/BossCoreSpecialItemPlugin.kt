package org.selkie.kol.impl.campaign.cores

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.AICoreOfficerPlugin
import com.fs.starfarer.api.campaign.CargoStackAPI
import com.fs.starfarer.api.campaign.CargoTransferHandlerAPI
import com.fs.starfarer.api.campaign.SpecialItemPlugin
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.econ.SubmarketAPI
import com.fs.starfarer.api.campaign.impl.items.BaseSpecialItemPlugin
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.Strings
import com.fs.starfarer.api.loading.Description
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import org.selkie.kol.impl.campaign.AICoreCampaignPlugin
import org.selkie.kol.impl.skills.cores.DawnBossCoreSkill
import org.selkie.kol.impl.skills.cores.DuskBossCoreSkill
import org.selkie.kol.impl.skills.cores.ElysiaBossCoreSkill
import java.util.*

class BossCoreSpecialItemPlugin : BaseSpecialItemPlugin() {

    lateinit var commoditySpec: CommoditySpecAPI
    lateinit var plugin: AICoreOfficerPlugin

    var cores = mapOf(
        "zea_dusk_boss_core" to "zea_dusk_boss_core_skill"
        , "zea_dawn_boss_core" to "zea_dawn_boss_core_skill",
        "zea_elysia_boss_core"  to "zea_elysia_boss_core_skill")

    override fun init(stack: CargoStackAPI) {
        super.init(stack)

        var data = stack.specialDataIfSpecial.data

        if (!cores.keys.contains(data)) {
            data = "zea_dusk_boss_core"
            stack.specialDataIfSpecial.data = data
        }

        commoditySpec = Global.getSettings().getCommoditySpec(data)
        plugin = AICoreCampaignPlugin().pickAICoreOfficerPlugin(commoditySpec.id)!!.plugin

    }

    override fun render(x: Float, y: Float, w: Float, h: Float, alphaMult: Float, glowMult: Float, renderer: SpecialItemPlugin.SpecialItemRendererAPI?) {
        var centerX = x+w/2
        var centerY = y+h/2

        var sprite = Global.getSettings().getSprite(commoditySpec!!.iconName)
        sprite.setNormalBlend()
        sprite.alphaMult = alphaMult
        sprite.setSize(w - 20, h - 20)
        sprite.renderAtCenter(centerX, centerY)

        if (glowMult > 0) {
            sprite.setAdditiveBlend()
            sprite.alphaMult = alphaMult * glowMult * 0.5f
            sprite.setSize(w - 20, h - 20)
            sprite.renderAtCenter(centerX, centerY)
        }
    }

    override fun getName(): String {
        return commoditySpec.name
    }

    override fun getTooltipWidth(): Float {
        return 500f
    }

    override fun createTooltip(tooltip: TooltipMakerAPI?, expanded: Boolean, transferHandler: CargoTransferHandlerAPI?, stackSource: Any?) {
        val opad = 10f

        var skillSpec = Global.getSettings().getSkillSpec(cores.get(commoditySpec.id))

        tooltip!!.addTitle(name)

        val design = designType
        Misc.addDesignTypePara(tooltip, design, opad)

        tooltip.addSpacer(10f)
        var desc = Global.getSettings().getDescription(commoditySpec.id, Description.Type.RESOURCE)
        tooltip.addPara(desc.text1, 0f, Misc.getTextColor(), Misc.getHighlightColor())

        var corePerson = plugin.createPerson(commoditySpec.id, Factions.NEUTRAL, Random())
        var pointsMult = corePerson.memoryWithoutUpdate.getFloat(AICoreOfficerPlugin.AUTOMATED_POINTS_MULT).toInt()

        tooltip.addSpacer(10f)
        tooltip.addPara("Automated Points Multiplier: ${pointsMult}${Strings.X}", 0f, Misc.getTextColor(), Misc.getHighlightColor(),  "Automated Points Multiplier")

        if (commoditySpec.id == "zea_dusk_boss_core") {
            tooltip.addSpacer(10f)
            tooltip.addSectionHeading("Transcendence", Alignment.MID, 0f)
            tooltip.addSpacer(10f)

            tooltip.addPara("This core can interface with crewed and automated ships. To slot it in to crewed ships, navigate to the refit screen and select \"Additional Options\". " +
                    "Its skills can not be configured on crewed ships.", 0f,
                Misc.getTextColor(), Misc.getHighlightColor(), "crewed and automated", "Additional Options")
        }


        tooltip.addSpacer(10f)
        tooltip.addSectionHeading("Unique Skill: ${skillSpec.name}", Alignment.MID, 0f)
        tooltip.addSpacer(10f)

        var skillImg = tooltip.beginImageWithText(skillSpec.spriteName, 48f)

        when (skillSpec.id) {
            "zea_dawn_boss_core_skill" -> DawnBossCoreSkill().createCustomDescription(null, null, skillImg, tooltip.widthSoFar)
            "zea_dusk_boss_core_skill" -> DuskBossCoreSkill().createCustomDescription(null, null, skillImg, tooltip.widthSoFar)
            "zea_elysia_boss_core_skill" -> ElysiaBossCoreSkill().createCustomDescription(null, null, skillImg, tooltip.widthSoFar)
        }

        tooltip.addImageWithText(0f)


        addCostLabel(tooltip, opad, transferHandler, stackSource)
    }

    override fun getPrice(market: MarketAPI?, submarket: SubmarketAPI?): Int {
        val base = commoditySpec.basePrice
        return base.toInt()
    }




}