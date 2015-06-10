/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.appland.settlers.javaview;

import static org.appland.settlers.javaview.HouseType.BAKERY;
import static org.appland.settlers.javaview.HouseType.BARRACKS;
import static org.appland.settlers.javaview.HouseType.COALMINE;
import static org.appland.settlers.javaview.HouseType.DONKEY_FARM;
import static org.appland.settlers.javaview.HouseType.FARM;
import static org.appland.settlers.javaview.HouseType.FISHERY;
import static org.appland.settlers.javaview.HouseType.FORTRESS;
import static org.appland.settlers.javaview.HouseType.GOLDMINE;
import static org.appland.settlers.javaview.HouseType.GRANITEMINE;
import static org.appland.settlers.javaview.HouseType.GUARD_HOUSE;
import static org.appland.settlers.javaview.HouseType.HEADQUARTER;
import static org.appland.settlers.javaview.HouseType.IRONMINE;
import static org.appland.settlers.javaview.HouseType.MILL;
import static org.appland.settlers.javaview.HouseType.MINT;
import static org.appland.settlers.javaview.HouseType.PIG_FARM;
import static org.appland.settlers.javaview.HouseType.QUARRY;
import static org.appland.settlers.javaview.HouseType.SAWMILL;
import static org.appland.settlers.javaview.HouseType.SLAUGHTER_HOUSE;
import static org.appland.settlers.javaview.HouseType.WATCH_TOWER;
import static org.appland.settlers.javaview.HouseType.WELL;
import static org.appland.settlers.javaview.HouseType.WOODCUTTER;
import org.appland.settlers.model.Bakery;
import org.appland.settlers.model.Barracks;
import org.appland.settlers.model.Building;
import org.appland.settlers.model.Catapult;
import org.appland.settlers.model.CoalMine;
import org.appland.settlers.model.DonkeyFarm;
import org.appland.settlers.model.Farm;
import org.appland.settlers.model.Fishery;
import org.appland.settlers.model.ForesterHut;
import org.appland.settlers.model.Fortress;
import org.appland.settlers.model.GoldMine;
import org.appland.settlers.model.GraniteMine;
import org.appland.settlers.model.GuardHouse;
import org.appland.settlers.model.Headquarter;
import org.appland.settlers.model.HunterHut;
import org.appland.settlers.model.IronMine;
import org.appland.settlers.model.Mill;
import org.appland.settlers.model.Mint;
import org.appland.settlers.model.PigFarm;
import org.appland.settlers.model.Player;
import org.appland.settlers.model.Quarry;
import org.appland.settlers.model.Sawmill;
import org.appland.settlers.model.SlaughterHouse;
import org.appland.settlers.model.WatchTower;
import org.appland.settlers.model.Well;
import org.appland.settlers.model.Woodcutter;

/**
 *
 * @author johan
 */
public class BuildingFactory {

    public static Building createBuilding(Player player, HouseType type) {

        Building building = null;

        switch (type) {
        case WOODCUTTER:
            building = new Woodcutter(player);
            break;
        case HEADQUARTER:
            building = new Headquarter(player);
            break;
        case FORESTER:
            building = new ForesterHut(player);
            break;
        case SAWMILL:
            building = new Sawmill(player);
            break;
        case QUARRY:
            building = new Quarry(player);
            break;
        case FARM:
            building = new Farm(player);
            break;
        case BARRACKS:
            building = new Barracks(player);
            break;
        case WELL:
            building = new Well(player);
            break;
        case MILL:
            building = new Mill(player);
            break;
        case BAKERY:
            building = new Bakery(player);
            break;
        case FISHERY:
            building = new Fishery(player);
            break;
        case GOLDMINE:
            building = new GoldMine(player);
            break;
        case IRONMINE:
            building = new IronMine(player);
            break;
        case COALMINE:
            building = new CoalMine(player);
            break;
        case GRANITEMINE:
            building = new GraniteMine(player);
            break;
        case PIG_FARM:
            building = new PigFarm(player);
            break;
        case MINT:
            building = new Mint(player);
            break;
        case SLAUGHTER_HOUSE:
            building = new SlaughterHouse(player);
            break;
        case DONKEY_FARM:
            building = new DonkeyFarm(player);
            break;
        case GUARD_HOUSE:
            building = new GuardHouse(player);
            break;
        case WATCH_TOWER:
            building = new WatchTower(player);
            break;
        case FORTRESS:
            building = new Fortress(player);
            break;
        case CATAPULT:
            building = new Catapult(player);
            break;
        case HUNTER_HUT:
            building = new HunterHut(player);
        }

        return building;
    }
}
