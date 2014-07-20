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
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import org.appland.settlers.model.Building;
import org.appland.settlers.model.Cargo;
import org.appland.settlers.model.Courier;
import org.appland.settlers.model.Flag;
import org.appland.settlers.model.Material;
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
        
        public ControlPanel() {
            super();
            
            turboToggle = false;
            
            setMinimumSize(new Dimension(100, 100));
            setPreferredSize(new Dimension(100, 500));
            
            setLayout(new GridLayout(1, 1));
            
            JButton turboButton = new JButton("Toggle turbo");
            
            add(turboButton);
            
            turboButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent ae) {
                    if (commandListener != null) {
                        turboToggle = !turboToggle;
                        
                        commandListener.setTurboMode(turboToggle);
                    }
                }
            });
            
            setVisible(true);
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
                
                info += "Target is " + courier.getTargetFlag() + "<br>";
                
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
}
