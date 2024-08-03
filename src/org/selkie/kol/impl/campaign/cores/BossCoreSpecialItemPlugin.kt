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
import org.selkie.kol.impl.helpers.ZeaStaticStrings.BossCore
import org.selkie.kol.impl.skills.cores.DawnBossCoreSkill
import org.selkie.kol.impl.skills.cores.DuskBossCoreSkill
import org.selkie.kol.impl.skills.cores.ElysiaBossCoreSkill
import java.util.*

class BossCoreSpecialItemPlugin : BaseSpecialItemPlugin() {

    lateinit var commoditySpec: CommoditySpecAPI
    lateinit var plugin: AICoreOfficerPlugin

    override fun init(stack: CargoStackAPI) {
        super.init(stack)

        var data = stack.specialDataIfSpecial.data

        if (data !in BossCore.ITEM_ID_LIST) {
            data = BossCore.DUSK_CORE.itemID
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
        return 600f
    }

    override fun createTooltip(tooltip: TooltipMakerAPI?, expanded: Boolean, transferHandler: CargoTransferHandlerAPI?, stackSource: Any?) {
        val opad = 10f

        val skillSpec = Global.getSettings().getSkillSpec(BossCore.getCore(commoditySpec.id).exclusiveSkillID)

        val design = designType
        Misc.addDesignTypePara(tooltip, design, opad)

        val corePerson = plugin.createPerson(commoditySpec.id, Factions.NEUTRAL, Random())
        val pointsMult = corePerson.memoryWithoutUpdate.getFloat(AICoreOfficerPlugin.AUTOMATED_POINTS_MULT)

        val desc = Global.getSettings().getDescription(commoditySpec.id, Description.Type.RESOURCE)

        val img = tooltip!!.beginImageWithText(corePerson.portraitSprite, 96f)
        img!!.addTitle(name)

        img.addPara(desc.text1, 0f)

        img.addSpacer(5f)

        img.addPara("Level: ${corePerson.stats.level}", 0f, Misc.getTextColor(), Misc.getHighlightColor(), "Level")
        img.addPara("Automated Points Multiplier: ${pointsMult}${Strings.X}", 0f,
            Misc.getTextColor(), Misc.getHighlightColor(), "Automated Points Multiplier")

        tooltip.addImageWithText(0f)

        tooltip.addSpacer(10f)
        tooltip.addSectionHeading("Unique Skill: ${skillSpec.name}", Alignment.MID, 0f)
        tooltip.addSpacer(10f)

        BossCore.getCore(commoditySpec.id).exclusiveSkill.newInstance().createCustomDescription(null, null, tooltip, tooltip.widthSoFar)

        addCostLabel(tooltip, opad, transferHandler, stackSource)
    }

    override fun getPrice(market: MarketAPI?, submarket: SubmarketAPI?): Int {
        val base = commoditySpec.basePrice
        return base.toInt()
    }
}