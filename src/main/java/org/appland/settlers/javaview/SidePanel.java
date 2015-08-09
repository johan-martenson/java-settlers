package org.appland.settlers.javaview;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import static org.appland.settlers.computer.PlayerType.ATTACKING;
import static org.appland.settlers.computer.PlayerType.BUILDING;
import static org.appland.settlers.computer.PlayerType.EXPANDING;
import static org.appland.settlers.computer.PlayerType.FOOD_PRODUCER;
import static org.appland.settlers.computer.PlayerType.MINERALS;
import static org.appland.settlers.computer.PlayerType.COIN_PRODUCER;
import static org.appland.settlers.computer.PlayerType.COMPOSITE_PLAYER;
import static org.appland.settlers.computer.PlayerType.MILITARY_PRODUCER;
import org.appland.settlers.model.Building;
import org.appland.settlers.model.CoalMine;
import org.appland.settlers.model.Fishery;
import org.appland.settlers.model.GameMap;
import org.appland.settlers.model.GoldMine;
import org.appland.settlers.model.GraniteMine;
import org.appland.settlers.model.IronMine;
import org.appland.settlers.model.Material;
import org.appland.settlers.model.Player;
import org.appland.settlers.model.Point;
import org.appland.settlers.model.Quarry;

/**
 *
 * @author johan
 */
public class SidePanel extends JTabbedPane {
    private final static String TO_BUILD_PANEL       = "To build";
    private final static String FLAG_PANEL           = "Flag";
    private final static String BUILDING_PANEL       = "Building";
    private final static String ROAD_PANEL           = "Road";
    private final static String ENEMY_BUILDING_PANEL = "Enemy building";
    private final static String NONE_PANEL           = "None";
    private static final long serialVersionUID = 1L;

    private final ControlPanel         controlPanel;
    private final SpotToBuildOnPanel   toBuild;
    private final FlagSpotPanel        flagSpotPanel;
    private final OwnBuildingSpotPanel ownBuildingSpotPanel;
    private final RoadSpotPanel        roadSpotPanel;
    private final EnemyBuildingPanel   enemyBuildingPanel;
    private final NonePanel            nonePanel;
    private final JPanel               gamePlayPanel;
    private final CardLayout           gamePanelSelector;

    private CommandListener commandListener;
    private Point           selectedPoint;
    private GameMap         map;
    private Player          player;

    SidePanel() {
        this(null);
    }

    void setMap(GameMap m) {
        map = m;

        controlPanel.updatedMap();
    }

    void setPlayer(Player p) {
        player = p;
    }

    void setCommandListener(CommandListener canvas) {
        commandListener = canvas;
    }

    private class NonePanel extends JPanel {
        private static final long serialVersionUID = 1L;

        public NonePanel() {
            JPanel panel = new JPanel();

            panel.setLayout(new GridLayout(1 + HouseType.values().length, 1));

            add(panel);

            /* Create buttons */

            /* Add buttons to the panel */

            /* Add action listeners */

            /* Show the panel */
            panel.setVisible(true);
        }
    }
    
    private class EnemyBuildingPanel extends JPanel {
        private static final long serialVersionUID = 1L;

        private final JLabel  titleField;
        private final JButton attackButton;
        
        public EnemyBuildingPanel() {
            JPanel panel = new JPanel();

            panel.setLayout(new GridLayout(1 + HouseType.values().length, 1));

            /* Create title field */
            titleField = new JLabel("");

            /* Create buttons */
            attackButton = new JButton("Attack");

            /* Add buttons to the panel */
            panel.add(attackButton);

            /* Add action listeners */
            attackButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent ae) {
                    try {
                        commandListener.attackHouse(selectedPoint);
                    } catch (Exception ex) {
                        Logger.getLogger(SidePanel.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            });

            /* Populate the tab */
            setLayout(new BorderLayout());

            add(titleField, BorderLayout.NORTH);
            add(panel, BorderLayout.CENTER);

            /* Show the panel */
            panel.setVisible(true);
        }

