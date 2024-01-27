package org.selkie.kol;

import com.fs.starfarer.api.util.Misc;

import java.awt.*;

public class Utils {
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

}
