package data.utils;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import data.utils.I18nUtil;

////////////////////////
///这是一个军官数据库///
////////////////////////

public class SRA_PersonData {
    // public static PersonAPI createGuGu() {
    //     PersonAPI person = Global.getFactory().createPerson();
    //     person.setName(new FullName("", "Liteve", FullName.Gender.FEMALE));
    //     person.setFaction("TDB");
    //     person.setPortraitSprite(Global.getSettings().getSpriteName("intel", "TDB_GuGu"));
    //     person.setPersonality(Personalities.RECKLESS);
    //     person.setRankId(Ranks.SPACE_CAPTAIN);
    //     person.setPostId(null);
    //     person.setId("TDB_GuGu");

    //     person.getStats().setSkipRefresh(true);

    //     person.getStats().setLevel(11);
    //     person.getStats().setSkillLevel(Skills.HELMSMANSHIP, 2);
    //     person.getStats().setSkillLevel(Skills.COMBAT_ENDURANCE, 2);
    //     person.getStats().setSkillLevel(Skills.FIELD_MODULATION, 2);
    //     person.getStats().setSkillLevel(Skills.GUNNERY_IMPLANTS, 2);
    //     person.getStats().setSkillLevel(Skills.SYSTEMS_EXPERTISE, 2);

    //     person.getStats().setSkillLevel(Skills.TACTICAL_DRILLS, 1);
    //     person.getStats().setSkillLevel(Skills.COORDINATED_MANEUVERS, 1);
    //     person.getStats().setSkillLevel(Skills.WOLFPACK_TACTICS, 1);
    //     person.getStats().setSkillLevel(Skills.FIGHTER_UPLINK, 1);
    //     person.getStats().setSkillLevel(Skills.CARRIER_GROUP, 1);
    //     person.getStats().setSkillLevel(Skills.SUPPORT_DOCTRINE, 1);

    //     person.getStats().setSkipRefresh(false);

    //     Global.getSector().getImportantPeople().addPerson(person);

    //     return person;
    // }

}