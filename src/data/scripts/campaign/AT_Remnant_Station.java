// Source code is decompiled from a .class file using FernFlower decompiler.
package data.scripts.campaign;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.AICoreOfficerPlugin;
import com.fs.starfarer.api.campaign.CampaignEventListener;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketImmigrationModifier;
import com.fs.starfarer.api.campaign.econ.Industry.IndustryTooltipMode;
import com.fs.starfarer.api.campaign.listeners.FleetEventListener;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.impl.campaign.DebugFlags;
import com.fs.starfarer.api.impl.campaign.econ.impl.InstallableItemEffect;
import com.fs.starfarer.api.impl.campaign.econ.impl.ItemEffectsRepo;
import com.fs.starfarer.api.impl.campaign.econ.impl.MilitaryBase;
import com.fs.starfarer.api.impl.campaign.econ.impl.OrbitalStation;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactory;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.fleets.PatrolAssignmentAIV4;
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactory.PatrolType;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.impl.campaign.procgen.themes.RemnantOfficerGeneratorPlugin;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.MarketCMD;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import org.lwjgl.util.vector.Vector2f;

public class AT_Remnant_Station extends OrbitalStation implements RouteManager.RouteFleetSpawner, FleetEventListener {
   protected IntervalUtil tracker = new IntervalUtil(Global.getSettings().getFloat("averagePatrolSpawnInterval") * 0.7F, Global.getSettings().getFloat("averagePatrolSpawnInterval") * 1.3F);
   protected float returningPatrolValue = 0.0F;
   public static float BASE_DEFENSE_BONUS_AT_REMNANT_BATTLESTATION = 4.0F;
   public static int BASE_STABILITY_MOD_AT_REMNANT_BATTLESTATION = 4;

   public AT_Remnant_Station() {
   }

   private ArrayList<Pair<String, Integer>> getCommodityDemand() {
      ArrayList<Pair<String, Integer>> demandArray = new ArrayList();
      demandArray.add(new Pair("supplies", 4));

      return demandArray;
   }

   private String[] getCommodityStringArray() {
      ArrayList<Pair<String, Integer>> demandArray = this.getCommodityDemand();
      String[] commodityArray = new String[demandArray.size()];

      for(int i = 0; i < demandArray.size(); ++i) {
         commodityArray[i] = (String)((Pair)demandArray.get(i)).one;
      }

      return commodityArray;
   }

   public void apply() {
      MemoryAPI memory = this.market.getMemoryWithoutUpdate();
      Misc.setFlagWithReason(memory, "$patrol", this.getModId(), true, -1.0F);
      this.updateSupplyAndDemandModifiers();
      this.applyAICoreModifiers();
      this.applyImproveModifiers();
      if (this instanceof MarketImmigrationModifier) {
         this.market.addTransientImmigrationModifier((MarketImmigrationModifier)this);
      }

      if (this.special != null) {
         InstallableItemEffect effect = (InstallableItemEffect)ItemEffectsRepo.ITEM_EFFECTS.get(this.special.getId());
         if (effect != null) {
            List<String> unmet = effect.getUnmetRequirements(this);
            if (unmet != null && !unmet.isEmpty()) {
               effect.unapply(this);
            } else {
               effect.apply(this);
            }
         }
      }

      this.modifyStabilityWithBaseMod();
      this.applyIncomeAndUpkeep(4.0F);

      this.market.getStats().getDynamic().getMod("ground_defenses_mod").modifyMult(this.getModId(), this.getGroundDefenseMultiplierAfterCommodityShortage(), this.getNameForModifier());
      this.matchCommanderToAICore(this.aiCoreId);
      if (!this.isFunctional()) {
         this.supply.clear();
         this.unapply();
      } else {
         this.applyCRToStation();
      }

   }

   public void unapply() {
      super.unapply();
      this.unmodifyStabilityWithBaseMod();
      this.matchCommanderToAICore((String)null);
      this.market.getStats().getDynamic().getMod("ground_defenses_mod").unmodifyMult(this.getModId());
      MemoryAPI memory = this.market.getMemoryWithoutUpdate();
      Misc.setFlagWithReason(memory, "$patrol", this.getModId(), false, -1.0F);
   }

