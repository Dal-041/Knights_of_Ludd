package org.selkie.kol.world;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.PersonImportance;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import org.selkie.kol.helpers.KolStaticStrings;
import org.selkie.zea.helpers.ZeaStaticStrings;

public class GenerateMGA {
    public static void genCorvus() {
        StarSystemAPI arcadia = Global.getSector().getStarSystem(ZeaStaticStrings.ARCADIA);
        SectorEntityToken nomiosRapidAutofab = arcadia.addCustomEntity("mga_station_nomios_rapid_autofab",null, "mga_station_nomios", Factions.INDEPENDENT);

        nomiosRapidAutofab.setCircularOrbitPointingDown(arcadia.getEntityById("nomios"), 30, 434, 55);
        //nomiosRapidAutofab.setInteractionImage("illustrations", "galatia_academy");
       // nomiosRapidAutofab.setCustomDescriptionId("station_galatia_academy");

        MarketAPI market = Global.getFactory().createMarket("mba_market", nomiosRapidAutofab.getName(), 3);
        market.setSize(3);
        market.setHidden(true);
        market.setFactionId(Factions.INDEPENDENT);
        market.setSurveyLevel(MarketAPI.SurveyLevel.FULL);
        market.setFactionId(nomiosRapidAutofab.getFaction().getId());
        market.getMemoryWithoutUpdate().set(MemFlags.MARKET_HAS_CUSTOM_INTERACTION_OPTIONS, true);

        market.setPrimaryEntity(nomiosRapidAutofab);
        nomiosRapidAutofab.setMarket(market);

        createMBAPersonnel(market);

        /*
        nomiosRapidAutofab.getMemoryWithoutUpdate().set("$metProvost", true);
        // cockblock the Extract Researcher mission so Academy quest chain can't progress
        Global.getSector().getMemoryWithoutUpdate().set("$gaTJ_ref", false);

        Global.getSector().getMemoryWithoutUpdate().set("$nex_randomSector_galatiaAcademy", nomiosRapidAutofab);
        */
    }

    public static void createMBAPersonnel(MarketAPI market){

        PersonAPI person = Global.getFactory().createPerson();
        person.setId(KolStaticStrings.MGA_DIRECTOR);
        person.setImportance(PersonImportance.VERY_HIGH);
        person.setFaction(Factions.INDEPENDENT);
        person.setGender(FullName.Gender.MALE);
        person.setRankId(Ranks.FACTION_LEADER);
        person.setPostId(Ranks.POST_FACTION_LEADER);
        person.getName().setFirst("Amadou");
        person.getName().setLast("Sembene");
        person.setPortraitSprite(Global.getSettings().getSpriteName(ZeaStaticStrings.GfxCat.CHARACTERS, "kol_grandmaster"));

        market.getCommDirectory().addPerson(person, 0);
        market.getCommDirectory().getEntryForPerson(person).setHidden(true);
        market.addPerson(person);
        Global.getSector().getImportantPeople().addPerson(person); // so the person can be retrieved by id
    }
}
