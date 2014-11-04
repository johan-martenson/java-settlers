package org.appland.settlers.javaview;

import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import static java.lang.Math.abs;
import static java.lang.Math.ceil;
import static java.lang.Math.floor;
import static java.lang.Math.round;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JPanel;
import java.util.Timer;
import java.util.TimerTask;
import static org.appland.settlers.javaview.App.HouseType.BAKERY;
import static org.appland.settlers.javaview.App.HouseType.BARRACKS;
import static org.appland.settlers.javaview.App.HouseType.COALMINE;
import static org.appland.settlers.javaview.App.HouseType.DONKEY_FARM;
import static org.appland.settlers.javaview.App.HouseType.FARM;
import static org.appland.settlers.javaview.App.HouseType.FISHERY;
import static org.appland.settlers.javaview.App.HouseType.FORESTER;
import static org.appland.settlers.javaview.App.HouseType.GOLDMINE;
import static org.appland.settlers.javaview.App.HouseType.GRANITEMINE;
import static org.appland.settlers.javaview.App.HouseType.GUARD_HOUSE;
import static org.appland.settlers.javaview.App.HouseType.HEADQUARTER;
import static org.appland.settlers.javaview.App.HouseType.IRONMINE;
import static org.appland.settlers.javaview.App.HouseType.MILL;
import static org.appland.settlers.javaview.App.HouseType.MINT;
import static org.appland.settlers.javaview.App.HouseType.PIG_FARM;
import static org.appland.settlers.javaview.App.HouseType.QUARRY;
import static org.appland.settlers.javaview.App.HouseType.SAWMILL;
import static org.appland.settlers.javaview.App.HouseType.SLAUGHTER_HOUSE;
import static org.appland.settlers.javaview.App.HouseType.WELL;
import static org.appland.settlers.javaview.App.HouseType.WOODCUTTER;
import static org.appland.settlers.javaview.App.UiState.BUILDING_ROAD;
import static org.appland.settlers.javaview.App.UiState.IDLE;
import static org.appland.settlers.javaview.App.UiState.POINT_SELECTED;
import org.appland.settlers.model.Bakery;
import org.appland.settlers.model.Barracks;
import org.appland.settlers.model.Building;
import org.appland.settlers.model.CoalMine;
import org.appland.settlers.model.DonkeyFarm;
import org.appland.settlers.model.Farm;
import org.appland.settlers.model.Fishery;
import org.appland.settlers.model.Flag;
import org.appland.settlers.model.ForesterHut;
import org.appland.settlers.model.GameMap;
import org.appland.settlers.model.GoldMine;
import org.appland.settlers.model.GraniteMine;
import org.appland.settlers.model.GuardHouse;
import org.appland.settlers.model.Headquarter;
import org.appland.settlers.model.IronMine;
import org.appland.settlers.model.Material;
import static org.appland.settlers.model.Material.COAL;
import static org.appland.settlers.model.Material.GOLD;
import org.appland.settlers.model.Mill;
import org.appland.settlers.model.Mint;
import org.appland.settlers.model.PigFarm;
import org.appland.settlers.model.Point;
import org.appland.settlers.model.Quarry;
import org.appland.settlers.model.Road;
import org.appland.settlers.model.Sawmill;
import static org.appland.settlers.model.Size.LARGE;
import org.appland.settlers.model.SlaughterHouse;
import org.appland.settlers.model.Stone;
import org.appland.settlers.model.Tile;
import static org.appland.settlers.model.Tile.Vegetation.MOUNTAIN;
import static org.appland.settlers.model.Tile.Vegetation.WATER;
import org.appland.settlers.model.Well;
import org.appland.settlers.model.Woodcutter;

public class App extends JFrame {
    private SidePanel sidePanel;

    public App() {
        super();
        
        GameCanvas canvas = new GameCanvas();
        sidePanel = new SidePanel();

        sidePanel.setCommandListener(canvas);
        
        try {
            canvas.initGame(40, 40);
        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace(System.out);
            System.exit(1);
        }

        setSize(600, 500);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        getContentPane().add(canvas);
        getContentPane().add(sidePanel, BorderLayout.EAST);

        setVisible(true);
    }

    enum UiState {
        IDLE, BUILDING_ROAD, POINT_SELECTED
    }

    public enum HouseType {
        WOODCUTTER, HEADQUARTER, FORESTER, SAWMILL, QUARRY, FARM, BARRACKS, WELL,
        MILL, BAKERY, FISHERY, GOLDMINE, IRONMINE, COALMINE, GRANITEMINE, PIG_FARM,
        MINT, SLAUGHTER_HOUSE, DONKEY_FARM, GUARD_HOUSE
    }

    class GameCanvas extends JPanel implements MouseListener, KeyListener, CommandListener, MouseWheelListener {
        
        private final int INPUT_CLEAR_DELAY = 5000;
        
