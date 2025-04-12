
package originem.SRAMod.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SoundPlayerAPI;
import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.BeamEffectPlugin;
import com.fs.starfarer.api.combat.CombatAsteroidAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import java.awt.Color;
import java.util.Random;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.entities.SimpleEntity;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicLensFlare;
import originem.utils.SRAMod.ALMisc;
import originem.utils.SRAMod.DamageUtils;
import originem.utils.SRAMod.MyMath;
import originem.utils.SRAMod.visual.CustomBeamTrail;
import originem.utils.SRAMod.visual.SpriteUtils;



public class SRA_UltraBeamEffect implements BeamEffectPlugin {
    private static final Color COLOR1 = new Color(20, 20, 255, 255);
    private static final Color COLOR2 = new Color(40, 40, 255, 255);
    private IntervalUtil beamDamageDurationInterval = new IntervalUtil(0.1F, 0.1F);
    private boolean beamInit = false;
    private boolean fireInit = false;
    private float originalWidth;
    private CustomBeamTrail beamTrail;
    private CustomBeamTrail beamTrail2;

    public SRA_UltraBeamEffect() {
    }

    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
        if (!this.beamInit) {
            this.beamInit = true;
            this.originalWidth = beam.getWidth();
        }

        if (this.beamTrail == null) {
            this.beamTrail = new UltraBeamTrail(CombatEngineLayers.ABOVE_PARTICLES_LOWER, beam, SpriteUtils.getZappy2TrailSprite());
            this.beamTrail.setNodeInterval(100.0F);
            this.beamTrail.setTexPerNode(0.1F);
            this.beamTrail.setTexScrollSpeedMultiplier(-0.8F);
            this.beamTrail2 = new UltraBeamTrail(CombatEngineLayers.ABOVE_PARTICLES_LOWER, beam, SpriteUtils.getZappy2TrailSprite());
            this.beamTrail2.setNodeInterval(80.0F);
            this.beamTrail2.setTexPerNode(0.05F);
            this.beamTrail2.setTexScrollSpeedMultiplier(-0.4F);
            this.beamTrail2.setOverrideColor(new Color(110, 53, 30));
        }

