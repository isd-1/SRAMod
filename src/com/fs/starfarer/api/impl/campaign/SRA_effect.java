package com.fs.starfarer.api.impl.campaign;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.AICoreAdminPluginImpl;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import com.fs.starfarer.api.impl.campaign.missions.RecoverAPlanetkiller;
import com.fs.starfarer.api.impl.campaign.rulecmd.Nex_IsFactionRuler;
import com.fs.starfarer.api.impl.campaign.shared.PlayerTradeDataForSubmarket;
import com.fs.starfarer.api.impl.campaign.shared.SharedData;
import com.fs.starfarer.api.impl.campaign.submarkets.StoragePlugin;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Misc.Token;

public class SRA_effect {
    // 通用殖民地转移方法
    public static void SRA_transferMarketToPlayer(MarketAPI market) {
        if (market == null) return;
        
        Set<SectorEntityToken> linkedEntities = market.getConnectedEntities();
        if (market.getPlanetEntity() != null) {
            PlanetAPI planet = market.getPlanetEntity();
            planet.setFaction(Global.getSector().getPlayerFaction().getId());
        }
        
        for (SectorEntityToken entity : linkedEntities)
        {
            entity.setFaction(Global.getSector().getPlayerFaction().getId());
            CampaignFleetAPI statFleet = Misc.getStationBaseFleet(entity);
            if (statFleet != null) statFleet.setFaction(Global.getSector().getPlayerFaction().getId(), true);
            statFleet = Misc.getStationFleet(entity);
            if (statFleet != null) statFleet.setFaction(Global.getSector().getPlayerFaction().getId(), true);
        }
        
        // Use comm board people instead of market people, 
        // because some appear on the former but not the latter 
        // (specifically when a new market admin is assigned, old one disappears from the market)
        // Also, this way it won't mess with player-assigned admins
        for (CommDirectoryEntryAPI dir : market.getCommDirectory().getEntriesCopy())
        {
            if (dir.getType() != CommDirectoryEntryAPI.EntryType.PERSON) continue;
            PersonAPI person = (PersonAPI)dir.getEntryData();
            person.setFaction(Global.getSector().getPlayerFaction().getId());
        }
        market.setFactionId(Global.getSector().getPlayerFaction().getId());
        market.setPlayerOwned(Global.getSector().getPlayerFaction().getId().equals(Factions.PLAYER));
        
        // 设置实体所属派系
        SectorEntityToken primaryEntity = market.getPrimaryEntity();
        if (primaryEntity != null) {
            primaryEntity.setFaction(Global.getSector().getPlayerFaction().getId());
        }
        // player: free storage unlock
        if (Nex_IsFactionRuler.isRuler(Global.getSector().getPlayerFaction().getId()))
        {
            SubmarketAPI storage = market.getSubmarket(Submarkets.SUBMARKET_STORAGE);
            if (storage != null)
            {
                StoragePlugin plugin = (StoragePlugin)market.getSubmarket(Submarkets.SUBMARKET_STORAGE).getPlugin();
                if (plugin != null)
                    plugin.setPlayerPaidToUnlock(true);
            }
        }
        
        
        market.setPlayerOwned(true);
        // set submarket factions
        List<SubmarketAPI> submarkets = market.getSubmarketsCopy();
        for (SubmarketAPI submarket : submarkets)
        {
            //if (submarket.getFaction() != oldOwner) continue;
            //log.info(String.format("Submarket %s has spec faction %s", submarket.getNameOneLine(), submarket.getSpec().getFactionId()));
            String submarketId = submarket.getSpecId();
            // reset smuggling suspicion
            if (submarketId.equals(Submarkets.SUBMARKET_BLACK)) {  
              PlayerTradeDataForSubmarket tradeData = SharedData.getData().getPlayerActivityTracker().getPlayerTradeData(submarket);  
              tradeData.setTotalPlayerTradeValue(0);
              continue;
            }
            
            submarket.setFaction(Global.getSector().getPlayerFaction());
        }
        
        market.reapplyConditions();
        market.reapplyIndustries();
        
        market.setAdmin(Global.getSector().getPlayerPerson());//设置为市场管理员
    }
}