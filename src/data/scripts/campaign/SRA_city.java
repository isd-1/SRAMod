package data.scripts.campaign;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI.ShipPickMode;
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.listeners.FleetEventListener;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry;
import com.fs.starfarer.api.impl.campaign.econ.impl.MilitaryBase.PatrolFleetData;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactory.PatrolType;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.fleets.PatrolAssignmentAIV4;
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager;
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager.OptionalFleetData;
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager.RouteData;
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager.RouteFleetSpawner;
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager.RouteSegment;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.MarketCMD.RaidDangerLevel;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;
import com.fs.starfarer.api.util.WeightedRandomPicker;

import java.util.Random;

public class SRA_city extends BaseIndustry implements RouteFleetSpawner, FleetEventListener {

    // @Override
    // public boolean isHidden() {
    //     return !market.getFactionId().equals("SRA_AT_Wisdom_Pivot_Order");
    // }

    // @Override
    // public boolean isFunctional() {
    //     return super.isFunctional() && market.getFactionId().equals("SRA_AT_Wisdom_Pivot_Order");
    // }

    public static String txt(String id) {
        return Global.getSettings().getString("scripts", id);
    }
    public void apply() {
        super.apply(true);
        supply(Commodities.SUPPLIES, 8);
        supply(Commodities.FUEL, 8);
        supply(Commodities.CREW, 8);
        supply(Commodities.MARINES, 8);

        supply(Commodities.FOOD, 8);
        supply(Commodities.ORGANICS, 8);
        supply(Commodities.VOLATILES, 8);
        supply(Commodities.ORE, 8);
        supply(Commodities.RARE_ORE, 8);
        supply(Commodities.METALS, 8);
        supply(Commodities.RARE_METALS, 8);
        supply(Commodities.HEAVY_MACHINERY, 8);
        supply(Commodities.DOMESTIC_GOODS, 8);

        supply(Commodities.HAND_WEAPONS, 8);
        supply(Commodities.SHIPS, 8);

        Pair<String, Integer> deficit = getMaxDeficit(Commodities.HAND_WEAPONS);
        applyDeficitToProduction(1, deficit, Commodities.MARINES);

        modifyStabilityWithBaseMod();

        MemoryAPI memory = market.getMemoryWithoutUpdate();
        Misc.setFlagWithReason(memory, MemFlags.MARKET_PATROL, getModId(), true, -1);
        Misc.setFlagWithReason(memory, MemFlags.MARKET_MILITARY, getModId(), true, -1);

        market.getStats().getDynamic().getMod(Stats.COMBAT_FLEET_SIZE_MULT).modifyFlat(id, 1f, txt("SRA_city_1"));
        market.getStats().getDynamic().getMod(Stats.COMBAT_FLEET_SIZE_MULT).modifyMult( getModId(), 4f, txt("SRA_city_1") + "(" + getNameForModifier() + ")");
        market.getStats().getDynamic().getMod(Stats.PRODUCTION_QUALITY_MOD).modifyFlat( getModId(1), 1f, txt("SRA_city_1"));
    }

    @Override
    public void unapply() {
        super.unapply();
        MemoryAPI memory = market.getMemoryWithoutUpdate();
        Misc.setFlagWithReason(memory, MemFlags.MARKET_PATROL, getModId(), false, -1);
        Misc.setFlagWithReason(memory, MemFlags.MARKET_MILITARY, getModId(), false, -1);

        unmodifyStabilityWithBaseMod();
    }

    protected boolean hasPostDemandSection(boolean hasDemand, IndustryTooltipMode mode) {
        return mode != IndustryTooltipMode.NORMAL;
    }

    @Override
    protected void addPostDemandSection(TooltipMakerAPI tooltip, boolean hasDemand, IndustryTooltipMode mode) {
        if (mode != IndustryTooltipMode.NORMAL) {
            addStabilityPostDemandSection(tooltip, hasDemand, mode);
        }
    }

    @Override
    protected int getBaseStabilityMod() {
        return 10;
    }

    public String getNameForModifier() {
        if (getSpec().getName().contains("HQ")) {
            return getSpec().getName();
        }
        return Misc.ucFirst(getSpec().getName());
    }

    @Override
    protected Pair<String, Integer> getStabilityAffectingDeficit() {
        return getMaxDeficit(Commodities.SUPPLIES, Commodities.FUEL, Commodities.SHIPS, Commodities.HAND_WEAPONS);
    }


