package data.scripts.world.systems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.themes.SalvageSpecialAssigner;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial;
import com.fs.starfarer.api.impl.campaign.terrain.DebrisFieldTerrainPlugin.DebrisFieldParams;
import com.fs.starfarer.api.impl.campaign.terrain.DebrisFieldTerrainPlugin.DebrisFieldSource;
import com.fs.starfarer.api.impl.campaign.world.TTBlackSite;
import com.fs.starfarer.api.loading.VariantSource;
import com.fs.starfarer.api.util.Misc;
import data.utils.I18nUtil;
import data.utils.SRA_PersonData;

import org.magiclib.util.MagicCampaign;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;


public class SRA_AT_Wisdom_Pivot_Order_System_A {

    public static String txt(String id) {
        return Global.getSettings().getString("scripts", id);
    }

    public void generate(SectorAPI sector) {
        //create a star system 创建一个新的星系（名字）
        String systemName = "AllTerritory";
        StarSystemAPI system = sector.createStarSystem(systemName);
        //set its location 星系位置
        system.getLocation().set(6400f, -800f);
        //set background image 星系背景图片
        system.setBackgroundTextureFilename("graphics/backgrounds/SRA_systembg_1.png");

        //the star 恒星大小（半径）（日冕大小）
        PlanetAPI star = system.initStar(systemName, "star_blue_giant", 800f, 350f);
        //background light color 背景光颜色
        system.setLightColor(new Color(102, 142, 234));

        //make asteroid belt surround it 让小行星带环绕它
        system.addAsteroidBelt(star, 200, 5400f, 150f, 180, 360, Terrain.ASTEROID_BELT, "");
        system.addRingBand(star, "misc", "rings_ice0", 256f, 1, Color.blue, 256f, 5400, 90f, Terrain.RING, "星浮环");

        //a new planet for people 一个新的星球（给势力
        PlanetAPI planet1 = system.addPlanet("SRA_planet1", star, I18nUtil.getStarSystemsString("SRA_planet1_name"), "terran", 215, 180f, 3600f, 365f);

        //a new market for planet 设置星球市场
        MarketAPI planet1Market = addMarketplace(planet1, planet1.getName(), 8, // this number is size 设置殖民地规模
                new ArrayList<>(Arrays.asList(Conditions.POPULATION_8, // population, should be equal to size
                        Conditions.HABITABLE,
                        Conditions.MILD_CLIMATE,
                        Conditions.REGIONAL_CAPITAL)),
                new ArrayList<>(Arrays.asList(Submarkets.GENERIC_MILITARY,
                        Submarkets.SUBMARKET_BLACK,
                        Submarkets.SUBMARKET_OPEN,
                        Submarkets.SUBMARKET_STORAGE)),
                new ArrayList<>(Arrays.asList(
                        Industries.POPULATION,
                        Industries.MEGAPORT,
                        "AT_Remnant_Station",
                        Industries.HEAVYBATTERIES,
                        Industries.ORBITALWORKS,
                        Industries.WAYSTATION,
                        Industries.HIGHCOMMAND,
                        "SRA_city")));
        //make a custom description which is specified in descriptions.csv    引用星球介绍位置
        planet1.setCustomDescriptionId("SRA_planet1_description");

        //give the orbital works a gamma core   给轨道工程一个核心和一个纳米炉
        planet1Market.getIndustry(Industries.ORBITALWORKS).setAICoreId(Commodities.ALPHA_CORE);
        planet1Market.getIndustry(Industries.ORBITALWORKS).setSpecialItem(new SpecialItemData(Items.PRISTINE_NANOFORGE, null));

        //then give designed command a blue core 给最高指挥部一个核心和一个低温引擎
        planet1Market.getIndustry(Industries.HIGHCOMMAND).setAICoreId(Commodities.ALPHA_CORE);
        planet1Market.getIndustry(Industries.HIGHCOMMAND).setSpecialItem(new SpecialItemData(Items.CRYOARITHMETIC_ENGINE, null));
        
        //给两个全域建筑核心
        planet1Market.getIndustry("AT_Remnant_Station").setAICoreId(Commodities.ALPHA_CORE);
        planet1Market.getIndustry("SRA_city").setAICoreId(Commodities.ALPHA_CORE);

        //a new planet for people 一个新的星球（给势力
        PlanetAPI planet2 = system.addPlanet("SRA_planet2", star, I18nUtil.getStarSystemsString("SRA_planet2_name"), "terran-eccentric", 160, 90f, 2300f, 265f);
        //make a custom description which is specified in descriptions.csv    引用星球介绍位置
        planet2.setCustomDescriptionId("SRA_planet2_description");
        planet2.getMarket().addCondition(Conditions.INIMICAL_BIOSPHERE);
        planet2.getMarket().addCondition(Conditions.ORE_ULTRARICH);
        planet2.getMarket().addCondition(Conditions.RARE_ORE_ULTRARICH);
        planet2.getMarket().addCondition(Conditions.RUINS_WIDESPREAD);
        planet2.getMarket().addCondition(Conditions.METEOR_IMPACTS);

        system.addRingBand(planet2, "misc", "rings_ice0", 256f, 1, Color.blue, 256f, 400, 90f, Terrain.RING, "星浮环");
        //a new planet for people 一个新的星球（给势力
        PlanetAPI planet3 = system.addPlanet("SRA_planet3", star, I18nUtil.getStarSystemsString("SRA_planet3_name"), "frozen", 130, 80f, 4700f, 400f);
        //make a custom description which is specified in descriptions.csv    引用星球介绍位置
        //planet3.setCustomDescriptionId("SRA_planet3_description");

        SectorEntityToken planet4 = system.addCustomEntity("SRA_planet4", I18nUtil.getStarSystemsString("SRA_planet4_name"), "station_hightech1", "SRA_AT_Wisdom_Pivot_Order");
        planet4.setCircularOrbitWithSpin(planet2, 0, 150, 160, 2, 4);
        planet4.setCircularOrbitPointingDown(planet2, 60, 250, 120);
        //a new market for planet 设置星球市场
        MarketAPI planet4Market = addMarketplace(planet4, planet4.getName(), 2, // this number is size 设置殖民地规模
                new ArrayList<>(Collections.singletonList(Conditions.POPULATION_2// population, should be equal to size
                )),
                new ArrayList<>(Arrays.asList(Submarkets.GENERIC_MILITARY,
                        Submarkets.SUBMARKET_BLACK,
                        Submarkets.SUBMARKET_OPEN,
                        Submarkets.SUBMARKET_STORAGE)),
                new ArrayList<>(Arrays.asList(Industries.POPULATION,
                        Industries.MEGAPORT,
                        Industries.ORBITALSTATION_HIGH,
                        Industries.HEAVYBATTERIES,
                        Industries.MILITARYBASE)));
        planet4.setCustomDescriptionId("SRA_planet4_description");

        //ai核心
        planet4Market.getIndustry(Industries.MILITARYBASE).setAICoreId(Commodities.ALPHA_CORE);
        planet4Market.getIndustry(Industries.ORBITALSTATION_HIGH).setAICoreId(Commodities.ALPHA_CORE);

        // generates hyperspace destinations for in-system jump points  为星系生成指定跳跃点
        JumpPointAPI jumpPoint = Global.getFactory().createJumpPoint("SRA_jump_point", txt("SRA_AT_Wisdom_Pivot_Order_System_A_1"));
        jumpPoint.setOrbit(Global.getFactory().createCircularOrbit(planet1, 100f, 700f, 30f));
        jumpPoint.setRelatedPlanet(planet1);
        jumpPoint.setStandardWormholeToHyperspaceVisual();
        system.addEntity(jumpPoint);

        //扫描本星系所有跳跃点并为之配置数据
        system.autogenerateHyperspaceJumpPoints(true, false);

        //母星旁的空间站生成
        // SectorEntityToken SRAStation = system.addCustomEntity("SRA_Station", txt("SRA_AT_Wisdom_Pivot_Order_System_A_3"), "station_SRA_type", "SRA_AT_Wisdom_Pivot_Order");
        // SRAStation.setCircularOrbitPointingDown(system.getEntityById("SRA_planet1"), 45 + 180, 360, 30);
        // SRAStation.setCustomDescriptionId("SRA_station");
        // SRAStation.setMarket(planet1Market);

        // planet1Market.getConnectedEntities().add(SRAStation);

        //生成自家特殊舰队
        this.addFleet(planet1);

        //生成星门
        SectorEntityToken gate = system.addCustomEntity("SRA_gate", // unique id 设置星门id
                txt("SRA_AT_Wisdom_Pivot_Order_System_A_gate"), // name - if null, defaultName from custom_entities.json will be used 设置你星门的名字
                "inactive_gate", // type of object, defined in custom_entities.json 设置标签（让系统识别这是个星门）根据custom_entities.json设置
                "SRA_AT_Wisdom_Pivot_Order"); // faction
        gate.setCircularOrbit(system.getEntityById("AllTerritory"), 0, 3180, 350);

        //设置你星系的永久稳定点建筑
        SectorEntityToken A = system.addCustomEntity("SRA_A", txt("SRA_AT_Wisdom_Pivot_Order_System_A_5"), "comm_relay", "SRA_AT_Wisdom_Pivot_Order");
        A.setCircularOrbit(star, 180f, 3000f, 365f);
        SectorEntityToken B = system.addCustomEntity("SRA_B", txt("SRA_AT_Wisdom_Pivot_Order_System_A_6"), "nav_buoy", "SRA_AT_Wisdom_Pivot_Order");
        B.setCircularOrbit(star, 220f, 3000f, 365f);
        SectorEntityToken C = system.addCustomEntity("SRA_C", txt("SRA_AT_Wisdom_Pivot_Order_System_A_7"), "sensor_array", "SRA_AT_Wisdom_Pivot_Order");
        C.setCircularOrbit(star, 240f, 3000f, 365f);

        //Finally cleans up hyperspace 清理超空间（？
        MagicCampaign.hyperspaceCleanup(system);
    }

