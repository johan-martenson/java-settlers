/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.appland.settlers.javaview;

import org.appland.settlers.model.Barracks;
import org.appland.settlers.model.Building;
import org.appland.settlers.model.GameMap;
import org.appland.settlers.model.Headquarter;
import static org.appland.settlers.model.Material.COAL;
import static org.appland.settlers.model.Material.GOLD;
import static org.appland.settlers.model.Material.IRON;
import org.appland.settlers.model.Player;
import org.appland.settlers.model.Point;
import org.appland.settlers.model.Road;
import org.appland.settlers.model.Stone;
import org.appland.settlers.model.Tile;
import static org.appland.settlers.model.Tile.Vegetation.WATER;

/**
 *
 * @author johan
 */
public class ScenarioCreator {

    void placeOpponent(Player opponent, GameMap map) throws Exception {

        /* Place opponent's headquarter */
        Point point1 = new Point(45, 21);
        Headquarter headquarter1 = map.placeBuilding(new Headquarter(opponent), point1);

        /* Place barracks for opponent */
        Point point3 = new Point(29, 21);
        Building barracks0 = new Barracks(opponent);
        map.placeBuilding(barracks0, point3);

        /* Connect the barracks with the headquarter */
        Road road = map.placeAutoSelectedRoad(opponent, barracks0.getFlag(), headquarter1.getFlag());
    }

    private void placeWaterOnMap(Point p1, Point p2, Point p3, GameMap map) throws Exception {
        Tile tile = map.getTerrain().getTile(p1, p2, p3);

        tile.setVegetationType(WATER);

        ((GameMapRecordingAdapter)map).recordSetTileVegetation(p1, p2, p3, WATER);
    }

    void createInitialTerrain(GameMap map) throws Exception {
        /* The default vegetation is grass */

        /* Create a small lake */
        Point lakeCenter = new Point(10, 4);

        placeWaterOnMap(lakeCenter, lakeCenter.left(), lakeCenter.upLeft(), map);
        placeWaterOnMap(lakeCenter, lakeCenter.upLeft(), lakeCenter.upRight(), map);
        placeWaterOnMap(lakeCenter, lakeCenter.upRight(), lakeCenter.right(), map);
        placeWaterOnMap(lakeCenter, lakeCenter.right(), lakeCenter.downRight(), map);
        placeWaterOnMap(lakeCenter, lakeCenter.downRight(), lakeCenter.downLeft(), map);
        placeWaterOnMap(lakeCenter, lakeCenter.downLeft(), lakeCenter.left(), map);

        /* Create a small mountain */
        Point p = new Point(5, 13);
        Point p2 = new Point(8, 14);
        Point p3 = new Point(5, 15);
        map.placeMountainHexagonOnMap(p);
        map.placeMountainHexagonOnMap(p2);
        map.placeMountainHexagonOnMap(p3);

        /* Put gold at mountain */
        map.surroundPointWithMineral(p, GOLD);
        map.surroundPointWithMineral(p2, GOLD);
        map.surroundPointWithMineral(p3, GOLD);

        /* Create a small mountain */
        Point p4 = new Point(8, 16);
        Point p5 = new Point(11, 17);
        Point p6 = new Point(8, 18);
        map.placeMountainHexagonOnMap(p4);
        map.placeMountainHexagonOnMap(p5);
        map.placeMountainHexagonOnMap(p6);

        /* Put coal at mountain */
        map.surroundPointWithMineral(p4, COAL);
        map.surroundPointWithMineral(p5, COAL);
        map.surroundPointWithMineral(p6, COAL);

        /* Create another mountain with iron */
        Point point7 = new Point(15, 23);
        Point point8 = new Point(18, 24);
        Point point9 = new Point(15, 25);
        map.placeMountainHexagonOnMap(point7);
        map.placeMountainHexagonOnMap(point8);
        map.placeMountainHexagonOnMap(point9);

        map.surroundPointWithMineral(point7, IRON);
        map.surroundPointWithMineral(point8, IRON);
        map.surroundPointWithMineral(point9, IRON);

        /* Place stones */
        Point stonePoint = new Point(12, 12);

        Stone stone0 = map.placeStone(stonePoint);
        Stone stone1 = map.placeStone(stonePoint.downRight());

        /* Place forest */
        Point point0 = new Point(20, 4);
        Point point1 = new Point(22, 6);
        Point point2 = new Point(24, 4);
        Point point3 = new Point(21, 5);

        map.placeTree(point0);
        map.placeTree(point0.right());
        map.placeTree(point1);
        map.placeTree(point1.right());
        map.placeTree(point2);
        map.placeTree(point2.right());
        map.placeTree(point3);
        map.placeTree(point3.right());
    }

    void placeInitialPlayer(Player player, GameMap map) throws Exception {

        /* Place opponent's headquarter */
        Point point1 = new Point(8, 10);
        Headquarter headquarter1 = map.placeBuilding(new Headquarter(player), point1);
    }
}