   public void advance(float amount) {
      super.advance(amount);
      if (!Global.getSector().getEconomy().isSimMode()) {
         if (this.isFunctional()) {
            float days = Global.getSector().getClock().convertToDays(amount);
            float spawnRate = 1.0F;
            float rateMult = this.market.getStats().getDynamic().getStat("combat_fleet_spawn_rate_mult").getModifiedValue();
            spawnRate *= rateMult;
            if (Global.getSector().isInNewGameAdvance()) {
               spawnRate *= 3.0F;
            }

            float extraTime = 0.0F;
            if (this.returningPatrolValue > 0.0F) {
               float interval = this.tracker.getIntervalDuration();
               extraTime = interval * days;
               this.returningPatrolValue -= days;
               if (this.returningPatrolValue < 0.0F) {
                  this.returningPatrolValue = 0.0F;
               }
            }

            this.tracker.advance(days * spawnRate + extraTime);
            if (DebugFlags.FAST_PATROL_SPAWN) {
               this.tracker.advance(days * spawnRate * 100.0F);
            }

            if (this.tracker.intervalElapsed()) {
               String sid = this.getRouteSourceId();
               int light = this.getCount(PatrolType.FAST);
               int medium = this.getCount(PatrolType.COMBAT);
               int heavy = this.getCount(PatrolType.HEAVY);
               int maxLight = 4;
               int maxMedium = 4;
               int maxHeavy = 2;
               WeightedRandomPicker<FleetFactory.PatrolType> picker = new WeightedRandomPicker();
               picker.add(PatrolType.HEAVY, (float)(maxHeavy - heavy));
               picker.add(PatrolType.COMBAT, (float)(maxMedium - medium));
               picker.add(PatrolType.FAST, (float)(maxLight - light));
               if (picker.isEmpty()) {
                  return;
               }

               FleetFactory.PatrolType type = (FleetFactory.PatrolType)picker.pick();
               MilitaryBase.PatrolFleetData custom = new MilitaryBase.PatrolFleetData(type);
               RouteManager.OptionalFleetData extra = new RouteManager.OptionalFleetData(this.market);
               extra.fleetType = type.getFleetType();
               RouteManager.RouteData route = RouteManager.getInstance().addRoute(sid, this.market, Misc.genRandomSeed(), extra, this, custom);
               extra.strength = (float)getPatrolCombatFP(type, route.getRandom());
               extra.strength = Misc.getAdjustedStrength(extra.strength, this.market);
               float patrolDays = 35.0F + (float)Math.random() * 10.0F;
               route.addSegment(new RouteManager.RouteSegment(patrolDays, this.market.getPrimaryEntity()));
            }

         }
      }
   }

   public void reportAboutToBeDespawnedByRouteManager(RouteManager.RouteData route) {
   }

   public boolean shouldRepeat(RouteManager.RouteData route) {
      return false;
   }

   public int getCount(FleetFactory.PatrolType... types) {
      int count = 0;
      Iterator i$ = RouteManager.getInstance().getRoutesForSource(this.getRouteSourceId()).iterator();

      while(true) {
         while(true) {
            RouteManager.RouteData data;
            do {
               if (!i$.hasNext()) {
                  return count;
               }

               data = (RouteManager.RouteData)i$.next();
            } while(!(data.getCustom() instanceof MilitaryBase.PatrolFleetData));

            MilitaryBase.PatrolFleetData custom = (MilitaryBase.PatrolFleetData)data.getCustom();
            FleetFactory.PatrolType[] arr$ = types;
            int len$ = types.length;

            for(int j$ = 0; j$ < len$; ++j$) {
               FleetFactory.PatrolType type = arr$[j$];
               if (type == custom.type) {
                  ++count;
                  break;
               }
            }
         }
      }
   }

   public static int getPatrolCombatFP(FleetFactory.PatrolType type, Random random) {
      float combat = 0.0F;
      switch (type) {
          case FAST:
               combat = (float)Math.round(3.0F + random.nextFloat() * 2.0F) * 5.0F;
              break;
          case COMBAT:
               combat = (float)Math.round(6.0F + random.nextFloat() * 3.0F) * 5.0F;
              break;
          case HEAVY:
               combat = (float)Math.round(10.0F + random.nextFloat() * 5.0F) * 5.0F;
              break;
      }
      return Math.round(combat);
   }

   public boolean shouldCancelRouteAfterDelayCheck(RouteManager.RouteData route) {
      return false;
   }

