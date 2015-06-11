/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.appland.settlers.javaview;

import org.appland.settlers.computer.PlayerType;
import org.appland.settlers.model.Player;
import org.appland.settlers.model.Point;

/**
 *
 * @author johan
 */
interface CommandListener {
    void toggleTurbo();

    public void resetGame() throws Exception;

    public void dumpRecording();

    public void placeBuilding(HouseType houseType, Point selectedPoint) throws Exception;

    public void placeFlag(Point selectedPoint) throws Exception;

    public void startRoadCommand(Point selectedPoint);

    public void removeFlagCommand(Point selectedPoint) throws Exception;

    public void removeHouseCommand(Point selectedPoint) throws Exception;

    public void removeRoadAtPoint(Point selectedPoint) throws Exception;

    public void callGeologist(Point selectedPoint) throws Exception;

    public void stopProduction(Point selectedPoint) throws Exception;

    public void callScout(Point selectedPoint) throws Exception;

    public void attackHouse(Point selectedPoint);

    public void evacuate(Point selectedPoint) throws Exception;

    public void cancelEvacuation(Point selectedPoint);

    public void stopCoins(Point selectedPoint);

    public void startCoins(Point selectedPoint);

    public void setControlledPlayer(Player player);

    public void enableComputerPlayer(PlayerType type);
}
