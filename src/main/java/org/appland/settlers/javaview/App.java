package org.appland.settlers.javaview;

import java.awt.BasicStroke;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Stroke;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.swing.*;
import static org.appland.settlers.javaview.App.GameCanvas.HouseType.HEADQUARTER;
import static org.appland.settlers.javaview.App.GameCanvas.HouseType.WOODCUTTER;
import static org.appland.settlers.javaview.App.GameCanvas.UiState.BUILDING_ROAD;
import static org.appland.settlers.javaview.App.GameCanvas.UiState.IDLE;
import static org.appland.settlers.javaview.App.GameCanvas.UiState.POINT_SELECTED;
import org.appland.settlers.model.Building;
import org.appland.settlers.model.Flag;
import org.appland.settlers.model.GameMap;
import org.appland.settlers.model.Headquarter;
import org.appland.settlers.model.Point;
import org.appland.settlers.model.Road;
import org.appland.settlers.model.Size;
import static org.appland.settlers.model.Size.LARGE;
import static org.appland.settlers.model.Size.MEDIUM;
import org.appland.settlers.model.Terrain;
import org.appland.settlers.model.Tile;
import org.appland.settlers.model.Woodcutter;

public class App {

    static class GameCanvas extends Canvas implements MouseListener, KeyListener {

        private UiState            state;
        private List<Point>        roadPoints;
        private boolean            showAvailableSpots;
        private String             apiRecording;
        private Point              selectedPoint;
        private Map<Flag, String>  flagNames;
        private Map<Point, String> pointNames;
        
        private boolean isDoubleClick(MouseEvent me) {
            return me.getClickCount() > 1;
        }

        private Point screenToPoint(int x, int y) {
            double px = (double) x / (double) scaleX;
            double py = (double) (getHeight() - y) / (double) scaleY;

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

        private String recordPoint(Point p) {
            if (pointNames.containsKey(p)) {
                return pointNames.get(p);
            } else {
                return "new Point(" + p.x +", " + p.y + ")";
            }
        }
        
        private void buildRoad(List<Point> wayPoints) throws Exception {
            System.out.println("Building road (" + wayPoints + ")");
            
            map.placeRoad(wayPoints);
            
            apiRecording = apiRecording + "map.placeRoad(";
            
            boolean firstRun = true;
            
            for (Point p : wayPoints) {
                if (firstRun) {
                    firstRun = false;
                    
                    apiRecording += recordPoint(p);
                    
                    continue;
                }
                
                apiRecording += ", " + recordPoint(p);
            }
            
            apiRecording = apiRecording + ");\n";

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

                drawScaledLine(g, previous, current);

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

                drawScaledLine(g, previous, p);

                previous = p;
            }
        
            g.setStroke(oldStroke);
        }
        
        private void drawLastSelectedPoint(Graphics graphics) {
            Point selected = roadPoints.get(roadPoints.size() - 1);

            graphics.setColor(Color.RED);

            drawScaledFilledOval(graphics, selected, 7, 7);
        }

        private void drawScaledLine(Graphics graphics, Point p1, Point p2) {
            graphics.drawLine(toScreenX(p1), toScreenY(p1), toScreenX(p2), toScreenY(p2));
        }

        private void drawScaledFilledOval(Graphics graphics, Point p, int w, int h) {
            graphics.fillOval(toScreenX(p), toScreenY(p), w, h);
        }

        private void drawScaledRect(Graphics g, Point p, int i, int i0) {
            g.drawRect(toScreenX(p), toScreenY(p), i, i0);
        }

        private void drawScaledOval(Graphics g, Point p, int i, int i0) {
            g.drawOval(toScreenX(p), toScreenY(p), i, i0);
        }

        private void fillScaledRect(Graphics2D g, Point p, int w, int h) {
            g.fillRect(toScreenX(p), toScreenY(p), w, h);
        }

        private void fillScaledOval(Graphics2D g, Point p, int w, int h) {
            g.fillOval(toScreenX(p), toScreenY(p), w, h);
        }
        
        private void drawFlags(Graphics2D g) {
            g.setColor(Color.BLACK);
            
            for (Flag f : map.getFlags()) {
                fillScaledRect(g, f.getPosition(), 3, 3);
            }
        }

