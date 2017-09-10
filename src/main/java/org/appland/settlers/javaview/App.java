package org.appland.settlers.javaview;

import java.awt.BorderLayout;


import java.awt.Frame;
import java.awt.event.KeyEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JMenu;


import org.appland.settlers.model.Building;
import org.appland.settlers.model.Cargo;
import org.appland.settlers.model.Headquarter;
import org.appland.settlers.model.Material;
import org.appland.settlers.model.Player;
import org.appland.settlers.model.Point;

import static org.appland.settlers.model.Material.COAL;
import static org.appland.settlers.model.Material.COIN;
import static org.appland.settlers.model.Material.FISH;
import static org.appland.settlers.model.Material.FLOUR;
import static org.appland.settlers.model.Material.GENERAL;
import static org.appland.settlers.model.Material.GOLD;
import static org.appland.settlers.model.Material.MEAT;
import static org.appland.settlers.model.Material.PLANCK;
import static org.appland.settlers.model.Material.STONE;
import static org.appland.settlers.model.Material.WHEAT;
import static org.appland.settlers.model.Material.WOOD;

import org.appland.settlers.computer.PlayerType;

import static org.appland.settlers.javaview.HouseType.BAKERY;
import static org.appland.settlers.javaview.HouseType.BARRACKS;
import static org.appland.settlers.javaview.HouseType.CATAPULT;
import static org.appland.settlers.javaview.HouseType.COALMINE;
import static org.appland.settlers.javaview.HouseType.DONKEY_FARM;
import static org.appland.settlers.javaview.HouseType.FARM;
import static org.appland.settlers.javaview.HouseType.FISHERY;
import static org.appland.settlers.javaview.HouseType.FORTRESS;
import static org.appland.settlers.javaview.HouseType.GOLDMINE;
import static org.appland.settlers.javaview.HouseType.GRANITEMINE;
import static org.appland.settlers.javaview.HouseType.GUARD_HOUSE;
import static org.appland.settlers.javaview.HouseType.HUNTER_HUT;
import static org.appland.settlers.javaview.HouseType.IRONMINE;
import static org.appland.settlers.javaview.HouseType.MILL;
import static org.appland.settlers.javaview.HouseType.MINT;
import static org.appland.settlers.javaview.HouseType.PIG_FARM;
import static org.appland.settlers.javaview.HouseType.QUARRY;
import static org.appland.settlers.javaview.HouseType.SAWMILL;
import static org.appland.settlers.javaview.HouseType.SLAUGHTER_HOUSE;
import static org.appland.settlers.javaview.HouseType.WATCH_TOWER;
import static org.appland.settlers.javaview.HouseType.WELL;
import static org.appland.settlers.javaview.HouseType.WOODCUTTER;
import org.appland.settlers.model.GameMap;
import org.appland.settlers.model.Storage;


public class App extends JFrame implements View {

    private static final long serialVersionUID = 1L;

    private final static int INPUT_CLEAR_DELAY = 5000;
    private final static int STATS_PERIOD = 1000;
    private final static String TITLE = "Settlers 2";

    private final SidePanel                sidePanel;
    private final Map<Material, JMenuItem> materialMenuItemMap;
    private final Map<Integer, JMenuItem>  transportPriorityMap;
    private final GameDrawer               canvas;
    private final StatisticsTask           statisticsTask;
    private final Timer                    statisticsTimer;
    private final Game                     game;
    private final Timer                    clearInputTimer;
    private final Timer                    drawingTimer;

    private boolean      turboModeEnabled = false;
    private UiState      state;
    private List<Point>  roadPoints;
    private boolean      showAvailableSpots;
    private Point        selectedPoint;
    private String       previousKeys;
    private Player       controlledPlayer;
    private GameMap      map;

