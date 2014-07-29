/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.appland.settlers.javaview;

import org.appland.settlers.model.Point;

/**
 *
 * @author johan
 */
interface CommandListener {
    void setTurboMode(boolean toggle);

    public void reset();

    public void dumpRecording();

    public void placeBuilding(App.HouseType houseType, Point selectedPoint) throws Exception;

    public void placeFlag(Point selectedPoint) throws Exception;

    public void startRoadCommand(Point selectedPoint);
}
