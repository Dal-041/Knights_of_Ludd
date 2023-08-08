package org.selkie.kol.impl.fx;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.CombatLayeredRenderingPlugin;
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
  static CombatEngineLayers SMOKE_RENDER_LAYER = CombatEngineLayers.CONTRAILS_LAYER;
  
  public static List<Map<String, Float>> SMOKE = new ArrayList<>();
  
  public static void addMemberDirectly(Map<String, Float> data) {
    SMOKE.add(data);
  }
  
  public static void addFakeSmoke(float time, float size, Vector2f position, Vector2f velocity, float angularVelocity, float opacity, Color renderColor) {
    Map<String, Float> data = new HashMap<>();
    data.put("t", Float.valueOf(time));
    data.put("st", Float.valueOf(time));
    data.put("s", Float.valueOf(size));
    data.put("x", Float.valueOf(position.x));
    data.put("y", Float.valueOf(position.y));
    data.put("vx", Float.valueOf(velocity.x));
    data.put("vy", Float.valueOf(velocity.y));
    data.put("a", Float.valueOf(MathUtils.getRandomNumberInRange(0.0F, 360.0F)));
    data.put("va", Float.valueOf(angularVelocity));
    data.put("o", Float.valueOf(opacity));
    data.put("g", Float.valueOf(MathUtils.getRandomNumberInRange(1, 5)));
    data.put("cr", Float.valueOf(renderColor.getRed() / 255.0F));
    data.put("cg", Float.valueOf(renderColor.getGreen() / 255.0F));
    data.put("cb", Float.valueOf(renderColor.getBlue() / 255.0F));
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
    engine.addLayeredRenderingPlugin((CombatLayeredRenderingPlugin)renderer);
  }
  
  public void renderAllSmoke(ViewportAPI view) {
    CombatEngineAPI engine = Global.getCombatEngine();
    if (engine == null)
      return; 
    if (!SMOKE.isEmpty()) {
      float amount = engine.isPaused() ? 0.0F : engine.getElapsedInLastFrame();
      for (Map<String, Float> entry : SMOKE) {
        float time = ((Float)entry.get("t")).floatValue();
        time -= amount;
        float angleChange = ((Float)entry.get("va")).floatValue() * amount;
        entry.put("a", Float.valueOf(((Float)entry.get("a")).floatValue() + angleChange));
        float xChange = ((Float)entry.get("vx")).floatValue() * amount;
        entry.put("x", Float.valueOf(((Float)entry.get("x")).floatValue() + xChange));
        float yChange = ((Float)entry.get("vy")).floatValue() * amount;
        entry.put("y", Float.valueOf(((Float)entry.get("y")).floatValue() + yChange));
        if (time <= 0.0F) {
          this.toRemove.add(entry);
          continue;
        } 
        if (view.isNearViewport(new Vector2f(((Float)entry.get("x")).floatValue(), ((Float)entry.get("y")).floatValue()), ((Float)entry.get("s")).floatValue())) {
          float opacity = ((Float)entry.get("o")).floatValue() * ((Float)entry.get("t")).floatValue() / ((Float)entry.get("st")).floatValue();
          float sizeMod = 1.0F + 2.0F * (((Float)entry.get("st")).floatValue() - ((Float)entry.get("t")).floatValue()) / ((Float)entry.get("st")).floatValue();
          SpriteAPI sprite = this.smoke1;
          if (((Float)entry.get("g")).floatValue() == 2.0F) {
            sprite = this.smoke2;
          } else if (((Float)entry.get("g")).floatValue() == 3.0F) {
            sprite = this.smoke3;
          } else if (((Float)entry.get("g")).floatValue() == 4.0F) {
            sprite = this.smoke4;
          } else if (((Float)entry.get("g")).floatValue() == 5.0F) {
            sprite = this.smoke5;
          } 
          render(sprite, ((Float)entry.get("s")).floatValue() * sizeMod, ((Float)entry.get("a")).floatValue(), opacity, ((Float)entry.get("x")).floatValue(), ((Float)entry.get("y")).floatValue(), new Color(((Float)entry.get("cr")).floatValue(), ((Float)entry.get("cg")).floatValue(), ((Float)entry.get("cb")).floatValue()));
        } 
        entry.put("t", Float.valueOf(time));
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