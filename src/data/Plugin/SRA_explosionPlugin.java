package data.Plugin;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.CombatEntityPluginWithParticles;
import com.fs.starfarer.api.impl.combat.NegativeExplosionVisual.NEParams;
import com.fs.starfarer.api.impl.combat.RiftCascadeMineExplosion;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicFakeBeam;

import java.awt.*;
import java.util.Iterator;
import java.util.List;

public class SRA_explosionPlugin extends CombatEntityPluginWithParticles {

    protected WeaponAPI weapon;
    protected DamagingProjectileAPI proj;
    protected IntervalUtil ExpInterval = new IntervalUtil(0.1f, 0.1f);
    protected float delay = 0f;
    boolean Weakenproj = false;

    public SRA_explosionPlugin(WeaponAPI weapon) {
        super();
        this.weapon = weapon;
        ExpInterval = new IntervalUtil(0.08f, 0.08f);
        delay = 0f;
    }

    public void attachToProjectile (DamagingProjectileAPI proj) {
        this.proj = proj;
    }

    public void advance(float amount) {
        if (Global.getCombatEngine().isPaused()) return;
        if (proj != null) {
            entity.getLocation().set(proj.getLocation());
        } else {
            entity.getLocation().set(weapon.getFirePoint(0));
        }
        super.advance(amount);
        Weakenproj = false;
        boolean keepSpawningParticles = isWeaponCharging(weapon) || (proj != null && !isProjectileExpired(proj) && !proj.isFading());


        if (proj != null && !isProjectileExpired(proj) && !proj.isFading()) {
            ShipAPI ship = weapon.getShip();
            delay -= amount;
            if (delay <= 0) {
                ExpInterval.advance(amount);
                if (ExpInterval.intervalElapsed()) {
                    Vector2f point= proj.getLocation();
                    Vector2f proj_point=proj.getLocation();
                    List<ShipAPI> ExplosionTarget = CombatUtils.getShipsWithinRange(point, 40f);
                    Iterator<ShipAPI> iter = ExplosionTarget.iterator();
                    while (iter.hasNext() && !Weakenproj) {
                        ShipAPI possibleTarget = iter.next();
                        if (!possibleTarget.isAlive() ||  possibleTarget.getOwner() == ship.getOwner()) {
                            iter.remove();
                        }
                        if (possibleTarget.isAlive() && possibleTarget.getOwner() != ship.getOwner() && possibleTarget.getShield()!=null){
                            ShieldAPI shield = possibleTarget.getShield();
                            if (shield.isWithinArc(point) && shield.isOn()){
                                Weakenproj = true;
                            }
                        }
                    }
                    if(ExplosionTarget.isEmpty()){
                        Global.getCombatEngine().spawnDamagingExplosion(SRA_T_weapon_supremeshot_explosion(proj), proj.getSource(),point);
                    } else if (!Weakenproj){
                        NEParams p = RiftCascadeMineExplosion.createStandardRiftParams("SRA_riftcascade_minelayer", 10f);
                        //p.hitGlowSizeMult = 0.5f;
                        p.thickness = 160f;
                        RiftCascadeMineExplosion.spawnStandardRift(proj, p);
                        Global.getCombatEngine().spawnDamagingExplosion(SRA_T_weapon_supremeshot_explosion2(proj), proj.getSource(),point);
                    }else {
                        NEParams p = RiftCascadeMineExplosion.createStandardRiftParams("SRA_riftcascade_minelayer", 10f);
                        //p.hitGlowSizeMult = 0.5f;
                        p.thickness = 160f;
                        RiftCascadeMineExplosion.spawnStandardRift(proj, p);
                        Global.getCombatEngine().spawnDamagingExplosion(SRA_T_weapon_supremeshot_explosion3(proj), proj.getSource(),point);
                    }
                    MagicFakeBeam.spawnFakeBeam(
                            Global.getCombatEngine(),
                            proj_point,
                            MathUtils.getDistance(proj_point,point),
                            VectorUtils.getAngle(proj_point,point),
                            160f,
                            0f,
                            0.1f,
                            0f,
                            new Color(15, 15, 225, 255),
                            new Color(255, 255, 255, 255),
                            0f,
                            DamageType.ENERGY,
                            0f,
                            proj.getSource()
                    );
                }

                if (!ship.isAlive() || ship.isHulk() || ship != Global.getCombatEngine().getPlayerShip()){
                }
            }
        }else {
            Weakenproj = false;
        }

    }
    //无光效爆炸
    public DamagingExplosionSpec SRA_T_weapon_supremeshot_explosion(DamagingProjectileAPI proj) {
        float damage = 16000;
        DamagingExplosionSpec Explosion=new DamagingExplosionSpec(
                0.05f, // duration
                320f, // radius
                160f, // coreRadius
                damage, // maxDamage
                damage * 0.75f, // minDamage
                CollisionClass.PROJECTILE_NO_FF, // collisionClass
                CollisionClass.PROJECTILE_FIGHTER, // collisionClassByFighter
                0.5f, // particleSizeMin
                0.5f, // particleSizeRange
                0.5f, // particleDuration
                100, // particleCount
                new Color(15, 15, 255, 0), // particleColor
                new Color(15, 15, 255, 0)  // explosionColor
        );
        Explosion.setDamageType(DamageType.ENERGY);
        Explosion.setUseDetailedExplosion(false);
        Explosion.setSoundSetId(null);
        return Explosion;
    }