    private static MarketAPI addMarketplace(SectorEntityToken primaryEntity, String name, int size,ArrayList<String> marketConditions, ArrayList<String> submarkets, ArrayList<String> industries) {
        EconomyAPI globalEconomy = Global.getSector().getEconomy();
        String planetID = primaryEntity.getId();
        String marketID = planetID + "_market";

        MarketAPI newMarket = Global.getFactory().createMarket(marketID, name, size);
        newMarket.setFactionId("SRA_AT_Wisdom_Pivot_Order");
        newMarket.setPrimaryEntity(primaryEntity);
        newMarket.getTariff().modifyFlat("generator", (float) 0.3);

        //Adds submarkets   添加子市场
        if (null != submarkets) {
            for (String market : submarkets) {
                newMarket.addSubmarket(market);
            }
        }

        //Adds market conditions  增加了市场条件
        for (String condition : marketConditions) {
            newMarket.addCondition(condition);
        }

        //Add market industries
        for (String industry : industries) {
            newMarket.addIndustry(industry);
        }

        //Sets us to a free port, if we should
        newMarket.setFreePort(false);


        globalEconomy.addMarket(newMarket, true);
        primaryEntity.setMarket(newMarket);
        primaryEntity.setFaction("SRA_AT_Wisdom_Pivot_Order");

        //Finally, return the newly-generated market
        return newMarket;
    }