        ShipAPI source = beam.getSource();
        WeaponAPI weapon = beam.getWeapon();
        if (weapon != null) {
            float level = weapon.getChargeLevel();
            if (level < 0.9F) {
                level = 0.1F;
                beam.setFringeColor(COLOR1);
                beam.setCoreColor(COLOR1);
                this.beamTrail.setExtraAlphaMult(0.0F);
                this.beamTrail.setNodeRadiusMultiplier(0.0F);
                this.beamTrail2.setExtraAlphaMult(0.0F);
                this.beamTrail2.setNodeRadiusMultiplier(0.0F);
            } else {
                beam.setCoreColor(Color.WHITE);
                beam.setFringeColor(Color.WHITE);
                level = MyMath.clamp01((level - 0.9F) / 0.1F);
                this.beamTrail.setExtraAlphaMult(level * 0.6F);
                this.beamTrail.setNodeRadiusMultiplier(level);
                this.beamTrail2.setExtraAlphaMult(level * 0.4F);
                this.beamTrail2.setNodeRadiusMultiplier(level * 2.0F);
                if (!this.fireInit) {
                    level = -4.0F * level * level + 5.0F * level;
                }
            }

            beam.getDamage().getModifier().modifyMult("SRA_ultrabeam", level);
            beam.setWidth(this.originalWidth * level);
            if (!(beam.getBrightness() < 0.95F)) {
                SoundPlayerAPI soundPlayer = Global.getSoundPlayer();
                Vector2f beamTo = beam.getRayEndPrevFrame();
                Vector2f beamFrom = beam.getFrom();
                Vector2f sourceVelocity = source.getVelocity();
                float width = beam.getWidth();
                float beamFacing = VectorUtils.getAngle(beamFrom, beamTo);
                float beamLength = MathUtils.getDistance(beamFrom, beamTo);
                Random random = new Random();
                soundPlayer.playLoop("SRA_ultrabeam_loop", beam, 1.0F, 1.0F, beamFrom, sourceVelocity);
                if (!this.fireInit) {
                    this.fireInit = true;
                    soundPlayer.playSound("SRA_ultrabeam_fire", 1.0F, 1.0F, beamFrom, sourceVelocity);
                    Vector2f firePoint = weapon.getFirePoint(0);
                    RippleDistortion ripple = new RippleDistortion(firePoint, sourceVelocity);
                    ripple.setSize(200.0F);
                    ripple.setIntensity(20.0F);
                    ripple.setFrameRate(180.0F);
                    ripple.fadeInSize(0.3F);
                    ripple.fadeOutIntensity(0.3F);
                    DistortionShader.addDistortion(ripple);
                }

                int factor = Math.round(beamLength / 600.0F);

                float hitPointToLocAngle;
                Vector2f flareLoc;
                Vector2f point1;
                Vector2f vel;
                float minRepressLevel;
                float startAngle;
                float angle;
                for(int i = 0; i < factor; ++i) {
                    hitPointToLocAngle = Math.min(random.nextFloat() * beamLength + width, beamLength - width);
                    flareLoc = MathUtils.getPointOnCircumference(beamFrom, hitPointToLocAngle, beamFacing);
                    point1 = MathUtils.getPointOnCircumference((Vector2f)null, random.nextFloat() * 120.0F + 60.0F, beamFacing);
                    vel = MathUtils.getRandomPointInCircle(flareLoc, random.nextFloat() * width * 0.7F);
                    minRepressLevel = random.nextFloat() * 15.0F + 8.0F;
                    startAngle = random.nextFloat() * 0.8F + 0.2F;
                    angle = random.nextFloat() * 0.6F + 0.3F;
                    Color color = ALMisc.colorBlend(COLOR1, COLOR2, random.nextFloat());
                    engine.addSmoothParticle(vel, point1, minRepressLevel, startAngle, angle, color);
                }

                CombatEntityAPI damageTarget = beam.getDamageTarget();
                float repressLevel;
                float particleRandom;
                float duration;
                if (MyMath.rollChance(15.0F * amount)) {
                    if (damageTarget != null) {
                        hitPointToLocAngle = VectorUtils.getAngle(beamTo, damageTarget.getLocation());
                        particleRandom = MathUtils.getDistance(beamTo, damageTarget.getLocation());
                        duration = damageTarget.getCollisionRadius();
                        duration = 1.0F - particleRandom / duration;
                        duration = MyMath.clamp(0.0F, 1.0F, duration);
                        minRepressLevel = 0.2F + 0.8F * duration;
                        startAngle = random.nextFloat() * 360.0F;

                        for(angle = 0.0F; angle < 360.0F; angle += 45.0F) {
                            if (!random.nextBoolean()) {
                                float flareAngle = startAngle + angle;
                                float rotationToRepressAngle = Math.abs(MathUtils.getShortestRotation(flareAngle, hitPointToLocAngle));
                                if (rotationToRepressAngle <= 90.0F) {
                                    repressLevel = rotationToRepressAngle / 90.0F * (1.0F - minRepressLevel) + minRepressLevel;
                                } else {
                                    repressLevel = 1.0F;
                                }
                            }
                        }
                    } else {
                        vel = MathUtils.getRandomPointInCircle(beamTo, 75.0F);
                        particleRandom = random.nextFloat() * 150.0F + 75.0F;
                        duration = random.nextFloat() * 1.5F + 1.0F;
                        vel = MathUtils.getPointOnCircumference((Vector2f)null, random.nextFloat() * 90.0F, beamFacing + MathUtils.getRandomNumberInRange(-20.0F, 20.0F));
                        engine.spawnExplosion(vel, vel, COLOR1, particleRandom, duration);
                    }

                    MagicLensFlare.createSharpFlare(engine, source, beamFrom, 10.0F, 450.0F, 0.0F, COLOR2, Color.white);
                    Color flareColor = ALMisc.colorBlend(COLOR1, COLOR2, random.nextFloat());
                    flareLoc = MathUtils.getRandomPointInCircle(beamTo, beam.getWidth() * 0.2F);
                    if (random.nextBoolean()) {
                        engine.addSmoothParticle(beamTo, MyMath.ZERO, beam.getWidth() * 10.0F, 0.5F, amount * 3.0F, flareColor);
                    } else {
                        engine.addSmoothParticle(beamFrom, MyMath.ZERO, beam.getWidth() * 4.0F, 0.5F, amount * 3.0F, flareColor);
                    }

                    if (damageTarget != null) {
                        MagicLensFlare.createSharpFlare(engine, source, flareLoc, 5.0F + 10.0F * beam.getBrightness(), 500.0F + 200.0F * random.nextFloat(), 0.0F, flareColor, COLOR1);
                    } else {
                        MagicLensFlare.createSharpFlare(engine, source, flareLoc, 5.0F + 10.0F * beam.getBrightness(), 100.0F + 200.0F * random.nextFloat(), 0.0F, flareColor, COLOR1);
                    }
                }

                if (MyMath.rollChance(6.0F * amount)) {
                    hitPointToLocAngle = random.nextFloat() * 360.0F;
                    particleRandom = random.nextFloat() * 200.0F;
                    point1 = MathUtils.getPointOnCircumference(beamTo, particleRandom, hitPointToLocAngle);
                    vel = MathUtils.getPointOnCircumference(beamTo, particleRandom, hitPointToLocAngle + 60.0F);
                    Color color = ALMisc.colorBlend(COLOR1, COLOR2, random.nextFloat());
                    engine.spawnEmpArc(source, point1, new SimpleEntity(point1), new SimpleEntity(vel), DamageType.ENERGY, 0.0F, 0.0F, 1000.0F, (String)null, 5.0F + random.nextFloat() * 15.0F, color, Color.white);
                }

                this.beamDamageDurationInterval.advance(amount);
                if (this.beamDamageDurationInterval.intervalElapsed()) {
                    vel = MathUtils.getPointOnCircumference((Vector2f)null, random.nextFloat() * 360.0F + 45.0F, beamFacing + MathUtils.getRandomNumberInRange(-20.0F, 20.0F));
                    particleRandom = random.nextFloat();
                    if (particleRandom < 0.2F) {
                        engine.addSwirlyNebulaParticle(beamFrom, vel, 150.0F * level, 0.25F, 0.25F, 0.0F, 1.0F, COLOR1, true);
                    } else if (particleRandom < 0.4F) {
                        engine.spawnExplosion(beamFrom, vel, COLOR1, 120.0F * level, 0.4F);
                    } else {
                        engine.addNebulaParticle(beamFrom, vel, 150.0F * level, 0.25F, 0.25F, 0.0F, 1.0F, COLOR1);
                    }

                    if (damageTarget != null) {
                        if (damageTarget instanceof ShipAPI) {
                            ShipAPI target = (ShipAPI)damageTarget;

                            if (target.isHulk()) {
                                engine.applyDamage(target, beamTo, 2500.0F, DamageType.HIGH_EXPLOSIVE, 0.0F, false, false, source);
                            }
                        }

                        if (damageTarget instanceof CombatAsteroidAPI) {
                            engine.applyDamage(damageTarget, beamTo, 2000.0F, DamageType.HIGH_EXPLOSIVE, 0.0F, false, false, source);
                        }
                    }
                }

            }
        }
    }

    private static class UltraBeamTrail extends CustomBeamTrail {
        public UltraBeamTrail(CombatEngineLayers layer, BeamAPI beam, SpriteAPI textureSprite) {
            super(layer, beam, textureSprite);
        }

        public CustomBeamTrail.TrailNode createTrailNode() {
            return new UltraBeamTrailNode();
        }
    }

    private static class UltraBeamTrailNode extends CustomBeamTrail.TrailNode {
        public UltraBeamTrailNode() {
            this.elapsed = MathUtils.getRandomNumberInRange(-10.0F, 10.0F);
            this.color = SRA_UltraBeamEffect.COLOR2;
        }

        public void advance(float amount, int index) {
            this.radius = (float)(45.0 + Math.max(-10.0, 10.0 * FastTrig.sin((double)(this.elapsed * 4.0F))));
        }
    }
}