        private void updateButtons() {

            /* Get selected building */
            Building building = map.getBuildingAtPoint(selectedPoint);

            try {
                /* Only enable attack button if the building can be attacked */
                if (building.isMilitaryBuilding()                         &&
                        player.getAvailableAttackersForBuilding(building) > 0 &&
                        building.ready()) {
                    attackButton.setVisible(true);
                } else {
                    attackButton.setVisible(false);
                }
            } catch (Exception ex) {
                Logger.getLogger(SidePanel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        private void updateInfoField() {

            /* Get selected building */
            Building building = map.getBuildingAtPoint(selectedPoint);

            /* Update the title */
            titleField.setText(Utils.BuildingNameAsHeading(building));
        }
    }
    
    private class RoadSpotPanel extends JPanel {
        private static final long serialVersionUID = 1L;
        private final JButton removeRoadButton;

        public RoadSpotPanel() {
            JPanel panel = new JPanel();

            panel.setLayout(new GridLayout(1 + HouseType.values().length, 1));

            add(panel);

            /* Create buttons */
            removeRoadButton = new JButton("Remove road");

            /* Add buttons to the panel */
            panel.add(removeRoadButton);

            /* Add action listeners */
            removeRoadButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent ae) {
                    try {
                        commandListener.removeRoadAtPoint(selectedPoint);
                    } catch (Exception ex) {
                        Logger.getLogger(SidePanel.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            });

            /* Show the panel */
            panel.setVisible(true);
        }
    }
    
    private class OwnBuildingSpotPanel extends JPanel {
        private static final long serialVersionUID = 1L;

        private final JLabel  titleField;
        private final JLabel  infoField;

        private final JButton removeHouseButton;
        private final JButton stopProductionButton;
        private final JButton evacuateButton;
        private final JButton cancelEvacuationButton;
        private final JButton stopCoins;
        private final JButton startCoins;

        public OwnBuildingSpotPanel() {
            JPanel control = new JPanel();
            JPanel info    = new JPanel();

            control.setLayout(new GridLayout(1 + HouseType.values().length, 1));

            /* Create title field */
            titleField = new JLabel("");

            /* Create info field */
            infoField = new JLabel("");

            /* Add info field to the panel */
            info.add(infoField);

            /* Create buttons */
            removeHouseButton      = new JButton("Remove building");
            stopProductionButton   = new JButton("Stop production");
            evacuateButton         = new JButton("Evacuate building");
            cancelEvacuationButton = new JButton("Cancel evacuation");
            stopCoins              = new JButton("Stop coins");
            startCoins             = new JButton("Resume coins");

            /* Add buttons to the panel */
            control.add(removeHouseButton);
            control.add(stopProductionButton);
            control.add(evacuateButton);
            control.add(cancelEvacuationButton);
            control.add(stopCoins);
            control.add(startCoins);

            /* Add action listeners */
            removeHouseButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent ae) {
                    try {
                        commandListener.removeHouseCommand(selectedPoint);
                    } catch (Exception ex) {
                        Logger.getLogger(SidePanel.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            });

            stopProductionButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent ae) {
                    try {
                        commandListener.stopProduction(selectedPoint);
                    } catch (Exception ex) {
                        Logger.getLogger(SidePanel.class.getName()).log(Level.SEVERE, null, ex);
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

            stopCoins.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent ae) {
                    commandListener.stopCoins(selectedPoint);
                }
            });

            startCoins.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent ae) {
                    commandListener.startCoins(selectedPoint);
                }
            });

            /* Add the panels */
            setLayout(new BorderLayout());

            add(titleField, BorderLayout.NORTH);
            add(info, BorderLayout.CENTER);
            add(control, BorderLayout.SOUTH);