        private UiState     state;
        private List<Point> roadPoints;
        private boolean     showAvailableSpots;
        private Point       selectedPoint;
        private ApiRecorder recorder;
        private int         tick;
        private String      previousKeys;
        private GameDrawer  gameDrawer;
        private boolean     turboModeEnabled;
        private Timer       clearInputTimer;
        
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

        private Point screenToPoint(int x, int y) {
            double px = (double) x / (double) gameDrawer.getScaleX();
            double py = (double) (getHeight() - y) / (double) gameDrawer.getScaleY();

            int roundedX = (int) round(px);
            int roundedY = (int) round(py);

            if (abs(px - roundedX) < abs(py - Math.round(py))) {
                if ((roundedX + roundedY) % 2 != 0) {

                    if (roundedY > py) {
                        roundedY = (int) floor(py);
                    } else {
                        roundedY = (int) ceil(py);
                    }
                }
            } else {
                if ((roundedX + roundedY) % 2 != 0) {
                    if (roundedX > px) {
                        roundedX = (int) floor(px);
                    } else {
                        roundedX = (int) ceil(px);
                    }
                }
            }

            return new Point(roundedX, roundedY);
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
            
            Road r = map.placeRoad(wayPoints);
            
            recorder.recordPlaceRoad(r);

            roadPoints = new ArrayList<>();
        }

