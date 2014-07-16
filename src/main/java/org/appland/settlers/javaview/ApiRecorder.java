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

/**
 *
 * @author johan
 */
public class ApiRecorder {

    private Map<Point, String>    pointNames;
    private Map<Flag, String>     flagNames;
    private String                recording;
    private Map<Building, String> buildingNames;
    
    public ApiRecorder() {
        pointNames    = new HashMap<>();
        flagNames     = new HashMap<>();
        buildingNames = new HashMap<>();
        recording = "";
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

    public void registerPoint(Point p) {
        String name = "point" + pointNames.size();

        pointNames.put(p, name);

        record("Point " + name + " = new Point(" + p.x + ", " + p.y + ");\n");
    }

    String getRecording() {
        return recording;
    }

    public String registerFlag(Flag f) {
        String name = "flag" + flagNames.size();

        flagNames.put(f, name);

        return name;
    }

    public String registerBuilding(Building b) {
        String name = "building" + buildingNames.size();
        
        buildingNames.put(b, name);
        
        return name;
    }
    
    void clear() {
        recording = "";
    }

    void recordPlaceBuilding(Building b, String newHouse, Point p) {
        registerPoint(p);
        String name = registerBuilding(b);
        
        record("Building " + name + " = map.placeBuilding(" + newHouse + ", ");
        
        recordPoint(p);
                
        record(");\n");
    }

    void recordPlaceFlag(Flag f, Point p) {
        registerPoint(p);
        registerFlag(f);
        
        String pointName = pointNames.get(p);
        String flagName  = flagNames.get(f);
        
        record("Flag " + flagName + " = map.placeFlag(" + pointName + ");\n\n");
    }
}