            /* Show the panel */
            setVisible(true);
        }

        private void updateButtons() {

            /* Get the selected building */
            Building building = map.getBuildingAtPoint(selectedPoint);

            /* Only enable the military options if it's a military building */
            if (building.isMilitaryBuilding()) {
                startCoins.setEnabled(true);
                stopCoins.setEnabled(true);

                if (building.isEvacuated()) {
                    evacuateButton.setEnabled(false);
                    cancelEvacuationButton.setEnabled(true);
                } else {
                    evacuateButton.setEnabled(true);
                    cancelEvacuationButton.setEnabled(false);
                }

                if (building.isPromotionEnabled()) {
                    startCoins.setEnabled(false);
                    stopCoins.setEnabled(true);
                } else {
                    startCoins.setEnabled(true);
                    stopCoins.setEnabled(false);
                }
            } else {
                startCoins.setEnabled(false);
                stopCoins.setEnabled(false);
                evacuateButton.setEnabled(false);
                cancelEvacuationButton.setEnabled(false);                
            }

            /* Only set regular production options for capable buildings */
            //stopProductionButton.setEnabled(!militaryBuilding);
        }

        private void updateInfoField() {

            /* Get the selected building */
            Building building = map.getBuildingAtPoint(selectedPoint);

            /* Start the title */
            String title = Utils.BuildingNameAsHeading(building);

            /* Adapt the title to the state of the building */
            if (building.underConstruction()) {
                title = "(" + title + ")";
            } else if (building.burningDown()) {
                title = "Burning " + title;
            } else if (building.destroyed()) {
                title = "Destroyed" + title;
            }

            /* Put together the info text */
            String info = "<html>";

            /* Indicate if production is disabled */
            if (!building.isProductionEnabled()) {
                info += "<br>Production is stopped<br>";
            }

            /* Print if worker is needed */
            try {
                if (building.needsWorker()) {
                    info += "Needs " + building.getWorkerType().name() + "<br>";
                }
            } catch (Exception ex) {
                Logger.getLogger(SidePanel.class.getName()).log(Level.SEVERE, null, ex);
            }

            /* Print deployed militaries if it's a military building */
            if (building.isMilitaryBuilding()) {
                info += building.getHostedMilitary() + " of " + building.getMaxHostedMilitary() + " deployed <br>";

                /* Print if the building is evacuated */
                if (building.isEvacuated()) {
                    info += "Evacuation activated<br>";
                }

                /* Print if promotions are disabled */
                if (!building.isPromotionEnabled()) {
                    info += "Promotions disabled<br>";
                }
            }

            /* Note if the building has run out of natural resources */
            if ((building instanceof GoldMine    ||
                 building instanceof IronMine    ||
                 building instanceof CoalMine    ||
                 building instanceof Quarry      ||
                 building instanceof GraniteMine ||
                 building instanceof Fishery) &&
                 building.outOfNaturalResources()) {
                info += "No more available resources<br>";
            }

            /* Print material the building needs */
            List<Material> materialNeeded = new LinkedList<>();

            for (Material m : Material.values()) {
                if (building.needsMaterial(m)) {
                    materialNeeded.add(m);
                }
            }

            if (!materialNeeded.isEmpty()) {
                info += "Needs: ";

                boolean firstRun = true;
                for (Material m : materialNeeded) {
                    if (!firstRun) {
                        info += ", ";
                    }

                    info += m.name();
                    firstRun = false;
                }
            }

            /* Set the title */
            titleField.setText(title);

            /* Set the info text */
            infoField.setText(info);
        }
    }
    
    private class FlagSpotPanel extends JPanel {
        private static final long serialVersionUID = 1L;

        JButton startRoadButton;
        JButton removeFlagButton;
        JButton callScoutButton;
        JButton callGeologistButton;

        public FlagSpotPanel() {
            JPanel panel = new JPanel();

            panel.setLayout(new GridLayout(1 + HouseType.values().length, 1));

            add(panel);

            /* Create buttons */
            removeFlagButton    = new JButton("Remove flag");
            startRoadButton     = new JButton("Start new road");
            callGeologistButton = new JButton("Call geologist");
            callScoutButton     = new JButton("Call scout");

            /* Add buttons to the panel */
            panel.add(removeFlagButton);
            panel.add(startRoadButton);
            panel.add(callGeologistButton);
            panel.add(callScoutButton);

            /* Add action listeners */
            startRoadButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent ae) {
                    commandListener.startRoadCommand(selectedPoint);
                }
            });

            removeFlagButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent ae) {
                    try {
                        commandListener.removeFlagCommand(selectedPoint);
                    } catch (Exception ex) {
                        Logger.getLogger(SidePanel.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            });

            callGeologistButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent ae) {
                    try {
                        commandListener.callGeologist(selectedPoint);
                    } catch (Exception ex) {
                        Logger.getLogger(SidePanel.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            });

            callScoutButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent ae) {
                    try {
                        commandListener.callScout(selectedPoint);
                    } catch (Exception ex) {
                        Logger.getLogger(SidePanel.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            });

            /* Show the panel */
            panel.setVisible(true);
        }
    }
    
    private class SpotToBuildOnPanel extends JPanel {
        private static final long serialVersionUID = 1L;
        private final JButton raiseFlagButton;
        private final Map<JButton, HouseType> buttonToHouseType;

        SpotToBuildOnPanel() {

            /* Create panel to hold the buttons */
            JPanel panel = new JPanel();

            panel.setLayout(new GridLayout(1 + HouseType.values().length, 1));

            /* Add button for raising a flag */
            raiseFlagButton = new JButton("Raise flag");

            panel.add(raiseFlagButton);

            raiseFlagButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent ae) {
                    try {
                        commandListener.placeFlag(selectedPoint);
                    } catch (Exception ex) {
                        Logger.getLogger(SidePanel.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            });

            /* Place house buttons */
            buttonToHouseType = new HashMap<>();

            for (HouseType type : HouseType.values()) {
                JButton buildingButton = new JButton(Utils.prettifyBuildingName(type));

                panel.add(buildingButton);

                buttonToHouseType.put(buildingButton, type);
            }

            /* Add action handler to house buttons */
            for (JButton button : buttonToHouseType.keySet()) {
                button.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent ae) {
                        try {
                            commandListener.placeBuilding(buttonToHouseType.get(ae.getSource()), selectedPoint);
                        } catch (Exception ex) {
                            Logger.getLogger(SidePanel.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                });
            }

            /* Fix the dimensions */
            setMinimumSize(new Dimension(100, 100));
            setPreferredSize(new Dimension(100, 500));

            /* Place the panel in the tab */
            add(panel);

            /* Make it visible */
            setVisible(true);
        }
    }

    private class ControlPanel extends JPanel {
        private static final long serialVersionUID = 1L;

        private boolean              turboToggle;
        private final JPanel         controlPanel;
        private final Map<JButton, Player> buttonToPlayerMap;

        public ControlPanel() {
            super();

            turboToggle = false;

            setMinimumSize(new Dimension(100, 100));
            setPreferredSize(new Dimension(100, 500));

            setLayout(new BorderLayout());

            controlPanel = createControlPanel();

            add(controlPanel, BorderLayout.NORTH);

            buttonToPlayerMap = new HashMap<>();

            /* Show the control panel */
            setVisible(true);
        }

        private JPanel createControlPanel() {
            JPanel panel = new JPanel();

            panel.setLayout(new GridLayout(0, 1));

            /* Create control buttons */
            JButton turboButton          = new JButton("Toggle turbo");
            JButton dumpRecordingButton  = new JButton("Dump recording");
            JButton resetButton          = new JButton("Reset the game");
            JButton startBuildingPlayer  = new JButton("Start build player");
            JButton startExpandingPlayer = new JButton("Start expanding player");
            JButton startAttackingPlayer = new JButton("Start attacking player");
            JButton startMineralPlayer   = new JButton("Start mineral player");
            JButton startFoodPlayer      = new JButton("Start food player");
            JButton startCoinPlayer      = new JButton("Start coin player");
            JButton startWeaponPlayer    = new JButton("Start weapon producer");
            JButton startCompositePlayer = new JButton("Start composite player");

            /* Add control buttons to the panel */
            panel.add(turboButton);
            panel.add(dumpRecordingButton);
            panel.add(resetButton);
            panel.add(startBuildingPlayer);
            panel.add(startExpandingPlayer);
            panel.add(startAttackingPlayer);
            panel.add(startMineralPlayer);
            panel.add(startFoodPlayer);
            panel.add(startCoinPlayer);
            panel.add(startWeaponPlayer);
            panel.add(startCompositePlayer);

            /* Add action listeners to the control buttons */
            turboButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    turboToggle = !turboToggle;

                    commandListener.toggleTurbo();
                }
            });

            dumpRecordingButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent ae) {
                    commandListener.dumpRecording();
                }
            });

            resetButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent ae) {
                    try {
                        commandListener.resetGame();
                    } catch (Exception ex) {
                        Logger.getLogger(SidePanel.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            });

            startBuildingPlayer.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent ae) {
                    commandListener.enableComputerPlayer(BUILDING);
                }
            });

            startExpandingPlayer.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent ae) {
                    commandListener.enableComputerPlayer(EXPANDING);
                }
            });

            startAttackingPlayer.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent ae) {
                    commandListener.enableComputerPlayer(ATTACKING);
                }
            });

            startMineralPlayer.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent ae) {
                    commandListener.enableComputerPlayer(MINERALS);
                }
            });

            startFoodPlayer.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent ae) {
                    commandListener.enableComputerPlayer(FOOD_PRODUCER);
                }
            });

            startCoinPlayer.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent ae) {
                	commandListener.enableComputerPlayer(COIN_PRODUCER);
                }
            });

            startWeaponPlayer.addActionListener(new ActionListener() {

		@Override
                public void actionPerformed(ActionEvent arg0) {
                    commandListener.enableComputerPlayer(MILITARY_PRODUCER);
                }
            });

            startCompositePlayer.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent ae) {
                    commandListener.enableComputerPlayer(COMPOSITE_PLAYER);
                }
            });

            return panel;
        }

        private void updatedMap() {

            /* Clear player buttons */
            for (JButton playerButton : buttonToPlayerMap.keySet()) {
                controlPanel.remove(playerButton);
            }

            buttonToPlayerMap.clear();

            /* Create buttons to switch player to control */
            for (Player player : map.getPlayers()) {
                JButton playerButton = new JButton(player.getName());

                controlPanel.add(playerButton);

                buttonToPlayerMap.put(playerButton, player);

                playerButton.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent ae) {

                        /* Get player */
                        Player playerToControl = buttonToPlayerMap.get(ae.getSource());

                        /* Switch the controlled player */
                        commandListener.setControlledPlayer(playerToControl);
                    }
                });
            }
        }
    }

    SidePanel(CommandListener cl) {
        super();

        selectedPoint = null;

        commandListener = cl;

        /* Create panels */
        gamePlayPanel        = new JPanel();
        controlPanel         = new ControlPanel();
        toBuild              = new SpotToBuildOnPanel();
        flagSpotPanel        = new FlagSpotPanel();
        ownBuildingSpotPanel = new OwnBuildingSpotPanel();
        roadSpotPanel        = new RoadSpotPanel();
        enemyBuildingPanel   = new EnemyBuildingPanel();
        nonePanel            = new NonePanel();
        
        /* Add key shortcuts for the panels */
        addTab("Control", controlPanel);

        /* Add the panels as tabs */
        addTab("Game Play",      gamePlayPanel);

        /* Make the game play panel show one panel at the time */
        gamePanelSelector = new CardLayout();
        gamePlayPanel.setLayout(gamePanelSelector);

        /* Populate the game play panel */
        gamePlayPanel.add(toBuild, TO_BUILD_PANEL);
        gamePlayPanel.add(flagSpotPanel, FLAG_PANEL);
        gamePlayPanel.add(ownBuildingSpotPanel, BUILDING_PANEL);
        gamePlayPanel.add(roadSpotPanel, ROAD_PANEL);
        gamePlayPanel.add(enemyBuildingPanel, ENEMY_BUILDING_PANEL);
        gamePlayPanel.add(nonePanel, NONE_PANEL);

        /* Set which tab to show on startup */
        setSelectedComponent(gamePlayPanel);

        /* Show the tab bar */
        setVisible(true);
    }

    void setSelectedPoint(Point point) throws Exception {

        /* Ignore null as selected point */
        if (point == null) {
            return;
        }

        /* There is nothing to do if the point is already selected */
        if (point.equals(selectedPoint)) {
            return;
        }

        /* Set the new selected point */
        selectedPoint = point;

        if (player.isWithinBorder(selectedPoint)) {
            if (map.isFlagAtPoint(selectedPoint)) {
                gamePanelSelector.show(gamePlayPanel, FLAG_PANEL);
            } else if (map.isBuildingAtPoint(selectedPoint)) {

                gamePanelSelector.show(gamePlayPanel, BUILDING_PANEL);

                ownBuildingSpotPanel.updateButtons();
                ownBuildingSpotPanel.updateInfoField();
            } else if (map.isRoadAtPoint(selectedPoint)) {
                gamePanelSelector.show(gamePlayPanel, ROAD_PANEL);
            } else {
                gamePanelSelector.show(gamePlayPanel, TO_BUILD_PANEL);
            }
        } else {
            if (map.isBuildingAtPoint(selectedPoint)) {
                gamePanelSelector.show(gamePlayPanel, ENEMY_BUILDING_PANEL);

                enemyBuildingPanel.updateButtons();
                enemyBuildingPanel.updateInfoField();
            } else {
                gamePanelSelector.show(gamePlayPanel, NONE_PANEL);
            }
        }
    }
}