   public void reportFleetDespawnedToListener(CampaignFleetAPI fleet, CampaignEventListener.FleetDespawnReason reason, Object param) {
      if (fleet == this.stationFleet) {
         disrupt(this);
         if (this.stationFleet.getMembersWithFightersCopy().isEmpty()) {
            this.matchStationAndCommanderToCurrentIndustry();
         }

         this.stationFleet.setAbortDespawn(true);
      }

      if (this.isFunctional()) {
         if (reason == FleetDespawnReason.REACHED_DESTINATION) {
            RouteManager.RouteData route = RouteManager.getInstance().getRoute(this.getRouteSourceId(), fleet);
            if (route.getCustom() instanceof MilitaryBase.PatrolFleetData) {
               MilitaryBase.PatrolFleetData custom = (MilitaryBase.PatrolFleetData)route.getCustom();
               if (custom.spawnFP > 0) {
                  float fraction = (float)fleet.getFleetPoints() / (float)custom.spawnFP;
                  this.returningPatrolValue += fraction;
               }
            }
         }

      }
   }

   public CampaignFleetAPI spawnFleet(RouteManager.RouteData route) {
      MilitaryBase.PatrolFleetData custom = (MilitaryBase.PatrolFleetData)route.getCustom();
      FleetFactory.PatrolType type = custom.type;
      Random random = route.getRandom();
      CampaignFleetAPI fleet = createPatrol(type, this.market.getFactionId(), route, this.market, (Vector2f)null, random);
      if (fleet != null && !fleet.isEmpty()) {
         fleet.addEventListener(this);
         if (this.stationEntity == null) {
            return null;
         } else {
            this.stationEntity.getContainingLocation().addEntity(fleet);
            fleet.setFacing((float)Math.random() * 360.0F);
            fleet.setLocation(this.stationEntity.getLocation().x, this.stationEntity.getLocation().y);
            fleet.addScript(new PatrolAssignmentAIV4(fleet, route));
            fleet.getMemoryWithoutUpdate().set("$cfai_ignoreOtherFleets", true, 0.3F);
            if (custom.spawnFP <= 0) {
               custom.spawnFP = fleet.getFleetPoints();
            }

            return fleet;
         }
      } else {
         return null;
      }
   }