    public App(Game game, GameMap map) throws Exception {
        super();

        this.game = game;
        this.map = map;

        roadPoints         = new ArrayList<>();
        showAvailableSpots = false;
        clearInputTimer    = new Timer("Clear input timer");

        previousKeys = "";

        /* Set the default size of the window */
        setSize(600, 500);

        /* Maximize by default */
        setExtendedState(Frame.MAXIMIZED_BOTH);

        /* Exit if the window is closed */
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        /* Create the side panel */
        sidePanel = new SidePanel(this);

        /* Create the canvas to draw on */
        canvas = new GameDrawer(map, this);

        /* Connect the side panel with the canvas */
        sidePanel.setCommandListener(this);

        /* Add the canvas and the sidepanel */
        getContentPane().add(canvas);
        getContentPane().add(sidePanel, BorderLayout.EAST);

        /* Create window menu */
        materialMenuItemMap = new HashMap<>();
        transportPriorityMap = new HashMap<>();

        /* Create timers and tasks */
        statisticsTask  = new StatisticsTask(map, controlledPlayer);
        statisticsTimer = new Timer("Statistics timer");
        drawingTimer    = new Timer("Drawing timer");

        setJMenuBar(createMenuBar());
        setTitle(TITLE);

        /* Show the window */
        setVisible(true);

        /* Initial state is UiState.IDLE */
        state = UiState.IDLE;
    }

    private JMenuBar createMenuBar() {

        JMenuBar menubar = new JMenuBar();

        /* Show the inventory */
        JMenu headquarterMenu = new JMenu("Inventory");

        for (Material m : Material.values()) {
            JMenuItem item = new JMenuItem(m.name());

            item.setEnabled(false);

            materialMenuItemMap.put(m, item);
            headquarterMenu.add(item);
        }

        menubar.add(headquarterMenu);

        /* Show the transportation priority */
        JMenu transportPriorityMenu = new JMenu("Transport Priority");

        int i = 0;
        for (Material m : Material.values()) {
            JMenuItem item = new JMenuItem(m.name());

            item.setEnabled(false);

            transportPriorityMap.put(i, item);
            transportPriorityMenu.add(item);

            i++;
        }

        menubar.add(transportPriorityMenu);

        return menubar;
    }

