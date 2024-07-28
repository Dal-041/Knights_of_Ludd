package org.selkie.kol.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

public class HullmodBackgroundElement extends BaseCustomUIPanelPlugin {


    public SpriteAPI sprite;

    public TooltipMakerAPI tooltip;

    public HullmodBackgroundElement(TooltipMakerAPI tooltip, SpriteAPI sprite) {
        CustomPanelAPI panel = Global.getSettings().createCustom(0f, 0f, this);
        tooltip.addCustom(panel, 0f);

        this.tooltip = tooltip;
        this.sprite = sprite;
    }

    @Override
    public void render(float alphaMult) {

        sprite.setSize( tooltip.getWidthSoFar() + 20, tooltip.getHeightSoFar() + 10);
        sprite.setAdditiveBlend();
        sprite.setAlphaMult(0.8f);
        sprite.render(tooltip.getPosition().getX(), tooltip.getPosition().getY());

    }
}
