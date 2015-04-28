package org.appland.settlers.javaview;

import java.awt.BorderLayout;
import static java.awt.Color.BLACK;
import static java.awt.Color.BLUE;
import static java.awt.Color.ORANGE;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.io.File;
import static java.lang.Math.abs;
import static java.lang.Math.ceil;
import static java.lang.Math.floor;
import static java.lang.Math.round;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JPanel;
import java.util.Timer;
import java.util.TimerTask;
import javax.imageio.ImageIO;
import org.appland.settlers.computer.AttackPlayer;
import org.appland.settlers.model.Building;
import org.appland.settlers.model.Cargo;
import org.appland.settlers.model.Flag;
import org.appland.settlers.model.GameMap;
import org.appland.settlers.model.Headquarter;
import org.appland.settlers.model.Material;
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
import org.appland.settlers.model.Player;
import org.appland.settlers.model.Point;
import org.appland.settlers.model.Road;
import org.appland.settlers.computer.ComputerPlayer;
import org.appland.settlers.computer.ConstructionPreparationPlayer;
import org.appland.settlers.computer.ExpandLandPlayer;
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

public class App extends JFrame {
    private static final long serialVersionUID = 1L;
    private final SidePanel sidePanel;

    public App() throws Exception {
        super();

        /* Set the default size of the window */
        setSize(600, 500);

        /* Maximize by default */
        setExtendedState(Frame.MAXIMIZED_BOTH);

        /* Create the side panel */
        sidePanel = new SidePanel();

        /* Create the canvas to draw on */
        GameCanvas canvas = new GameCanvas(100, 100);

        /* Connect the side panel with the canvas */
        sidePanel.setCommandListener(canvas);

        /* Exit if the window is closed */
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        /* Add the canvas and the sidepanel */
        getContentPane().add(canvas);
        getContentPane().add(sidePanel, BorderLayout.EAST);

        /* Show the window early so we can calculate the width and height ratio */
        setVisible(true);

        /* Set title to "Settlers 2" */
        setTitle("Settlers 2");

        /* Create the starting position */
        canvas.prepareGame();

        /* Start the game */
        canvas.startGame();
    }

    private enum UiState {
        IDLE, BUILDING_ROAD, POINT_SELECTED
    }

    class GameCanvas extends JPanel implements MouseListener, KeyListener, CommandListener, MouseWheelListener, ComponentListener {
        private static final long serialVersionUID = 1L;

        private final int INPUT_CLEAR_DELAY = 5000;

        private final List<ComputerPlayer> computerPlayers;
        private final ScenarioCreator      creator;
        private final Timer                gameLoopTimer;
        private final int                  widthInPoints;
        private final int                  heightInPoints;

        private UiState              state;
        private List<Point>          roadPoints;
        private boolean              showAvailableSpots;
        private Point                selectedPoint;
        private int                  tick;
        private String               previousKeys;
        private GameDrawer           gameDrawer;
        private boolean              turboModeEnabled;
        private final Timer          clearInputTimer;
        private Player               controlledPlayer;
        private java.awt.Point       dragStarted;
        private int                  paddingPixelsLeft;
        private int                  paddingPixelsDown;
        private GameMap              map;

        public GameCanvas(int w, int h) throws Exception {
            super();

            computerPlayers    = new ArrayList<>();
            widthInPoints      = w;
            heightInPoints     = h;
            tick               = 250;
            turboModeEnabled   = false;
            roadPoints         = new ArrayList<>();
            showAvailableSpots = false;
            creator            = new ScenarioCreator();
            clearInputTimer    = new Timer("Clear input timer");
            gameLoopTimer      = new Timer("Game loop timer");
            dragStarted        = new java.awt.Point(0, 0);

            /* Create the game drawer with the right size of the playing field */
            gameDrawer = new GameDrawer(w, h, 40, 40);

            /* Create the initial game board */
            resetGame();

            /* Keep the game scene un-dragged */
            paddingPixelsLeft = 0;
            paddingPixelsDown = 0;

            /* Create listener */
            setFocusable(true);
            requestFocusInWindow();

            addMouseListener(this);
            addKeyListener(this);
            addMouseWheelListener(this);
            addComponentListener(this);

            previousKeys = "";
            
            /* Add action listeners */
            addComponentListener(new ComponentAdapter() {

                @Override
                public void componentResized(ComponentEvent evt) {                    
                    gameDrawer.recalculateScale(getWidth(), getHeight());

                    repaint();
                }
            });

            addMouseMotionListener(new MouseMotionAdapter() {

                @Override
                public void mouseMoved(MouseEvent me) {

                    /* Get point the mouse hovers over on the game map */
                    Point point = screenPointToGamePoint(new java.awt.Point(me.getX(), me.getY()));

                    /* Update the hovering spot in the game drawer */
                    gameDrawer.setHoveringSpot(point);

                    repaint();
                }

                @Override
                public void mouseDragged(MouseEvent me) {

                    /* Get the new point in surface coordinates */
                    java.awt.Point dropPoint = me.getPoint();

                    /* Determine the change from the original point */
                    int changeX = dropPoint.x - dragStarted.x;
                    int changeY = dropPoint.y - dragStarted.y;

                    paddingPixelsLeft += changeX;
                    paddingPixelsDown += changeY;

                    dragStarted = dropPoint;

                    repaint();
                }
            });

            /* Initial state is UiState.IDLE */
            state = UiState.IDLE;

            setVisible(true);

            requestFocus();
        }

