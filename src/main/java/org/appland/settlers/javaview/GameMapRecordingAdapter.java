/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.appland.settlers.javaview;

import java.util.Collection;
import java.util.List;
import org.appland.settlers.model.Building;
import org.appland.settlers.model.Flag;
import org.appland.settlers.model.GameMap;
import org.appland.settlers.model.Player;
import org.appland.settlers.model.Point;
import org.appland.settlers.model.Road;
import org.appland.settlers.model.Stone;
import org.appland.settlers.model.Tile;
import org.appland.settlers.model.Tree;

/**
 *
 * @author johan
 */
public class GameMapRecordingAdapter extends GameMap {
    private final ApiRecorder recorder;

    public GameMapRecordingAdapter(List<Player> players, int width, int height) throws Exception {
        super(players, width, height);

        recorder = new ApiRecorder();

        recorder.recordNewGame(players, width, height);
    }

    @Override
    public boolean isFlagAtPoint(Point p) {

        return super.isFlagAtPoint(p);
    }

    @Override
    public boolean isRoadAtPoint(Point p) {
        return super.isRoadAtPoint(p);
    }

    @Override
    public Flag getFlagAtPoint(Point p) throws Exception {
        return super.getFlagAtPoint(p);
    }

    @Override
    public boolean isBuildingAtPoint(Point p) {
        return super.isBuildingAtPoint(p);
    }

    @Override
    public Building getBuildingAtPoint(Point p) {
        return super.getBuildingAtPoint(p);
    }

    @Override
    public void removeFlag(Flag f) throws Exception {

        super.removeFlag(f);

        recorder.recordRemoveFlag(f);
    }

    @Override
    public void removeRoad(Road r) throws Exception {

        super.removeRoad(r);

        recorder.recordRemoveRoad(r);
    }

    @Override
    public List<Player> getPlayers() {
        return super.getPlayers();
    }

    @Override
    public Road placeRoad(Player player, List<Point> points) throws Exception {

        Road r = super.placeRoad(player, points);

        recorder.recordPlaceRoad(r);

        return r;
    }

    @Override
    public List<Point> findAutoSelectedRoad(Player controlledPlayer, Point last, Point point, Collection<Point> roadPoints) {
        return super.findAutoSelectedRoad(controlledPlayer, last, point, roadPoints);
    }

    @Override
    public void stepTime() throws Exception {
        super.stepTime();

        recorder.recordTick();
    }

    @Override
    public <T extends Building> T placeBuilding(T b, Point p) throws Exception {

        T building = super.placeBuilding(b, p);

        recorder.recordPlaceBuilding(b, p);

        return building;
    }

    @Override
    public Flag placeFlag(Player player, Point p) throws Exception {
        Flag f = super.placeFlag(player, p);

        recorder.recordPlaceFlag(f, p);

        return f;
    }

    @Override
    public List<Flag> getFlags() {
        return super.getFlags();
    }

    @Override
    public List<Road> getRoads() {
        return super.getRoads();
    }

    void printRecordingOnConsole() {
        recorder.printRecordingOnConsole();
    }

    void recordMarker() {
        recorder.record("\n\n/* --- MARKER --- */\n\n");
    }

    void recordAttack(Player player, Building buildingToAttack) {
        recorder.recordAttack(player, buildingToAttack);
    }

    void clear() {
        recorder.clear();
    }

    void recordTearDown(Building b) {
        recorder.recordTearDown(b);
    }

    void recordCallGeologistFromFlag(Flag flag) {
        recorder.recordCallGeologistFromFlag(flag);
    }

    void recordCallScoutFromFlag(Flag flag) {
        recorder.recordCallScoutFromFlag(flag);
    }

    @Override
    public Tree placeTree(Point p) throws Exception {
        Tree t = super.placeTree(p);

        recorder.recordPlaceTree(t);

        return t;
    }

    @Override
    public Stone placeStone(Point p) {
        Stone s = super.placeStone(p);

        recorder.recordPlaceStone(s, p);

        return s;
    }

    void recordSetTileVegetation(Point p1, Point p2, Point p3, Tile.Vegetation vegetation) {
        recorder.recordSetTileVegetation(p1, p2, p3, vegetation);
    }
}
