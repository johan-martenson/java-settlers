package org.appland.settlers.javaview;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import static java.lang.Math.abs;
import static java.lang.Math.ceil;
import static java.lang.Math.floor;
import static java.lang.Math.round;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import static org.appland.settlers.javaview.App.HouseType.BARRACKS;
import static org.appland.settlers.javaview.App.HouseType.FARM;
import static org.appland.settlers.javaview.App.HouseType.FORESTER;
import static org.appland.settlers.javaview.App.HouseType.HEADQUARTER;
import static org.appland.settlers.javaview.App.HouseType.QUARRY;
import static org.appland.settlers.javaview.App.HouseType.SAWMILL;
import static org.appland.settlers.javaview.App.HouseType.WELL;
import static org.appland.settlers.javaview.App.HouseType.WOODCUTTER;
import static org.appland.settlers.javaview.App.UiState.BUILDING_ROAD;
import static org.appland.settlers.javaview.App.UiState.IDLE;
import static org.appland.settlers.javaview.App.UiState.POINT_SELECTED;
import org.appland.settlers.model.Barracks;
import org.appland.settlers.model.Building;
import org.appland.settlers.model.Crop;
import org.appland.settlers.model.Farm;
import org.appland.settlers.model.Flag;
import org.appland.settlers.model.ForesterHut;
import org.appland.settlers.model.GameLogic;
import org.appland.settlers.model.GameMap;
import org.appland.settlers.model.Headquarter;
import org.appland.settlers.model.Point;
import org.appland.settlers.model.Quarry;
import org.appland.settlers.model.Road;
import org.appland.settlers.model.Sawmill;
import org.appland.settlers.model.Size;
import static org.appland.settlers.model.Size.LARGE;
import static org.appland.settlers.model.Size.MEDIUM;
import static org.appland.settlers.model.Size.SMALL;
import org.appland.settlers.model.Stone;
import org.appland.settlers.model.Terrain;
import org.appland.settlers.model.Tile;
import org.appland.settlers.model.Tree;
import org.appland.settlers.model.Well;
import org.appland.settlers.model.Woodcutter;
import org.appland.settlers.model.Worker;

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
        WOODCUTTER, HEADQUARTER, FORESTER, SAWMILL, QUARRY, FARM, BARRACKS, WELL
    }

    
    class GameCanvas extends JPanel implements MouseListener, KeyListener, CommandListener {

        private final Color BORDER_COLOR = Color.BLACK;
        
        private UiState            state;
        private List<Point>        roadPoints;
        private boolean            showAvailableSpots;
        private Point              selectedPoint;
        private Map<Flag, String>  flagNames;
        private Map<Point, String> pointNames;
        private GameLogic          gameLogic;
        private Image              houseImage;
        private ScaledDrawer       drawer;
        private ApiRecorder        recorder;
        private int                tick;
        private char               previousKey;
        
        private boolean isDoubleClick(MouseEvent me) {
            return me.getClickCount() > 1;
        }

        @Override
        public void setTurboMode(boolean toggle) {
            if (toggle) {
                tick = 50;
            } else {
                tick = 250;
            }
        }
        
        private Point screenToPoint(int x, int y) {
            double px = (double) x / (double) drawer.getScaleX();
            double py = (double) (getHeight() - y) / (double) drawer.getScaleY();

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

        private void drawPreliminaryRoad(Graphics2D g) {
            Point previous = null;

            Stroke oldStroke = g.getStroke();
            g.setStroke(new BasicStroke(3));
            
            for (Point current : roadPoints) {
                if (previous == null) {
                    previous = current;

                    continue;
                }

                g.setColor(Color.YELLOW);

                drawer.drawScaledLine(g, previous, current);

                previous = current;
            }
            
            g.setStroke(oldStroke);
        }

        private void drawRoad(Graphics2D g, Road r) {
            List<Point> wayPoints = r.getWayPoints();
            
            g.setColor(Color.ORANGE);
            Stroke oldStroke = g.getStroke();
            
            g.setStroke(new BasicStroke(4));
            
            Point previous = null;

            for (Point p : wayPoints) {
                if (previous == null) {
                    previous = p;
                    continue;
                }

                drawer.drawScaledLine(g, previous, p);

                previous = p;
            }
        
            g.setStroke(oldStroke);
        }
        
        private void drawLastSelectedPoint(Graphics graphics) {
            Point selected = roadPoints.get(roadPoints.size() - 1);

            graphics.setColor(Color.RED);

            drawer.drawScaledFilledOval(graphics, selected, 7, 7);
        }

        
        private void drawFlags(Graphics2D g) {
            for (Flag f : map.getFlags()) {
                Point p = f.getPosition();
                
                if (!f.getStackedCargo().isEmpty()) {
                    g.setColor(Color.RED);
                    g.fillRect((int)(p.x*drawer.getScaleX()) - 2, getHeight() - (int)(p.y*drawer.getScaleY()) - 5, 5, 5);
                }
    
                g.setColor(Color.BLACK);
            
                drawer.fillScaledRect(g, f.getPosition(), 3, 3);
            }
        }

        private void drawPossibleRoadConnections(Graphics2D g, Point point) {
            g.setColor(Color.GREEN);
            
            for (Point p : map.getPossibleAdjacentRoadConnectionsIncludingEndpoints(point)) {
                if (map.isFlagAtPoint(p)) {
                    continue;
                }
                
                drawer.fillScaledRect(g, p, 3, 3);
            }
        }

        private Point getLastSelectedWayPoint() {
            return roadPoints.get(roadPoints.size() - 1);
        }

        @Override
        public void keyTyped(KeyEvent ke) {
        }

        @Override
        public void keyPressed(KeyEvent ke) {            
            System.out.println("KEY PRESSED");
            
            char key = ke.getKeyChar();
            
            if (key == ' ') {
                System.out.println("Toggle show available spots");
                
                showAvailableSpots = !showAvailableSpots;
                
                repaint();
            } else if (key == 'd') {
                recorder.printRecordingOnConsole();
            } else if (key == 'o') {
                if (previousKey == 'w') {
                    try {
                        placeBuilding(WOODCUTTER, selectedPoint);

                        setState(IDLE);

                        repaint();
                    } catch (Exception ex) {
                        System.out.println("Exception while building woodcutter: " + ex);

                        setState(IDLE);
                    }
                }
            } else if (key == 'e') {
                if (previousKey == 'w') {
                    try {
                        placeBuilding(WELL, selectedPoint);
                        repaint();
                    } catch (Exception ex) {
                        Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    setState(IDLE);
                }
            } else if (key == 'o') {
                if (previousKey == 'f') {
                    try {
                        placeBuilding(FORESTER, selectedPoint);
                        setState(IDLE);

                        repaint();
                    } catch (Exception ex) {
                        Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
                        setState(IDLE);
                    }
                }
            } else if (key == 'a') {
                if (previousKey == 'f') {
                    try {
                        placeBuilding(FARM, selectedPoint);
                        setState(IDLE);
                        
                        repaint();
                    } catch (Exception ex) {
                        Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
                        setState(IDLE);
                    }
                }
            } else if (key == 'b') {
                try {
                    placeBuilding(BARRACKS, selectedPoint);
                    setState(IDLE);
                    repaint();
                } catch (Exception ex) {
                    Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
                    setState(IDLE);
                }
            } else if (key == 's') {
                try {
                    placeBuilding(SAWMILL, selectedPoint);
                    setState(IDLE);
                    repaint();
                } catch (Exception ex) {
                    Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
                    setState(IDLE);
                }
            } else if (key == 'q') {
                try {
                    placeBuilding(QUARRY, selectedPoint);
                    setState(IDLE);
                    repaint();
                } catch (Exception ex) {
                    Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
                    setState(IDLE);
                }
            } else if (key == 'X') {
                recorder.record("\n\n\n\n/*   MARKER   */\n");
                System.out.println("Added marker to api recording");
            } else if (key == KeyEvent.VK_ESCAPE) {
                System.out.println("Resetting state to idle");
                
                setState(IDLE);

                repaint();
            } else if (key == 'R') {
                try {
                    resetGame();
                    repaint();
                } catch (Exception ex) {
                    System.out.println("Failed to reset game. Exiting. Exception: " + ex);
                    System.exit(1);
                }
            }

            previousKey = ke.getKeyChar();
        }

        @Override
        public void keyReleased(KeyEvent ke) {
        }

        private void drawAvailableSpots(Graphics2D g) {
            Map<Point, Size> houses = map.getAvailableHousePoints();
            List<Point> flags = map.getAvailableFlagPoints();
            
            for (Entry<Point, Size> pair : houses.entrySet()) {
                drawAvailableHouse(g, pair.getKey(), pair.getValue());
            }
        
            for (Point p : flags) {
                if (houses.keySet().contains(p)) {
                    continue;
                }
            
                drawAvailableFlag(g, p);
            }
        }

        @Override
        public void placeFlag(Point p) throws Exception {
            Flag f = new Flag(p);                            
                            
            System.out.println("Placed flag at " + p);

            map.placeFlag(f);
            
            recorder.recordPlaceFlag(f, p);
        }

        private void drawAvailableFlag(Graphics2D g, Point p) {
            g.setColor(Color.ORANGE);

            drawer.fillScaledRect(g, p, 2, 2);
        }
        

        private void drawAvailableHouse(Graphics2D g, Point key, Size value) {
            int height = 3;
            
            g.setColor(Color.YELLOW);
            
            if (value == MEDIUM) {
                height = 5;
            } else if (value == LARGE) {
                height = 9;
            }
            
            drawer.fillScaledRect(g, key, 3, height);
        }

        private String recordPointList(List<Point> wayPoints) {
            String result = "Arrays.asList(new Point[] {";

            boolean firstRun = true;
            
            for (Point p : wayPoints) {
                if (firstRun) {
                    result = result + "new Point(" + p.x + ", " + p.y + ")";
                    firstRun = false;
                } else {
                    result = result + ", new Point(" + p.x + ", " + p.y + ")";
                }
            }
            
            result = result + "})";

            return result;
        }

        private void cancelRoadBuilding() {
            roadPoints = new ArrayList<>();
        }

        @Override
        public void placeBuilding(HouseType houseType, Point p) throws Exception {
            Building b = null;
            String newHouse = "";
            
            System.out.println("Placing " + houseType + " at " + selectedPoint);
            
            switch (houseType) {
            case WOODCUTTER:
                b = new Woodcutter();
                newHouse = "new Woodcutter()";
                break;
            case HEADQUARTER:
                b = new Headquarter();
                newHouse = "new Headquarter()";
                break;
            case FORESTER:
                b = new ForesterHut();
                newHouse = "new ForesterHut()";
                break;
            case SAWMILL:
                b = new Sawmill();
                newHouse = "new Sawmill()";
                break;
            case QUARRY:
                b = new Quarry();
                newHouse = "new Quarry()";
                break;
            case FARM:
                b = new Farm();
                newHouse = "new Farm()";
                break;
            case BARRACKS:
                b = new Barracks();
                newHouse = "new Barracks()";
                break;
            case WELL:
                b = new Well();
                newHouse = "new Well()";
                break;
            }    
        
            if (b == null) {
                throw new Exception("Can't build " + houseType);
            }

            map.placeBuilding(b, p);
            
            recorder.recordPlaceBuilding(b, newHouse, p);
        }

        private void drawSelectedPoint(Graphics2D g) {
            g.setColor(Color.BLUE);
            
            drawer.fillScaledOval(g, selectedPoint, 4, 4);
        }

        private void resetGame() throws Exception {
            System.out.println("Resetting game");
            
            setState(IDLE);
            
            map = new GameMap(widthInPoints, heightInPoints);
            
            placeBuilding(HEADQUARTER, new Point(5, 5));
            
            recorder.clear();
            flagNames.clear();
            pointNames.clear();
        }

        private void setState(UiState uiState) {
            System.out.println("State change: " + state + " --> " + uiState);
            state = uiState;            
        }

        private void drawPersons(Graphics2D g) {
            for (Worker w : map.getAllWorkers()) {
                if (w.isInsideBuilding()) {
                    continue;
                }

                drawPerson(g, w);
            }
        }

        private void drawPerson(Graphics2D g, Worker w) {
            g.setColor(Color.BLACK);
            
            if (w.isTraveling()) {
                Point next = null;
                
                try {
                    next = w.getNextPoint();
                } catch (Exception ex) {
                    Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
                }
                
                Point last = w.getLastPoint();
                
                if (next == null) {
                    drawer.fillScaledOval(g, last, 5, 10);
                } else {
                    int percent = w.getPercentageOfDistanceTraveled();
                    
                    double actualX = last.x + (next.x - last.x)*((double)percent/(double)100);
                    double actualY = last.y + (next.y - last.y)*((double)percent/(double)100);
                    
                    g.fillOval((int)(actualX*drawer.getScaleX()) - 4, getHeight() - (int)(actualY*drawer.getScaleY()) - 10, 5, 15);
                    
                    if (w.getCargo() != null ) {
                        g.setColor(Color.RED);
                        g.fillRect((int)(actualX*drawer.getScaleX()) -2, getHeight() - (int)(actualY*drawer.getScaleY()) - 6, 5, 5);
                    }
                }
            } else {
                drawer.fillScaledOval(g, w.getPosition(), 5, 10);
            }
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
                sidePanel.clearInfo();
            }

            sidePanel.setSelectedPoint(p);
            
            requestFocus();
        }

        private Image loadImage(String file) {
            try {
                final URL imgURL = Thread.currentThread().getContextClassLoader().getResource(file); //getClass().getClassLoader().getResource(file);
                return Toolkit.getDefaultToolkit().getImage(imgURL);
            } catch (Exception e) {
                System.out.print("Error while loading image " + file + ": " + e);
            }

            return null;
        }


        private BufferedImage createOffScreenImage(int width, int height) {
            return new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        }

	private BufferedImage createTerrainTexture(int w, int h) {
	    BufferedImage image = createOffScreenImage(w, h);
	    Set<Tile> handled = new HashSet<>();
	    Terrain terrain = map.getTerrain();
	    Graphics2D g = image.createGraphics();

	    g.setBackground(Color.WHITE);

	    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

	    boolean rowOffsetFlip = true;

	    /* Place all possible flag points in the list */
	    int x, y;

	    for (y = 0; y < heightInPoints; y++) {

                int startX;
                if (rowOffsetFlip) {
                    startX = 0;
                } else {
                    startX = 1;
                }

                /* Draw upwards triangles */
		if (y < heightInPoints) {
                    for (x = startX; x < widthInPoints; x+= 2) {
			Point p1 = new Point(x, y);
			Point p2 = new Point(x + 2, y);
			Point p3 = new Point(x + 1, y + 1);

			Tile t = terrain.getTile(p1, p2, p3);

			drawTile(g, t, p1, p2, p3);
		    }
		}

		/* Draw downwards triangles */
		if (y > 0) {
		    for (x = startX; x < widthInPoints; x += 2) {
			Point p1 = new Point(x, y);
			Point p2 = new Point(x + 2, y);
			Point p3 = new Point(x + 1, y - 1);

			Tile t = terrain.getTile(p1, p2, p3);

			drawTile(g, t, p1, p2, p3);
		    }
		}

		rowOffsetFlip = !rowOffsetFlip;
	    }

	    return image;
	}

        private void drawTile(Graphics2D g, Tile t, Point p1, Point p2, Point p3) {
            switch (t.getVegetationType()) {
            case GRASS:
                g.setColor(Color.GREEN);
                break;
            case SWAMP:
                g.setColor(Color.GRAY);
                break;
            case WATER:
                g.setColor(Color.BLUE);
                break;
            default:
                g.setColor(Color.GRAY);
            }

            drawer.fillScaledTriangle(g, p1, p2, p3);
        }

        GameMap map;

        int widthInPoints;
        int heightInPoints;
        List<Point> grid;
	BufferedImage terrain;

        public GameCanvas() {
            super();
        }

        public void initGame(int w, int h) throws Exception {
            System.out.println("Create game map");

            widthInPoints  = w;
            heightInPoints = h;
            tick           = 250;
            roadPoints = new ArrayList<>();
            showAvailableSpots = false;
            flagNames    = new HashMap<>();
            pointNames   = new HashMap<>();
            gameLogic    = new GameLogic();

            drawer       = new ScaledDrawer(500, 500, w, h);
            recorder     = new ApiRecorder();

            /* Create the initial game board */
            map = new GameMap(widthInPoints, heightInPoints);

	    terrain = createTerrainTexture(500, 500);
            
            houseImage = loadImage("house-sketched.png");

            recorder.record("GameMap map = new GameMap(" + w + ", " + h + ");\n");
            
            Headquarter hq = new Headquarter();
            
            Point hqPoint = new Point(5, 5);

            map.placeBuilding(hq, hqPoint);

            recorder.recordPlaceBuilding(hq, "new Headquarter()", hqPoint);

            Point stonePoint = new Point(12, 12);
            
            Stone stone0 = map.placeStone(stonePoint);
            Stone stone1 = map.placeStone(stonePoint.downRight());
            
            recorder.recordPlaceStone(stone0, stonePoint);
            recorder.recordPlaceStone(stone1, stonePoint.downRight());
            
            grid = buildGrid(widthInPoints, heightInPoints);
            
            /* Create listener */
            setFocusable(true);
            requestFocusInWindow();
            
            addMouseListener(this);
            addKeyListener(this);
            
            previousKey = ' ';
            
            addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent evt) {
                    
                    drawer.recalculateScale(getWidth(), getHeight());
                    
                    terrain = createTerrainTexture(getWidth(), getHeight());
                    
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
            
                    int count = 0;
                    while (true) {
                        recorder.recordTick();
                        
                        if (count == 10) {
                            count = 0;

                            try {
                                gameLogic.gameLoop(map);
                            } catch (Exception ex) {
                                Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        } else {
                            count++;
                            map.stepTime();
                            sidePanel.update();
                        }

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
        
        List<Point> buildGrid(int width, int height) {
            java.util.List<Point> result = new ArrayList<>();
            boolean rowFlip = true;
            boolean columnFlip;

            /* Place all possible flag points in the list */
            int x, y;
            for (y = 1; y < height; y++) {
                columnFlip = rowFlip;

                for (x = 1; x < width; x++) {
                    if (columnFlip) {
                        result.add(new Point(x, y));
                    }

                    columnFlip = !columnFlip;
                }

                rowFlip = !rowFlip;
            }

            return result;
        }

        @Override
        public void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics;

	    if (terrain != null) {
		g.drawImage(terrain, 0, 0, getWidth(), getHeight(), 0, 0, terrain.getWidth(), terrain.getHeight(), null);
	    }

            drawGrid(g);

            drawRoads(g);
            
            drawFlags(g);

            drawHouses(g);
            
            drawTrees(g);
            
            drawStones(g);
            
            drawCrops(g);
            
            drawPersons(g);

            drawBorders(g);
            
            if (showAvailableSpots) {
                drawAvailableSpots(g);
            }
            
            if (state == BUILDING_ROAD) {
                drawPreliminaryRoad(g);

                drawLastSelectedPoint(g);
                
                drawPossibleRoadConnections(g, getLastSelectedWayPoint());
            } else if (state == POINT_SELECTED) {
                drawSelectedPoint(g);
            }
        }

        private void drawGrid(Graphics graphics) {
            graphics.setColor(Color.GRAY);

            for (Point p : grid) {
                drawer.drawScaledRect(graphics, p, 2, 2);
            }
        }


        private void drawRoads(Graphics2D g) {
            List<Road> roads = map.getRoads();

            g.setColor(Color.ORANGE);

            for (Road r : roads) {
                drawRoad(g, r);
            }
        }

        private void drawHouses(Graphics2D graphics) {
            List<Building> houses = map.getBuildings();

            for (Building b : houses) {
                drawHouse(graphics, b);
            }
        }

        private void drawHouse(Graphics2D g, Building b) {
            Point p = b.getPosition();

            g.setColor(Color.BLACK);

            if (houseImage != null) {
                g.drawImage(houseImage, 
                        p.x*drawer.getScaleX()- 25, getHeight() - p.y*drawer.getScaleY() - 25, 
                        p.x*drawer.getScaleX() + 25, getHeight() - p.y*drawer.getScaleY() + 25, 
                        0, 0, houseImage.getWidth(null), houseImage.getHeight(null), null);
            } else {
                drawer.fillScaledRect(g, p, 15, 15);
            }

            String title = b.getClass().getSimpleName();
            
            if (b.getConstructionState() == Building.ConstructionState.UNDER_CONSTRUCTION) {
                title = "(" + title + ")";
            }
            
            g.drawString(title, p.x*drawer.getScaleX() - 30, getHeight() - (p.y*drawer.getScaleY()) - 30);
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
                        } else {
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
                this.repaint();
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

        private void drawTrees(Graphics2D g) {
            for (Tree t : map.getTrees()) {
                drawTree(g, t);
            }
        }

        private void drawTree(Graphics2D g, Tree t) {
            Point p = t.getPosition();

            int base = 5;
            int height = 30;
            
            if (t.getSize() == SMALL) {
                base = 2;
                height = 15;
            } else if (t.getSize() == MEDIUM) {
                base = 3;
                height = 24;
            }
            
            Path2D.Double triangle = new Path2D.Double();
            triangle.moveTo(drawer.toScreenX(p) - base, drawer.toScreenY(p));
            triangle.lineTo(drawer.toScreenX(p) + base, drawer.toScreenY(p));
            triangle.lineTo(drawer.toScreenX(p), drawer.toScreenY(p) - height   );
            triangle.closePath();
            g.fill(triangle);
        }

        private void drawStones(Graphics2D g) {
            for (Stone s : map.getStones()) {
                drawStone(g, s);
            }
        }

        private void drawStone(Graphics2D g, Stone s) {
            g.setColor(Color.DARK_GRAY);
            
            drawer.fillScaledRect(g, s.getPosition(), 15, 15);
        }

        private void drawCrops(Graphics2D g) {
            for (Crop c : map.getCrops()) {
                drawCrop(g, c);
            }
        }

        private void drawCrop(Graphics2D g, Crop c) {
            
            switch (c.getGrowthState()) {
            case JUST_PLANTED:
                g.setColor(new Color(88, 0xCC, 88));
                break;
            case HALFWAY:
                g.setColor(Color.yellow);
                break;
            case FULL_GROWN:
                g.setColor(Color.orange);
                break;
            case HARVESTED:
                g.setColor(new Color(0xAA, 0xCC, 55));
            }

            drawer.fillScaledOval(g, c.getPosition(), 20, 10, 10, 5);
            
            g.setColor(Color.BLACK);
            
            drawer.drawScaledOval(g, c.getPosition(), 20, 10, 10, 5);
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

        private void drawBorders(Graphics2D g) {
            g.setColor(BORDER_COLOR);

            for (Collection<Point> border : map.getBorders()) {
                Point previous = null;
                for (Point p : border) {
                    if (!isWithinScreen(p)) {
                        continue;
                    }

                    drawer.fillScaledOval(g, p, 4, 4, -2, -2);
                    previous = p;
                }
            }
        }

        private boolean isWithinScreen(Point p) {
            return p.x > 0 && p.x < widthInPoints && p.y > 0 && p.y < heightInPoints;
        }
    }

    public static void main(String[] args) {
        new App();
    }
}
