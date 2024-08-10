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
import com.fs.starfarer.api.graphics.SpriteAPI
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.Strings
import com.fs.starfarer.api.loading.Description
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.opengl.GL11
import org.selkie.kol.impl.campaign.AICoreCampaignPlugin
import org.selkie.kol.impl.helpers.ZeaStaticStrings.BossCore
import java.util.*
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin


class BossCoreSpecialItemPlugin : BaseSpecialItemPlugin() {

    lateinit var commoditySpec: CommoditySpecAPI
    lateinit var plugin: AICoreOfficerPlugin
    val DUSK_GLOW_LOOP_MS = 3000
    var dawnGlowActive = true


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

        var sprite = Global.getSettings().getSprite(commoditySpec.iconName)
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

        if(commoditySpec.id == BossCore.DUSK_CORE.itemID){
            val glow: SpriteAPI = Global.getSettings().getSprite("cores", "dusk_glow")
            val mask: SpriteAPI = Global.getSettings().getSprite("cores", "dusk_mask")

            val secondFraction = (System.currentTimeMillis() % DUSK_GLOW_LOOP_MS) / DUSK_GLOW_LOOP_MS.toFloat()

            //sprite render
            glow.setNormalBlend()
            glow.alphaMult = getModifiedSineWaveY(secondFraction)
            glow.renderAtCenter(centerX, centerY)

            // mask
            GL11.glColorMask(false, false, false, true)
            GL11.glPushMatrix()
            GL11.glTranslatef(centerX, centerY, 0f)
            Misc.renderQuadAlpha(x * 3f, y * 3f, w * 3f, h * 3f, Misc.zeroColor, 0f)
            GL11.glPopMatrix()
            glow.setBlendFunc(GL11.GL_ONE, GL11.GL_ZERO)
            glow.renderAtCenter(centerX, centerY)


            mask.alphaMult = alphaMult * 0.9f
            mask.angle = -secondFraction * 90f
            mask.setBlendFunc(GL11.GL_ZERO, GL11.GL_SRC_ALPHA)
            mask.renderAtCenter(centerX, centerY)

            GL11.glColorMask(true, true, true, false)
            mask.setBlendFunc(GL11.GL_DST_ALPHA, GL11.GL_ONE_MINUS_DST_ALPHA)
            mask.renderAtCenter(centerX, centerY)
        }

        if(commoditySpec.id == BossCore.DAWN_CORE.itemID){
            val glow: SpriteAPI = Global.getSettings().getSprite("cores", "dawn_glow")

            // Yes I know this is tied to framerate, but eh too lazy and its random flicker effect in the first place
            dawnGlowActive = if (dawnGlowActive && Math.random().toFloat() > 0.995) false else if (!dawnGlowActive && Math.random().toFloat() > 0.9f) true else dawnGlowActive

            glow.setNormalBlend()
            glow.alphaMult = if (dawnGlowActive) 1f else Math.random().toFloat()
            glow.renderAtCenter(centerX, centerY)
        }
    }

    fun getModifiedSineWaveY(progress: Float): Float {
        var progress = progress
        val sineDuration = 0.08f
        var s: String? = "progress - $progress"
        var y = 0f
        if (progress < sineDuration) {
            //A*sin(bx)
            //A = 1 damit obere/untere bounds 1
            //B macht die kurve schärfer je höher es ist, b= 1 heißt y0 = 0, y360 = 0, b = 3 heißt y0 = 0, y120 (360/3) = 1
            //geht ab 0.5 ins minus!
            s += " - sine: "
            y = sin(2f * Math.PI * (progress / sineDuration)).toFloat()
        } else {
            progress -= sineDuration //so it starts with 0
            progress *= 10f //don't ask
            val riseSteepness = 10f
            val fallCenter = 1f
            val fallWidth = 1.5f

            //exponential rise
            val risePart = (1f / (1f + exp((-riseSteepness * (progress - 0.2f)).toDouble()))).toFloat()

            //gaussian fall (slow fall off)
            val fallPart = exp(-(progress - fallCenter).pow(2f) / (2f * fallWidth.pow(2f))).toFloat()
            s += " - gaussian zombie: "
            y = MathUtils.clamp(risePart * fallPart, 0f, 1f)
        }
        s += y
        return 0.5f + 0.5f * y //y can be -1 for the sine!
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
        val pointsMult = corePerson.memoryWithoutUpdate.getFloat(AICoreOfficerPlugin.AUTOMATED_POINTS_MULT).toInt()
        val pointsMultSameFaction = String.format("%.1f", BossAICoreOfficerPlugin.AUTOMATED_POINTS_MULT_SAME_FACTION)

        val desc = Global.getSettings().getDescription(commoditySpec.id, Description.Type.RESOURCE)

        val img = tooltip!!.beginImageWithText(corePerson.portraitSprite, 96f)
        img!!.addTitle(name)

        img.addPara(desc.text1, 0f)

        img.addSpacer(5f)

        img.addPara("Level: ${corePerson.stats.level}", 0f, Misc.getTextColor(), Misc.getHighlightColor(), "Level")
        img.addPara("Automated Points Multiplier: ${pointsMult}${Strings.X} (${pointsMultSameFaction}${Strings.X} on same faction ships)", 0f,
            Misc.getTextColor(), Misc.getHighlightColor(), "Automated Points Multiplier:")

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