        private boolean isDoubleClick(MouseEvent me) {
            return me.getClickCount() > 1;
        }

        @Override
        public void setTurboMode(boolean toggle) {
            turboModeEnabled = toggle;

            if (toggle) {
                tick = 30;
            } else {
                tick = 250;
            }

            /* Update the timer */
            gameLoopTimer.schedule(new GameLoopTask(), tick, tick);
        }

        private void writeSnapshots() throws Exception {
            String name = "Snapshot-" + Calendar.getInstance().getTime().toString();

            /* Write an image to file for each player */
            for (Player player : map.getPlayers()) {

                /* Create an image to draw on */
                BufferedImage bi = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);

                Graphics2D graphics = bi.createGraphics();

                /* Draw the scene for the player */
                gameDrawer.drawScene(graphics, controlledPlayer, null, null, false);

                /* Write the image to a file */
                File outputfile = new File(name + "-" + player.getName() + ".png");

                ImageIO.write(bi, "png", outputfile);

                System.out.println("Wrote scene to " + outputfile.getAbsolutePath());
            }
        }

        private void startRoad(Point p) throws Exception {

            if (!roadPoints.isEmpty()) {
                throw new Exception("Already building a road, can't start a new one");
            }

            addRoadPoint(p);

            System.out.println("Started road");
        }
        
        private void buildRoad(List<Point> wayPoints) throws Exception {
            
            Road r = map.placeRoad(controlledPlayer, wayPoints);

            roadPoints = new ArrayList<>();

            System.out.println("Built road (" + wayPoints + ")");
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

            repaint();
        }

        private Point getLastSelectedWayPoint() {
            return roadPoints.get(roadPoints.size() - 1);
        }

        @Override
        public void keyPressed(KeyEvent ke) {}

        @Override
        public void keyTyped(KeyEvent ke) {            
            char key = ke.getKeyChar();
            boolean keepPreviousKeys = false;
            
            previousKeys += key;
            
            try {
                if (previousKeys.equals(" ")) {
                    System.out.println("Toggle show available spots");

                    showAvailableSpots = !showAvailableSpots;
                } else if (previousKeys.equals("+")) {
                    addBonusResourcesForPlayer(controlledPlayer);
                } else if (previousKeys.equals("A")) {
                    if (map.isBuildingAtPoint(selectedPoint) && !controlledPlayer.isWithinBorder(selectedPoint)) {
                        attackHouse(selectedPoint);
                    }
                } else if (previousKeys.equals("bak")) {
                    placeBuilding(controlledPlayer, BAKERY, selectedPoint);
                    setState(UiState.IDLE);
                } else if (previousKeys.equals("bar")) {
                    placeBuilding(controlledPlayer, BARRACKS, selectedPoint);
                    setState(UiState.IDLE);
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
                    repaint();
                } else if (previousKeys.equals("gu")) {
                    placeBuilding(controlledPlayer, GUARD_HOUSE, selectedPoint);
                    setState(UiState.IDLE);
                } else if (previousKeys.equals("i")) {
                    placeBuilding(controlledPlayer, IRONMINE, selectedPoint);
                    repaint();
                } else if (previousKeys.equals("mil")) {
                    placeBuilding(controlledPlayer, MILL, selectedPoint);
                    repaint();
                } else if (previousKeys.equals("min")) {
                    placeBuilding(controlledPlayer, MINT, selectedPoint);
                    repaint();
                } else if (previousKeys.equals("p")) {
                    placeBuilding(controlledPlayer, PIG_FARM, selectedPoint);
                    setState(UiState.IDLE);
                } else if (previousKeys.equals("S")) {
                    writeSnapshots();
                } else if (previousKeys.equals("sa")) {
                    placeBuilding(controlledPlayer, SAWMILL, selectedPoint);
                    setState(UiState.IDLE);
                } else if (previousKeys.equals("sl")) {
                    placeBuilding(controlledPlayer, SLAUGHTER_HOUSE, selectedPoint);
                    setState(UiState.IDLE);
                } else if (previousKeys.equals("T")) {
                    setTurboMode(!turboModeEnabled);
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
                } else if (key == 'X') {
                    ((GameMapRecordingAdapter)map).recordMarker();
                    System.out.println("Added marker to api recording");
                } else if (key == KeyEvent.VK_ESCAPE) {
                    System.out.println("Resetting state to idle");

                    setState(UiState.IDLE);

                    previousKeys = "";

                    roadPoints.clear();
                } else {
                    keepPreviousKeys = true;

                    setTitle("Settlers 2 (" + previousKeys +")");
                
                    clearInputTimer.purge();
                    
                    clearInputTimer.schedule(new ClearInputTask(), INPUT_CLEAR_DELAY);
                }
            } catch (Exception ex) {
                Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
            }

            if (!keepPreviousKeys) {
                previousKeys = "";
                setTitle("Settlers 2");

                repaint();
            }
        }