   public static CampaignFleetAPI createPatrol(FleetFactory.PatrolType type, String factionId, RouteManager.RouteData route, MarketAPI market, Vector2f locInHyper, Random random) {
      if (random == null) {
         random = new Random();
      }

      float combat = (float)getPatrolCombatFP(type, random);
      float tanker = 0.0F;
      float freighter = 0.0F;
      String fleetType = type.getFleetType();
      switch (type) {
          case FAST:
              break;
          case COMBAT:
               tanker = (float)Math.round(random.nextFloat() * 5.0F);
              break;
          case HEAVY:
               tanker = (float)Math.round(random.nextFloat() * 10.0F);
               freighter = (float)Math.round(random.nextFloat() * 10.0F);
              break;
      }

      FleetParamsV3 params = new FleetParamsV3(market, locInHyper, "SRA_AT_Wisdom_Pivot_Order", route == null ? null : route.getQualityOverride(), fleetType, combat, freighter, tanker, 0.0F, 0.0F, 0.0F, 0.0F);
      if (route != null) {
         params.timestamp = route.getTimestamp();
      }

      params.random = random;
      CampaignFleetAPI fleet = FleetFactoryV3.createFleet(params);
      if (fleet != null && !fleet.isEmpty()) {
         fleet.setFaction(market.getFactionId(), true);
         fleet.setNoFactionInName(true);
         if (fleet != null && !fleet.isEmpty()) {
            if (!fleet.getFaction().getCustomBoolean("patrolsHaveNoPatrolMemoryKey")) {
               fleet.getMemoryWithoutUpdate().set("$isPatrol", true);
               if (type == PatrolType.FAST || type == PatrolType.COMBAT) {
                  fleet.getMemoryWithoutUpdate().set("$isCustomsInspector", true);
               }
            } else if (fleet.getFaction().getCustomBoolean("pirateBehavior")) {
               fleet.getMemoryWithoutUpdate().set("$isPirate", true);
               if (market != null && market.isHidden()) {
                  fleet.getMemoryWithoutUpdate().set("$isRaider", true);
               }
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
            fleet.addTag("ATRemnantStationPatrol");
            return fleet;
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   public String getRouteSourceId() {
      return this.getMarket().getId() + "_" + "AT_REMNANT_STATION";
   }

   public MarketCMD.RaidDangerLevel adjustCommodityDangerLevel(String commodityId, MarketCMD.RaidDangerLevel level) {
      return level.next();
   }

   public MarketCMD.RaidDangerLevel adjustItemDangerLevel(String itemId, String data, MarketCMD.RaidDangerLevel level) {
      return level.next();
   }

   protected float getCR() {
      float q = Misc.getShipQuality(this.market);
      if (q < 0.0F) {
         q = 0.0F;
      }

      if (q > 1.0F) {
         q = 1.0F;
      }

      float d = this.getCommodityCrModifier();
      return 0.5F + 0.5F * Math.min(d, q);
   }

   private float getCommodityCrModifier() {
      if (this.demand.size() == 0) {
         return 1.0F;
      } else {
         float lowestModifier = 1.0F;

         String commodity;
         for(Iterator i$ = this.demand.keySet().iterator(); i$.hasNext(); lowestModifier = Math.min(lowestModifier, this.computeCommodityDeficitCrModifierForSingleCommodity(commodity))) {
            commodity = (String)i$.next();
         }

         return lowestModifier;
      }
   }

   private float computeCommodityDeficitCrModifierForSingleCommodity(String commodity) {
      float deficit = Math.max(0.0F, (float)(Integer)this.getMaxDeficit(new String[]{commodity}).two);
      float demand = Math.max(0.0F, (float)this.getDemand(commodity).getQuantity().getModifiedInt());
      if (demand < 1.0F) {
         demand = 1.0F;
         deficit = 0.0F;
      }

      return deficit > demand ? 0.0F : (demand - deficit) / demand;
   }

   protected Pair<String, Integer> getStabilityAffectingDeficit() {
      return this.getMaxDeficit(this.getCommodityStringArray());
   }

   protected int getBaseStabilityMod() {
      return BASE_STABILITY_MOD_AT_REMNANT_BATTLESTATION;
   }

   protected void addPostDemandSection(TooltipMakerAPI tooltip, boolean hasDemand, Industry.IndustryTooltipMode mode) {
      if (mode != IndustryTooltipMode.NORMAL || this.isFunctional()) {
         Color h = Misc.getHighlightColor();
         float opad = 10.0F;
         float cr = this.getCR();
         tooltip.addPara("防御轨道站战备值 (CR)：%s", opad, h, new String[]{Math.round(cr * 100.0F) + "%"});
         this.addStabilityPostDemandSectionATRemnantStation(tooltip, hasDemand, mode);
         this.addGroundDefensesImpactSectionATRemnantStation(tooltip, BASE_DEFENSE_BONUS_AT_REMNANT_BATTLESTATION, this.getCommodityStringArray());
      }

   }

   protected void addStabilityPostDemandSectionATRemnantStation(TooltipMakerAPI tooltip, boolean hasDemand, Industry.IndustryTooltipMode mode) {
      Color h = Misc.getHighlightColor();
      float opad = 10.0F;
      MutableStat fake = new MutableStat(0.0F);
      int stabilityMod = BASE_STABILITY_MOD_AT_REMNANT_BATTLESTATION;
      int stabilityPenalty = this.getStabilityPenalty();
      if (stabilityPenalty > stabilityMod) {
         stabilityPenalty = stabilityMod;
      }

      String str = getDeficitText((String)this.getStabilityAffectingDeficit().one);
      fake.modifyFlat("1", (float)stabilityMod, this.getNameForModifier());
      if (stabilityPenalty != 0) {
         fake.modifyFlat("2", (float)(-stabilityPenalty), str);
      }

      int total = stabilityMod - stabilityPenalty;
      String totalStr = "+" + total;
      if (total < 0) {
         totalStr = "" + total;
         h = Misc.getNegativeHighlightColor();
      }

      float pad = 3.0F;
      if (total >= 0) {
         tooltip.addPara("稳定性奖励: %s", opad, h, new String[]{totalStr});
      } else {
         tooltip.addPara("稳定性惩罚: %s", opad, h, new String[]{totalStr});
      }

      tooltip.addStatModGrid(400.0F, 35.0F, opad, pad, fake, null);
   }

   protected void addGroundDefensesImpactSectionATRemnantStation(TooltipMakerAPI tooltip, float bonus, String... commodities) {
      Color h = Misc.getHighlightColor();
      float opad = 10.0F;
      MutableStat fake = new MutableStat(1.0F);
      fake.modifyFlat("1", bonus, this.getNameForModifier());
      float mult;
      String totalStr;
      if (commodities != null) {
         mult = this.getGroundDefenseMultiplierAfterCommodityShortage();
         if (mult < 1.0F + BASE_DEFENSE_BONUS_AT_REMNANT_BATTLESTATION) {
            totalStr = (String)this.getMaxDeficit(commodities).one;
            fake.modifyFlat("2", this.getGroundDefenseTooltipCommodityShortageSubtractionAmount(), getDeficitText(totalStr));
         }
      }

      mult = Misc.getRoundedValueFloat(fake.getModifiedValue());
      totalStr = "×" + mult;
      if (mult < 1.0F) {
         h = Misc.getNegativeHighlightColor();
      }

      float pad = 3.0F;
      tooltip.addPara("地面防御战力: %s", opad, h, new String[]{totalStr});
      tooltip.addStatModGrid(400.0F, 35.0F, opad, pad, fake, null);
   }

   private float getGroundDefenseTooltipCommodityShortageSubtractionAmount() {
      return -1.0F * Math.min(BASE_DEFENSE_BONUS_AT_REMNANT_BATTLESTATION, (float)(Integer)this.getMaxDeficit(this.getCommodityStringArray()).two);
   }

   private float getGroundDefenseMultiplierAfterCommodityShortage() {
      float groundDefenseMult = 1.0F + BASE_DEFENSE_BONUS_AT_REMNANT_BATTLESTATION - (float)(Integer)this.getMaxDeficit(this.getCommodityStringArray()).two;
      return Math.max(1.0F, groundDefenseMult);
   }

   protected int getHumanCommanderLevel() {
      return Global.getSettings().getInt("tier3StationOfficerLevel");
   }

   public float getPatherInterest() {
      return 10.0F;
   }

   protected boolean isMiltiarized() {
      return true;
   }

	public boolean isAvailableToBuild() {
		// if (!Global.getSector().getPlayerFaction().knowsIndustry(getId())) {
		// 	return false;
		// }else {
      //    return super.isAvailableToBuild();
      // }
      return super.isAvailableToBuild();
	}
	
   public boolean showWhenUnavailable() {
      return false;
   }

   public String getUnavailableReason() {
      return "-ERROR-";
   }

   protected void buildingFinished() {
      super.buildingFinished();
      this.tracker.forceIntervalElapsed();
   }

   protected void matchCommanderToAICore(String aiCore) {
      if (this.stationFleet != null) {
         PersonAPI commander = null;
         AICoreOfficerPlugin plugin;
         if ("alpha_core".equals(aiCore)) {
            plugin = Misc.getAICoreOfficerPlugin("alpha_core");
            commander = plugin.createPerson("alpha_core", "remnant", (Random)null);
            if (this.stationFleet.getFlagship() != null) {
               RemnantOfficerGeneratorPlugin.integrateAndAdaptCoreForAIFleet(this.stationFleet.getFlagship());
            }
         } else if ("beta_core".equals(aiCore)) {
            plugin = Misc.getAICoreOfficerPlugin("beta_core");
            commander = plugin.createPerson("beta_core", "remnant", (Random)null);
            if (this.stationFleet.getFlagship() != null) {
               RemnantOfficerGeneratorPlugin.integrateAndAdaptCoreForAIFleet(this.stationFleet.getFlagship());
            }
         } else {
            plugin = Misc.getAICoreOfficerPlugin("gamma_core");
            commander = plugin.createPerson("gamma_core", "remnant", (Random)null);
            if (this.stationFleet.getFlagship() != null) {
               RemnantOfficerGeneratorPlugin.integrateAndAdaptCoreForAIFleet(this.stationFleet.getFlagship());
            }
         }

         if (commander != null && this.stationFleet.getFlagship() != null) {
            this.stationFleet.getFlagship().setCaptain(commander);
            this.stationFleet.getFlagship().setFlagship(false);
         }
      }

   }
}