    @Override
    public void onSaveTroubleshootingInformation() {

        /* Save snapshots for each player */
        try {
            canvas.writeSnapshots();
        } catch (Exception ex1) {
            Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex1);
        }
    }

    void onGamePointHovered(Point point) {
        canvas.setHoveringSpot(point);
    }

    public void setTick(int tick) {
        game.setSpeed(tick);
    }

    void onGamePointClicked(Point p) {
        try {
            if (state == UiState.BUILDING_ROAD) {

                if (map.isFlagAtPoint(p)) {

                    addRoadPoint(p);
                    map.placeRoad(controlledPlayer, roadPoints);

                    setState(UiState.IDLE);
                } else if (!map.isRoadAtPoint(p)) {
                    addRoadPoint(p);
                }
            } else if (state == UiState.IDLE) {
                selectPoint(p);

                setState(UiState.POINT_SELECTED);
            } else if (state == UiState.POINT_SELECTED) {
                selectPoint(p);

                setState(UiState.POINT_SELECTED);
            }
        } catch (Exception ex) {
            roadPoints.clear();
            setState(UiState.IDLE);
            Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void setMap(GameMap map) {
        this.map = map;

        sidePanel.setMap(map);
        canvas.setMap(map);
        statisticsTask.setMap(map);
    }

    @Override
    public void onGameStarted() {

        setControlledPlayer(map.getPlayers().get(0));

        statisticsTimer.schedule(statisticsTask, STATS_PERIOD, STATS_PERIOD);

        /* Start the drawing timer */
        drawingTimer.schedule(new DrawerTask(), 17, 17);
    }

    void startRoad(Point p) {

        if (state != UiState.POINT_SELECTED) {
            return;
        }

        roadPoints.clear();

        addRoadPoint(p);

        state = UiState.BUILDING_ROAD;

        System.out.println("Started road");
    }

    private void addRoadPoint(Point point) {
        if (roadPoints.isEmpty()) {
            roadPoints.add(point);
        } else {
            Point last = getLastSelectedWayPoint();

            if (!point.isAdjacent(last)) {
                List<Point> pointsBetween = map.findAutoSelectedRoad(controlledPlayer, last, point, roadPoints);

                boolean firstRun = true;

                for (Point p : pointsBetween) {
                    if (firstRun) {
                        firstRun = false;
                        continue;
                    }

                    roadPoints.add(p);
                }
            } else {
                roadPoints.add(point);
            }
        }
    }

    List<Point> getRoadPoints() {
        return this.roadPoints;
    }

    private void placeBuilding(Player player, HouseType houseType, Point p) throws Exception {

        Building b = BuildingFactory.createBuilding(player, houseType);

        if (b == null) {
            throw new Exception("Can't build " + houseType);
        }

        map.placeBuilding(b, p);

        System.out.println("Placing " + houseType + " at " + selectedPoint);
    }

    public void resetGame() throws Exception {

        game.resetGame();

        setState(UiState.IDLE);

        repaint();
    }

    private void setState(UiState uiState) {
        System.out.println("State change: " + state + " --> " + uiState);
        state = uiState;
    }

    private void selectPoint(Point p) throws Exception {
        selectedPoint = p;

        sidePanel.setSelectedPoint(p);

        requestFocus();
    }

    public void toggleTurbo() {
        turboModeEnabled = !turboModeEnabled;

        if (turboModeEnabled) {
            game.setSpeed(1);
        } else {
            game.setSpeed(game.DEFAULT_TICK);
        }
    }

    private Point getLastSelectedWayPoint() {
        return roadPoints.get(roadPoints.size() - 1);
    }

    private void addBonusResourcesForPlayer(Player controlledPlayer) throws Exception {
        Building headquarter = null;

        /* Find headquarter */
        for (Building b : controlledPlayer.getBuildings()) {
            if (b instanceof Headquarter) {
                headquarter = b;

                break;
            }
        }

        /* Fill headquarter with bonus resources */
        if (headquarter != null) {
            fillStorageWithMaterial(headquarter, WOOD,    100);
            fillStorageWithMaterial(headquarter, PLANCK,  100);
            fillStorageWithMaterial(headquarter, STONE,   100);
            fillStorageWithMaterial(headquarter, GOLD,    100);
            fillStorageWithMaterial(headquarter, COAL,    100);
            fillStorageWithMaterial(headquarter, COIN,    100);
            fillStorageWithMaterial(headquarter, FISH,    100);
            fillStorageWithMaterial(headquarter, WHEAT,   100);
            fillStorageWithMaterial(headquarter, FLOUR,   100);
            fillStorageWithMaterial(headquarter, MEAT,    100);
            fillStorageWithMaterial(headquarter, GENERAL, 100);
        }
    }

    private void fillStorageWithMaterial(Building headquarter, Material material, int amount) throws Exception {
        for (int i = 0; i < amount; i++) {
            headquarter.putCargo(new Cargo(material, map));
        }
    }

    public void setControlledPlayer(Player player) {
        System.out.println("Changed control to " + player);

        controlledPlayer = player;

        sidePanel.setPlayer(player);
        statisticsTask.setControlledPlayer(controlledPlayer);
        canvas.setControlledPlayer(player);

        centerOn(controlledPlayer);
    }

    public void enableComputerPlayer(PlayerType type) {
        System.out.println("Enabling " + type.name() + "computer player for " + controlledPlayer.getName());
        game.enableComputerPlayer(controlledPlayer, type);
    }

    public void dumpRecording() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    boolean showAvailableSpots() {
        return showAvailableSpots;
    }

    private enum UiState {
        IDLE, BUILDING_ROAD, POINT_SELECTED
    }

    void onGamePointDoubleClicked(Point point) {
        try {
            if (state == UiState.IDLE || state == UiState.POINT_SELECTED) {
                if (map.isFlagAtPoint(point)) {
                        startRoad(point);

                        setState(UiState.BUILDING_ROAD);
                } else {
                    map.placeFlag(controlledPlayer, point);

                    setState(UiState.IDLE);
                }
            } else if (state == UiState.BUILDING_ROAD) {
                map.placeFlag(controlledPlayer, point);

                if (!point.equals(roadPoints.get(roadPoints.size() - 1))) {
                    addRoadPoint(point);
                }

                map.placeRoad(controlledPlayer, roadPoints);

                setState(UiState.IDLE);
            }
        } catch (Exception ex) {
            roadPoints.clear();
            setState(UiState.IDLE);
            Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private class ClearInputTask extends TimerTask {

        @Override
        public void run() {
            previousKeys = "";

            setTitle(TITLE);
        }
    }

    private class StatisticsTask extends TimerTask {

        private GameMap map;
        private Player  player;

        StatisticsTask(GameMap map, Player player) {
            this.map = map;
            this.player = player;
        }

        void setMap(GameMap map) {
            this.map = map;
        }

        void setControlledPlayer(Player player) {
            this.player = player;
        }

        @Override
        public void run() {
            Map<Material, Integer> inventory = new HashMap<>();
            Map<Integer, String> transport = new HashMap<>();

            synchronized (map) {
                for (Map.Entry<Material, Integer> pair : player.getInventory().entrySet()) {
                    Material m     = pair.getKey();
                    int amount     = pair.getValue();

                    inventory.put(m, amount);
                }

                int i = 0;
                for (Material m : player.getTransportPriorityList()) {
                    transport.put(i, m.name() + " (" + player.getInventory().get(m) + ")");
                }
            }

            for (Map.Entry<Material, Integer> pair : inventory.entrySet()) {
                Material m = pair.getKey();
                int amount = pair.getValue();

                JMenuItem item = materialMenuItemMap.get(m);

                item.setEnabled(amount > 0);
                item.setText(m.name() + ": " + amount);
                item.updateUI();
            }

            int i = 0;
            for (Material m : Material.values()) {
                    JMenuItem item = transportPriorityMap.get(i);

                    item.setText(transport.get(i));
                    item.updateUI();

                    i++;
            }
        }
    }

    public void keyTyped(KeyEvent ke) {
        char key = ke.getKeyChar();
        boolean keepPreviousKeys = false;

        previousKeys += key;

        try {
            if (previousKeys.equals(" ")) {
                System.out.println("Toggle show available spots");

                showAvailableSpots = !showAvailableSpots;
            } else if (previousKeys.equals("+")) {
                canvas.zoomIn(1);
            } else if (previousKeys.equals("-")) {
                canvas.zoomOut(1);
            } else if (previousKeys.equals("A")) {
                if (map.isBuildingAtPoint(selectedPoint) && !controlledPlayer.isWithinBorder(selectedPoint)) {
                    /* Find building to attack */
                    Building buildingToAttack = map.getBuildingAtPoint(selectedPoint);

                    /* Order attack */
                    int attackers = controlledPlayer.getAvailableAttackersForBuilding(buildingToAttack);

                    controlledPlayer.attack(buildingToAttack, attackers);
                }
            } else if (previousKeys.equals("B")) {
                addBonusResourcesForPlayer(controlledPlayer);
            } else if (previousKeys.equals("bak")) {
                placeBuilding(controlledPlayer, BAKERY, selectedPoint);
                setState(UiState.IDLE);
            } else if (previousKeys.equals("bar")) {
                placeBuilding(controlledPlayer, BARRACKS, selectedPoint);
                setState(UiState.IDLE);
            } else if (previousKeys.equals("C")) {
                enableComputerPlayer(PlayerType.COMPOSITE_PLAYER);
            } else if (previousKeys.equals("ca")) {
                placeBuilding(controlledPlayer, CATAPULT, selectedPoint);
                setState(UiState.IDLE);
            } else if (previousKeys.equals("co")) {
                placeBuilding(controlledPlayer, COALMINE, selectedPoint);
                setState(UiState.IDLE);
            } else if (previousKeys.equals("d")) {
                placeBuilding(controlledPlayer, DONKEY_FARM, selectedPoint);
                setState(UiState.IDLE);
            } else if (previousKeys.equals("D")) {
                ((GameMapRecordingAdapter)map).printRecordingOnConsole();
            } else if (previousKeys.equals("fi")) {
                placeBuilding(controlledPlayer, FISHERY, selectedPoint);
                setState(UiState.IDLE);
            } else if (previousKeys.equals("fore")) {
                placeBuilding(controlledPlayer, HouseType.FORESTER, selectedPoint);
                setState(UiState.IDLE);
            } else if (previousKeys.equals("fort")) {
                placeBuilding(controlledPlayer, FORTRESS, selectedPoint);
                setState(UiState.IDLE);
            } else if (previousKeys.equals("fa")) {
                placeBuilding(controlledPlayer, FARM, selectedPoint);
                setState(UiState.IDLE);
            } else if (previousKeys.equals("go")) {
                placeBuilding(controlledPlayer, GOLDMINE, selectedPoint);
                setState(UiState.IDLE);
            } else if (previousKeys.equals("gr")) {
                placeBuilding(controlledPlayer, GRANITEMINE, selectedPoint);
            } else if (previousKeys.equals("gu")) {
                placeBuilding(controlledPlayer, GUARD_HOUSE, selectedPoint);
                setState(UiState.IDLE);
            } else if (previousKeys.equals("h")) {
                placeBuilding(controlledPlayer, HUNTER_HUT, selectedPoint);
                setState(UiState.IDLE);
            } else if (previousKeys.equals("i")) {
                placeBuilding(controlledPlayer, IRONMINE, selectedPoint);
            } else if (previousKeys.equals("mil")) {
                placeBuilding(controlledPlayer, MILL, selectedPoint);
            } else if (previousKeys.equals("min")) {
                placeBuilding(controlledPlayer, MINT, selectedPoint);
            } else if (previousKeys.equals("p")) {
                placeBuilding(controlledPlayer, PIG_FARM, selectedPoint);
                setState(UiState.IDLE);
            } else if (previousKeys.equals("S")) {
                canvas.writeSnapshots();
            } else if (previousKeys.equals("sa")) {
                placeBuilding(controlledPlayer, SAWMILL, selectedPoint);
                setState(UiState.IDLE);
            } else if (previousKeys.equals("sl")) {
                placeBuilding(controlledPlayer, SLAUGHTER_HOUSE, selectedPoint);
                setState(UiState.IDLE);
            } else if (previousKeys.equals("T")) {
                toggleTurbo();
            } else if (previousKeys.equals("wa")) {
                placeBuilding(controlledPlayer, WATCH_TOWER, selectedPoint);
                setState(UiState.IDLE);
            } else if (previousKeys.equals("we")) {
                placeBuilding(controlledPlayer, WELL, selectedPoint);
                setState(UiState.IDLE);
            } else if (previousKeys.equals("wo")) {
                placeBuilding(controlledPlayer, WOODCUTTER, selectedPoint);
                setState(UiState.IDLE);
            } else if (key == 'q') {
                placeBuilding(controlledPlayer, QUARRY, selectedPoint);
                setState(UiState.IDLE);
            } else if (key == 'R') {
                resetGame();
            } else if (key == KeyEvent.VK_ESCAPE) {
                System.out.println("Resetting state to idle");

                setState(UiState.IDLE);

                previousKeys = "";

                roadPoints.clear();
            } else {
                keepPreviousKeys = true;

                setTitle(TITLE + " (" + previousKeys +")");

                clearInputTimer.purge();

                clearInputTimer.schedule(new ClearInputTask(), INPUT_CLEAR_DELAY);
            }
        } catch (Exception ex) {
            Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
        }

        if (!keepPreviousKeys) {
            previousKeys = "";
            setTitle("Settlers 2");
        }
    }

    void centerOn(Player controlledPlayer) {

        /* Find the point to center on */
        Point point = null;

        for (Building b : controlledPlayer.getBuildings()) {
            if (b instanceof Headquarter) {
                point = b.getPosition();

                break;
            }

            if (point == null && b instanceof Storage) {
                point = b.getPosition();
            }
        }

        /* Only center if we know where to center */
        if (point != null) {
            canvas.centerOn(point);
        }
    }

    Point getSelectedPoint() {
        return this.selectedPoint;
    }

    private class DrawerTask extends TimerTask {

        @Override
        public void run() {
            repaint();
        }
    }
}
