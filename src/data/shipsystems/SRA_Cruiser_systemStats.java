package data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;
import com.fs.starfarer.api.util.Misc;
import data.utils.SRA_Colors;
import data.utils.SRAI18nUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.plugins.MagicTrailPlugin;

import java.util.List;


public class SRA_Cruiser_systemStats extends BaseShipSystemScript {


    public static final float SHIP_ALPHA_MULT = 0.25f;


    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
        } else {
            return;
        }
        if (state == ShipSystemStatsScript.State.OUT) {
            stats.getMaxSpeed().unmodify(id); // to slow down ship to its regular top speed while powering drive down
            stats.getMaxTurnRate().unmodify(id);
            stats.getTurnAcceleration().unmodify(id);
            VectorUtils.clampLength(ship.getVelocity(),ship.getMaxSpeed());
        } else {
            stats.getMaxSpeed().modifyFlat(id, 400f * effectLevel);
            stats.getAcceleration().modifyFlat(id, 1600f * effectLevel);
            stats.getMaxTurnRate().modifyFlat(id, 200f * effectLevel);
            stats.getTurnAcceleration().modifyFlat(id, 200f * effectLevel);
        }
        if (state == State.COOLDOWN || state == State.IDLE) {
            unapply(stats, id);
            return;
        }
        float levelForAlpha = effectLevel;
        if (state == State.IN || state == State.ACTIVE) {
            ship.setPhased(true);
            levelForAlpha = effectLevel;
        } else if (state == State.OUT) {
            ship.setPhased(effectLevel > 0.5f);
            levelForAlpha = effectLevel;
        }
        ship.setExtraAlphaMult(1f - (1f - SHIP_ALPHA_MULT) * levelForAlpha);
        // ship.setApplyExtraAlphaToEngines(true);
    }


    public void unapply(MutableShipStatsAPI stats, String id) {
        ShipAPI ship;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();

        } else {
            return;
        }
        ship.setPhased(false);
        ship.setExtraAlphaMult(1f);
        stats.getMaxSpeed().unmodify(id);
        stats.getMaxTurnRate().unmodify(id);
        stats.getTurnAcceleration().unmodify(id);
        stats.getAcceleration().unmodify(id);
        stats.getDeceleration().unmodify(id);
    }

    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0) {
            return new StatusData(SRAI18nUtil.getShipSystemString("SRA_Cruiser_systemInfo"), false);
        }
        return null;
    }


}
