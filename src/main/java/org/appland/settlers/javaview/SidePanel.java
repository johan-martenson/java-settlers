/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.appland.settlers.javaview;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import static org.appland.settlers.javaview.App.HouseType.BARRACKS;
import static org.appland.settlers.javaview.App.HouseType.FARM;
import static org.appland.settlers.javaview.App.HouseType.FORESTER;
import static org.appland.settlers.javaview.App.HouseType.QUARRY;
import static org.appland.settlers.javaview.App.HouseType.SAWMILL;
import static org.appland.settlers.javaview.App.HouseType.WOODCUTTER;
import org.appland.settlers.model.Building;
import org.appland.settlers.model.Cargo;
import org.appland.settlers.model.Courier;
import org.appland.settlers.model.Flag;
import org.appland.settlers.model.Material;
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

    private Flag flag;
    private Road road;
    private Building house;
    
    private Point selectedPoint;
    
    void update() {
        if (flag != null) {
            infoPanel.displayInfo(flag);
        } else if (house != null) {
            infoPanel.displayInfo(house);
        } else if (road != null) {
            infoPanel.displayInfo(road);
        }
    }
    
    void displayFlag(Flag flagAtPoint) {
        flag = flagAtPoint;
        road = null;
        house = null;
        
        infoPanel.displayInfo(flagAtPoint);
    }

    void displayHouse(Building b) {
        flag = null;
        road = null;
        house = b;
        
        infoPanel.displayInfo(b);
    }
    
    void clearInfo() {
        infoPanel.clear();
    }

    Infoview getInfoview() {
        return infoPanel;
    }

    void displayRoad(Road roadAtPoint) {
        flag = null;
        road = roadAtPoint;
        house = null;
        
        infoPanel.displayInfo(roadAtPoint);
    }

    private class ControlPanel extends JPanel {

        boolean turboToggle;
        private JButton buildFarm;
        private JButton buildBarracks;
        private JButton buildQuarry;
        private JButton buildSawmill;
        private JButton buildForester;
        private JButton buildWoodcutter;
        
        public ControlPanel() {
            super();
            
            turboToggle = false;
            
            setMinimumSize(new Dimension(100, 100));
            setPreferredSize(new Dimension(100, 500));

            setLayout(new GridLayout(2, 1));
            
            JPanel controlPanel = createControlPanel();
            JPanel constructionPanel = createConstructionPanel();
            
            add(controlPanel);
            add(constructionPanel);
            
            setVisible(true);
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
            flagAndRoadPanel.setLayout(new GridLayout(1, 2));
            
            JButton raiseFlagButton = new JButton("Raise flag");
            JButton startRoadButtom = new JButton("Start new road");
            
            flagAndRoadPanel.add(raiseFlagButton);
            flagAndRoadPanel.add(startRoadButtom);

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
            
            startRoadButtom.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent ae) {
                    if (commandListener != null) {
                        commandListener.startRoadCommand(selectedPoint);
                    }
                }
            });
            
            flagAndRoadPanel.setVisible(true);

            /* Create panel for construction of buildings */
            buildingPanel.setLayout(new GridLayout(2, 3));
            
            buildWoodcutter = new JButton("Woodcutter");
            buildForester   = new JButton("Forester");
            buildSawmill    = new JButton("Sawmill");
            buildQuarry     = new JButton("Quarry");
            buildBarracks   = new JButton("Barracks");
            buildFarm       = new JButton("Farm");
            
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
                            }
                        } catch (Exception ex) {
                            Logger.getLogger(SidePanel.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            };
            
            buildWoodcutter.addActionListener(buildListener);
            buildForester.addActionListener(buildListener);
            buildSawmill.addActionListener(buildListener);
            buildQuarry.addActionListener(buildListener);
            buildBarracks.addActionListener(buildListener);
            buildFarm.addActionListener(buildListener);
            
            buildingPanel.add(buildForester);
            buildingPanel.add(buildWoodcutter);
            buildingPanel.add(buildQuarry);
            buildingPanel.add(buildBarracks);
            buildingPanel.add(buildSawmill);
            buildingPanel.add(buildFarm);
            
            buildingPanel.setVisible(true);
            
            /* Build the container panel */
            panel.setLayout(new GridLayout(4, 1));
            
            panel.add(new JLabel("Create flags and roads"));
            panel.add(flagAndRoadPanel);
            panel.add(new JLabel("Create buildings"));
            panel.add(buildingPanel);
            
            panel.setVisible(true);
            
            return panel;
        }
    }

    public class Infoview extends JPanel {
        private final JLabel titleLabel;
        private final JLabel infoLabel;

        public Infoview() {
            super();
            
            flag = null;
            house = null;
            road = null;
            
            setMinimumSize(new Dimension(200, 100));
            setPreferredSize(new Dimension(300, 500));
            
            titleLabel = new JLabel();
            infoLabel = new JLabel();
            
            titleLabel.setText("none");
            
            setLayout(new GridLayout(2, 1));
            
            add(titleLabel);
            add(infoLabel);

            setVisible(true);
        }

        void displayInfo(Building b) {
            titleLabel.setText(b.getClass().getSimpleName());
            
            String info = "<html>";
            
            info += b.getConstructionState() + "<br>";
            
            /* Print if worker is needed */
            try {
                if (b.needsWorker()) {
                    info += "Needs " + b.getWorkerType().name() + "<br>";
                }
            } catch (Exception ex) {
                Logger.getLogger(SidePanel.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            /* Print inventory */
            if (b instanceof Storage) {
                info += "<br><b>Inventory</b><br>";
                
                for (Material m : Material.values()) {
                    Storage s = (Storage)b;
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

    public SidePanel() {
        super();

        selectedPoint = null;
        
        commandListener = null;
        
        infoPanel = new Infoview();
        controlPanel = new ControlPanel();
        
        addTab("Info", infoPanel);
        setMnemonicAt(0, KeyEvent.VK_1);

        addTab("Control", controlPanel);
        setMnemonicAt(1, KeyEvent.VK_2);

        setVisible(true);
    }

    void setCommandListener(CommandListener cl) {
        commandListener = cl;
    }

    void setSelectedPoint(Point p) {
        selectedPoint = p;
    }
}
