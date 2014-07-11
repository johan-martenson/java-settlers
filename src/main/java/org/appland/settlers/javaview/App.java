package org.appland.settlers.javaview;

import java.awt.BasicStroke;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import static java.lang.Math.abs;
import static java.lang.Math.ceil;
import static java.lang.Math.floor;
import static java.lang.Math.round;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import static org.appland.settlers.javaview.App.GameCanvas.UiState.BUILDING_ROAD;
import static org.appland.settlers.javaview.App.GameCanvas.UiState.IDLE;
import org.appland.settlers.model.Building;
import org.appland.settlers.model.Flag;
import org.appland.settlers.model.GameMap;
import org.appland.settlers.model.Headquarter;
import org.appland.settlers.model.Point;
import org.appland.settlers.model.Road;
import org.appland.settlers.model.Size;
import static org.appland.settlers.model.Size.LARGE;
import static org.appland.settlers.model.Size.MEDIUM;

public class App {

    static class GameCanvas extends Canvas implements MouseListener, KeyListener {

        private UiState state;
        private List<Point> roadPoints;
        private boolean showAvailableSpots;
        private String  apiRecording;
        
        private boolean isDoubleClick(MouseEvent me) {
            return me.getClickCount() > 1;
        }

        private Point screenToPoint(int x, int y) {
            double px = (double) x / (double) scaleX;
            double py = (double) (getHeight() - y) / (double) scaleY;

            int roundedX = (int) round(px);
            int roundedY = (int) round(py);

            System.out.println("BEFORE " + roundedX + " " + roundedY);
            System.out.println("REAL COORDS " + px + " " + py);
            if (abs(px - roundedX) < abs(py - Math.round(py))) {
                if ((roundedX + roundedY) % 2 != 0) {
                    System.out.println("NOT EVEN, X CLOSER");

                    if (roundedY > py) {
                        roundedY = (int) floor(py);
                    } else {
                        roundedY = (int) ceil(py);
                    }
                }
            } else {
                if ((roundedX + roundedY) % 2 != 0) {
                    System.out.println("NOT EVEN, Y CLOSER");

                    if (roundedX > px) {
                        roundedX = (int) floor(px);
                    } else {
                        roundedX = (int) ceil(px);
                    }
                }
            }

            System.out.println("AFTER " + roundedX + " " + roundedY);

            return new Point(roundedX, roundedY);
        }

        private void startRoad(Point p) throws Exception {
            if (!roadPoints.isEmpty()) {
                throw new Exception("Already building a road, can't start a new one");
            }

            addRoadPoint(p);
        }

        private String recordPoint(Point p) {
            return "new Point(" + p.x +", " + p.y + ")";
        }
        
        private void buildRoad(List<Point> wayPoints) throws Exception {
            System.out.println("BUILDING ROAD WITH POINTS " + wayPoints);
            
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
        
            state = IDLE;

            roadPoints = new ArrayList<>();
        }

        private void addRoadPoint(Point point) {
            if (roadPoints.isEmpty()) {
                roadPoints.add(point);
            } else {
                Point last = getLastSelectedWayPoint();

                if (!point.isAdjacent(last)) {
                    System.out.println("NOT ADJACENT");
                    
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

            System.out.println("DRAWING ROAD " + r);
            System.out.println("WAY POINTS " + wayPoints);
            
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
        
        private void drawFlags(Graphics2D g) {
            g.setColor(Color.BLACK);
            
            for (Flag f : map.getFlags()) {
                fillScaledRect(g, f.getPosition(), 3, 3);
            }
        }

        private void drawPossibleRoadConnections(Graphics2D g, Point point) {
            g.setColor(Color.GREEN);
            
            for (Point p : map.getPossibleAdjacentRoadConnections(point)) {
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
            System.out.println("ANY KEY PRESSED " + ke.getKeyChar());
            
            if (ke.getKeyChar() == ' ') {
                System.out.println("SPACE PRESSED");
                
                showAvailableSpots = !showAvailableSpots;
                
                repaint();
            } else if (ke.getKeyChar() == 'd') {
                System.out.println("--------------------------------------");
                System.out.println(apiRecording);
                System.out.println("--------------------------------------");
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
                            
            map.placeFlag(f);
            
            apiRecording = apiRecording + "map.placeFlag(new Flag(new Point(" + p.x + ", " + p.y + ")));\n";
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

        enum UiState {

            IDLE, BUILDING_ROAD
        }

        GameMap map;

        int width;
        int height;
        List<Point> grid;
        int scaleX;
        int scaleY;

        public GameCanvas() {
        }

        public void initGame(int w, int h) throws Exception {
            System.out.println("Create game map");

            width = w;
            height = h;
            roadPoints = new ArrayList<>();
            showAvailableSpots = false;
            apiRecording = "";

            /* Create the initial game board */
            map = new GameMap(width, height);

            apiRecording = apiRecording + "GameMap map = new GameMap(" + w + ", " + h + ");\n";
            
            Headquarter hq = new Headquarter();
            
            Point hqPoint = new Point(5, 5);

            map.placeBuilding(hq, hqPoint);

            apiRecording = apiRecording + "map.placeBuilding(new Headquarter(), new Point(" + 5 + ", " + 5 + "));\n";

            
            grid = buildGrid(width, height);

            scaleX = 500 / width;
            scaleY = 500 / height;

            /* Create listener */
            addMouseListener(this);
            addKeyListener(this);
            
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
            System.out.println("DRAWING ROADS");
            
            List<Road> roads = map.getRoads();

            g.setColor(Color.ORANGE);

            for (Road r : roads) {
                drawRoad(g, r);
            }
        }

        private void drawHouses(Graphics graphics) {
            List<Building> houses = map.getBuildings();

            for (Building b : houses) {
                drawHouse(graphics, b);
            }
        }

        private void drawHouse(Graphics graphics, Building b) {
            Point p = b.getPosition();

            graphics.setColor(Color.BLACK);

            drawScaledOval(graphics, p, 10, 10);
        }

        @Override
        public void mouseClicked(MouseEvent me) {
            Point p = screenToPoint(me.getX(), me.getY());
            
            if (isDoubleClick(me)) {
                System.out.println("Double click at" + me.getX() + ", " + me.getY() + " - " + p);
            } else {
                System.out.println("Single click at" + me.getX() + ", " + me.getY() + " - " + p);
            }

            try {

                if (isDoubleClick(me)) {
                    if (state == IDLE) {
                        if (map.isFlagAtPoint(p)) {
                            System.out.println("Starting road");

                            startRoad(p);

                            state = BUILDING_ROAD;
                        } else {
                            placeFlag(p);
                            
                            System.out.println("Placed flag at " + p);
                        }
                    } else if (state == BUILDING_ROAD) {
                        System.out.println("Placing flag at " + p);
                        
                        placeFlag(p);
                        
                        System.out.println("Building road " + roadPoints);
                        buildRoad(roadPoints);

                    }
                }

                if (!isDoubleClick(me) && state == BUILDING_ROAD) {

                    if (map.isFlagAtPoint(p)) {

                        roadPoints.add(p);
                        buildRoad(roadPoints);
                    } else {
                        addRoadPoint(p);
                    }

                    System.out.println("Added point to road " + roadPoints);
                }
                this.repaint();
            } catch (Exception ex) {
                Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
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
