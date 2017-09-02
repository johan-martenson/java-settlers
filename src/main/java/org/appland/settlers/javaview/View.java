/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.appland.settlers.javaview;

import org.appland.settlers.model.GameMap;

/**
 *
 * @author johan
 */
public interface View {

    public void onSaveTroubleshootingInformation();

    public void setMap(GameMap map);

    public void onGameStarted();
}
