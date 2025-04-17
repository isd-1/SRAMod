package data.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.impl.combat.RiftCascadeMineExplosion;

import data.utils.SRA_Colors;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.Color;
import org.lwjgl.util.vector.Vector2f;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.util.IntervalUtil;

public class SRA_riftcascade_exEffect implements BeamEffectPlugin {

    boolean reday = true;
    private boolean runOnce;

	public static Color STANDARD_RIFT_COLOR = new Color(100,60,255,255);
	public static Color EXPLOSION_UNDERCOLOR = new Color(100, 0, 25, 100);
	public static Color NEGATIVE_SOURCE_COLOR = new Color(200,255,200,25);
	
	public static String RIFTCASCADE_MINELAYER = "SRA_riftcascade_ex_minelayer";
	
	public static int MAX_RIFTS = 5;
	public static float UNUSED_RANGE_PER_SPAWN = 200;
	public static float SPAWN_SPACING = 175;
	public static float SPAWN_INTERVAL = 0.1f;
	
	
	
	protected Vector2f arcFrom = null;
	protected Vector2f prevMineLoc = null;
	
	protected boolean doneSpawningMines = false;
	protected float spawned = 0;
	protected int numToSpawn = 0;
	protected float untilNextSpawn = 0;
	protected float spawnDir = 0;
	
	protected IntervalUtil tracker = new IntervalUtil(0.1f, 0.2f);

    public SRA_riftcascade_exEffect() {
        runOnce = false;
    }

    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
        WeaponAPI weapon = beam.getWeapon();
        CombatEntityAPI target = beam.getDamageTarget();
        ShipAPI ship = weapon.getShip();
        float width = beam.getWidth();
        float i;

        Vector2f point = beam.getRayEndPrevFrame();

        engine.addHitParticle(point, MathUtils.getPointOnCircumference(null, MathUtils.getRandomNumberInRange(100f, 200f), MathUtils.getRandomNumberInRange(0f, 360f)), 7f, 5f, MathUtils.getRandomNumberInRange(0.5f, 1f), beam.getFringeColor());


		if (reday)
		{
			if (beam.didDamageThisFrame()) {
				// engine.spawnExplosion(point, new Vector2f(), new Color(0, 165, 165, 255), 350f, 3f);
				spawnMine(ship, point);
				reday = false;
			}
		}
    }

	
	public float getSizeMult() {
		float sizeMult = 1f - spawned / (float) Math.max(1, numToSpawn - 1);
		sizeMult = 0.75f + (1f - sizeMult) * 0.5f;
		//sizeMult = 0.5f + 0.5f * sizeMult;
		return sizeMult;
	}
	
	public void spawnMine(ShipAPI source, Vector2f mineLoc) {
		CombatEngineAPI engine = Global.getCombatEngine();
		
		MissileAPI mine = (MissileAPI) engine.spawnProjectile(source, null, 
															  RIFTCASCADE_MINELAYER, 
															  mineLoc, 
															  (float) Math.random() * 360f, null);
		
		// "spawned" does not include this mine
		float sizeMult = getSizeMult();
		mine.setCustomData(RiftCascadeMineExplosion.SIZE_MULT_KEY, sizeMult);
		
		if (source != null) {
			Global.getCombatEngine().applyDamageModifiersToSpawnedProjectileWithNullWeapon(
											source, WeaponType.ENERGY, false, mine.getDamage());
		}
		
		mine.getDamage().getModifier().modifyMult("mine_sizeMult", sizeMult);
		
		
		float fadeInTime = 0.05f;
		mine.getVelocity().scale(0);
		mine.fadeOutThenIn(fadeInTime);
		
		//Global.getCombatEngine().addPlugin(createMissileJitterPlugin(mine, fadeInTime));
		
		//mine.setFlightTime((float) Math.random());
		float liveTime = 0f;
		//liveTime = 0.01f;
		mine.setFlightTime(mine.getMaxFlightTime() - liveTime);
		mine.addDamagedAlready(source);
		mine.setNoMineFFConcerns(true);
		
		prevMineLoc = mineLoc;
	}
}
