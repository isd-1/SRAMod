package data.weapons.dem;

import java.awt.*;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.impl.combat.RiftCascadeMineExplosion;
import com.fs.starfarer.api.impl.combat.dem.DEMScript;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import org.lwjgl.util.vector.Vector2f;
import java.util.List;

/**
 * 
 */
public class SRA_dragonEffect implements OnFireEffectPlugin {
    Color DAMAGE_FIELD_COLOR = new Color(100, 100, 255, 150);
    float DAMAGE_FIELD_RADIUS = 20f;
    float DAMAGE_PER_SECOND = 100f;
    float PULSE_INTERVAL = 1f;
    float timeSinceLastPulse = 0f;
	public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
		if (!(projectile instanceof MissileAPI)) return;
		
		MissileAPI missile = (MissileAPI) projectile;
		
		ShipAPI ship = null;
		if (weapon != null) ship = weapon.getShip();
		if (ship == null) return;
		
		DEMScript script = new DEMScript(missile, ship, weapon);
		engine.addPlugin(script);
		if (projectile != null) {
        	engine.addPlugin(new SRA_dragon_missileEffect(engine, projectile));
		}
	}
	public class SRA_dragon_missileEffect extends BaseEveryFrameCombatPlugin {
    	CombatEngineAPI engine;
    	DamagingProjectileAPI projectile;
    	public SRA_dragon_missileEffect(CombatEngineAPI engine, DamagingProjectileAPI projectile) {
    	    this.projectile = projectile;
    	    this.engine = engine;
    	}
    
		public void advance(float amount, List<InputEventAPI> events) {
		//if (engine.isPaused()) return;
        timeSinceLastPulse += amount;
        if (timeSinceLastPulse >= PULSE_INTERVAL) {
        	engine.spawnDamagingExplosion(SRA_dragon_missile_explosion(), projectile.getSource(),projectile.getLocation());
            timeSinceLastPulse = 0f;
    		}
    	}
	}
    public DamagingExplosionSpec SRA_dragon_missile_explosion() {
        DamagingExplosionSpec Explosion=new DamagingExplosionSpec(
                1f, // duration
                DAMAGE_FIELD_RADIUS, // radius
                DAMAGE_FIELD_RADIUS, // coreRadius
                DAMAGE_PER_SECOND, // maxDamage
                DAMAGE_PER_SECOND * 0.75f, // minDamage
                CollisionClass.PROJECTILE_NO_FF, // collisionClass
                CollisionClass.PROJECTILE_FIGHTER, // collisionClassByFighter
                0.5f, // particleSizeMin
                0.5f, // particleSizeRange
                0.5f, // particleDuration
                0, // particleCount
                new Color(15, 15, 255, 150), // particleColor
                new Color(15, 15, 255, 150)  // explosionColor
        );
        Explosion.setDamageType(DamageType.FRAGMENTATION);
        Explosion.setUseDetailedExplosion(false);
        Explosion.setShowGraphic(false);
        Explosion.setSoundSetId(null);
        return Explosion;
    }
}