        private void addRoadPoint(Point point) {
            if (roadPoints.isEmpty()) {
                roadPoints.add(point);
            } else {
                Point last = getLastSelectedWayPoint();

                if (!point.isAdjacent(last)) {
                    List<Point> pointsBetween = map.findAutoSelectedRoad(last, point, roadPoints);
                    
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
                } else if (previousKeys.equals("bak")) {
                    placeBuilding(BAKERY, selectedPoint);
                    setState(IDLE);
                    repaint();
                } else if (previousKeys.equals("bar")) {
                    placeBuilding(BARRACKS, selectedPoint);
                    setState(IDLE);
                    repaint();
                } else if (previousKeys.equals("c")) {
                    placeBuilding(COALMINE, selectedPoint);
                    setState(IDLE);
                    repaint();
                } else if (previousKeys.equals("d")) {
                    placeBuilding(DONKEY_FARM, selectedPoint);
                    setState(IDLE);
                    repaint();
                } else if (previousKeys.equals("D")) {
                    recorder.printRecordingOnConsole();
                } else if (previousKeys.equals("fi")) {
                    placeBuilding(FISHERY, selectedPoint);
                    setState(IDLE);
                    repaint();
                } else if (previousKeys.equals("fo")) {
                    placeBuilding(FORESTER, selectedPoint);
                    setState(IDLE);
                    repaint();
                } else if (key == 'd') {
                    recorder.printRecordingOnConsole();
                } else if (previousKeys.equals("fa")) {
                    placeBuilding(FARM, selectedPoint);
                    setState(IDLE);
                    repaint();
                } else if (previousKeys.equals("go")) {
                    placeBuilding(GOLDMINE, selectedPoint);
                    setState(IDLE);
                    repaint();
                } else if (previousKeys.equals("gr")) {
                    placeBuilding(GRANITEMINE, selectedPoint);
                    repaint();
                    setState(IDLE);
                } else if (previousKeys.equals("gu")) {
                    placeBuilding(GUARD_HOUSE, selectedPoint);
                    setState(IDLE);
                    repaint();
                } else if (previousKeys.equals("i")) {
                    placeBuilding(IRONMINE, selectedPoint);
                    repaint();
                    setState(IDLE);
                } else if (previousKeys.equals("mil")) {
                    placeBuilding(MILL, selectedPoint);
                    repaint();
                    setState(IDLE);
                } else if (previousKeys.equals("min")) {
                    placeBuilding(MINT, selectedPoint);
                    repaint();
                    setState(IDLE);
                } else if (previousKeys.equals("p")) {
                    placeBuilding(PIG_FARM, selectedPoint);
                    setState(IDLE);
                    repaint();
                } else if (previousKeys.equals("sa")) {
                    placeBuilding(SAWMILL, selectedPoint);
                    setState(IDLE);
                    repaint();
                } else if (previousKeys.equals("sl")) {
                    placeBuilding(SLAUGHTER_HOUSE, selectedPoint);
                    repaint();
                    setState(IDLE);
                } else if (previousKeys.equals("T")) {
                    setTurboMode(!turboModeEnabled);
                } else if (previousKeys.equals("we")) {
                    placeBuilding(WELL, selectedPoint);
                    setState(IDLE);
                    repaint();
                } else if (previousKeys.equals("wo")) {
                    placeBuilding(WOODCUTTER, selectedPoint);
                    setState(IDLE);
                    repaint();
                } else if (key == 'q') {
                    placeBuilding(QUARRY, selectedPoint);
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
            Flag f = new Flag(p);                            
                            
            System.out.println("Placed flag at " + p);

            map.placeFlag(f);
            
            recorder.recordPlaceFlag(f, p);
        }

        private void cancelRoadBuilding() {
            roadPoints = new ArrayList<>();
        }

        @Override
        public void placeBuilding(HouseType houseType, Point p) throws Exception {
            Building b = null;
            
            System.out.println("Placing " + houseType + " at " + selectedPoint);
            
            switch (houseType) {
            case WOODCUTTER:
                b = new Woodcutter();
                break;
            case HEADQUARTER:
                b = new Headquarter();
                break;
            case FORESTER:
                b = new ForesterHut();
                break;
            case SAWMILL:
                b = new Sawmill();
                break;
            case QUARRY:
                b = new Quarry();
                break;
            case FARM:
                b = new Farm();
                break;
            case BARRACKS:
                b = new Barracks();
                break;
            case WELL:
                b = new Well();
                break;
            case MILL:
                b = new Mill();
                break;
            case BAKERY:
                b = new Bakery();
                break;
            case FISHERY:
                b = new Fishery();
                break;
            case GOLDMINE:
                b = new GoldMine();
                break;
            case IRONMINE:
                b = new IronMine();
                break;
            case COALMINE:
                b = new CoalMine();
                break;
            case GRANITEMINE:
                b = new GraniteMine();
                break;
            case PIG_FARM:
                b = new PigFarm();
                break;
            case MINT:
                b = new Mint();
                break;
            case SLAUGHTER_HOUSE:
                b = new SlaughterHouse();
                break;
            case DONKEY_FARM:
                b = new DonkeyFarm();
                break;
            case GUARD_HOUSE:
                b = new GuardHouse();
            }

            if (b == null) {
                throw new Exception("Can't build " + houseType);
            }

            map.placeBuilding(b, p);

            recorder.recordPlaceBuilding(b, houseType, p);
        }

        private void resetGame() throws Exception {
            recorder.clear();

            setState(IDLE);
            
            recorder.recordComment("Starting new game");
            
            map = new GameMap(widthInPoints, heightInPoints);

            recorder.recordNewGame(widthInPoints, heightInPoints);

            createInitialTerrain(map);

            placeBuilding(HEADQUARTER, new Point(5, 5));

            gameDrawer.setMap(map);

            repaint();
        }

        private void setState(UiState uiState) {
            System.out.println("State change: " + state + " --> " + uiState);
            state = uiState;            
        }

        private void selectPoint(Point p) {
            selectedPoint = p;
            
            if (map.isFlagAtPoint(p)) {
                try {
                    sidePanel.displayFlag(map.getFlagAtPoint(p));
                } catch (Exception ex) {
                    Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else if (map.isBuildingAtPoint(p)) {
                sidePanel.displayHouse(map.getBuildingAtPoint(p));
            } else if (map.isRoadAtPoint(p)) {
                sidePanel.displayRoad(map.getRoadAtPoint(p));
            } else {
                sidePanel.emptyPointSelected();
            }

            sidePanel.setSelectedPoint(p);
            
            requestFocus();
        }

        private GameMap map;

        int widthInPoints;
        int heightInPoints;

        public GameCanvas() {
            super();
        }

        public void initGame(int w, int h) throws Exception {
            System.out.println("Create game map");

            widthInPoints      = w;
            heightInPoints     = h;
            tick               = 250;
            turboModeEnabled   = false;
            roadPoints         = new ArrayList<>();
            showAvailableSpots = false;
            recorder           = new ApiRecorder();
            gameDrawer         = new GameDrawer(map, w, h, 40, 40);
            clearInputTimer    = new Timer("Clear input timer");

            /* Create the initial game board */
            resetGame();

            /* Create listener */
            setFocusable(true);
            requestFocusInWindow();

            addMouseListener(this);
            addKeyListener(this);
            addMouseWheelListener(this);

            previousKeys = "";
            
            addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent evt) {                    
                    gameDrawer.recalculateScale(getWidth(), getHeight());
                    
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
                        recorder.recordTick();

                        map.stepTime();
                        sidePanel.update();

                        repaint();
                        
                        try {
                            Thread.sleep(tick);
                        } catch (InterruptedException ex) {
                            Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            });

            t.start();
            
            requestFocus();
        }

        @Override
        public void paintComponent(Graphics graphics) {
            gameDrawer.drawScene((Graphics2D)graphics, selectedPoint, roadPoints, showAvailableSpots);
        }

        @Override
        public void mouseClicked(MouseEvent me) {
            Point p = screenToPoint(me.getX(), me.getY());

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
                        selectPoint(p);
                        
                        setState(POINT_SELECTED);
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
            
            recorder.recordCallScoutFromFlag(flag);
        }
    }

    public static void main(String[] args) {
        new App();
    }
}
