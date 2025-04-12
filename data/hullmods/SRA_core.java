package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShieldAPI.ShieldType;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;

public class SRA_core extends BaseHullMod {
	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {

		stats.getVentRateMult().modifyPercent(id, 100f);					//强制Flux消散速度			
		
		stats.getShieldUnfoldRateMult().modifyPercent(id, 1000f);			//护盾展开速度

		stats.getCombatEngineRepairTimeMult().modifyPercent(id, -50f);			//战斗修理引擎时间
		stats.getCombatWeaponRepairTimeMult().modifyPercent(id, -50f);			//战斗修理武器时间
			
		stats.getFluxDamageTakenMult().modifyPercent(id, -100f);			//减少收到的(EMP)伤害
		
		stats.getSensorStrength().modifyPercent(id, 3000f);			//提高雷达范围
		
		
	}
}

