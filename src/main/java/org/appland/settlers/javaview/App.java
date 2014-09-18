package org.appland.settlers.javaview;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import static java.awt.Color.BLACK;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import static java.lang.Math.abs;
import static java.lang.Math.ceil;
import static java.lang.Math.floor;
import static java.lang.Math.round;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JPanel;
import static org.appland.settlers.javaview.App.HouseType.BAKERY;
import static org.appland.settlers.javaview.App.HouseType.BARRACKS;
import static org.appland.settlers.javaview.App.HouseType.COALMINE;
import static org.appland.settlers.javaview.App.HouseType.FARM;
import static org.appland.settlers.javaview.App.HouseType.FISHERY;
import static org.appland.settlers.javaview.App.HouseType.FORESTER;
import static org.appland.settlers.javaview.App.HouseType.GOLDMINE;
import static org.appland.settlers.javaview.App.HouseType.GRANITEMINE;
import static org.appland.settlers.javaview.App.HouseType.HEADQUARTER;
import static org.appland.settlers.javaview.App.HouseType.IRONMINE;
import static org.appland.settlers.javaview.App.HouseType.MILL;
import static org.appland.settlers.javaview.App.HouseType.QUARRY;
import static org.appland.settlers.javaview.App.HouseType.SAWMILL;
import static org.appland.settlers.javaview.App.HouseType.WELL;
import static org.appland.settlers.javaview.App.HouseType.WOODCUTTER;
import static org.appland.settlers.javaview.App.UiState.BUILDING_ROAD;
import static org.appland.settlers.javaview.App.UiState.IDLE;
import static org.appland.settlers.javaview.App.UiState.POINT_SELECTED;
import org.appland.settlers.model.Bakery;
import org.appland.settlers.model.Barracks;
import org.appland.settlers.model.Building;
import org.appland.settlers.model.CoalMine;
import org.appland.settlers.model.Crop;
import org.appland.settlers.model.Farm;
import org.appland.settlers.model.Fishery;
import org.appland.settlers.model.Flag;
import org.appland.settlers.model.ForesterHut;
import org.appland.settlers.model.GameMap;
import org.appland.settlers.model.GoldMine;
import org.appland.settlers.model.GraniteMine;
import org.appland.settlers.model.Headquarter;
import org.appland.settlers.model.IronMine;
import org.appland.settlers.model.Material;
import static org.appland.settlers.model.Material.GOLD;
import org.appland.settlers.model.Mill;
import org.appland.settlers.model.Point;
import org.appland.settlers.model.Quarry;
import org.appland.settlers.model.Road;
import org.appland.settlers.model.Sawmill;
import org.appland.settlers.model.Sign;
import org.appland.settlers.model.Size;
import static org.appland.settlers.model.Size.LARGE;
import static org.appland.settlers.model.Size.MEDIUM;
import static org.appland.settlers.model.Size.SMALL;
import org.appland.settlers.model.Stone;
import org.appland.settlers.model.Terrain;
import org.appland.settlers.model.Tile;
import static org.appland.settlers.model.Tile.Vegetation.MOUNTAIN;
import static org.appland.settlers.model.Tile.Vegetation.WATER;
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
        WOODCUTTER, HEADQUARTER, FORESTER, SAWMILL, QUARRY, FARM, BARRACKS, WELL,
        MILL, BAKERY, FISHERY, GOLDMINE, IRONMINE, COALMINE, GRANITEMINE
    }

    
    class GameCanvas extends JPanel implements MouseListener, KeyListener, CommandListener {

        private final Color BORDER_COLOR = Color.BLACK;
        
        private final Color FOG_OF_WAR_COLOR = Color.BLACK;
        
        private final Color POSSIBLE_WAYPOINT_COLOR = Color.ORANGE;
        
        private final Color SIGN_BACKGROUND_COLOR = new Color(0xCCAAAA);
        
        private final Color FLAG_POLE_COLOR = BLACK;
        
        private final Color FLAG_COLOR = Color.WHITE;

        private final Color WOOD_COLOR = new Color(0xBF8026);
        private final Color WHEAT_COLOR = Color.ORANGE;
        private final Color PLANCK_COLOR = Color.YELLOW;
        private final Color WATER_COLOR = Color.BLUE;
        private final Color FLOUR_COLOR = Color.WHITE;
        private final Color STONE_COLOR = Color.GRAY;
        private final Color FISH_COLOR = Color.DARK_GRAY;
        private final Color GOLD_COLOR = Color.YELLOW;
        private final Color IRON_COLOR = Color.RED;
        private final Color COAL_COLOR = Color.BLACK;
        
        private final Color MOUNTAIN_COLOR = Color.LIGHT_GRAY;
        private final Color GRASS_COLOR = Color.GREEN;
        
        private UiState            state;
        private List<Point>        roadPoints;
        private boolean            showAvailableSpots;
        private Point              selectedPoint;
        private Image              houseImage;
        private ScaledDrawer       drawer;
        private ApiRecorder        recorder;
        private int                tick;
        private String             previousKeys;
        
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
                    Color cargoColor = getColorForMaterial(f.getStackedCargo().get(f.getStackedCargo().size() - 1).getMaterial());
                    
                    g.setColor(cargoColor);
                    
                    g.fillRect((int)(p.x*drawer.getScaleX()) - 2, getHeight() - (int)(p.y*drawer.getScaleY()) - 5, 5, 5);
                }
    
                g.setColor(FLAG_COLOR);
                drawer.fillScaledRect(g, f.getPosition(), 7, 7, 0, -15);
                
                g.setColor(BLACK);
                drawer.drawScaledRect(g, f.getPosition(), 7, 7, 0, -15);
                
                g.setColor(FLAG_POLE_COLOR);
                drawer.fillScaledRect(g, f.getPosition(), 2, 15, -1, -15);
                
                g.setColor(Color.BLACK);
                drawer.fillScaledRect(g, f.getPosition(), 6, 3, -3, 0);
            }
        }

        private void drawPossibleRoadConnections(Graphics2D g, Point point) throws Exception {
            
            for (Point p : map.getPossibleAdjacentRoadConnectionsIncludingEndpoints(point)) {
                if (map.isFlagAtPoint(p)) {
                    continue;
                }
                
                g.setColor(POSSIBLE_WAYPOINT_COLOR);
                drawer.fillScaledOval(g, p, 10, 10, -5, -5);

                g.setColor(Color.DARK_GRAY);
                drawer.drawScaledOval(g, p, 10, 10, -5, -5);
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
            char key = ke.getKeyChar();
            boolean keepPreviousKeys = false;
            
            previousKeys += key;
            
            if (previousKeys.equals(" ")) {
                System.out.println("Toggle show available spots");
                
                showAvailableSpots = !showAvailableSpots;
                
                repaint();
            } else if (previousKeys.equals("bak")) {
                try {
                    placeBuilding(BAKERY, selectedPoint);
                } catch (Exception ex) {
                    Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
                }
                setState(IDLE);
                repaint();
            } else if (previousKeys.equals("bar")) {
                try {
                    placeBuilding(BARRACKS, selectedPoint);
                } catch (Exception ex) {
                    Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
                }                    
                setState(IDLE);
                repaint();
            } else if (previousKeys.equals("c")) {
                try {
                    placeBuilding(COALMINE, selectedPoint);
                } catch (Exception ex) {
                    Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
                }
                repaint();
                setState(IDLE);
            } else if (previousKeys.equals("fi")) {
                try {
                    placeBuilding(FISHERY, selectedPoint);
                } catch (Exception ex) {
                    Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
                }
                setState(IDLE);
                repaint();
            } else if (previousKeys.equals("fo")) {
                try {
                    placeBuilding(FORESTER, selectedPoint);
                } catch (Exception ex) {
                    Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
                }
                setState(IDLE);
                repaint();
            } else if (key == 'd') {
                recorder.printRecordingOnConsole();
            } else if (previousKeys.equals("fa")) {
                try {
                    placeBuilding(FARM, selectedPoint);
                } catch (Exception ex) {
                    Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
                }
                setState(IDLE);
                repaint();
            } else if (previousKeys.equals("go")) {
                try {
                    placeBuilding(GOLDMINE, selectedPoint);
                } catch (Exception e) {
                    Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, e);
                }
                setState(IDLE);
                repaint();
            } else if (previousKeys.equals("gr")) {
                try {
                    placeBuilding(GRANITEMINE, selectedPoint);
                } catch (Exception ex) {
                    Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
                }
                repaint();
                setState(IDLE);
            } else if (previousKeys.equals("i")) {
                try {
                    placeBuilding(IRONMINE, selectedPoint);
                } catch (Exception ex) {
                    Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
                }
                repaint();
                setState(IDLE);
            } else if (previousKeys.equals("m")) {
                try {
                    placeBuilding(MILL, selectedPoint);
                    repaint();
                } catch (Exception ex) {
                    Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
                }
                setState(IDLE);
            } else if (previousKeys.equals("we")) {
                try {
                    placeBuilding(WELL, selectedPoint);
                } catch (Exception ex) {
                    Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
                }
                setState(IDLE);
                repaint();
            } else if (previousKeys.equals("wo")) {
                try {
                    placeBuilding(WOODCUTTER, selectedPoint);
                } catch (Exception ex) {
                    System.out.println("Exception while building woodcutter: " + ex);
                }
                setState(IDLE);
                repaint();
            } else if (key == 'q') {
                try {
                    placeBuilding(QUARRY, selectedPoint);
                } catch (Exception ex) {
                    Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
                }
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
            } else if (key == 's') {
                try {
                    placeBuilding(SAWMILL, selectedPoint);
                } catch (Exception ex) {
                    Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
                }
                setState(IDLE);
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
            }

            if (!keepPreviousKeys) {
                previousKeys = "";
                setTitle("Settlers 2");
            }
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
            }
        
            if (b == null) {
                throw new Exception("Can't build " + houseType);
            }

            map.placeBuilding(b, p);
            
            recorder.recordPlaceBuilding(b, houseType, p);
        }

        private void drawSelectedPoint(Graphics2D g) {
            g.setColor(Color.BLUE);
            
            drawer.fillScaledOval(g, selectedPoint, 4, 4);
        }

        private void resetGame() throws Exception {
            recorder.clear();

            setState(IDLE);
            
            recorder.recordComment("Starting new game");
            
            map = new GameMap(widthInPoints, heightInPoints);

            recorder.recordNewGame(widthInPoints, heightInPoints);
            
            createInitialTerrain(map);
            
            placeBuilding(HEADQUARTER, new Point(5, 5));            
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
            
            double actualX = w.getPosition().x;
            double actualY = w.getPosition().y;            

            if (w.isTraveling()) {
                Point next = null;
                
                try {
                    next = w.getNextPoint();
                } catch (Exception ex) {
                    Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
                }

                Point last = w.getLastPoint();
                
                if (next == null) {
                    actualX = last.x;
                    actualY = last.y;
                } else {
                    int percent = w.getPercentageOfDistanceTraveled();
                    
                    actualX = last.x + (next.x - last.x)*((double)percent/(double)100);
                    actualY = last.y + (next.y - last.y)*((double)percent/(double)100);
                }
            }

            g.fillOval((int)(actualX*drawer.getScaleX()) - 4, getHeight() - (int)(actualY*drawer.getScaleY()) - 10, 5, 15);

            if (w.getCargo() != null ) {
                Color cargoColor = getColorForMaterial(w.getCargo().getMaterial());
                
                g.setColor(cargoColor);
                g.fillRect((int)(actualX*drawer.getScaleX()) -2, getHeight() - (int)(actualY*drawer.getScaleY()) - 6, 5, 5);
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
                sidePanel.emptyPointSelected();
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

	private BufferedImage createTerrainTexture(int w, int h) throws Exception {
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
                g.setColor(GRASS_COLOR);
                break;
            case SWAMP:
                g.setColor(Color.GRAY);
                break;
            case WATER:
                g.setColor(Color.BLUE);
                break;
            case MOUNTAIN:
                g.setColor(MOUNTAIN_COLOR);
            default:
                g.setColor(Color.GRAY);
            }

            drawer.fillScaledTriangle(g, p1, p2, p3);
        }

        GameMap map;

        int widthInPoints;
        int heightInPoints;
        List<Point> grid;
	BufferedImage terrainImage;

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
            drawer       = new ScaledDrawer(500, 500, w, h);
            recorder     = new ApiRecorder();

            /* Create the initial game board */            
            resetGame();
            
            houseImage = loadImage("house-sketched.png");
	    terrainImage = createTerrainTexture(500, 500);
            grid = buildGrid(widthInPoints, heightInPoints);
            
            /* Create listener */
            setFocusable(true);
            requestFocusInWindow();
            
            addMouseListener(this);
            addKeyListener(this);
            
            previousKeys = "";
            
            addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent evt) {
                    
                    drawer.recalculateScale(getWidth(), getHeight());
                    
                    try {
                        terrainImage = createTerrainTexture(getWidth(), getHeight());
                    } catch (Exception ex) {
                        Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
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

	    if (terrainImage != null) {
		g.drawImage(terrainImage, 0, 0, getWidth(), getHeight(), 0, 0, terrainImage.getWidth(), terrainImage.getHeight(), null);
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
            
            drawSigns(g);
            
            if (showAvailableSpots) {
                drawAvailableSpots(g);
            }
            
            if (state == BUILDING_ROAD) {
                drawPreliminaryRoad(g);

                drawLastSelectedPoint(g);
                
                try {
                    drawPossibleRoadConnections(g, getLastSelectedWayPoint());
                } catch (Exception ex) {
                    Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else if (state == POINT_SELECTED) {
                drawSelectedPoint(g);
            }
            
            drawFogOfWar(g);
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
            
            if (b.underConstruction()) {
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
            
            drawer.fillScaledRect(g, s.getPosition(), 20, 20, -10, -10);
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

            drawer.fillScaledOval(g, c.getPosition(), 30, 15, -15, -7);
            
            g.setColor(Color.BLACK);
            
            drawer.drawScaledOval(g, c.getPosition(), 30, 15, -15, -7);
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

        private Color getColorForMaterial(Material material) {
            switch (material) {
            case WATER:
                return WATER_COLOR;
            case FLOUR:
                return FLOUR_COLOR;
            case STONE:
                return STONE_COLOR;
            case WHEAT:
                return WHEAT_COLOR;
            case PLANCK:
                return PLANCK_COLOR;
            case WOOD:
                return WOOD_COLOR;
            case FISH:
                return FISH_COLOR;
            case GOLD:
                return GOLD_COLOR;
            case IRON:
                return IRON_COLOR;
            case COAL:
                return COAL_COLOR;
            default:
                return Color.RED;
            }
        }

        private void drawFogOfWar(Graphics2D g) {
            
            /* Create the area with the whole screen */
            Area area = new Area(new Rectangle(0, 0, getWidth(), getHeight()));
            
            List<Point> fov = map.getFieldOfView();
            
            /* Create a polygon with the discovered land to cut out of the whole screen */
            Point lastPointInFov = fov.get(fov.size() - 1);
            
            Path2D.Double discoveredLand = new Path2D.Double();
            discoveredLand.moveTo(drawer.toScreenX(lastPointInFov), drawer.toScreenY(lastPointInFov));
            
            for (Point p : fov) {
                discoveredLand.lineTo(drawer.toScreenX(p), drawer.toScreenY(p));
            }

            /* Remove the discovered land from the fog of war area */
            area.subtract(new Area(discoveredLand));

            g.setColor(FOG_OF_WAR_COLOR);
            
            Shape oldClip = g.getClip();
            
            g.setClip(area);
            
            g.fillRect(0, 0, getWidth(), getHeight());
            
            g.setClip(oldClip);
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
            
            /* Place stones */
            Point stonePoint = new Point(12, 12);
            
            Stone stone0 = map.placeStone(stonePoint);
            Stone stone1 = map.placeStone(stonePoint.downRight());
            
            recorder.recordPlaceStone(stone0, stonePoint);
            recorder.recordPlaceStone(stone1, stonePoint.downRight());
        }
        
        private void placeWaterOnMap(Point p1, Point p2, Point p3, GameMap map) throws Exception {        
            Tile tile = map.getTerrain().getTile(p1, p2, p3);

            tile.setVegetationType(WATER);

            map.terrainIsUpdated();

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

            map.terrainIsUpdated();
            
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

            map.terrainIsUpdated();
        }

        private void drawSigns(Graphics2D g) {
            for (Sign s : map.getSigns()) {
                g.setColor(SIGN_BACKGROUND_COLOR);
                
                drawer.fillScaledRect(g, s.getPosition(), 8, 8);

                if (s.isEmpty()) {
                    continue;
                }
                
                switch (s.getType()) {
                case GOLD:
                    g.setColor(GOLD_COLOR);
                    break;
                case IRON:
                    g.setColor(IRON_COLOR);
                    break;
                case COAL:
                    g.setColor(COAL_COLOR);
                    break;
                case STONE:
                    g.setColor(STONE_COLOR);
                    break;
                case WATER:
                    g.setColor(WATER_COLOR);
                    break;
                default:
                    g.setColor(Color.RED);
                }

                switch (s.getSize()) {
                case LARGE:
                    drawer.fillScaledRect(g, s.getPosition(), 6, 6);
                    break;
                case MEDIUM:
                    drawer.fillScaledRect(g, s.getPosition(), 4, 4);
                    break;
                case SMALL:
                    drawer.fillScaledRect(g, s.getPosition(), 2, 2);
                    break;
                }
            }
        }
    }

    public static void main(String[] args) {
        new App();
    }
}
