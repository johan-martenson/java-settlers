package org.appland.settlers.javaview;

import java.awt.BorderLayout;
import static java.awt.Color.BLUE;
import static java.awt.Color.ORANGE;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
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
import static org.appland.settlers.javaview.App.UiState.BUILDING_ROAD;
import static org.appland.settlers.javaview.App.UiState.IDLE;
import static org.appland.settlers.javaview.App.UiState.POINT_SELECTED;
import org.appland.settlers.model.Barracks;
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
import static org.appland.settlers.model.Size.LARGE;
import org.appland.settlers.model.Stone;
import org.appland.settlers.model.Tile;
import static org.appland.settlers.model.Tile.Vegetation.MOUNTAIN;
import static org.appland.settlers.model.Tile.Vegetation.WATER;
import org.appland.settlers.model.Tree;
import org.appland.settlers.computer.ComputerPlayer;
import org.appland.settlers.computer.ConstructionPreparationPlayer;
import org.appland.settlers.computer.ExpandLandPlayer;
import org.appland.settlers.computer.PlayerType;
import static org.appland.settlers.javaview.HouseType.BAKERY;
import static org.appland.settlers.javaview.HouseType.BARRACKS;
import static org.appland.settlers.javaview.HouseType.COALMINE;
import static org.appland.settlers.javaview.HouseType.DONKEY_FARM;
import static org.appland.settlers.javaview.HouseType.FARM;
import static org.appland.settlers.javaview.HouseType.FISHERY;
import static org.appland.settlers.javaview.HouseType.FORTRESS;
import static org.appland.settlers.javaview.HouseType.GOLDMINE;
import static org.appland.settlers.javaview.HouseType.GRANITEMINE;
import static org.appland.settlers.javaview.HouseType.GUARD_HOUSE;
import static org.appland.settlers.javaview.HouseType.HEADQUARTER;
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
    private final SidePanel sidePanel;

    public App() throws Exception {
        super();

        /* Set the default size of the window */
        setSize(600, 500);

        /* Show the window early so we can calculate the width and height ratio */
        setVisible(true);

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

        /* Set title to "Settlers 2" */
        setTitle("Settlers 2");

        repaint();
    }

    enum UiState {
        IDLE, BUILDING_ROAD, POINT_SELECTED
    }

    class GameCanvas extends JPanel implements MouseListener, KeyListener, CommandListener, MouseWheelListener {
        
        private final int INPUT_CLEAR_DELAY = 5000;

        private UiState              state;
        private List<Point>          roadPoints;
        private boolean              showAvailableSpots;
        private Point                selectedPoint;
        private ApiRecorder          recorder;
        private int                  tick;
        private String               previousKeys;
        private GameDrawer           gameDrawer;
        private boolean              turboModeEnabled;
        private Timer                clearInputTimer;
        private Player               controlledPlayer;
        private List<ComputerPlayer> computerPlayers;
        private BufferedImage        surface;
        private double               ratio;
        private int                  viewedHeight;
        private int                  viewedWidth;
        private java.awt.Point       fixed;
        private java.awt.Point       dragStarted;
        private final int fullScreenWidth;
        private final int fullScreenHeight;

        private boolean isDoubleClick(MouseEvent me) {
            return me.getClickCount() > 1;
        }

        @Override
        public void setTurboMode(boolean toggle) {
            turboModeEnabled = toggle;

            if (toggle) {
                tick = 50;
            } else {
                tick = 250;
            }
        }

        private void writeSnapshots() {
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

                try {
                    ImageIO.write(bi, "png", outputfile);

                    System.out.println("Wrote scene to " + outputfile.getAbsolutePath());
                } catch (IOException e) {
                    System.out.println("Could not write to " + outputfile.getAbsolutePath());
                }
            }
        }

        private java.awt.Point screenPointToSurfacePoint(java.awt.Point point) {
            double xScale = (double)viewedWidth / (double)getWidth();
            double yScale = (double)viewedHeight / (double)getHeight();

            int surfaceX = (int)(fixed.x + point.x * xScale);
            int surfaceY = (int)(fixed.y + point.y * yScale);

            return new java.awt.Point(surfaceX, surfaceY);
        }

        private java.awt.Point surfacePointToScreenPoint(java.awt.Point point) {
            double xScale = viewedWidth / getWidth();
            double yScale = viewedHeight / getHeight();

            int screenX = (int)((point.x - fixed.x) / xScale);
            int screenY = (int)((point.y - fixed.y) / yScale);

            return new java.awt.Point(screenX, screenY);
        }

        private void startRoad(Point p) throws Exception {
            System.out.println("Starting road");

            if (!roadPoints.isEmpty()) {
                throw new Exception("Already building a road, can't start a new one");
            }

            addRoadPoint(p);
        }
        
        private void buildRoad(List<Point> wayPoints) throws Exception {
            System.out.println("Building road (" + wayPoints + ")");
            
            Road r = map.placeRoad(controlledPlayer, wayPoints);
            
            recorder.recordPlaceRoad(r);

            roadPoints = new ArrayList<>();
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
        public void keyPressed(KeyEvent ke) {
        }

        @Override
        public void keyTyped(KeyEvent ke) {            
            char key = ke.getKeyChar();
            boolean keepPreviousKeys = false;
            
            previousKeys += key;
            
            try {
                if (previousKeys.equals(" ")) {
                    System.out.println("Toggle show available spots");

                    showAvailableSpots = !showAvailableSpots;

                    repaint();
                } else if (previousKeys.equals("+")) {
                    addBonusResourcesForPlayer(controlledPlayer);
                } else if (previousKeys.equals("A")) {
                    if (map.isBuildingAtPoint(selectedPoint) && !controlledPlayer.isWithinBorder(selectedPoint)) {
                        attackHouse(selectedPoint);
                    }
                } else if (previousKeys.equals("bak")) {
                    placeBuilding(controlledPlayer, BAKERY, selectedPoint);
                    setState(IDLE);
                    repaint();
                } else if (previousKeys.equals("bar")) {
                    placeBuilding(controlledPlayer, BARRACKS, selectedPoint);
                    setState(IDLE);
                    repaint();
                } else if (previousKeys.equals("c")) {
                    placeBuilding(controlledPlayer, COALMINE, selectedPoint);
                    setState(IDLE);
                    repaint();
                } else if (previousKeys.equals("d")) {
                    placeBuilding(controlledPlayer, DONKEY_FARM, selectedPoint);
                    setState(IDLE);
                    repaint();
                } else if (previousKeys.equals("D")) {
                    recorder.printRecordingOnConsole();
                } else if (previousKeys.equals("fi")) {
                    placeBuilding(controlledPlayer, FISHERY, selectedPoint);
                    setState(IDLE);
                    repaint();
                } else if (previousKeys.equals("fore")) {
                    placeBuilding(controlledPlayer, HouseType.FORESTER, selectedPoint);
                    setState(IDLE);
                    repaint();
                } else if (previousKeys.equals("fort")) {
                    placeBuilding(controlledPlayer, FORTRESS, selectedPoint);
                    setState(IDLE);
                    repaint();
                } else if (key == 'd') {
                    recorder.printRecordingOnConsole();
                } else if (previousKeys.equals("fa")) {
                    placeBuilding(controlledPlayer, FARM, selectedPoint);
                    setState(IDLE);
                    repaint();
                } else if (previousKeys.equals("go")) {
                    placeBuilding(controlledPlayer, GOLDMINE, selectedPoint);
                    setState(IDLE);
                    repaint();
                } else if (previousKeys.equals("gr")) {
                    placeBuilding(controlledPlayer, GRANITEMINE, selectedPoint);
                    repaint();
                    setState(IDLE);
                } else if (previousKeys.equals("gu")) {
                    placeBuilding(controlledPlayer, GUARD_HOUSE, selectedPoint);
                    setState(IDLE);
                    repaint();
                } else if (previousKeys.equals("i")) {
                    placeBuilding(controlledPlayer, IRONMINE, selectedPoint);
                    repaint();
                    setState(IDLE);
                } else if (previousKeys.equals("mil")) {
                    placeBuilding(controlledPlayer, MILL, selectedPoint);
                    repaint();
                    setState(IDLE);
                } else if (previousKeys.equals("min")) {
                    placeBuilding(controlledPlayer, MINT, selectedPoint);
                    repaint();
                    setState(IDLE);
                } else if (previousKeys.equals("p")) {
                    placeBuilding(controlledPlayer, PIG_FARM, selectedPoint);
                    setState(IDLE);
                    repaint();
                } else if (previousKeys.equals("S")) {
                    writeSnapshots();
                } else if (previousKeys.equals("sa")) {
                    placeBuilding(controlledPlayer, SAWMILL, selectedPoint);
                    setState(IDLE);
                    repaint();
                } else if (previousKeys.equals("sl")) {
                    placeBuilding(controlledPlayer, SLAUGHTER_HOUSE, selectedPoint);
                    repaint();
                    setState(IDLE);
                } else if (previousKeys.equals("T")) {
                    setTurboMode(!turboModeEnabled);
                } else if (previousKeys.equals("wa")) {
                    placeBuilding(controlledPlayer, WATCH_TOWER, selectedPoint);
                    setState(IDLE);
                    repaint();
                } else if (previousKeys.equals("we")) {
                    placeBuilding(controlledPlayer, WELL, selectedPoint);
                    setState(IDLE);
                    repaint();
                } else if (previousKeys.equals("wo")) {
                    placeBuilding(controlledPlayer, WOODCUTTER, selectedPoint);
                    setState(IDLE);
                    repaint();
                } else if (key == 'q') {
                    placeBuilding(controlledPlayer, QUARRY, selectedPoint);
                    setState(IDLE);
                    repaint();
                } else if (key == 'R') {
                    resetGame();
                    repaint();
                } else if (key == 'X') {
                    recorder.record("\n\n\n\n/*   MARKER   */\n");
                    System.out.println("Added marker to api recording");
                } else if (key == KeyEvent.VK_ESCAPE) {
                    System.out.println("Resetting state to idle");

                    setState(IDLE);

                    previousKeys = "";

                    repaint();
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
            }
        }

        private void placeOpponent(Player opponent, GameMap map) throws Exception {

            /* Place opponent's headquarter */
            Building headquarter1 = new Headquarter(opponent);
            Point point1 = new Point(45, 21);
            map.placeBuilding(headquarter1, point1);

            recorder.recordPlaceBuilding(headquarter1, HEADQUARTER, point1);

            /* Place barracks for opponent */
            Point point3 = new Point(29, 21);
            Building barracks0 = new Barracks(opponent);
            map.placeBuilding(barracks0, point3);

            recorder.recordPlaceBuilding(barracks0, BARRACKS, point3);

            /* Connect the barracks with the headquarter */
            Road road = map.placeAutoSelectedRoad(opponent, barracks0.getFlag(), headquarter1.getFlag());

            recorder.recordPlaceRoad(road);
        }

        @Override
        public void attackHouse(Point selectedPoint) {

            try {
                /* Find building to attack */
                Building buildingToAttack = map.getBuildingAtPoint(selectedPoint);
                
                /* Record?
                
                assertEquals(map.getBuildingAtPoint(<point-with-my-name>), <building-with-my-name>)
                
                */
                
                /* Order attack */
                int attackers = controlledPlayer.getAvailableAttackersForBuilding(buildingToAttack);
                
                controlledPlayer.attack(buildingToAttack, attackers);
                
                recorder.recordAttack(controlledPlayer, buildingToAttack);
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

        private void zoomOut(int notches) {
            double deltaX = notches * ratio;
            double deltaY = notches;
            double halfDeltaX = (int)(deltaX / 2);
            double halfDeltaY = (int)(deltaY / 2);

            if (viewedWidth + deltaX >= surface.getWidth() || 
                viewedHeight + deltaY >= surface.getHeight()) {
                return;
            }

            if (fixed.x + viewedWidth + halfDeltaX >= surface.getWidth() &&
                fixed.x > deltaX) {
                fixed.x -= deltaX;
            } else if (fixed.x > halfDeltaX) {
                fixed.x -= halfDeltaX;
            }

            viewedWidth += deltaX;

            if (fixed.y + viewedHeight + halfDeltaY >= surface.getHeight() &&
                fixed.y > deltaY) {
                fixed.y -= deltaY;
            } else if (fixed.y > halfDeltaY) {
                fixed.y -= halfDeltaY;
            }

            viewedHeight += deltaY;

            repaint();
        }

        private void zoomIn(int notches) {
            double deltaX = Math.abs(notches * ratio);
            double deltaY = Math.abs(notches);
            double halfDeltaX = (int)(deltaX / 2);
            double halfDeltaY = (int)(deltaY / 2);

            if (viewedWidth - deltaX < 100 || viewedHeight - deltaY < 100) {
                return;
            }

            viewedWidth -= deltaX;
            viewedHeight -= deltaY;

            fixed.x += halfDeltaX;
            fixed.y += halfDeltaY;

            repaint();
        }

        private Point surfacePointToGamePoint(java.awt.Point surfacePoint) {

            /* Go from surface coordinates to game points */
            double px = (double) surfacePoint.x / gameDrawer.getScaleX();
            double py = (double) (surface.getHeight() - surfacePoint.y) / gameDrawer.getScaleY();

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

        class ClearInputTask extends TimerTask {

            @Override
            public void run() {
                previousKeys = "";

                setTitle("Settlers 2");
            }
        }
        
        @Override
        public void keyReleased(KeyEvent ke) {
        }

        @Override
        public void placeFlag(Point p) throws Exception {
            System.out.println("Placed flag at " + p);

            Flag flag = map.placeFlag(controlledPlayer, p);
            
            recorder.recordPlaceFlag(flag, p);
        }

        private void cancelRoadBuilding() {
            roadPoints = new ArrayList<>();
        }

        @Override
        public void placeBuilding(HouseType type, Point p) throws Exception {
            placeBuilding(controlledPlayer, type, p);
        }

        private void placeBuilding(Player player, HouseType houseType, Point p) throws Exception {

            System.out.println("Placing " + houseType + " at " + selectedPoint);

            Building b = BuildingFactory.createBuilding(player, houseType);

            if (b == null) {
                throw new Exception("Can't build " + houseType);
            }

            map.placeBuilding(b, p);

            recorder.recordPlaceBuilding(b, houseType, p);
        }

        private void resetGame() throws Exception {
            computerPlayers.clear();

            recorder.clear();

            setState(IDLE);
            
            recorder.recordComment("Starting new game");

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
            map = new GameMap(players, widthInPoints, heightInPoints);

            sidePanel.setMap(map);

            recorder.recordNewGame(players, widthInPoints, heightInPoints);

            /* Create the terrain */
            createInitialTerrain(map);

            /* Place player to be controlled */
            placeBuilding(controlledPlayer, HEADQUARTER, new Point(5, 5));

            /* Place the opponent */
            placeOpponent(player1, map);

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

        private GameMap map;

        private int widthInPoints;
        private int heightInPoints;

        public GameCanvas(int w, int h) throws Exception {
            super();

            System.out.println("Create game map");

            computerPlayers    = new ArrayList<>();
            widthInPoints      = w;
            heightInPoints     = h;
            tick               = 250;
            turboModeEnabled   = false;
            roadPoints         = new ArrayList<>();
            showAvailableSpots = false;
            recorder           = new ApiRecorder();
            clearInputTimer    = new Timer("Clear input timer");
            fixed              = new java.awt.Point(0, 0);
            dragStarted        = new java.awt.Point(0, 0);

            /* Create the initial game board */
            resetGame();

            /* Calculate the full screen size */
            fullScreenWidth = Toolkit.getDefaultToolkit().getScreenSize().width;
            fullScreenHeight = Toolkit.getDefaultToolkit().getScreenSize().height;

            /* Create the surface to draw on */
            int surfaceWidth = map.getWidth() * 30;
            int surfaceHeight = map.getHeight() * 30;
            surface = Utils.createOptimizedBufferedImage(surfaceWidth, surfaceHeight, turboModeEnabled);

            /* Create the game drawer with the right size of the playing field */
            gameDrawer = new GameDrawer(map, surfaceWidth, surfaceHeight);

            /* Zoom out fully */
            fixed.x = 0;
            fixed.y = 0;

            viewedWidth = surface.getWidth();
            viewedHeight = surface.getHeight();

            /* Create listener */
            setFocusable(true);
            requestFocusInWindow();

            addMouseListener(this);
            addKeyListener(this);
            addMouseWheelListener(this);

            previousKeys = "";
            
            /* Add action listeners */
            addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent evt) {                    
                    //gameDrawer.recalculateScale(getWidth(), getHeight());

                    repaint();
                }
            });

            addMouseMotionListener(new MouseMotionAdapter() {

                @Override
                public void mouseMoved(MouseEvent me) {

                    /* Get point the mouse hovers over on the game map */
                    Point point = screenPointToGamePoint(me.getPoint());

                    /* Update the hovering spot in the game drawer */
                    gameDrawer.setHoveringSpot(point);

                    repaint();
                }

                @Override
                public void mouseDragged(MouseEvent me) {

                    /* Get the new point in surface coordinates */
                    java.awt.Point dropPoint = screenPointToSurfacePoint(me.getPoint());

                    /* Determine the change from the original point */
                    int changeX = dropPoint.x - dragStarted.x;
                    int changeY = dropPoint.y - dragStarted.y;

                    int newFixedX = fixed.x - changeX;
                    int newFixedY = fixed.y - changeY;

                    if (newFixedX >= 0 && newFixedX + viewedWidth <= surface.getWidth()) {
                        fixed.x = newFixedX;
                    }

                    if (newFixedY >= 0 && newFixedY + viewedHeight <= surface.getHeight()) {
                        fixed.y = newFixedY;
                    }

                    repaint();
                }
            });

            /* Initial state is IDLE */
            state = IDLE;
            
            /* Start game tick */
            Thread t;
            t = new Thread(new Runnable() {
                
                @Override
                public void run() {
            
                    while (true) {

                        /* Keep count of how long the game has run */
                        recorder.recordTick();

                        /* Call any computer players if available */
                        for (ComputerPlayer computerPlayer : computerPlayers) {
                            try {
                                computerPlayer.turn();
                            } catch (Exception ex) {

                                /* Print exception and backtrace */
                                Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);

                                /* Print API recording to make the fault reproducable */
                                recorder.printRecordingOnConsole();

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
                                writeSnapshots();
                                
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
                            recorder.printRecordingOnConsole();

                            System.exit(1);
                        }

                        /* Re-draw the scene */
                        repaint();

                        /* Wait until next step */
                        try {
                            Thread.sleep(tick);
                        } catch (InterruptedException ex) {
                            Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            });

            t.start();

            setVisible(true);

            requestFocus();

            /* Calculate the ratio */
            ratio = fullScreenWidth / fullScreenHeight;

            /* Set the default zoom */
            viewedWidth  = (int)(600 * ratio);
            viewedHeight = 600;

            /* Set the anchor of which part of the full scene to draw */
            fixed.x = 0;
            fixed.y = surfaceHeight - viewedHeight;

        }

        @Override
        public void paintComponent(Graphics graphics) {

            /* Get graphics for the surface */
            Graphics2D sg = surface.createGraphics();

            /* Limit the drawing the the region that is actually visible */
            sg.setClip(fixed.x, fixed.y, viewedWidth, viewedHeight);

            /* Draw the scene on the surface */
            gameDrawer.drawScene(sg, controlledPlayer, selectedPoint, roadPoints, showAvailableSpots);

            /* Draw a part of the surface on the screen */
/*            BufferedImage subSurface = surface.getSubimage(fixed.x, fixed.y, viewedWidth, viewedHeight);
            graphics.drawImage(subSurface, 0, 0, getWidth(), getHeight(), null);*/
            graphics.drawImage(surface, fixed.x, fixed.y, 
                                        fixed.x + viewedWidth, fixed.y + viewedHeight, 
                                        0, 0, 
                                        getWidth(), getHeight(), null);
        }

        private Point screenPointToGamePoint(java.awt.Point screenPoint) {

            /* Translate screen point to a point on the surface */
            java.awt.Point surfacePoint = screenPointToSurfacePoint(screenPoint);

            /* Translate from the surface point to a point in the game */
            Point gamePoint = surfacePointToGamePoint(surfacePoint);
            
            return gamePoint;
        }
        
        @Override
        public void mouseClicked(MouseEvent me) {

            /* Translate the screen coordinates to a point in the game */
            Point p = screenPointToGamePoint(me.getPoint());

            try {
                if (isDoubleClick(me)) {
                    if (state == IDLE || state == POINT_SELECTED) {
                        if (map.isFlagAtPoint(p)) {
                            startRoad(p);

                            setState(BUILDING_ROAD);
                        } else {
                            placeFlag(p);
                            
                            setState(IDLE);
                        }
                    } else if (state == BUILDING_ROAD) {                        
                        placeFlag(p);

                        if (!p.equals(roadPoints.get(roadPoints.size() - 1))) {
                            addRoadPoint(p);
                        }

                        buildRoad(roadPoints);
        
                        setState(IDLE);
                    }
                }

                if (!isDoubleClick(me)) {
                    if (state == BUILDING_ROAD) {

                        if (map.isFlagAtPoint(p)) {

                            addRoadPoint(p);
                            buildRoad(roadPoints);
                            
                            setState(IDLE);
                        } else if (!map.isRoadAtPoint(p)) {
                            addRoadPoint(p);
                        }
                    } else if (state == IDLE) {
                        selectPoint(p);
                        
                        setState(POINT_SELECTED);
                    } else if (state == POINT_SELECTED) {
                        if (me.getSource().equals(this)) {
                            selectPoint(p);

                            setState(POINT_SELECTED);
                        }
                    }
                }
                repaint();
            } catch (Exception ex) {
                System.out.println("Exception at single click: " + ex);
                ex.printStackTrace();
                
                cancelRoadBuilding();
                setState(IDLE);
                repaint();
            }
        }

        @Override
        public void mousePressed(MouseEvent me) {

            /* Remember the point in case this is the start of dragging operation
               NOTE: The point is adjusted to the surface */
            dragStarted = screenPointToSurfacePoint(me.getPoint());
        }

        @Override
        public void mouseReleased(MouseEvent me) {
        }

        @Override
        public void mouseEntered(MouseEvent me) {
        }

        @Override
        public void mouseExited(MouseEvent me) {
        }

        @Override
        public void reset() {
            try {
                resetGame();
            } catch (Exception ex) {
                Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        @Override
        public void dumpRecording() {
            recorder.printRecordingOnConsole();
        }

        @Override
        public void startRoadCommand(Point selectedPoint) {
            if (state != POINT_SELECTED) {
                return;
            }
            
            try {
                startRoad(selectedPoint);
                
                state = BUILDING_ROAD;
            } catch (Exception ex) {
                Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        private void placeWaterOnMap(Point p1, Point p2, Point p3, GameMap map) throws Exception {        
            Tile tile = map.getTerrain().getTile(p1, p2, p3);

            tile.setVegetationType(WATER);

            recorder.recordSetTileVegetation(p1, p2, p3, WATER);
        }

        private void placeMountainHexagonOnMap(Point p, GameMap map) throws Exception {
            placeMountainOnTile(p, p.left(), p.upLeft(), map);
            placeMountainOnTile(p, p.upLeft(), p.upRight(), map);
            placeMountainOnTile(p, p.upRight(), p.right(), map);
            placeMountainOnTile(p, p.right(), p.downRight(), map);
            placeMountainOnTile(p, p.downRight(), p.downLeft(), map);
            placeMountainOnTile(p, p.downLeft(), p.left(), map);            
        }
        
        private void placeMountainOnTile(Point p1, Point p2, Point p3, GameMap map) throws Exception {
            Tile tile = map.getTerrain().getTile(p1, p2, p3);

            tile.setVegetationType(MOUNTAIN);
            
            recorder.recordSetTileVegetation(p1, p2, p3, MOUNTAIN);
        }

        @Override
        public void removeFlagCommand(Point selectedPoint) throws Exception {
            Flag flag = map.getFlagAtPoint(selectedPoint);
            try {
                map.removeFlag(flag);
            } catch (Exception e) {
                System.out.println("  EXCEPTION DURING FLAG REMOVAL " + e);
            }
            
            recorder.recordRemoveFlag(flag);
        }

        @Override
        public void removeHouseCommand(Point selectedPoint) throws Exception {
            Building b = map.getBuildingAtPoint(selectedPoint);
            
            System.out.println("Removing " + b);
            
            try {
                b.tearDown();
            } catch (Exception e) {
                System.out.println("  EXCEPTION DURING HOUSE REMOVAL " + e);
            }

            recorder.recordTearDown(b);
        }

        @Override
        public void removeRoadAtPoint(Point selectedPoint) throws Exception {
            Road r = map.getRoadAtPoint(selectedPoint);
            
            try {
                map.removeRoad(r);
            } catch (Exception e) {
                System.out.println("  EXCEPTION DURING REMOVE ROAD " + e);
            }
            
            recorder.recordRemoveRoad(r);
        }

        @Override
        public void callGeologist(Point selectedPoint) throws Exception {
            Flag flag = map.getFlagAtPoint(selectedPoint);
            
            flag.callGeologist();
            
            recorder.recordCallGeologistFromFlag(flag);
        }

        private void surroundPointWithMineral(Point p, Material material, GameMap map) throws Exception {
            for (Tile t : map.getTerrain().getSurroundingTiles(p)) {
                t.setAmountMineral(material, LARGE);
            }
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

        private void createInitialTerrain(GameMap map) throws Exception {
            /* The default vegetation is grass */

            /* Create a small lake */
            Point lakeCenter = new Point(10, 4);

            placeWaterOnMap(lakeCenter, lakeCenter.left(), lakeCenter.upLeft(), map);
            placeWaterOnMap(lakeCenter, lakeCenter.upLeft(), lakeCenter.upRight(), map);
            placeWaterOnMap(lakeCenter, lakeCenter.upRight(), lakeCenter.right(), map);
            placeWaterOnMap(lakeCenter, lakeCenter.right(), lakeCenter.downRight(), map);
            placeWaterOnMap(lakeCenter, lakeCenter.downRight(), lakeCenter.downLeft(), map);
            placeWaterOnMap(lakeCenter, lakeCenter.downLeft(), lakeCenter.left(), map);

            /* Create a small mountain */
            Point p = new Point(5, 13);
            Point p2 = new Point(8, 14);
            Point p3 = new Point(5, 15);
            placeMountainHexagonOnMap(p, map);
            placeMountainHexagonOnMap(p2, map);
            placeMountainHexagonOnMap(p3, map);

            /* Put gold at mountain */
            surroundPointWithMineral(p, GOLD, map);
            surroundPointWithMineral(p2, GOLD, map);
            surroundPointWithMineral(p3, GOLD, map);

            /* Create a small mountain */
            Point p4 = new Point(8, 16);
            Point p5 = new Point(11, 17);
            Point p6 = new Point(8, 18);
            placeMountainHexagonOnMap(p4, map);
            placeMountainHexagonOnMap(p5, map);
            placeMountainHexagonOnMap(p6, map);

            /* Put coal at mountain */
            surroundPointWithMineral(p4, COAL, map);
            surroundPointWithMineral(p5, COAL, map);
            surroundPointWithMineral(p6, COAL, map);

            /* Place stones */
            Point stonePoint = new Point(12, 12);

            Stone stone0 = map.placeStone(stonePoint);
            Stone stone1 = map.placeStone(stonePoint.downRight());

            recorder.recordPlaceStone(stone0, stonePoint);
            recorder.recordPlaceStone(stone1, stonePoint.downRight());

            /* Place forest */
            Point point0 = new Point(20, 4);
            Point point1 = new Point(22, 6);
            Point point2 = new Point(24, 4);
            Point point3 = new Point(21, 5);

            List<Tree> smallForest = new LinkedList<>();

            smallForest.add(map.placeTree(point0));
            smallForest.add(map.placeTree(point0.right()));
            smallForest.add(map.placeTree(point1));
            smallForest.add(map.placeTree(point1.right()));
            smallForest.add(map.placeTree(point2));
            smallForest.add(map.placeTree(point2.right()));
            smallForest.add(map.placeTree(point3));
            smallForest.add(map.placeTree(point3.right()));

            recorder.recordPlaceTrees(smallForest);
        }

        @Override
        public void mouseWheelMoved(MouseWheelEvent mwe) {

            try {
                int notches = mwe.getWheelRotation();
                if (notches < 0) {
                    //gameDrawer.zoomIn(notches);
                    zoomIn(notches);
                } else {
                    //gameDrawer.zoomOut(notches);
                    zoomOut(notches);
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
            
            recorder.recordCallScoutFromFlag(flag);
        }
    }

    public static void main(String[] args) {

        try {
            /* Create the game window */
            App app = new App();
        } catch (Exception ex) {
            Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(1);
        }
    }
}
