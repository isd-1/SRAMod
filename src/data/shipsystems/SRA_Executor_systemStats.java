package data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.impl.hullmods.ShardSpawner;
import com.fs.starfarer.api.impl.hullmods.ShardSpawner.ShardType;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.WeightedRandomPicker;


import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import data.utils.SRAI18nUtil;

public class SRA_Executor_systemStats extends BaseShipSystemScript {

    public static final Object KEY_SHIP = new Object();
    public static final float TIMEFLOW_MULT = 8;
    public static final float SHARD_BURN_TIME = 0.75f;
    public static final Color JITTER_COLOR = new Color(168,255,192,55);
    public static final Color JITTER_UNDER_COLOR = new Color(168,255,192,155);
    public static final Color WARP_COLOR = new Color(192,208,255,224);
    public static final boolean USE_SHARD_EVERYFRAME = true;

    protected boolean triedAspectSpawnThisSystemUse;
    protected boolean registeredPlugin = !USE_SHARD_EVERYFRAME;

    protected Set<ShipAPI> aspects = new HashSet<>();

    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {

        float shipTimeMult = 1f + (TIMEFLOW_MULT - 1f) * effectLevel;
        stats.getTimeMult().modifyMult(id, shipTimeMult);

        stats.getZeroFluxMinimumFluxLevel().modifyFlat(id, 2);

        applyGfx(stats, state, effectLevel);

        if (!registeredPlugin) {
            registeredPlugin = true;
            Global.getCombatEngine().addPlugin(new ShardMonitorPlugin((ShipAPI)stats.getEntity()));
        }
    }

    public void unapply(MutableShipStatsAPI stats, String id) {
        stats.getTimeMult().unmodify(id);
        stats.getZeroFluxMinimumFluxLevel().unmodify(id);
        triedAspectSpawnThisSystemUse = false;
    }


    public void applyGfx(MutableShipStatsAPI stats, State state, float effectLevel) {
        ShipAPI ship = null;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
        } else {
            return;
        }

        ship.fadeToColor(KEY_SHIP, new Color(192,255,224,192), 0.1f, 0.1f, effectLevel);
        //ship.setWeaponGlow(effectLevel, new Color(100,165,255,255), EnumSet.of(WeaponAPI.WeaponType.BALLISTIC, WeaponAPI.WeaponType.ENERGY, WeaponAPI.WeaponType.MISSILE));
        ship.getEngineController().fadeToOtherColor(KEY_SHIP, new Color(0,0,0,0), new Color(0,0,0,0), effectLevel, 0.5f * effectLevel);

        // temporal shell copypasta
        float jitterLevel = effectLevel;
        float jitterRangeBonus = 0;
        float maxRangeBonus = 10f;
        if (state == State.IN) {
            jitterLevel = effectLevel / (1f / ship.getSystem().getChargeUpDur());
            if (jitterLevel > 1) {
                jitterLevel = 1f;
            }
            jitterRangeBonus = jitterLevel * maxRangeBonus;
        } else if (state == State.ACTIVE) {
            jitterLevel = 1f;
            jitterRangeBonus = maxRangeBonus;
        } else if (state == State.OUT) {
            jitterRangeBonus = jitterLevel * maxRangeBonus;
        }
        jitterLevel = (float) Math.sqrt(jitterLevel);

        ship.setJitter(this, JITTER_COLOR, jitterLevel, 3, 0, 0 + jitterRangeBonus);
        ship.setJitterUnder(this, JITTER_UNDER_COLOR, jitterLevel, 25, 0f, 7f + jitterRangeBonus);
    }

    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0) {
            return new StatusData(SRAI18nUtil.getShipSystemString("SRA_Executor_systemInfo"), false);
        }
        return null;
    }


    public static class ShardMonitorPlugin extends BaseEveryFrameCombatPlugin {

        protected ShipAPI ship;

        public ShardMonitorPlugin(ShipAPI ship) {
            this.ship = ship;
        }

    }
}


