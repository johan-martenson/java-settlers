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

/**
 *
 * @author johan
 */
public class ScenarioCreator {

    void placeOpponent(Player opponent, GameMap map) throws Exception {

        /* Place opponent's headquarter */
        Point point1 = new Point(53, 31);
        Headquarter headquarter1 = map.placeBuilding(new Headquarter(opponent), point1);

        /* Place barracks for opponent */
        Point point3 = new Point(37, 29);
        Building barracks0 = new Barracks(opponent);
        map.placeBuilding(barracks0, point3);

        /* Connect the barracks with the headquarter */
        Road road = map.placeAutoSelectedRoad(opponent, barracks0.getFlag(), headquarter1.getFlag());
    }

    void createInitialTerrain(GameMap map) throws Exception {
        /* Set up terrain for the first player  */

        /* Create a small lake */
        Point lakeCenter0 = new Point(10, 4);

        map.surroundPointWithWater(lakeCenter0);
        map.surroundPointWithWater(lakeCenter0.right().right());

        /* Create a small mountain */
        Point p0 = new Point(5, 13);
        Point p2 = new Point(8, 14);
        Point p3 = new Point(5, 15);
        map.placeMountainHexagonOnMap(p0);
        map.placeMountainHexagonOnMap(p2);
        map.placeMountainHexagonOnMap(p3);

        /* Put gold at mountain */
        map.surroundPointWithMineral(p0, GOLD);
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
        Point stonePoint0 = new Point(12, 12);

        Stone stone0 = map.placeStone(stonePoint0);
        Stone stone1 = map.placeStone(stonePoint0.downRight());
        Stone stone2 = map.placeStone(stonePoint0.upRight());

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

        /* Set up terrain for the second player */

        /* Create a small lake */
        Point lakeCenter1 = new Point(40, 20);

        map.surroundPointWithWater(lakeCenter1);
        map.surroundPointWithWater(lakeCenter1.right().right());

        /* Create a small mountain */
        Point p10 = new Point(45, 27);
        Point p11 = new Point(48, 28);
        Point p12 = new Point(45, 29);
        map.placeMountainHexagonOnMap(p10);
        map.placeMountainHexagonOnMap(p11);
        map.placeMountainHexagonOnMap(p12);

        /* Put gold at mountain */
        map.surroundPointWithMineral(p10, GOLD);
        map.surroundPointWithMineral(p11, GOLD);
        map.surroundPointWithMineral(p12, GOLD);

        /* Create a small mountain */
        Point p13 = new Point(48, 26);
        Point p14 = new Point(51, 27);
        Point p15 = new Point(48, 28);
        map.placeMountainHexagonOnMap(p13);
        map.placeMountainHexagonOnMap(p14);
        map.placeMountainHexagonOnMap(p15);

        /* Put coal at mountain */
        map.surroundPointWithMineral(p13, COAL);
        map.surroundPointWithMineral(p14, COAL);
        map.surroundPointWithMineral(p15, COAL);

        /* Create another mountain with iron */
        Point point16 = new Point(65, 23);
        Point point17 = new Point(68, 24);
        Point point18 = new Point(65, 25);
        map.placeMountainHexagonOnMap(point16);
        map.placeMountainHexagonOnMap(point17);
        map.placeMountainHexagonOnMap(point18);

        map.surroundPointWithMineral(point16, IRON);
        map.surroundPointWithMineral(point17, IRON);
        map.surroundPointWithMineral(point18, IRON);

        /* Place stones */
        Point stonePoint1 = new Point(42, 8);

        Stone stone3 = map.placeStone(stonePoint1);
        Stone stone4 = map.placeStone(stonePoint1.downRight());
        Stone stone5 = map.placeStone(stonePoint1.upRight());

        /* Place forest */
        Point point19 = new Point(50, 4);
        Point point20 = new Point(52, 6);
        Point point21 = new Point(54, 4);
        Point point22 = new Point(51, 5);

        map.placeTree(point19);
        map.placeTree(point19.right());
        map.placeTree(point20);
        map.placeTree(point20.right());
        map.placeTree(point21);
        map.placeTree(point21.right());
        map.placeTree(point22);
        map.placeTree(point22.right());
    
    }

    void placeInitialPlayer(Player player, GameMap map) throws Exception {

        /* Place opponent's headquarter */
        Point point1 = new Point(8, 10);
        Headquarter headquarter1 = map.placeBuilding(new Headquarter(player), point1);
    }
}
