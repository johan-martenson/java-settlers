/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.appland.settlers.javaview;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import static org.appland.settlers.javaview.App.HouseType.BAKERY;
import static org.appland.settlers.javaview.App.HouseType.BARRACKS;
import static org.appland.settlers.javaview.App.HouseType.COALMINE;
import static org.appland.settlers.javaview.App.HouseType.DONKEY_FARM;
import static org.appland.settlers.javaview.App.HouseType.FARM;
import static org.appland.settlers.javaview.App.HouseType.FISHERY;
import static org.appland.settlers.javaview.App.HouseType.FORESTER;
import static org.appland.settlers.javaview.App.HouseType.FORTRESS;
import static org.appland.settlers.javaview.App.HouseType.GOLDMINE;
import static org.appland.settlers.javaview.App.HouseType.GRANITEMINE;
import static org.appland.settlers.javaview.App.HouseType.GUARD_HOUSE;
import static org.appland.settlers.javaview.App.HouseType.IRONMINE;
import static org.appland.settlers.javaview.App.HouseType.MILL;
import static org.appland.settlers.javaview.App.HouseType.MINT;
import static org.appland.settlers.javaview.App.HouseType.PIG_FARM;
import static org.appland.settlers.javaview.App.HouseType.QUARRY;
import static org.appland.settlers.javaview.App.HouseType.SAWMILL;
import static org.appland.settlers.javaview.App.HouseType.SLAUGHTER_HOUSE;
import static org.appland.settlers.javaview.App.HouseType.WATCH_TOWER;
import static org.appland.settlers.javaview.App.HouseType.WELL;
import static org.appland.settlers.javaview.App.HouseType.WOODCUTTER;
import org.appland.settlers.model.Building;
import org.appland.settlers.model.Cargo;
import org.appland.settlers.model.Courier;
import org.appland.settlers.model.Flag;
import org.appland.settlers.model.GameMap;
import org.appland.settlers.model.Material;
import org.appland.settlers.model.Player;
import org.appland.settlers.model.Point;
import org.appland.settlers.model.Road;
import org.appland.settlers.model.Storage;

/**
 *
 * @author johan
 */
public class SidePanel extends JTabbedPane {

    private final Infoview infoPanel;
    private final ControlPanel controlPanel;
    private CommandListener commandListener;

    private Point selectedPoint;
    private GameMap map;
    private Player player;

    void update() throws Exception {

        if (selectedPoint == null) {
            return;
        }

        if (player.isWithinBorder(selectedPoint)) {
            if (map.isFlagAtPoint(selectedPoint)) {
                Flag flag = map.getFlagAtPoint(selectedPoint);

                displayFlag(flag);
                infoPanel.displayInfo(flag);
            } else if (map.isBuildingAtPoint(selectedPoint)) {
                Building building = map.getBuildingAtPoint(selectedPoint);

                displayHouse(building);
                infoPanel.displayInfo(building);
            } else if (map.isRoadAtPoint(selectedPoint)) {
                Road road = map.getRoadAtPoint(selectedPoint);

                displayRoad(road);
                infoPanel.displayInfo(road);
            } else {
                emptyPointSelected();
            }
        } else {
            if (map.isBuildingAtPoint(selectedPoint)) {
                displayEnemyHouse(map.getBuildingAtPoint(selectedPoint));
            }
        }
    }

    void displayFlag(Flag flag) {
        infoPanel.displayInfo(flag);
        controlPanel.flagSelected();
    }

    void displayHouse(Building building) {
        infoPanel.displayInfo(building);
        controlPanel.buildingSelected(building);
    }

    void emptyPointSelected() {
        infoPanel.clear();

        controlPanel.emptyPointSelected();
    }

    Infoview getInfoview() {
        return infoPanel;
    }

    void displayRoad(Road road) {
        infoPanel.displayInfo(road);
        controlPanel.roadSelected();
    }

    private void displayEnemyHouse(Building building) throws Exception {
        infoPanel.displayInfo(building);
        controlPanel.enemyBuildingSelected(building);
    }

    void setMap(GameMap m) {
        map = m;
    }

