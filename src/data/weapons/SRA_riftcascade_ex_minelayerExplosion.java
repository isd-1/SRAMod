package data.weapons;

import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.ProximityExplosionEffect;
import com.fs.starfarer.api.impl.combat.NegativeExplosionVisual.NEParams;
import com.fs.starfarer.api.impl.combat.RiftCascadeMineExplosion;

public class SRA_riftcascade_ex_minelayerExplosion implements ProximityExplosionEffect {
	
	public void onExplosion(DamagingProjectileAPI explosion, DamagingProjectileAPI originalProjectile) {
		NEParams p = RiftCascadeMineExplosion.createStandardRiftParams("SRA_riftcascade_ex_minelayer", 10f);
		//p.hitGlowSizeMult = 0.5f;
		p.thickness = 320f;
		RiftCascadeMineExplosion.spawnStandardRift(explosion, p);
	}
}
