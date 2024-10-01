package org.selkie.kol.impl.fx;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

public class FakeSmokePlugin extends BaseEveryFrameCombatPlugin {
  static final CombatEngineLayers SMOKE_RENDER_LAYER = CombatEngineLayers.CONTRAILS_LAYER;
  
  public static final List<Map<String, Float>> SMOKE = new ArrayList<>();
  
  public static void addMemberDirectly(Map<String, Float> data) {
    SMOKE.add(data);
  }
  
  public static void addFakeSmoke(float time, float size, Vector2f position, Vector2f velocity, float angularVelocity, float opacity, Color renderColor) {
    Map<String, Float> data = new HashMap<>();
    data.put("t", time);
    data.put("st", time);
    data.put("s", size);
    data.put("x", position.x);
    data.put("y", position.y);
    data.put("vx", velocity.x);
    data.put("vy", velocity.y);
    data.put("a", MathUtils.getRandomNumberInRange(0.0F, 360.0F));
    data.put("va", angularVelocity);
    data.put("o", opacity);
    data.put("g", (float) MathUtils.getRandomNumberInRange(1, 5));
    data.put("cr", renderColor.getRed() / 255.0F);
    data.put("cg", renderColor.getGreen() / 255.0F);
    data.put("cb", renderColor.getBlue() / 255.0F);
    SMOKE.add(data);
  }
  
  private final List<Map<String, Float>> toRemove = new ArrayList<>();
  
  private final SpriteAPI smoke1 = Global.getSettings().getSprite("SRD_fake_smoke", "1");
  
  private final SpriteAPI smoke2 = Global.getSettings().getSprite("SRD_fake_smoke", "2");
  
  private final SpriteAPI smoke3 = Global.getSettings().getSprite("SRD_fake_smoke", "3");
  
  private final SpriteAPI smoke4 = Global.getSettings().getSprite("SRD_fake_smoke", "4");
  
  private final SpriteAPI smoke5 = Global.getSettings().getSprite("SRD_fake_smoke", "5");
  
  public void init(CombatEngineAPI engine) {
    SMOKE.clear();
    FakeSmokeRenderer renderer = new FakeSmokeRenderer(this);
    engine.addLayeredRenderingPlugin(renderer);
  }
  
  public void renderAllSmoke(ViewportAPI view) {
    CombatEngineAPI engine = Global.getCombatEngine();
    if (engine == null)
      return; 
    if (!SMOKE.isEmpty()) {
      float amount = engine.isPaused() ? 0.0F : engine.getElapsedInLastFrame();
      for (Map<String, Float> entry : SMOKE) {
        float time = entry.get("t");
        time -= amount;
        float angleChange = entry.get("va") * amount;
        entry.put("a", entry.get("a") + angleChange);
        float xChange = entry.get("vx") * amount;
        entry.put("x", entry.get("x") + xChange);
        float yChange = entry.get("vy") * amount;
        entry.put("y", entry.get("y") + yChange);
        if (time <= 0.0F) {
          this.toRemove.add(entry);
          continue;
        } 
        if (view.isNearViewport(new Vector2f(entry.get("x"), entry.get("y")), entry.get("s"))) {
          float opacity = entry.get("o") * entry.get("t") / entry.get("st");
          float sizeMod = 1.0F + 2.0F * (entry.get("st") - entry.get("t")) / entry.get("st");
          SpriteAPI sprite = this.smoke1;
          if (entry.get("g") == 2.0F) {
            sprite = this.smoke2;
          } else if (entry.get("g") == 3.0F) {
            sprite = this.smoke3;
          } else if (entry.get("g") == 4.0F) {
            sprite = this.smoke4;
          } else if (entry.get("g") == 5.0F) {
            sprite = this.smoke5;
          } 
          render(sprite, entry.get("s") * sizeMod, entry.get("a"), opacity, entry.get("x"), entry.get("y"), new Color(entry.get("cr"), entry.get("cg"), entry.get("cb")));
        } 
        entry.put("t", time);
      } 
      if (!this.toRemove.isEmpty()) {
        for (Map<String, Float> w : this.toRemove)
          SMOKE.remove(w); 
        this.toRemove.clear();
      } 
    } 
  }
  
  private void render(SpriteAPI sprite, float width, float angle, float opacity, float posX, float posY, Color renderColor) {
    GL11.glEnable(3042);
    GL11.glEnable(3553);
    GL11.glBlendFunc(770, 771);
    GL11.glBindTexture(3553, sprite.getTextureId());
    GL11.glBegin(7);
    float size = width / 2.0F;
    Vector2f position = new Vector2f(posX, posY);
    GL11.glColor4ub((byte)renderColor.getRed(), (byte)renderColor.getGreen(), (byte)renderColor.getBlue(), (byte)(int)(renderColor.getAlpha() * opacity));
    GL11.glTexCoord2f(0.0F, 1.0F);
    Vector2f vec = MathUtils.getPointOnCircumference(position, size, angle + 315.0F);
    GL11.glVertex2f(vec.getX(), vec.getY());
    GL11.glTexCoord2f(0.0F, 0.0F);
    vec = MathUtils.getPointOnCircumference(position, size, angle + 225.0F);
    GL11.glVertex2f(vec.getX(), vec.getY());
    GL11.glTexCoord2f(1.0F, 0.0F);
    vec = MathUtils.getPointOnCircumference(position, size, angle + 135.0F);
    GL11.glVertex2f(vec.getX(), vec.getY());
    GL11.glTexCoord2f(1.0F, 1.0F);
    vec = MathUtils.getPointOnCircumference(position, size, angle + 45.0F);
    GL11.glVertex2f(vec.getX(), vec.getY());
    GL11.glEnd();
  }
}