    public boolean isDemandLegal(CommodityOnMarketAPI com) {
        return true;
    }

    public boolean isSupplyLegal(CommodityOnMarketAPI com) {
        return true;
    }

    protected final IntervalUtil tracker = new IntervalUtil(Global.getSettings().getFloat("averagePatrolSpawnInterval") * 0.7f,
            Global.getSettings().getFloat("averagePatrolSpawnInterval") * 1.3f);

    protected float returningPatrolValue = 0f;

    @Override
    protected void buildingFinished() {
        super.buildingFinished();

        tracker.forceIntervalElapsed();
    }

    @Override
    protected void upgradeFinished(Industry previous) {
        super.upgradeFinished(previous);

        tracker.forceIntervalElapsed();
    }

    @Override
    public void advance(float amount) {
        super.advance(amount);

        if (Global.getSector().getEconomy().isSimMode()) return;

        float days = Global.getSector().getClock().convertToDays(amount);

        float spawnRate = 1f;
        float rateMult = market.getStats().getDynamic().getStat(Stats.COMBAT_FLEET_SPAWN_RATE_MULT).getModifiedValue();
        spawnRate *= rateMult;


        float extraTime = 0f;
        if (returningPatrolValue > 0) {
            // apply "returned patrols" to spawn rate, at a maximum rate of 1 interval per day
            float interval = tracker.getIntervalDuration();
            extraTime = interval * days;
            returningPatrolValue -= days;
            if (returningPatrolValue < 0) returningPatrolValue = 0;
        }
        tracker.advance(days * spawnRate + extraTime);

        //tracker.advance(days * spawnRate * 100f);

        if (tracker.intervalElapsed()) {
            String sid = getRouteSourceId();

            int light = getCount(PatrolType.FAST);
            int medium = getCount(PatrolType.COMBAT);
            int heavy = getCount(PatrolType.HEAVY);

            int maxLight = 2;
            int maxMedium = 2;
            int maxHeavy = 2;

            WeightedRandomPicker<PatrolType> picker = new WeightedRandomPicker<>();
            picker.add(PatrolType.HEAVY, maxHeavy - heavy);
            picker.add(PatrolType.COMBAT, maxMedium - medium);
            picker.add(PatrolType.FAST, maxLight - light);

            if (picker.isEmpty()) return;

            PatrolType type = picker.pick();
            PatrolFleetData custom = new PatrolFleetData(type);

            OptionalFleetData extra = new OptionalFleetData(market);
            extra.fleetType = type.getFleetType();

            RouteData route = RouteManager.getInstance().addRoute(sid, market, Misc.genRandomSeed(), extra, this, custom);
            float patrolDays = 35f + (float) Math.random() * 10f;

            route.addSegment(new RouteSegment(patrolDays, market.getPrimaryEntity()));
        }
    }

    public void reportAboutToBeDespawnedByRouteManager(RouteData route) {
    }

    public boolean shouldRepeat(RouteData route) {
        return false;
    }

    public int getCount(PatrolType... types) {
        int count = 0;
        for (RouteData data : RouteManager.getInstance().getRoutesForSource(getRouteSourceId())) {
            if (data.getCustom() instanceof PatrolFleetData) {
                PatrolFleetData custom = (PatrolFleetData) data.getCustom();
                for (PatrolType type : types) {
                    if (type == custom.type) {
                        count++;
                        break;
                    }
                }
            }
        }
        return count;
    }

    /*public int getMaxPatrols(PatrolType type) {
        if (type == PatrolType.FAST) {
            return (int) market.getStats().getDynamic().getMod(Stats.PATROL_NUM_LIGHT_MOD).computeEffective(0);
        }
        if (type == PatrolType.COMBAT) {
            return (int) market.getStats().getDynamic().getMod(Stats.PATROL_NUM_MEDIUM_MOD).computeEffective(0);
        }
        if (type == PatrolType.HEAVY) {
            return (int) market.getStats().getDynamic().getMod(Stats.PATROL_NUM_HEAVY_MOD).computeEffective(0);
        }
        return 0;
    }*/

    public boolean shouldCancelRouteAfterDelayCheck(RouteData route) {
        return false;
    }

    public void reportBattleOccurred(CampaignFleetAPI fleet, CampaignFleetAPI primaryWinner, BattleAPI battle) {

    }

