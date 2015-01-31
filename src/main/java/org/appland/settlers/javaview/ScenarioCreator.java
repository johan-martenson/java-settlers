/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.appland.settlers.javaview;

import java.util.LinkedList;
import java.util.List;
import static org.appland.settlers.javaview.HouseType.BARRACKS;
import static org.appland.settlers.javaview.HouseType.HEADQUARTER;
import org.appland.settlers.model.Barracks;
import org.appland.settlers.model.Building;
import org.appland.settlers.model.GameMap;
import org.appland.settlers.model.Headquarter;
import org.appland.settlers.model.Material;
import static org.appland.settlers.model.Material.COAL;
import static org.appland.settlers.model.Material.GOLD;
import org.appland.settlers.model.Player;
import org.appland.settlers.model.Point;
import org.appland.settlers.model.Road;
import static org.appland.settlers.model.Size.LARGE;
import org.appland.settlers.model.Stone;
import org.appland.settlers.model.Tile;
import static org.appland.settlers.model.Tile.Vegetation.MOUNTAIN;
import static org.appland.settlers.model.Tile.Vegetation.WATER;
import org.appland.settlers.model.Tree;

/**
 *
 * @author johan
 */
public class ScenarioCreator {

    private final ApiRecorder recorder;

    public ScenarioCreator(ApiRecorder r) {
        recorder = r;
    }

    void placeOpponent(Player opponent, GameMap map) throws Exception {

        /* Place opponent's headquarter */
        Point point1 = new Point(45, 21);
        Headquarter headquarter1 = map.placeBuilding(new Headquarter(opponent), point1);

        recorder.recordPlaceBuilding(headquarter1, HEADQUARTER, point1);

        /* Place barracks for opponent */
        Point point3 = new Point(29, 21);
        Building barracks0 = new Barracks(opponent);
        map.placeBuilding(barracks0, point3);

        recorder.recordPlaceBuilding(barracks0, BARRACKS, point3);

        /* Connect the barracks with the headquarter */
        Road road = map.placeAutoSelectedRoad(opponent, barracks0.getFlag(), headquarter1.getFlag());

        recorder.recordPlaceRoad(road);
    }

    private void placeWaterOnMap(Point p1, Point p2, Point p3, GameMap map) throws Exception {
        Tile tile = map.getTerrain().getTile(p1, p2, p3);

        tile.setVegetationType(WATER);

        recorder.recordSetTileVegetation(p1, p2, p3, WATER);
    }

    private void placeMountainHexagonOnMap(Point p, GameMap map) throws Exception {
        placeMountainOnTile(p, p.left(), p.upLeft(), map);
        placeMountainOnTile(p, p.upLeft(), p.upRight(), map);
        placeMountainOnTile(p, p.upRight(), p.right(), map);
        placeMountainOnTile(p, p.right(), p.downRight(), map);
        placeMountainOnTile(p, p.downRight(), p.downLeft(), map);
        placeMountainOnTile(p, p.downLeft(), p.left(), map);
    }

    private void placeMountainOnTile(Point p1, Point p2, Point p3, GameMap map) throws Exception {
        Tile tile = map.getTerrain().getTile(p1, p2, p3);

        tile.setVegetationType(MOUNTAIN);

        recorder.recordSetTileVegetation(p1, p2, p3, MOUNTAIN);
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
        placeMountainHexagonOnMap(p, map);
        placeMountainHexagonOnMap(p2, map);
        placeMountainHexagonOnMap(p3, map);

        /* Put gold at mountain */
        surroundPointWithMineral(p, GOLD, map);
        surroundPointWithMineral(p2, GOLD, map);
        surroundPointWithMineral(p3, GOLD, map);

        /* Create a small mountain */
        Point p4 = new Point(8, 16);
        Point p5 = new Point(11, 17);
        Point p6 = new Point(8, 18);
        placeMountainHexagonOnMap(p4, map);
        placeMountainHexagonOnMap(p5, map);
        placeMountainHexagonOnMap(p6, map);

        /* Put coal at mountain */
        surroundPointWithMineral(p4, COAL, map);
        surroundPointWithMineral(p5, COAL, map);
        surroundPointWithMineral(p6, COAL, map);

        /* Place stones */
        Point stonePoint = new Point(12, 12);

        Stone stone0 = map.placeStone(stonePoint);
        Stone stone1 = map.placeStone(stonePoint.downRight());

        recorder.recordPlaceStone(stone0, stonePoint);
        recorder.recordPlaceStone(stone1, stonePoint.downRight());

        /* Place forest */
        Point point0 = new Point(20, 4);
        Point point1 = new Point(22, 6);
        Point point2 = new Point(24, 4);
        Point point3 = new Point(21, 5);

        List<Tree> smallForest = new LinkedList<>();

        smallForest.add(map.placeTree(point0));
        smallForest.add(map.placeTree(point0.right()));
        smallForest.add(map.placeTree(point1));
        smallForest.add(map.placeTree(point1.right()));
        smallForest.add(map.placeTree(point2));
        smallForest.add(map.placeTree(point2.right()));
        smallForest.add(map.placeTree(point3));
        smallForest.add(map.placeTree(point3.right()));

        recorder.recordPlaceTrees(smallForest);
    }

    private void surroundPointWithMineral(Point p, Material material, GameMap map) throws Exception {
        for (Tile t : map.getTerrain().getSurroundingTiles(p)) {
            t.setAmountMineral(material, LARGE);
        }
    }

    void placeInitialPlayer(Player player, GameMap map) throws Exception {

        /* Place opponent's headquarter */
        Point point1 = new Point(8, 10);
        Headquarter headquarter1 = map.placeBuilding(new Headquarter(player), point1);

        recorder.recordPlaceBuilding(headquarter1, HEADQUARTER, point1);
    }
}