    void setPlayer(Player p) {
        player = p;
    }

    private class ControlPanel extends JPanel {

        boolean turboToggle;

        JPanel controlPanel;
        JPanel constructionPanel;
        private JButton raiseFlagButton;
        private JButton startRoadButton;
        private JButton removeFlagButton;
        private JButton removeHouseButton;
        private JButton stopProductionButton;
        private JButton removeRoadButton;
        private JButton callGeologistButton;
        private JButton callScoutButton;

        private List<JButton> houseCreationButtons;

        private JButton buildWoodcutter;
        private JButton buildForester;
        private JButton buildBarracks;
        private JButton buildFishery;
        private JButton buildWell;
        private JButton buildGoldmine;
        private JButton buildIronmine;
        private JButton buildCoalmine;
        private JButton buildGranitemine;
        private JButton buildSawmill;
        private JButton buildQuarry;
        private JButton buildMill;
        private JButton buildBakery;
        private JButton buildFarm;
        private JButton buildPigFarm;
        private JButton buildMint;
        private JButton buildSlaughterHouse;
        private JButton buildDonkeyFarm;
        private JButton buildGuardHouse;
        private JButton buildWatchTower;
        private JButton buildFortress;

        private JButton attackHouseButton;
        private JButton evacuateButton;
        private JButton cancelEvacuationButton;

        public ControlPanel() {
            super();

            turboToggle = false;

            setMinimumSize(new Dimension(100, 100));
            setPreferredSize(new Dimension(100, 500));

            setLayout(new BorderLayout());

            controlPanel = createControlPanel();
            constructionPanel = createConstructionPanel();

            add(controlPanel, BorderLayout.NORTH);
            add(constructionPanel, BorderLayout.CENTER);

            setVisible(true);
        }

        void emptyPointSelected() {
            raiseFlagButton.setVisible(true);

            setBuildingCreationVisibility(true);

            attackHouseButton.setVisible(false);
            evacuateButton.setVisible(false);
            cancelEvacuationButton.setVisible(false);

            removeFlagButton.setVisible(false);
            removeHouseButton.setVisible(false);
            stopProductionButton.setVisible(false);
            startRoadButton.setVisible(false);
            callGeologistButton.setVisible(false);
            callScoutButton.setVisible(false);
            removeRoadButton.setVisible(false);
        }

        void flagSelected() {
            removeFlagButton.setVisible(true);
            startRoadButton.setVisible(true);
            callGeologistButton.setVisible(true);
            callScoutButton.setVisible(true);

            attackHouseButton.setVisible(false);
            evacuateButton.setVisible(false);
            cancelEvacuationButton.setVisible(false);

            raiseFlagButton.setVisible(false);
            removeHouseButton.setVisible(false);
            stopProductionButton.setVisible(false);
            removeRoadButton.setVisible(false);

            setBuildingCreationVisibility(false);
        }

        void buildingSelected(Building building) {
            removeHouseButton.setVisible(true);
            stopProductionButton.setVisible(true);

            if (building.isMilitaryBuilding() && building.ready()) {
                evacuateButton.setVisible(true);
                cancelEvacuationButton.setVisible(true);
            } else {
                evacuateButton.setVisible(false);
                cancelEvacuationButton.setVisible(false);
            }

            attackHouseButton.setVisible(false);

            startRoadButton.setVisible(false);
            removeFlagButton.setVisible(false);
            raiseFlagButton.setVisible(false);
            removeRoadButton.setVisible(false);
            callGeologistButton.setVisible(false);
            callScoutButton.setVisible(false);

            setBuildingCreationVisibility(false);
        }

        void enemyBuildingSelected(Building building) throws Exception {
            removeHouseButton.setVisible(false);
            stopProductionButton.setVisible(false);

            evacuateButton.setVisible(false);
            cancelEvacuationButton.setVisible(false);

            if (building.isMilitaryBuilding()                         && 
                player.getAvailableAttackersForBuilding(building) > 0 &&
                building.ready()) {
                attackHouseButton.setVisible(true);
            } else {
                attackHouseButton.setVisible(false);
            }

            startRoadButton.setVisible(false);
            removeFlagButton.setVisible(false);
            raiseFlagButton.setVisible(false);
            removeRoadButton.setVisible(false);
            callGeologistButton.setVisible(false);
            callScoutButton.setVisible(false);

            setBuildingCreationVisibility(false);
        }

