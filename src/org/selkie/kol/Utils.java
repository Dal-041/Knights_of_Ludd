package org.selkie.kol;

import com.fs.graphics.util.Fader;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.NegativeExplosionVisual;
import com.fs.starfarer.api.impl.combat.RiftCascadeMineExplosion;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

import static com.fs.starfarer.api.util.Misc.ZERO;

public class Utils {

    public static class FogSpawner{
        boolean useNormal = true;
        final IntervalUtil fogIntervalNormal = new IntervalUtil(0.1f, 0.15f);
        final IntervalUtil fogIntervalSpecial = new IntervalUtil(0.35f, 0.6f);
        public void spawnFog(float amount, float radius, Vector2f location){

            final Color rgbPos = new Color(90,160,222,60);
            final Color rgbNeg = new Color(145,85,115,60);
            final Color specialOne = new Color(215, 30, 19, 60);
            final Color specialTwo = new Color(14, 220, 200, 60);



            CombatEngineAPI engine = Global.getCombatEngine();
            fogIntervalNormal.advance(amount);
            if (fogIntervalNormal.intervalElapsed()) {

                Vector2f point = MathUtils.getRandomPointInCircle(location, radius);

                engine.addNebulaParticle(
                        point,
                        MathUtils.getRandomPointInCircle(ZERO, 50f),
                        MathUtils.getRandomNumberInRange(150f, 300f),
                        0.3f,
                        0.5f,
                        0.5f,
                        MathUtils.getRandomNumberInRange(2.0f, 3.4f),
                        rgbPos
                );

                point = MathUtils.getRandomPointInCircle(location, radius * 0.75f);

                if (useNormal && MathUtils.getRandom().nextInt() % 64 != 0) {
                    engine.addNegativeNebulaParticle(
                            point,
                            MathUtils.getRandomPointInCircle(ZERO, 50f),
                            MathUtils.getRandomNumberInRange(150f, 300f),
                            0.3f,
                            0.5f,
                            0.5f,
                            MathUtils.getRandomNumberInRange(2.0f, 3.4f),
                            rgbNeg
                    );
                } else {
                    useNormal = false;
                    fogIntervalSpecial.advance(amount);
                    if (fogIntervalSpecial.intervalElapsed()) {
                        useNormal = true;
                    }
                    engine.addNegativeNebulaParticle(
                            point,
                            MathUtils.getRandomPointInCircle(ZERO, 35f),
                            MathUtils.getRandomNumberInRange(75f, 200f),
                            0.1f,
                            0.5f,
                            0.5f,
                            MathUtils.getRandomNumberInRange(2.0f, 3.4f),
                            specialTwo
                    );
                    engine.addNebulaParticle(
                            point,
                            MathUtils.getRandomPointInCircle(ZERO, 35f),
                            MathUtils.getRandomNumberInRange(75f, 200f),
                            0.1f,
                            0.5f,
                            0.5f,
                            MathUtils.getRandomNumberInRange(2.0f, 3.4f),
                            specialOne
                    );
                }
            }
        }
    }
    private static long dialogTime = 0;
    private static long commandTime = 0;
    private static long hudTime = 0;
    public static float getUIAlpha(boolean inUIRenderMethod) {
        final float DIALOG_ALPHA = 0.33f;
        final float DIALOG_FADE_OUT_TIME = 333f;
        final float DIALOG_FADE_IN_TIME = 250f;
        final float COMMAND_FADE_OUT_TIME = 200f;
        final float COMMAND_FADE_IN_TIME = 111f;

        // Used to properly interpolate between UI fade colors
        float alpha;
        if(!Global.getCombatEngine().isUIShowingHUD() && !Global.getCombatEngine().isUIShowingDialog()){
            alpha = 0;
        } else if (Global.getCombatEngine().getCombatUI().isShowingCommandUI()) {
            commandTime = System.currentTimeMillis();
            alpha = (inUIRenderMethod ? 1f : 0.5f) - (float) Math.pow(Math.min((commandTime - hudTime) / COMMAND_FADE_OUT_TIME, 1f), 10f);
        } else if (Global.getCombatEngine().isUIShowingDialog()) {
            dialogTime = System.currentTimeMillis();
            if (inUIRenderMethod) alpha = 1;
            else alpha = Misc.interpolate(1f, DIALOG_ALPHA, Math.min((dialogTime - hudTime) / DIALOG_FADE_OUT_TIME, 1f));
        }  else if (dialogTime > commandTime) {
            hudTime = System.currentTimeMillis();
            if (inUIRenderMethod) alpha = 1;
            else alpha = Misc.interpolate(DIALOG_ALPHA, 1f, Math.min((hudTime - dialogTime) / DIALOG_FADE_IN_TIME, 1f));
        } else {
            hudTime = System.currentTimeMillis();
            alpha =(float) Math.pow(Math.min((hudTime - commandTime) / COMMAND_FADE_IN_TIME, 1f), 0.5f);
        }
        return MathUtils.clamp(alpha, 0f, ((Fader) ReflectionUtils.get("fader", Global.getCombatEngine().getCombatUI())).getBrightness());
    }

