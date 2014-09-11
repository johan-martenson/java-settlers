/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.appland.settlers.javaview;

import java.util.HashMap;
import java.util.Map;
import org.appland.settlers.javaview.App.HouseType;
import org.appland.settlers.model.Building;
import org.appland.settlers.model.Flag;
import org.appland.settlers.model.Point;
import org.appland.settlers.model.Road;
import org.appland.settlers.model.Stone;
import org.appland.settlers.model.Tile;

/**
 *
 * @author johan
 */
public class ApiRecorder {

    private static final String INDENT = "        ";
    
    private final Map<Point, String>    pointNames;
    private final Map<Flag, String>     flagNames;
    private final Map<Building, String> buildingNames;
    private final Map<Road, String>     roadNames;
    private final Map<Stone, String>    stoneNames;
    private String recording;
    private int    tickCount;
    private int    previousRecordedTicks;
    
    public ApiRecorder() {
        pointNames    = new HashMap<>();
        flagNames     = new HashMap<>();
        buildingNames = new HashMap<>();
        roadNames     = new HashMap<>();
        stoneNames    = new HashMap<>();
        recording = "";
        tickCount = 0;
        previousRecordedTicks = 0;
    }
    
    void record(String string) {
        recording += string;
    }

    public void recordPoint(Point p) {
        if (pointNames.containsKey(p)) {
            record(pointNames.get(p));
        } else {
            record("new Point(" + p.x +", " + p.y + ")");
        }
    }

    public String registerPoint(Point p) {
        if (pointNames.containsKey(p)) {
            return pointNames.get(p);
        }
        
        String name = "point" + pointNames.size();

        pointNames.put(p, name);

        record(INDENT + "Point " + name + " = new Point(" + p.x + ", " + p.y + ");\n");
        
        return name;
    }

    String getRecording() {
        return recording;
    }

    public String registerFlag(Flag f) {
        String name = "flag" + flagNames.size();

        flagNames.put(f, name);

        return name;
    }

    private String registerRoad(Road r) {
        String name = "road" + roadNames.size();
        
        roadNames.put(r, name);
        
        return name;
    }

    public String registerBuilding(Building b) {
        String name = b.getClass().getSimpleName();

        name = name.toLowerCase().charAt(0) + name.substring(1);
        
        for (int i = 0; i < 1000; i++) {
            if (!buildingNames.containsValue(name + i)) {
                name = name + i;

                buildingNames.put(b, name);
                
                break;
            }
        }
        
        return name;
    }
    
    public String registerStone(Stone s) {
        String name = "stone" + stoneNames.size();
        
        stoneNames.put(s, name);
        
        return name;
    }
    
    void clear() {
        pointNames.clear();
        flagNames.clear();
        buildingNames.clear();
        roadNames.clear();
        stoneNames.clear();
        recording = "";
        tickCount = 0;
        previousRecordedTicks = 0;
    }

    void recordPlaceBuilding(Building b, HouseType type, Point p) {
        recordTicks();
        
        String simpleClassName = b.getClass().getSimpleName();
        
        recordComment("Placing " + type.name().toLowerCase());
        
        registerPoint(p);
        String name = registerBuilding(b);
        
        record(INDENT + "Building " + name + " = map.placeBuilding(new " + simpleClassName + "(), ");
        
        recordPoint(p);
                
        record(");\n");
    }

    void recordPlaceFlag(Flag f, Point p) {
        recordTicks();

        recordComment("Placing flag");
        
        registerPoint(p);
        registerFlag(f);
        
        String pointName = pointNames.get(p);
        String flagName  = flagNames.get(f);
        
        record(INDENT + "Flag " + flagName + " = map.placeFlag(" + pointName + ");\n");
    }

    void recordPlaceRoad(Road r) {
        recordTicks();
        
        recordComment("Placing road between " + r.getStart() + " and " + r.getEnd());
        
        String roadName = registerRoad(r);
        
        for (Point p : r.getWayPoints()) {
            registerPoint(p);
        }
        
        record(INDENT + "Road " + roadName + " = map.placeRoad(");

        boolean firstRun = true;

        for (Point p : r.getWayPoints()) {
            if (firstRun) {
                firstRun = false;

                recordPoint(p);

                continue;
            }

            record(", ");
            recordPoint(p);
        }

        record(");\n");
    }

    void recordPlaceStone(Stone stone, Point stonePoint) {
        recordTicks();
        
        recordComment("Placing stone");
        
        String pointName = registerPoint(stonePoint);
        
        String stoneName = registerStone(stone);
        
        record(INDENT + "Stone " + stoneName + " = map.placeStone(" + pointName + ");\n");
    }

    void printRecordingOnConsole() {
        System.out.println("--------------------------------------");
        System.out.println(getRecording());
        System.out.println("--------------------------------------");    
    }

    void recordTick() {
        tickCount++;
    }

    void recordComment(String string) {
        record("\n\n" + INDENT + "/* " + string + " */\n");
    }

    void recordSetTileVegetation(Point p1, Point p2, Point p3, Tile.Vegetation vegetation) {
        recordTicks();
        
        recordComment("Place a " + vegetation.name().toLowerCase() + " tile");
        
        String pointName1 = registerPoint(p1);
        String pointName2 = registerPoint(p2);
        String pointName3 = registerPoint(p3);
        
        record(INDENT + "map.getTerrain().getTile(" + pointName1 + ", " + pointName2 + ", " + pointName3 +").setVegetationType(Vegetation." + vegetation.name() + ");\n");
        
        record(INDENT + "map.terrainIsUpdated();\n");
    }

    private void recordTicks() {
        if (tickCount == previousRecordedTicks) {
            return;
        }
        
        int delta = tickCount - previousRecordedTicks;
        
        recordComment(tickCount + " ticks from start");
        record(INDENT + "Utils.fastForward(" + delta + ", map);\n");
        
        previousRecordedTicks = tickCount;
    }

    void recordRemoveFlag(Flag flag) {
        recordTicks();
        
        recordComment("Remove flag at " + flag.getPosition());
        
        String name = flagNames.get(flag);
        
        record(INDENT + "map.removeFlag(" + name + ");\n");
    }

    void recordTearDown(Building b) {
        recordTicks();
        
        String name = buildingNames.get(b);
        
        recordComment("Tear down " + name);
        
        record(INDENT + name + ".tearDown()\n");
    }

    void recordRemoveRoad(Road r) {
        recordTicks();
        
        recordComment("Removing road between " + r.getStart() + " and " + r.getEnd());
        
        record(INDENT + "map.removeRoad(" + roadNames.get(r) + ");\n");
    }

    void recordCallGeologistFromFlag(Flag flag) {
        recordTicks();
        
        String flagName = flagNames.get(flag);
        
        recordComment("Calling geologist from " + flagName + " at " + flag.getPosition());

        record(INDENT + flagName + ".callGeologist();\n");
    }

    void recordNewGame(int widthInPoints, int heightInPoints) {
        recordComment("Creating new game map with size " + widthInPoints + "x" + heightInPoints);
        
        record(INDENT + "GameMap map = new GameMap(" + widthInPoints + ", " + heightInPoints + ");\n");
    }
}