    public void reportFleetDespawnedToListener(CampaignFleetAPI fleet, FleetDespawnReason reason, Object param) {
        if (!isFunctional()) return;

        if (reason == FleetDespawnReason.REACHED_DESTINATION) {
            RouteData route = RouteManager.getInstance().getRoute(getRouteSourceId(), fleet);
            if (route.getCustom() instanceof PatrolFleetData) {
                PatrolFleetData custom = (PatrolFleetData) route.getCustom();
                if (custom.spawnFP > 0) {
                    float fraction = fleet.getFleetPoints() / custom.spawnFP;
                    returningPatrolValue += fraction;
                }
            }
        }
    }

    public CampaignFleetAPI spawnFleet(RouteData route) {

        PatrolFleetData custom = (PatrolFleetData) route.getCustom();
        PatrolType type = custom.type;

        Random random = route.getRandom();

        float combat = 0f;
        float freighter = 0f;
        String fleetType = type.getFleetType();
        switch (type) {
            case FAST:
                combat = 32f;
                break;
            case COMBAT:
                combat = 64f;
                break;
            case HEAVY:
                combat = 128f;
                break;
        }

        FleetParamsV3 params = new FleetParamsV3(
                market,
                null, // loc in hyper; don't need if have market
                //"HP_heg_guard",
                "SRA_AT_Wisdom_Pivot_Order",
                route.getQualityOverride(), // quality override
                fleetType,
                combat, // combatPts
                freighter, // freighterPts
                0f, // tankerPts
                0f, // transportPts
                0f, // linerPts
                0f, // utilityPts
                0f // qualityMod - since the Lion's Guard is in a different-faction market, counter that penalty
        );
        params.timestamp = route.getTimestamp();
        params.random = random;
        //params.modeOverride = Misc.getShipPickMode(market);
        params.modeOverride = ShipPickMode.PRIORITY_THEN_ALL;
        CampaignFleetAPI fleet = FleetFactoryV3.createFleet(params);

        if (fleet == null || fleet.isEmpty()) return null;

        fleet.setFaction(market.getFactionId(), true);
        fleet.setNoFactionInName(true);

        fleet.addEventListener(this);

//		PatrolAssignmentAIV2 ai = new PatrolAssignmentAIV2(fleet, custom);
//		fleet.addScript(ai);

        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_PATROL_FLEET, true);
        fleet.getMemoryWithoutUpdate().set(MemFlags.FLEET_IGNORES_OTHER_FLEETS, true, 0.3f);

        if (type == PatrolType.FAST || type == PatrolType.COMBAT) {
            fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_CUSTOMS_INSPECTOR, true);
        }

        String postId = Ranks.POST_PATROL_COMMANDER;
        String rankId = Ranks.SPACE_COMMANDER;
        switch (type) {
            case FAST:
                rankId = Ranks.SPACE_LIEUTENANT;
                break;
            case COMBAT:
                rankId = Ranks.SPACE_COMMANDER;
                break;
            case HEAVY:
                rankId = Ranks.SPACE_CAPTAIN;
                break;
        }

        fleet.getCommander().setPostId(postId);
        fleet.getCommander().setRankId(rankId);

        market.getContainingLocation().addEntity(fleet);
        fleet.setFacing((float) Math.random() * 360f);
        // this will get overridden by the patrol assignment AI, depending on route-time elapsed etc
        fleet.setLocation(market.getPrimaryEntity().getLocation().x, market.getPrimaryEntity().getLocation().y);

        fleet.addScript(new PatrolAssignmentAIV4(fleet, route));

        //market.getContainingLocation().addEntity(fleet);
        //fleet.setLocation(market.getPrimaryEntity().getLocation().x, market.getPrimaryEntity().getLocation().y);

        if (custom.spawnFP <= 0) {
            custom.spawnFP = fleet.getFleetPoints();
        }

        return fleet;
    }

    public String getRouteSourceId() {
        return getMarket().getId() + "_" + "SRA_AT_Wisdom_Pivot_Order";
    }

    @Override
    public boolean isAvailableToBuild() {
        return false;
    }

    public boolean showWhenUnavailable() {
        return false;
    }

    @Override
    public boolean canImprove() {
        return false;
    }

    @Override
    public RaidDangerLevel adjustCommodityDangerLevel(String commodityId, RaidDangerLevel level) {
        return level.next();
    }

    @Override
    public RaidDangerLevel adjustItemDangerLevel(String itemId, String data, RaidDangerLevel level) {
        return level.next();
    }

}