    public static void shipSpawnExplosion(float size, Vector2f location){
        NegativeExplosionVisual.NEParams p = RiftCascadeMineExplosion.createStandardRiftParams(new Color(80,160,240,255), size);
        p.fadeOut = 0.15f;
        p.hitGlowSizeMult = 0.25f;
        p.underglow = new Color(5,120,180,150);
        p.withHitGlow = false;
        p.noiseMag = 1.25f;
        CombatEntityAPI e = Global.getCombatEngine().addLayeredRenderingPlugin(new NegativeExplosionVisual(p));
        e.getLocation().set(location);
    }

    public static class LinearSRBG {
        LinearSRBG(float R, float G, float B){
            this.R = R;
            this.G = G;
            this.B = B;
        }
        public float R, G, B;
    }

    public static class OKLab {
        OKLab(float L, float a, float b) {
            this.L = L;
            this.a = a;
            this.b = b;
        }
        public float L, a, b;
    }

    public static Color OKLabInterpolateColor(Color from, Color to, float progress){
        progress = Math.min(Math.max(0, progress), 1);
        OKLab OKLabFrom = sRBGLinearToOKLab(sRBGLinear(from));
        OKLab OKLabTo = sRBGLinearToOKLab(sRBGLinear(to));
        OKLab out = new OKLab(Misc.interpolate(OKLabFrom.L, OKLabTo.L, progress), Misc.interpolate(OKLabFrom.a, OKLabTo.a, progress), Misc.interpolate(OKLabFrom.b, OKLabTo.b, progress));
        return sRBG(OKLabToLinearSRBG(out), Misc.interpolate(from.getAlpha() / 255f, to.getAlpha() / 255f, progress));
    }

    public static OKLab sRBGLinearToOKLab(LinearSRBG c)
    {
        float l = 0.4122214708f * c.R + 0.5363325363f * c.G + 0.0514459929f * c.B;
        float m = 0.2119034982f * c.R + 0.6806995451f * c.G + 0.1073969566f * c.B;
        float s = 0.0883024619f * c.R + 0.2817188376f * c.G + 0.6299787005f * c.B;

        float l_ = (float) Math.cbrt(l);
        float m_ = (float) Math.cbrt(m);
        float s_ = (float) Math.cbrt(s);

        return new OKLab(
            0.2104542553f*l_ + 0.7936177850f*m_ - 0.0040720468f*s_,
            1.9779984951f*l_ - 2.4285922050f*m_ + 0.4505937099f*s_,
            0.0259040371f*l_ + 0.7827717662f*m_ - 0.8086757660f*s_
        );
    }

    public static LinearSRBG OKLabToLinearSRBG(OKLab c)
    {
        float l_ = c.L + 0.3963377774f * c.a + 0.2158037573f * c.b;
        float m_ = c.L - 0.1055613458f * c.a - 0.0638541728f * c.b;
        float s_ = c.L - 0.0894841775f * c.a - 1.2914855480f * c.b;

        float l = l_*l_*l_;
        float m = m_*m_*m_;
        float s = s_*s_*s_;

        return new LinearSRBG(
            +4.0767416621f * l - 3.3077115913f * m + 0.2309699292f * s,
            -1.2684380046f * l + 2.6097574011f * m - 0.3413193965f * s,
            -0.0041960863f * l - 0.7034186147f * m + 1.7076147010f * s
        );
    }

    public static LinearSRBG sRBGLinear(Color in){
        return new LinearSRBG(sRBGLinearSingle(in.getRed() / 255f), sRBGLinearSingle(in.getGreen() / 255f), sRBGLinearSingle(in.getBlue() / 255f));
    }

    public static Color sRBG(LinearSRBG in, float alpha){
        return new Color(Math.min(Math.max(0, sRBGSingle(in.R)), 1), Math.min(Math.max(0, sRBGSingle(in.G)), 1), Math.min(Math.max(0, sRBGSingle(in.B)), 1), alpha);
    }

    private static float sRBGLinearSingle(float x){
        if (x >= 0.0031308)
            return (float) (1.055 * Math.pow(x, 1.0 / 2.4) - 0.055);
        else
            return 12.92f * x;
    }

    private static float sRBGSingle(float x){
        if (x >= 0.04045)
            return (float) Math.pow((x + 0.055) / (1.055), 2.4);
        else
            return x / 12.92f;
    }

    public static float linMap(float minOut, float maxOut, float minIn, float maxIn, float input){
        if(input > maxIn) return maxOut;
        if(input < minIn) return minOut;
        return minOut + (input - minIn) * (maxOut - minOut) / (maxIn - minIn);
    }

    private static final String DRONE_SHIELD_TARGET_KEY = "droneShieldTargetKey";

    public static ShipAPI getDroneShieldTarget(ShipAPI drone) {
        return drone.getCustomData().containsKey(DRONE_SHIELD_TARGET_KEY) ? (ShipAPI) drone.getCustomData().get(DRONE_SHIELD_TARGET_KEY) : null;
    }

    public static void setDroneShieldTarget(ShipAPI drone, ShipAPI target) {
        if (target == null) {
            drone.getCustomData().remove(DRONE_SHIELD_TARGET_KEY);
        } else {
            drone.setCustomData(DRONE_SHIELD_TARGET_KEY, target);
        }
    }

    public static boolean anyDronesShieldingShip(ShipAPI target) {
        for (ShipAPI drone : AIUtils.getAlliesOnMap(target)) {
            if (drone.getCustomData().containsKey(DRONE_SHIELD_TARGET_KEY) && drone.getCustomData().get(DRONE_SHIELD_TARGET_KEY) == target) {
                return true;
            }
        }
        return false;
    }
}
