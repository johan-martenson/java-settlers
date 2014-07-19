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

    void displayFlag(Flag flagAtPoint) {
        infoPanel.displayInfo(flagAtPoint);
    }

    void displayHouse(Building b) {
        infoPanel.displayInfo(b);
    }
    
    void clearInfo() {
        infoPanel.clear();
    }

    Infoview getInfoview() {
        return infoPanel;
    }

    void displayRoad(Road roadAtPoint) {
        infoPanel.displayInfo(roadAtPoint);
    }

    private class ControlPanel extends JComponent {

        boolean turboToggle;
        
        public ControlPanel() {
            super();
            
            turboToggle = false;
            
            setMinimumSize(new Dimension(100, 100));
            setPreferredSize(new Dimension(100, 500));
            
            JPanel panel = new JPanel();
            
            panel.setLayout(new GridLayout(1, 1));
            
            JButton turboButton = new JButton("Toggle turbo");
            
            panel.add(turboButton);
            
            turboButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent ae) {
                    if (commandListener != null) {
                        turboToggle = !turboToggle;
                        
                        commandListener.setTurboMode(turboToggle);
                    }
                }
            });
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
            
            add(titleLabel, BorderLayout.NORTH);
            add(infoLabel, BorderLayout.CENTER);

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
            
            String info = "Assigned courier: " + r.getCourier();
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

    public void setCommandListener(CommandListener cl) {
        commandListener = cl;
    }
}