    public void addFleet(SectorEntityToken rock) {
        CampaignFleetAPI fleet = FleetFactoryV3.createEmptyFleet("SRA_AT_Wisdom_Pivot_Order", FleetTypes.TASK_FORCE, null);
        fleet.setName(txt("SRA_AT_Wisdom_Pivot_Order_System_A_8"));
        fleet.setNoFactionInName(true);
        fleet.setId("SRA_AT_Wisdom_Pivot_Order_System_A_8");
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_PATROL_FLEET, true); // so it keeps transponder on

        fleet.getFleetData().addFleetMember("SRA_SOGZ_main_attack");
        fleet.getFleetData().ensureHasFlagship();

//		fleet.addAbility(Abilities.TRANSPONDER);
        fleet.getAbility(Abilities.TRANSPONDER).activate();

        // so it never shows as "Unidentified Fleet" but isn't easier to spot due to using the actual transponder ability
        fleet.setTransponderOn(true);

        // PersonAPI person = SRA_PersonData.createGuGu();
        // fleet.setCommander(person);

        FleetMemberAPI flagship = fleet.getFlagship();
        // flagship.setCaptain(person);
        flagship.updateStats();
        flagship.getRepairTracker().setCR(flagship.getRepairTracker().getMaxCR());
        flagship.setShipName(txt("SRA_AT_Wisdom_Pivot_Order_System_A_9"));

        // to "perm" the variant so it gets saved and not recreated from the "ziggurat_Experimental" id
        flagship.setVariant(flagship.getVariant().clone(), false, false);
        flagship.getVariant().setSource(VariantSource.REFIT);

        rock.getContainingLocation().addEntity(fleet);

        fleet.addAssignment(FleetAssignment.DEFEND_LOCATION,rock,1000000f);

    }


}