    //正常爆炸
    public DamagingExplosionSpec SRA_T_weapon_supremeshot_explosion2(DamagingProjectileAPI proj) {
        float damage = 16000;
        DamagingExplosionSpec Explosion=new DamagingExplosionSpec(
                0.1f, // duration
                320f, // radius
                160f, // coreRadius
                damage, // maxDamage
                damage * 0.75f, // minDamage
                CollisionClass.PROJECTILE_NO_FF, // collisionClass
                CollisionClass.PROJECTILE_FIGHTER, // collisionClassByFighter
                0.5f, // particleSizeMin
                0.5f, // particleSizeRange
                1f, // particleDuration
                100, // particleCount
                new Color(105,105,205, 225), // particleColor
                new Color(105,105,205, 180)  // explosionColor
        );
        Explosion.setDamageType(DamageType.ENERGY);
        Explosion.setUseDetailedExplosion(false);
        Explosion.setSoundSetId("SRA_star_eater_weapon_hit_01");
        return Explosion;
    }

    //击中护盾时的爆炸
    public DamagingExplosionSpec SRA_T_weapon_supremeshot_explosion3(DamagingProjectileAPI proj) {
        float damage = 16000;
        DamagingExplosionSpec Explosion=new DamagingExplosionSpec(
                0.05f, // duration
                320f, // radius
                160f, // coreRadius
                damage, // maxDamage
                damage * 0.75f, // minDamage
                CollisionClass.PROJECTILE_NO_FF, // collisionClass
                CollisionClass.PROJECTILE_FIGHTER, // collisionClassByFighter
                0.5f, // particleSizeMin
                0.5f, // particleSizeRange
                0.5f, // particleDuration
                100, // particleCount
                new Color(105,105,205, 225), // particleColor
                new Color(105,105,205, 180)  // explosionColor
        );
        Explosion.setDamageType(DamageType.ENERGY);
        Explosion.setUseDetailedExplosion(false);
        Explosion.setSoundSetId(null);
        return Explosion;
    }
    @Override
    public void render(CombatEngineLayers layer, ViewportAPI viewport) {
        super.render(layer, viewport, null);
    }

    public boolean isExpired() {
        boolean keepSpawningParticles = isWeaponCharging(weapon) ||
                (proj != null && !isProjectileExpired(proj) && !proj.isFading());
        return super.isExpired() && (!keepSpawningParticles || (!weapon.getShip().isAlive() && proj == null));
    }

    public float getRenderRadius() {
        return 500f;
    }

    @Override
    protected float getGlobalAlphaMult() {
        if (proj != null && proj.isFading()) {
            return proj.getBrightness();
        }
        return super.getGlobalAlphaMult();
    }

    public static boolean isProjectileExpired(DamagingProjectileAPI proj) {
        return proj.isExpired() || proj.didDamage() || !Global.getCombatEngine().isEntityInPlay(proj);
    }

    public static boolean isWeaponCharging(WeaponAPI weapon) {
        return weapon.getChargeLevel() > 0 && weapon.getCooldownRemaining() <= 0;
    }
}