        @Override
        public void attackHouse(Point selectedPoint) {

            try {
                /* Find building to attack */
                Building buildingToAttack = map.getBuildingAtPoint(selectedPoint);

                /* Order attack */
                int attackers = controlledPlayer.getAvailableAttackersForBuilding(buildingToAttack);
                
                controlledPlayer.attack(buildingToAttack, attackers);
                
                ((GameMapRecordingAdapter)map).recordAttack(controlledPlayer, buildingToAttack);
            } catch (Exception ex) {
                Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        @Override
        public void evacuate(Point selectedPoint) throws Exception {

            /* Find military building to evacuate */
            Building building = map.getBuildingAtPoint(selectedPoint);

            /* Order evacuation */
            building.evacuate();
        }

        @Override
        public void cancelEvacuation(Point selectedPoint) {

            /* Find military building to re-populate */
            Building building = map.getBuildingAtPoint(selectedPoint);

            /* Order re-population */
            building.cancelEvacuation();
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

        @Override
        public void stopCoins(Point selectedPoint) {

            /* Find building to stop coin delivery to */
            Building b = map.getBuildingAtPoint(selectedPoint);

            /* Stop coin delivery */
            b.disablePromotions();
        }

        @Override
        public void startCoins(Point selectedPoint) {

            /* Find building to resume coin delivery to */
            Building b = map.getBuildingAtPoint(selectedPoint);

            /* Resume coin delivery */
            b.enablePromotions();
        }

        @Override
        public void setControlledPlayer(Player player) {
            System.out.println("Changed control to " + player);

            controlledPlayer = player;

            sidePanel.setPlayer(player);

            repaint();
        }

        @Override
        public void enableComputerPlayer(PlayerType type) {
            ComputerPlayer existingPlayer = null;

            for (ComputerPlayer player : computerPlayers) {
                if (player.getControlledPlayer().equals(controlledPlayer)) {
                    existingPlayer = player;
                }
            }

            if (existingPlayer != null) {
                System.out.println("Replacing active computer player");

                computerPlayers.remove(existingPlayer);
            }

            System.out.println("Enabling " + type.name() + "computer player for " + controlledPlayer.getName());

            switch (type) {
            case BUILDING:
                computerPlayers.add(new ConstructionPreparationPlayer(controlledPlayer, map));
                break;
            case EXPANDING:
                computerPlayers.add(new ExpandLandPlayer(controlledPlayer, map));
                break;
            case ATTACKING:
                computerPlayers.add(new AttackPlayer(controlledPlayer, map));
            }
        }

        private Point screenPointToGamePoint(java.awt.Point screenPoint) {

            /* Adjust for padding */
            java.awt.Point point = new java.awt.Point(screenPoint.x - paddingPixelsLeft, screenPoint.y - paddingPixelsDown);

            /* Go from surface coordinates to game points */
            double px = (double) point.x / gameDrawer.getScaleX();
            double py = (double) (getHeight() - point.y) / gameDrawer.getScaleY();

            /* Round to integers */
            int roundedX = (int) round(px);
            int roundedY = (int) round(py);

            /* Calculate the error */
            double errorX = abs(px - roundedX);
            double errorY = abs(py - roundedY);

            /* Adjust the values if needed to avoid invalid points */
            if ((roundedX + roundedY) % 2 != 0) {
                if (errorX < errorY) {
                    if (roundedY > py) {
                        roundedY = (int) floor(py);
                    } else {
                        roundedY = (int) ceil(py);
                    }
                } else if (errorX > errorY) {
                    if (roundedX > px) {
                        roundedX = (int) floor(px);
                    } else {
                        roundedX = (int) ceil(px);
                    }
                } else {
                    roundedX++;
                }
            }

            return new Point(roundedX, roundedY);
        }

        @Override
        public void componentResized(ComponentEvent ce) {}

        @Override
        public void componentMoved(ComponentEvent ce) {}

        @Override
        public void componentShown(ComponentEvent ce) {}

        @Override
        public void componentHidden(ComponentEvent ce) {}

        private void prepareGame() throws Exception {

            /* Create the initial terrain and player positions */
            resetGame();
        }

        private void startGame() {

            /* Start game tick */
            TimerTask task = new GameLoopTask();

            gameLoopTimer.schedule(task, tick, tick);
        }

        private class GameLoopTask extends TimerTask {

            @Override
            public void run() {

                /* Call any computer players if available */
                for (ComputerPlayer computerPlayer : computerPlayers) {
                    try {
                        computerPlayer.turn();
                    } catch (Exception ex) {

                        /* Print exception and backtrace */
                        Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);

                        /* Print API recording to make the fault reproducable */
                        ((GameMapRecordingAdapter)map).printRecordingOnConsole();

                        for (Flag flag : map.getFlags()) {
                            System.out.println("FLAG: " + flag.getPosition());
                        }

                        for (Road road : map.getRoads()) {
                            System.out.println("" + road.getWayPoints());
                        }

                        for (Building building : computerPlayer.getControlledPlayer().getBuildings()) {
                            System.out.println("" + building.getClass() + " " + building.getPosition());
                        }

                        /* Save snapshots for each player */
                        try {
                            writeSnapshots();
                        } catch (Exception ex1) {
                            Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex1);
                        }

                        System.exit(1);
                    }
                }

                /* Run the game logic one more step */
                try {
                    map.stepTime();
                } catch (Exception ex) {

                    /* Print exception and backtrace */
                    Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);

                    /* Print API recording to make the fault reproducable */
                    ((GameMapRecordingAdapter)map).printRecordingOnConsole();

                    System.exit(1);
                }

                /* Re-draw the scene */
                repaint();
            }
        }

