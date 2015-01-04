/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.appland.settlers.javaview;

import org.appland.settlers.javaview.App.HouseType;
import org.appland.settlers.model.Building;

/**
 *
 * @author johan
 */
public class Utils {

    static String BuildingNameAsHeading(Building building) {
        return building.getClass().getSimpleName();
    }
    
    static String BuildingNameAsVariable(Building building) {
        String className = building.getClass().getSimpleName();

        /* Change the initial letter to a small letter */
        String name = className.toLowerCase().charAt(0) + className.substring(1);

        /* Add space before each remaining capital letter */
        // TODO

        return name;
    }

    static String prettifyBuildingName(HouseType type) {
        String name = type.name();

        /* Change all to lower case */
        name = name.toLowerCase();

        /* Change _ to space */
        name = name.replaceAll("_", " ");

        /* Make initial letter capital again */
        name = name.toUpperCase().charAt(0) + name.substring(1);

        return name;
    }
}
