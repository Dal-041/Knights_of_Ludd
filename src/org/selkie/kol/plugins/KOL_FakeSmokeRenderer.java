package org.selkie.kol.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseCombatLayeredRenderingPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.ViewportAPI;
import java.util.EnumSet;

class KOL_FakeSmokeRenderer extends BaseCombatLayeredRenderingPlugin {
  private final KOL_FakeSmokePlugin parentPlugin;
  
  KOL_FakeSmokeRenderer(KOL_FakeSmokePlugin parentPlugin) {
    this.parentPlugin = parentPlugin;
  }
  
  public void render(CombatEngineLayers layer, ViewportAPI view) {
    CombatEngineAPI engine = Global.getCombatEngine();
    if (engine == null)
      return; 
    if (getActiveLayers().contains(layer))
      this.parentPlugin.renderAllSmoke(view); 
  }
  
  public float getRenderRadius() {
    return 9.9999999E14F;
  }
  
  public EnumSet<CombatEngineLayers> getActiveLayers() {
    return EnumSet.of(KOL_FakeSmokePlugin.SMOKE_RENDER_LAYER);
  }
}