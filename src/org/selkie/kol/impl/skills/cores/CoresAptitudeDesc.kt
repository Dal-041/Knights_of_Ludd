package org.selkie.kol.impl.skills.cores

import com.fs.starfarer.api.characters.DescriptionSkillEffect
import com.fs.starfarer.api.util.Misc
import java.awt.Color

class CoresAptitudeDesc : DescriptionSkillEffect {
    override fun getTextColor(): Color {
        return Misc.getTextColor();

    }

    override fun getString(): String {
        return ""
    }

    override fun getHighlights(): Array<String> {
        return arrayOf("")
    }

    override fun getHighlightColors(): Array<Color> {
        val h = Misc.getHighlightColor()
        val s = Misc.getStoryOptionColor()
        return arrayOf(h, h, h, s)
    }


}