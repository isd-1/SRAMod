
package originem.utils.SRAMod;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CombatDamageData;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import org.lwjgl.util.vector.Vector2f;

public class DamageUtils {
    public DamageUtils() {
    }

    public static boolean isArmorHit(Vector2f hitPoint, ShipAPI target) {
        ShieldAPI shield = target.getShield();
        return shield == null || !shield.isWithinArc(hitPoint);
    }

    public static void recordHullDamage(ShipAPI source, ShipAPI target, float hullDamage) {
        FleetMemberAPI sourceFleetMember = source.getFleetMember();
        FleetMemberAPI targetFleetMember = target.getFleetMember();
        if (sourceFleetMember != null && targetFleetMember != null) {
            CombatDamageData.DealtByFleetMember dealtBy = Global.getCombatEngine().getDamageData().getDealtBy(sourceFleetMember);
            dealtBy.addHullDamage(targetFleetMember, hullDamage);
        }

    }

    public static void damageEnemiesInCircle() {
    }

    public static void destroy(CombatEntityAPI entity) {
        Global.getCombatEngine().applyDamage(entity, entity.getLocation(), entity.getMaxHitpoints() * 1000.0F, DamageType.HIGH_EXPLOSIVE, 0.0F, true, true, entity);
    }
}