        void roadSelected() {
            raiseFlagButton.setVisible(true);
            removeRoadButton.setVisible(true);

            attackHouseButton.setVisible(false);
            evacuateButton.setVisible(false);
            cancelEvacuationButton.setVisible(false);

            removeFlagButton.setVisible(false);
            startRoadButton.setVisible(false);
            removeHouseButton.setVisible(false);
            stopProductionButton.setVisible(false);
            callGeologistButton.setVisible(false);
            callScoutButton.setVisible(false);

            setBuildingCreationVisibility(false);
        }

        private JPanel createControlPanel() {
            JPanel panel = new JPanel();

            panel.setLayout(new GridLayout(1, 3));

            JButton turboButton = new JButton("Toggle turbo");
            JButton dumpRecordingButton = new JButton("Dump recording");
            JButton resetButton = new JButton("Reset the game");

            panel.add(turboButton);
            panel.add(dumpRecordingButton);
            panel.add(resetButton);

            turboButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    if (commandListener != null) {
                        turboToggle = !turboToggle;

                        commandListener.setTurboMode(turboToggle);
                    }
                }
            });

            dumpRecordingButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent ae) {
                    if (commandListener != null) {
                        commandListener.dumpRecording();
                    }
                }
            });

            resetButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent ae) {
                    if (commandListener != null) {
                        commandListener.reset();
                    }
                }
            });

            return panel;
        }

        private JPanel createConstructionPanel() {
            JPanel panel = new JPanel();
            JPanel flagAndRoadPanel = new JPanel();
            JPanel buildingPanel = new JPanel();

            /* Create flag and road panel */
            flagAndRoadPanel.setLayout(new GridLayout(3, 1));

            raiseFlagButton = new JButton("Raise flag");
            removeFlagButton = new JButton("Remove flag");
            startRoadButton = new JButton("Start new road");
            removeRoadButton = new JButton("Remove road");
            callGeologistButton = new JButton("Call geologist");
            callScoutButton = new JButton("Call scout");

            flagAndRoadPanel.add(raiseFlagButton);
            flagAndRoadPanel.add(removeFlagButton);
            flagAndRoadPanel.add(startRoadButton);
            flagAndRoadPanel.add(removeRoadButton);
            flagAndRoadPanel.add(callGeologistButton);
            flagAndRoadPanel.add(callScoutButton);

            raiseFlagButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    if (commandListener != null) {
                        try {
                            commandListener.placeFlag(selectedPoint);
                        } catch (Exception ex) {
                            Logger.getLogger(SidePanel.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            });

            startRoadButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent ae) {
                    if (commandListener != null) {
                        commandListener.startRoadCommand(selectedPoint);
                    }
                }
            });

            removeFlagButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent ae) {
                    if (commandListener != null) {
                        try {
                            commandListener.removeFlagCommand(selectedPoint);
                        } catch (Exception ex) {
                            Logger.getLogger(SidePanel.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            });

            removeRoadButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent ae) {
                    if (commandListener != null) {
                        try {
                            commandListener.removeRoadAtPoint(selectedPoint);
                        } catch (Exception ex) {
                            Logger.getLogger(SidePanel.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            });

            callGeologistButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent ae) {
                    if (commandListener != null) {
                        try {
                            commandListener.callGeologist(selectedPoint);
                        } catch (Exception ex) {
                            Logger.getLogger(SidePanel.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            });

            callScoutButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent ae) {
                    if (commandListener != null) {
                        try {
                            commandListener.callScout(selectedPoint);
                        } catch (Exception ex) {
                            Logger.getLogger(SidePanel.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            });

            flagAndRoadPanel.setVisible(true);

            /* Create panel for construction of buildings */
            buildingPanel.setLayout(new GridLayout(15, 1));

            removeHouseButton      = new JButton("Remove house");
            stopProductionButton   = new JButton("Production on/off");
            attackHouseButton      = new JButton("Attack");
            evacuateButton         = new JButton("Evacuate");
            cancelEvacuationButton = new JButton("Cancel evacuation");

            buildWoodcutter     = new JButton("Woodcutter");
            buildForester       = new JButton("Forester");
            buildBarracks       = new JButton("Barracks");
            buildFishery        = new JButton("Fishery");
            buildWell           = new JButton("Well");
            buildGoldmine       = new JButton("Gold Mine");
            buildIronmine       = new JButton("Iron Mine");
            buildCoalmine       = new JButton("Coal Mine");
            buildGranitemine    = new JButton("Granite Mine");
            buildSawmill        = new JButton("Sawmill");
            buildQuarry         = new JButton("Quarry");
            buildMill           = new JButton("Mill");
            buildBakery         = new JButton("Bakery");
            buildFarm           = new JButton("Farm");
            buildPigFarm        = new JButton("Pig Farm");
            buildMint           = new JButton("Mint");
            buildSlaughterHouse = new JButton("Slaughter House");
            buildDonkeyFarm     = new JButton("Donkey Farm");
            buildGuardHouse     = new JButton("Guard House");
            buildWatchTower     = new JButton("Watch Tower");
            buildFortress       = new JButton("Fortress");

            houseCreationButtons = new LinkedList<>();

            houseCreationButtons.add(buildWoodcutter);
            houseCreationButtons.add(buildForester);
            houseCreationButtons.add(buildBarracks);
            houseCreationButtons.add(buildFishery);
            houseCreationButtons.add(buildWell);
            houseCreationButtons.add(buildGoldmine);
            houseCreationButtons.add(buildIronmine);
            houseCreationButtons.add(buildCoalmine);
            houseCreationButtons.add(buildGranitemine);
            houseCreationButtons.add(buildSawmill);
            houseCreationButtons.add(buildQuarry);
            houseCreationButtons.add(buildMill);
            houseCreationButtons.add(buildBakery);
            houseCreationButtons.add(buildFarm);
            houseCreationButtons.add(buildPigFarm);
            houseCreationButtons.add(buildMint);
            houseCreationButtons.add(buildSlaughterHouse);
            houseCreationButtons.add(buildDonkeyFarm);
            houseCreationButtons.add(buildGuardHouse);
            houseCreationButtons.add(buildWatchTower);
            houseCreationButtons.add(buildFortress);

            ActionListener buildListener = new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent ae) {
                    if (commandListener != null) {
                        try {
                            if (ae.getSource().equals(buildBarracks)) {
                                commandListener.placeBuilding(BARRACKS, selectedPoint);
                            } else if (ae.getSource().equals(buildWoodcutter)) {
                                commandListener.placeBuilding(WOODCUTTER, selectedPoint);
                            } else if (ae.getSource().equals(buildForester)) {
                                commandListener.placeBuilding(FORESTER, selectedPoint);
                            } else if (ae.getSource().equals(buildSawmill)) {
                                commandListener.placeBuilding(SAWMILL, selectedPoint);
                            } else if (ae.getSource().equals(buildQuarry)) {
                                commandListener.placeBuilding(QUARRY, selectedPoint);
                            } else if (ae.getSource().equals(buildFarm)) {
                                commandListener.placeBuilding(FARM, selectedPoint);
                            } else if (ae.getSource().equals(buildFishery)) {
                                commandListener.placeBuilding(FISHERY, selectedPoint);
                            } else if (ae.getSource().equals(buildWell)) {
                                commandListener.placeBuilding(WELL, selectedPoint);
                            } else if (ae.getSource().equals(buildGoldmine)) {
                                commandListener.placeBuilding(GOLDMINE, selectedPoint);
                            } else if (ae.getSource().equals(buildIronmine)) {
                                commandListener.placeBuilding(IRONMINE, selectedPoint);
                            } else if (ae.getSource().equals(buildCoalmine)) {
                                commandListener.placeBuilding(COALMINE, selectedPoint);
                            } else if (ae.getSource().equals(buildGranitemine)) {
                                commandListener.placeBuilding(GRANITEMINE, selectedPoint);
                            } else if (ae.getSource().equals(buildMill)) {
                                commandListener.placeBuilding(MILL, selectedPoint);
                            } else if (ae.getSource().equals(buildBakery)) {
                                commandListener.placeBuilding(BAKERY, selectedPoint);
                            } else if (ae.getSource().equals(buildPigFarm)) {
                                commandListener.placeBuilding(PIG_FARM, selectedPoint);
                            } else if (ae.getSource().equals(buildMint)) {
                                commandListener.placeBuilding(MINT, selectedPoint);
                            } else if (ae.getSource().equals(buildSlaughterHouse)) {
                                commandListener.placeBuilding(SLAUGHTER_HOUSE, selectedPoint);
                            } else if (ae.getSource().equals(buildDonkeyFarm)) {
                                commandListener.placeBuilding(DONKEY_FARM, selectedPoint);
                            } else if (ae.getSource().equals(buildGuardHouse)) {
                                commandListener.placeBuilding(GUARD_HOUSE, selectedPoint);
                            } else if (ae.getSource().equals(buildWatchTower)) {
                                commandListener.placeBuilding(WATCH_TOWER, selectedPoint);
                            } else if (ae.getSource().equals(buildFortress)) {
                                commandListener.placeBuilding(FORTRESS, selectedPoint);
                            }
                        } catch (Exception ex) {
                            Logger.getLogger(SidePanel.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            };

            removeHouseButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent ae) {
                    if (commandListener != null) {
                        try {
                            commandListener.removeHouseCommand(selectedPoint);
                        } catch (Exception ex) {
                            Logger.getLogger(SidePanel.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            });

            stopProductionButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent ae) {
                    if (commandListener != null) {
                        try {
                            commandListener.stopProduction(selectedPoint);
                        } catch (Exception ex) {
                            Logger.getLogger(SidePanel.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            });

            attackHouseButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent ae) {
                    if (commandListener != null) {
                        try {
                            commandListener.attackHouse(selectedPoint);
                        } catch (Exception ex) {
                            Logger.getLogger(SidePanel.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            });

            evacuateButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent ae) {
                    try {
                        commandListener.evacuate(selectedPoint);
                    } catch (Exception ex) {
                        Logger.getLogger(SidePanel.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            });

            cancelEvacuationButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent ae) {
                    commandListener.cancelEvacuation(selectedPoint);
                }
            });
            
            for (JButton b : houseCreationButtons) {
                b.addActionListener(buildListener);
            }

            buildingPanel.add(new JLabel("Buildings"));

            buildingPanel.add(removeHouseButton);
            buildingPanel.add(stopProductionButton);
            buildingPanel.add(attackHouseButton);
            buildingPanel.add(evacuateButton);
            buildingPanel.add(cancelEvacuationButton);

            for (JButton b : houseCreationButtons) {
                buildingPanel.add(b);
            }

            buildingPanel.setVisible(true);

            /* Build the container panel */
            panel.setLayout(new BorderLayout());

            panel.add(flagAndRoadPanel, BorderLayout.NORTH);
            panel.add(buildingPanel, BorderLayout.CENTER);

            panel.setVisible(true);

            return panel;
        }

        private void setBuildingCreationVisibility(boolean visibility) {
            for (JButton b : houseCreationButtons) {
                b.setVisible(visibility);
            }
        }
    }

    public class Infoview extends JPanel {

        private final JLabel titleLabel;
        private final JLabel infoLabel;

        public Infoview() {
            super();

            setMinimumSize(new Dimension(200, 100));
            setPreferredSize(new Dimension(300, 500));

            titleLabel = new JLabel();
            infoLabel = new JLabel();

            titleLabel.setText("none");

            setLayout(new BorderLayout());

            add(titleLabel, BorderLayout.NORTH);
            add(infoLabel, BorderLayout.CENTER);

            setVisible(true);
        }

        void displayInfo(Building b) {
            titleLabel.setText(b.getClass().getSimpleName());

            String info = "<html>";

            if (b.underConstruction()) {
                info += "Under construction<br>";
            } else if (b.ready()) {
                info += "Ready<br>";
            } else if (b.burningDown()) {
                info += "Burning down<br>";
            } else {
                info += "Destroyed<br>";
            }

            if (!b.isProductionEnabled()) {
                info += "<br>Production is stopped<br>";
            }

            /* Print if worker is needed */
            try {
                if (b.needsWorker()) {
                    info += "Needs " + b.getWorkerType().name() + "<br>";
                }
            } catch (Exception ex) {
                Logger.getLogger(SidePanel.class.getName()).log(Level.SEVERE, null, ex);
            }

            /* Print deployed militaries if it's a military building */
            if (b.isMilitaryBuilding()) {
                info += b.getHostedMilitary() + " of " + b.getMaxHostedMilitary() + " deployed <br>";
            }

            /* Print inventory */
            if (b instanceof Storage) {
                info += "<br><b>Inventory</b><br>";

                Storage s = (Storage) b;

                for (Material m : Material.values()) {
                    if (s.getAmount(m) == 0) {
                        continue;
                    }

                    info += "" + m.name() + ": " + s.getAmount(m) + "<br>";
                }
            }

            /* Print material needed */
            for (Material m : Material.values()) {
                if (b.needsMaterial(m)) {
                    info += "<br>" + m.name() + " is needed<br>";
                }
            }

            infoLabel.setText(info);
        }

        void displayInfo(Flag f) {
            titleLabel.setText(f.getClass().getSimpleName() + " - " + f.getPosition());

            String info = "<html>";

            for (Cargo c : f.getStackedCargo()) {
                info += "" + c.getMaterial() + " to " + c.getTarget().getClass().getSimpleName();

                if (c.isDeliveryPromised()) {
                    info += " (promised)";
                }

                info += "<BR>";
            }

            info += "</html>";

            infoLabel.setText(info);
        }

        void displayInfo(Road r) {
            titleLabel.setText("Road - " + r.getStart() + " to " + r.getEnd());

            Courier courier = r.getCourier();

            String info = "<html>";

            if (courier != null) {
                info += "Assigned courier: <br>" + r.getCourier();

                if (courier.isExactlyAtPoint()) {
                    info += "Is at " + courier.getPosition() + "<br>";
                } else {
                    try {
                        info += "Is between" + courier.getLastPoint() + " and " + courier.getNextPoint() + "<br>";
                    } catch (Exception ex) {
                        Logger.getLogger(SidePanel.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }

                info += "Target is " + courier.getTarget() + "<br>";

                info += "Has arrived: " + courier.isArrived() + "<br>";

                if (courier.getCargo() == null) {
                    info += "Carrying no cargo<br>";
                } else {
                    info += "Carrying cargo of type " + courier.getCargo().getMaterial() + "<br>";
                    info += "Cargo is targeted for " + courier.getCargo().getTarget() + "<br>";
                }

                Cargo cargo = courier.getPromisedDelivery();

                if (cargo != null) {
                    info += "Has promised to pick up " + cargo.getMaterial() + "cargo at " + cargo.getPosition() + "<br>";
                } else {
                    info += "Has not promised to pick up any cargo <br>";
                }
            } else {
                info += "No assigned courier";
            }

            info += "</html>";

            infoLabel.setText(info);
        }

        private void clear() {
            titleLabel.setText("");
            infoLabel.setText("");
        }
    }

    SidePanel(CommandListener cl) {
        super();

        selectedPoint = null;

        commandListener = cl;

        infoPanel = new Infoview();
        controlPanel = new ControlPanel();

        addTab("Info", infoPanel);
        setMnemonicAt(0, KeyEvent.VK_1);

        addTab("Control", controlPanel);
        setMnemonicAt(1, KeyEvent.VK_2);

        setSelectedComponent(controlPanel);

        setVisible(true);
    }

    void setSelectedPoint(Point point) throws Exception {
        selectedPoint = point;
    }
}
