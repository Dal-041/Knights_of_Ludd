package org.selkie.kol.impl.hullmods;

import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import org.lwjgl.util.vector.Vector2f;

public class ConformalListener implements AdvanceableListener {
    private float x;
    private float y;
    private boolean inited;

    public ConformalListener() {}

    public void setInited(boolean inited) {
        this.inited = inited;
    }

    public boolean isInited() {
        return inited;
    }

    public Vector2f getBounds() {
        return new Vector2f(x, y);
    }

    public void setBounds(float xIn, float yIn) {
        this.x = xIn;
        this.y = yIn;
    }

    @Override
    public void advance(float amount) {

    }
}
