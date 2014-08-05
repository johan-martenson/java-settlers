/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.appland.settlers.javaview;

import java.util.HashMap;
import java.util.Map;
import org.appland.settlers.model.Building;
import org.appland.settlers.model.Flag;
import org.appland.settlers.model.Point;
import org.appland.settlers.model.Road;
import org.appland.settlers.model.Stone;

/**
 *
 * @author johan
 */
public class ApiRecorder {

    private Map<Point, String>    pointNames;
    private Map<Flag, String>     flagNames;
    private String                recording;
    private Map<Building, String> buildingNames;
    private Map<Road, String>     roadNames;
    private Map<Stone, String>    stoneNames;
    private int tickCount;
    
    public ApiRecorder() {
        pointNames    = new HashMap<>();
        flagNames     = new HashMap<>();
        buildingNames = new HashMap<>();
        roadNames     = new HashMap<>();
        stoneNames    = new HashMap<>();
        recording = "";
        tickCount = 0;
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

        record("Point " + name + " = new Point(" + p.x + ", " + p.y + ");\n");
        
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
        String name = "building" + buildingNames.size();
        
        buildingNames.put(b, name);
        
        return name;
    }
    
    public String registerStone(Stone s) {
        String name = "stone" + stoneNames.size();
        
        stoneNames.put(s, name);
        
        return name;
    }
    
    void clear() {
        recording = "";
        
        tickCount = 0;
    }

    void recordPlaceBuilding(Building b, String newHouse, Point p) {
        recordComment(tickCount + " ticks from start");
        
        registerPoint(p);
        String name = registerBuilding(b);
        
        record("Building " + name + " = map.placeBuilding(" + newHouse + ", ");
        
        recordPoint(p);
                
        record(");\n");
    }

    void recordPlaceFlag(Flag f, Point p) {
        recordComment(tickCount + " ticks from start");

        registerPoint(p);
        registerFlag(f);
        
        String pointName = pointNames.get(p);
        String flagName  = flagNames.get(f);
        
        record("Flag " + flagName + " = map.placeFlag(" + pointName + ");\n");
    }

    void recordPlaceRoad(Road r) {
        recordComment(tickCount + " ticks from start");
        
        String roadName = registerRoad(r);
        
        for (Point p : r.getWayPoints()) {
            registerPoint(p);
        }
        
        record("Road " + roadName + " = map.placeRoad(");

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
        recordComment(tickCount + " ticks from start");
        
        String pointName = registerPoint(stonePoint);
        
        String stoneName = registerStone(stone);
        
        record("Stone " + stoneName + " = map.placeStone(" + pointName + ");\n");
    }

    void printRecordingOnConsole() {
        System.out.println("--------------------------------------");
        System.out.println(getRecording());
        System.out.println("--------------------------------------");    
    }

    void recordTick() {
        tickCount++;
    }

    private void recordComment(String string) {
        record("\n/* " + string + " */\n");
    }
}