        private void drawPossibleRoadConnections(Graphics2D g, Point point) {
            g.setColor(Color.GREEN);
            
            for (Point p : map.getPossibleAdjacentRoadConnectionsIncludingEndpoints(point)) {
                if (map.isFlagAtPoint(p)) {
                    continue;
                }
                
                fillScaledRect(g, p, 3, 3);
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
            if (ke.getKeyChar() == ' ') {
                System.out.println("Toggle show available spots");
                
                showAvailableSpots = !showAvailableSpots;
                
                repaint();
            } else if (ke.getKeyChar() == 'd') {
                System.out.println("--------------------------------------");
                System.out.println(apiRecording);
                System.out.println("--------------------------------------");
            } else if (ke.getKeyChar() == 'w' && state == POINT_SELECTED) {
                try {
                    placeBuilding(WOODCUTTER, selectedPoint);
                    
                    setState(IDLE);
                    
                    repaint();
                } catch (Exception ex) {
                    System.out.println("Exception while building woodcutter: " + ex);

                    setState(IDLE);
                }
            } else if (ke.getKeyChar() == 'X') {
                apiRecording += "\n\n\n\n/*   MARKER   */\n";
                System.out.println("Added marker to api recording");
            } else if (ke.getKeyChar() == KeyEvent.VK_ESCAPE) {
                System.out.println("Resetting state to idle");
                
                setState(IDLE);

                repaint();
            } else if (ke.getKeyChar() == 'R') {
                try {
                    resetGame();
                    repaint();
                } catch (Exception ex) {
                    System.out.println("Failed to reset game. Exiting. Exception: " + ex);
                    System.exit(1);
                }
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

        private void placeFlag(Point p) throws Exception {
            Flag f = new Flag(p);                            
                            
            System.out.println("Placed flag at " + p);

            map.placeFlag(f);
            
            String pointName = registerPoint(p);
            String flagName  = registerFlag(f);
            
            apiRecording += pointName + " = new Point(" + p.x + ", " + p.y + ");\n";
            apiRecording += flagName  + " = new Flag(" + pointName + ");\n";
            
            apiRecording += "map.placeFlag(" + flagName + ");\n\n";
        }

        private void drawAvailableFlag(Graphics2D g, Point p) {
            g.setColor(Color.ORANGE);

            fillScaledRect(g, p, 2, 2);
        }
        

        private void drawAvailableHouse(Graphics2D g, Point key, Size value) {
            int height = 3;
            
            g.setColor(Color.YELLOW);
            
            if (value == MEDIUM) {
                height = 5;
            } else if (value == LARGE) {
                height = 9;
            }
            
            fillScaledRect(g, key, 3, height);
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

        private void placeBuilding(HouseType houseType, Point p) throws Exception {
            Building b = null;
            String newHouse = "";
            
            System.out.println("Placing " + houseType + " at " + selectedPoint);
            
            switch (houseType) {
            case WOODCUTTER:
                b = new Woodcutter();
                newHouse = "new Woodcutter()";
                break;
            }    
        
            if (b == null) {
                throw new Exception("Can't build " + houseType);
            }

            map.placeBuilding(b, p);
            
            apiRecording += "map.placeBuilding(" + newHouse + ", " + recordPoint(p) + ");\n";
        }

        private void drawSelectedPoint(Graphics2D g) {
            g.setColor(Color.BLUE);
            
            fillScaledOval(g, selectedPoint, 4, 4);
        }

        private void resetGame() throws Exception {
            System.out.println("Resetting game");
            
            setState(IDLE);
            
            map = new GameMap(widthInPoints, heightInPoints);
            
            placeBuilding(HEADQUARTER, new Point(5, 5));
            
            apiRecording = "";
            flagNames.clear();
            pointNames.clear();
        }

        private void setState(UiState uiState) {
            System.out.println("State change: " + state + " --> " + uiState);
            state = uiState;            
        }

        private String registerFlag(Flag f) {
            String name = "flag" + flagNames.size();
            
            flagNames.put(f, name);
            
            return name;
        }

        private String registerPoint(Point p) {
            String name = "point" + pointNames.size();
            
            pointNames.put(p, name);
            
            return name;
        }

        enum UiState {
            IDLE, BUILDING_ROAD, POINT_SELECTED
        }

        enum HouseType {
            WOODCUTTER, HEADQUARTER
        }

        private Image loadImage(String file) {
            try {
                URL imgURL = getClass().getClassLoader().getResource(file);
                ImageIcon imageIcon = new ImageIcon(imgURL);
            
                return imageIcon.getImage();
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

		/* Draw upwards triangles */
		if (y + 1 < heightInPoints) {
		    int startX;

		    if (rowOffsetFlip) {
			startX = 0;
		    } else {
			startX = 1;
		    }

		    for (x = startX; x < widthInPoints; x++) {
			Point p1 = new Point(x, y);
			Point p2 = new Point(x + 2, y);
			Point p3 = new Point(x + 1, y + 1);

			Tile t = terrain.getTile(p1, p2, p3);

			drawTile(g, t, p1, p2, p3);
		    }
		}

		/* Draw downwards triangles */
		if (y - 1 > 0) {
		    int startX;

		    if (rowOffsetFlip) {
			startX = 0;
		    } else {
			startX = 1;
		    }

		    for (x = startX; x < widthInPoints; x++) {
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

            fillScaledTriangle(g, p1, p2, p3);
        }

        private void fillScaledTriangle(Graphics2D g, Point p1, Point p2, Point p3) {

	    Path2D.Double triangle = new Path2D.Double();
	    triangle.moveTo(toScreenX(p1), toScreenY(p1));
	    triangle.lineTo(toScreenX(p2), toScreenY(p2));
	    triangle.lineTo(toScreenX(p3), toScreenY(p3));
	    triangle.closePath();
	    g.fill(triangle);
        }

        GameMap map;

        int widthInPoints;
        int heightInPoints;
        List<Point> grid;
        int scaleX;
        int scaleY;
	BufferedImage terrain;

        public GameCanvas() {
        }

        public void initGame(int w, int h) throws Exception {
            System.out.println("Create game map");

            widthInPoints = w;
            heightInPoints = h;
            roadPoints = new ArrayList<>();
            showAvailableSpots = false;
            apiRecording = "";
            flagNames    = new HashMap<>();
            pointNames   = new HashMap<>();

            scaleX = 500 / widthInPoints;
            scaleY = 500 / heightInPoints;

            /* Create the initial game board */
            map = new GameMap(widthInPoints, heightInPoints);

	    terrain = createTerrainTexture(500, 500);

            apiRecording = apiRecording + "GameMap map = new GameMap(" + w + ", " + h + ");\n";
            
            Headquarter hq = new Headquarter();
            
            Point hqPoint = new Point(5, 5);

            map.placeBuilding(hq, hqPoint);

            apiRecording = apiRecording + "map.placeBuilding(new Headquarter(), new Point(" + 5 + ", " + 5 + "));\n";
            
            grid = buildGrid(widthInPoints, heightInPoints);
            
            /* Create listener */
            addMouseListener(this);
            addKeyListener(this);
            addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent evt) {
                    
                    scaleX = getWidth() / widthInPoints;
                    scaleY = getHeight() / heightInPoints;
                    
                    terrain = createTerrainTexture(getWidth(), getHeight());
                    
                    repaint();
                }
            });
            
            /* Initial state is IDLE */
            state = IDLE;
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
        public void paint(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics;

	    if (terrain != null) {
		g.drawImage(terrain, 0, 0, getWidth(), getHeight(), 0, 0, terrain.getWidth(), terrain.getHeight(), null);
	    }

            drawGrid(g);

            drawRoads(g);
            
            drawFlags(g);

            drawHouses(g);

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
                drawScaledRect(graphics, p, 2, 2);
            }
        }

        private int toScreenX(Point p) {
            return p.x * scaleX;
        }

        private int toScreenY(Point p) {
            return getHeight() - p.y * scaleY;
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

        private void drawHouse(Graphics2D graphics, Building b) {
            Point p = b.getPosition();

            graphics.setColor(Color.BLACK);

            fillScaledRect(graphics, p, 15, 15);
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
                        selectedPoint = p;
                        
                        setState(POINT_SELECTED);
                        
                        System.out.println("Selected " + p);
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
    }

    public static void main(String[] args) {

        GameCanvas canvas = new GameCanvas();

        try {
            canvas.initGame(20, 20);
        } catch (Exception e) {
            System.out.println(e);
        }

        JFrame frame = new JFrame();

        frame.setSize(600, 500);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.getContentPane().add(canvas);

        frame.setVisible(true);
    }
}