        private class ClearInputTask extends TimerTask {

            @Override
            public void run() {
                previousKeys = "";

                setTitle("Settlers 2");
            }
        }

        @Override
        public void keyReleased(KeyEvent ke) {}

        @Override
        public void placeFlag(Point p) throws Exception {

            Flag flag = map.placeFlag(controlledPlayer, p);

            System.out.println("Placed flag at " + p);
        }

        @Override
        public void placeBuilding(HouseType type, Point p) throws Exception {
            placeBuilding(controlledPlayer, type, p);
        }

        private void placeBuilding(Player player, HouseType houseType, Point p) throws Exception {

            Building b = BuildingFactory.createBuilding(player, houseType);

            if (b == null) {
                throw new Exception("Can't build " + houseType);
            }

            map.placeBuilding(b, p);

            System.out.println("Placing " + houseType + " at " + selectedPoint);
        }

        @Override
        public void resetGame() throws Exception {
            computerPlayers.clear();

            if (map != null) {
                ((GameMapRecordingAdapter)map).clear();
            }

            setState(UiState.IDLE);

            /* Create players */
            Player player0 = new Player("Player 0", BLUE);
            Player player1 = new Player("Player 1", ORANGE);

            List<Player> players = new LinkedList<>();

            players.add(player0);
            players.add(player1);

            /* Choose the player to control */
            controlledPlayer = player0;

            sidePanel.setPlayer(controlledPlayer);

            /* Create game map */
            map = new GameMapRecordingAdapter(players, widthInPoints, heightInPoints);

            sidePanel.setMap(map);

            gameDrawer.setMap(map);

            /* Create the terrain */
            creator.createInitialTerrain(map);

            /* Place player to be controlled */
            creator.placeInitialPlayer(player0, map);

            /* Place the opponent */
            creator.placeOpponent(player1, map);

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

        @Override
        public void paintComponent(Graphics graphics) {

            /* Start with a black background */
            graphics.setColor(BLACK);
            graphics.fillRect(0, 0, getWidth(), getHeight());

            /* Move the scene a bit */
            graphics.translate(paddingPixelsLeft, paddingPixelsDown);

            /* Draw the scene */
            try {
                gameDrawer.drawScene((Graphics2D)graphics, controlledPlayer, selectedPoint, roadPoints, showAvailableSpots);
            } catch (Exception ex) {
                Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        @Override
        public void mouseClicked(MouseEvent me) {

            /* Translate the screen coordinates to a point in the game */
            Point p = screenPointToGamePoint(me.getPoint());

            try {
                if (isDoubleClick(me)) {
                    if (state == UiState.IDLE || state == UiState.POINT_SELECTED) {
                        if (map.isFlagAtPoint(p)) {
                            startRoad(p);

                            setState(UiState.BUILDING_ROAD);
                        } else {
                            placeFlag(p);
                            
                            setState(UiState.IDLE);
                        }
                    } else if (state == UiState.BUILDING_ROAD) {                        
                        placeFlag(p);

                        if (!p.equals(roadPoints.get(roadPoints.size() - 1))) {
                            addRoadPoint(p);
                        }

                        buildRoad(roadPoints);
        
                        setState(UiState.IDLE);
                    }
                }

                if (!isDoubleClick(me)) {
                    if (state == UiState.BUILDING_ROAD) {

                        if (map.isFlagAtPoint(p)) {

                            addRoadPoint(p);
                            buildRoad(roadPoints);
                            
                            setState(UiState.IDLE);
                        } else if (!map.isRoadAtPoint(p)) {
                            addRoadPoint(p);
                        }
                    } else if (state == UiState.IDLE) {
                        selectPoint(p);
                        
                        setState(UiState.POINT_SELECTED);
                    } else if (state == UiState.POINT_SELECTED) {
                        if (me.getSource().equals(this)) {
                            selectPoint(p);

                            setState(UiState.POINT_SELECTED);
                        }
                    }
                }
                repaint();
            } catch (Exception ex) {
                roadPoints.clear();
                setState(UiState.IDLE);
                repaint();
            }
        }

        @Override
        public void mousePressed(MouseEvent me) {

            /* Remember the point in case this is the start of dragging operation
               NOTE: The point is adjusted to the surface */
            dragStarted = me.getPoint();
        }

        @Override
        public void mouseReleased(MouseEvent me) {}

        @Override
        public void mouseEntered(MouseEvent me) {}

        @Override
        public void mouseExited(MouseEvent me) {}

        @Override
        public void dumpRecording() {
            ((GameMapRecordingAdapter)map).printRecordingOnConsole();
        }

        @Override
        public void startRoadCommand(Point selectedPoint) {
            if (state != UiState.POINT_SELECTED) {
                return;
            }
            
            try {
                startRoad(selectedPoint);
                
                state = UiState.BUILDING_ROAD;
            } catch (Exception ex) {
                Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        @Override
        public void removeFlagCommand(Point selectedPoint) throws Exception {
            Flag flag = map.getFlagAtPoint(selectedPoint);

            map.removeFlag(flag);
        }

        @Override
        public void removeHouseCommand(Point selectedPoint) throws Exception {
            Building b = map.getBuildingAtPoint(selectedPoint);

            ((GameMapRecordingAdapter)map).recordTearDown(b);

            b.tearDown();

            System.out.println("Removed " + b);
        }

        @Override
        public void removeRoadAtPoint(Point selectedPoint) throws Exception {
            Road r = map.getRoadAtPoint(selectedPoint);

            map.removeRoad(r);
        }

        @Override
        public void callGeologist(Point selectedPoint) throws Exception {
            Flag flag = map.getFlagAtPoint(selectedPoint);

            ((GameMapRecordingAdapter)map).recordCallGeologistFromFlag(flag);

            flag.callGeologist();
        }

        @Override
        public void stopProduction(Point selectedPoint) throws Exception {
            Building b = map.getBuildingAtPoint(selectedPoint);

            if (b.isProductionEnabled()) {
                b.stopProduction();
            } else {
                b.resumeProduction();
            }
        }

        @Override
        public void mouseWheelMoved(MouseWheelEvent mwe) {

            try {
                int notches = mwe.getWheelRotation();
                if (notches < 0) {
                    gameDrawer.zoomIn(notches);
                } else {
                    gameDrawer.zoomOut(notches);
                }
            } catch (Exception ex) {
                Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
            }

            repaint();
        }

        @Override
        public void callScout(Point selectedPoint) throws Exception {
            Flag flag = map.getFlagAtPoint(selectedPoint);

            flag.callScout();

            ((GameMapRecordingAdapter)map).recordCallScoutFromFlag(flag);
        }
    }

    public static void main(String[] args) {

        /* Create the game window */
        try {
            App app = new App();
        } catch (Exception ex) {
            Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(1);
        }
    }
}
