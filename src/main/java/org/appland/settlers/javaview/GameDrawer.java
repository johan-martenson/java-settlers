package org.appland.settlers.javaview;

import java.awt.BasicStroke;
import java.awt.Color;
import static java.awt.Color.DARK_GRAY;
import static java.awt.Color.ORANGE;
import static java.awt.Color.RED;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.TexturePaint;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import static java.lang.Math.abs;
import static java.lang.Math.ceil;
import static java.lang.Math.floor;
import static java.lang.Math.round;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import org.appland.settlers.model.Building;
import org.appland.settlers.model.Crop;
import org.appland.settlers.model.Donkey;
import org.appland.settlers.model.Flag;
import org.appland.settlers.model.GameMap;
import org.appland.settlers.model.Headquarter;
import org.appland.settlers.model.Material;
import static org.appland.settlers.model.Material.COAL;
import static org.appland.settlers.model.Material.GOLD;
import org.appland.settlers.model.Military;
import org.appland.settlers.model.Player;
import org.appland.settlers.model.Point;
import org.appland.settlers.model.Projectile;
import org.appland.settlers.model.Road;
import org.appland.settlers.model.Sign;
import org.appland.settlers.model.Size;
import static org.appland.settlers.model.Size.LARGE;
import static org.appland.settlers.model.Size.MEDIUM;
import static org.appland.settlers.model.Size.SMALL;
import org.appland.settlers.model.Stone;
import org.appland.settlers.model.Storage;
import org.appland.settlers.model.Tile;
import org.appland.settlers.model.Tree;
import org.appland.settlers.model.WildAnimal;
import org.appland.settlers.model.Worker;

/**
 *
 * @author johan
 */
public class GameDrawer {

    private final Color POSSIBLE_WAYPOINT_COLOR = Color.ORANGE;
    private final Color SIGN_BACKGROUND_COLOR   = new Color(0xCCAAAA);
    private final Color FLAG_POLE_COLOR         = DARK_GRAY;
    private final Color WOOD_COLOR              = new Color(0xBF8026);
    private final Color WHEAT_COLOR             = Color.ORANGE;
    private final Color PLANCK_COLOR            = Color.YELLOW;
    private final Color WATER_COLOR             = Color.BLUE;
    private final Color FLOUR_COLOR             = Color.WHITE;
    private final Color STONE_COLOR             = Color.GRAY;
    private final Color FISH_COLOR              = Color.DARK_GRAY;
    private final Color GOLD_COLOR              = Color.YELLOW;
    private final Color IRON_COLOR              = Color.RED;
    private final Color COAL_COLOR              = Color.BLACK;
    private final Color PIG_COLOR               = Color.PINK;
    private final Color DONKEY_COLOR            = Color.DARK_GRAY;
    private final Color MOUNTAIN_COLOR          = Color.LIGHT_GRAY;
    private final Color GRASS_COLOR             = Color.GREEN;
    private final Color SMALL_ROAD_COLOR        = Color.ORANGE;
    private final Color MAIN_ROAD_COLOR         = Color.LIGHT_GRAY;
    private final Color HOVERING_COLOR          = Color.LIGHT_GRAY;
    private final Color SELECTED_POINT_COLOR    = Color.ORANGE;
    private final Color BEER_COLOR              = new Color(0x555555);
    private final Color COIN_COLOR              = Color.YELLOW;

    private final static double OVAL_HEIGHT_TO_WIDTH_RATIO = 0.8;
    private final static int MARKER_WIDTH = 50;
    private final static int MARKER_HEIGHT = (int)(MARKER_WIDTH * OVAL_HEIGHT_TO_WIDTH_RATIO);

    private final static int SPOT_WIDTH = 50;
    private final static int SPOT_HEIGHT = (int)(SPOT_WIDTH * OVAL_HEIGHT_TO_WIDTH_RATIO);

    private final static int CARGO_WIDTH = 35;
    private final static int CARGO_HEIGHT = 35;

    private final int AVAILABLE_SMALL_HOUSE_WIDTH   = 20;
    private final int AVAILABLE_SMALL_HOUSE_HEIGHT  = 20;
    private final int AVAILABLE_MEDIUM_HOUSE_WIDTH  = 20;
    private final int AVAILABLE_MEDIUM_HOUSE_HEIGHT = 20;
    private final int AVAILABLE_LARGE_HOUSE_WIDTH   = 20;
    private final int AVAILABLE_LARGE_HOUSE_HEIGHT  = 20;

    private final Color AVAILABLE_CONSTRUCTION_COLOR = Color.ORANGE;

    private final int MAIN_ROAD_WIDTH  = 7;
    private final int SMALL_ROAD_WIDTH = 4;

    /* Image paths */
    private static final String GRASS_TEXTURE    = "grass.jpg";
    private static final String HOUSE_TEXTURE    = "house-sketched.png";
    private static final String WATER_TEXTURE    = "water.jpg";
    private static final String STONE_TEXTURE    = "stone.png";
    private static final String MOUNTAIN_TEXTURE = "rock.jpg";
    private static final String FIRE_TEXTURE     = "fire.png";
    private static final String RUBBLE_TEXTURE   = "rubble.png";
    private static final String TREE_TEXTURE     = "tree.png";
    private static final String HEADQUARTER_IMAGE = "headquarter.png";

    private int           height;
    private int           width;
    private int           heightInPoints;
    private int           widthInPoints;
    private GameMap       map;
    private Point         hoveringSpot;
    private TexturePaint  grassTexture;
    private TexturePaint  waterTexture;
    private TexturePaint  mountainTexture;
    private Image         houseImage;
    private Image         stoneImage;
    private Image         fireImage;
    private Image         rubbleImage;
    private Image         treeImage;
    private Image         headquarterImage;

    private final List<SpriteInfo>       spritesToDraw;
    private final Comparator<SpriteInfo> spriteSorter;

    private List<Worker>     workers;
    private List<WildAnimal> animals;

    private double scale;
    private int translateX;
    private int translateY;

    GameDrawer(int w, int h, int wP, int hP) throws IOException {
        width  = w;
        height = h;

        widthInPoints = wP;
        heightInPoints = hP;

        scale = 30;

        /* No hovering spot exists on startup */
        hoveringSpot = null;

        /* Prepare brushes */
        loadBrushes();

        /* Create the list to store pieces to draw for each frame */
        spritesToDraw = new ArrayList<>();
        spriteSorter = new SpriteSorter();

        /* Create lists to store temporary copies to avoid synchronization issues */
        workers = new ArrayList<>();
        animals = new ArrayList<>();
    }

    void drawScene(Graphics2D g, Player player, Point selected, List<Point> ongoingRoadPoints, boolean showAvailableSpots) throws Exception {

        synchronized (map) {

            /* Get copy of pieces to work on */
            workers = map.getWorkers();
            animals = map.getWildAnimals();

            /* Remove sprites drawn in the previous frame */
            spritesToDraw.clear();

            /* Draw the terrain */
            drawTerrain(player, g);

            /* Draw roads directly on the ground first */
            drawRoads(g, player);

            /* Collect sprites to draw */
            collectHouseSprites(map, player);
            collectTreeSprites(map, player);
            collectStoneSprites(map, player);

            /* Sort the sprites so the front sprites are drawn first */
            Collections.sort(spritesToDraw, spriteSorter);

            /* Draw sprites */
            for (SpriteInfo si : spritesToDraw) {
                drawSprite(g, si);
            }

            drawCrops(g, player);

            drawPersons(g, workers, player);

            drawBorders(g, player);

            drawFlags(g, player);

            drawSigns(g, player);

            drawProjectiles(g);

            drawWildAnimals(g, animals, player);

            /* Draw the available spots for the next point for a road if a road is being built */
            if (showAvailableSpots) {
                drawAvailableSpots(g, player);
            }

            /* Draw the chosen points so far for a road being built if needed */
            if (ongoingRoadPoints != null && !ongoingRoadPoints.isEmpty()) {
                drawPreliminaryRoad(g, ongoingRoadPoints, player);

                drawLastSelectedPoint(g, ongoingRoadPoints, player);

                drawPossibleRoadConnections(g, player, ongoingRoadPoints.get(ongoingRoadPoints.size() - 1), ongoingRoadPoints);
            } else if (selected != null) {
                drawSelectedPoint(g, selected, player);
            }

            /* Draw headings for the houses */
            drawHouseTitles(g, map.getBuildings(), player);

            /* Draw the hovering spot on top */
            if (hoveringSpot != null) {
                drawHoveringPoint(g, hoveringSpot, player);
            }
        }
    }

    private void drawTerrain(Player player, Graphics2D g) {

        /* Draw the terrain */
        int startX;
        boolean rowOffsetFlip = false;
        
        for (int y = 0; y < heightInPoints; y++) {
            
            int screenY = (int) (y * scale + translateY);
            
            if (rowOffsetFlip) {
                startX = 1;
            } else {
                startX = 0;
            }
            
            rowOffsetFlip = !rowOffsetFlip;
            
            /* Skip drawing lines not on screen */
            if (screenY < 0 || screenY > height + scale) {
                continue;
            }
            
            /* Draw upwards triangles */
            for (int x = startX; x < widthInPoints; x+= 2) {
                
                int screenX = (int) (x * scale + translateX);
                
                if (screenX < -scale || screenX > width - scale) {
                    continue;
                }
                
                Point p1 = new Point(x, y);
                Point p2 = new Point(x + 2, y);
                Point p3 = new Point(x + 1, y + 1);
                
                if (!player.getDiscoveredLand().contains(p1) ||
                        !player.getDiscoveredLand().contains(p2) ||
                        !player.getDiscoveredLand().contains(p3)) {
                    continue;
                }
                
                Tile t = map.getTerrain().getTile(p1, p2, p3);
                prepareToDrawVegetation(t, g);
                drawFilledTriangle(g,
                        pointToScreenX(p1), pointToScreenY(p1),
                        pointToScreenX(p2), pointToScreenY(p2),
                        pointToScreenX(p3), pointToScreenY(p3));
            }
            
            /* Draw downwards triangles */
            for (int x = startX; x < widthInPoints; x += 2) {
                
                int screenX = (int) (x * scale + translateX);
                
                if (screenX < -scale || screenX > width - scale) {
                    continue;
                }
                
                Point p1 = new Point(x, y);
                Point p2 = new Point(x + 2, y);
                Point p3 = new Point(x + 1, y - 1);
                
                if (!player.getDiscoveredLand().contains(p1) ||
                        !player.getDiscoveredLand().contains(p2) ||
                        !player.getDiscoveredLand().contains(p3)) {
                    continue;
                }
                
                Tile t = map.getTerrain().getTile(p1, p2, p3);
                
                prepareToDrawVegetation(t, g);
                drawFilledTriangle(g,
                        pointToScreenX(p1), pointToScreenY(p1),
                        pointToScreenX(p2), pointToScreenY(p2),
                        pointToScreenX(p3), pointToScreenY(p3));
            }
        }
    }

    private void drawSprite(Graphics2D g, SpriteInfo si) {

        Point p = si.getPosition();
        int spriteWidth = si.getWidth();
        int spriteHeight = si.getHeight();

        if (si.getSprite() != null) {
            Image img = si.getSprite();

            g.drawImage(img, 
                    pointToScreenX(p, si.offsetX), pointToScreenY(p, si.offsetY), 
                    scaleOffset(spriteWidth), scaleOffset(spriteHeight),
                    null);
        } else {
            g.setColor(Color.RED);
            g.fillRect(pointToScreenX(p), pointToScreenY(p),
                    scaleOffset(spriteWidth), scaleOffset(spriteHeight));
        }
    }

    private void collectHouseSprites(GameMap map, Player player) {

        List<Building> houses = map.getBuildings();

        synchronized (houses) {
            for (Building b : houses) {
                Point p = b.getPosition();

                if (!pointOnScreen(b.getPosition())) {
                    continue;
                }

                if (!player.getDiscoveredLand().contains((b.getPosition()))) {
                    continue;
                }

                int houseWidth = 250;
                int houseHeight = 250;
                
                if (b.burningDown()) {
                    spritesToDraw.add(new SpriteInfo(fireImage, p.upLeft(), houseWidth, houseHeight, -150, 0));
                } else if (b.destroyed()) {
                    spritesToDraw.add(new SpriteInfo(rubbleImage, p.upLeft(), houseWidth, houseHeight, -150, 0));
                } else if (b instanceof Headquarter) {
                    spritesToDraw.add(new SpriteInfo(headquarterImage, p.upLeft(), 200, 250, -15, -25));
                } else {
                    spritesToDraw.add(new SpriteInfo(houseImage, p.upLeft(), houseWidth, houseHeight, (int)(houseWidth / 4), (int)(houseHeight / 4.0)));
                }
            }
        }
    }

    private void drawHouseTitles(Graphics2D g, List<Building> houses, Player player) {

        for (Building b : houses) {
            if (!pointOnScreen(b.getPosition())) {
                continue;
            }

            if (!player.getDiscoveredLand().contains((b.getPosition()))) {
                continue;
            }

            if (b.outOfNaturalResources()) {
                g.setColor(RED);
            } else {
                g.setColor(ORANGE);
            }

            String title = b.getClass().getSimpleName();
            Point p = b.getPosition();

            if (b.underConstruction()) {
                title = "(" + title + ")";
            }

            int titleWidth = g.getFontMetrics().stringWidth(title);
            g.drawString(title, pointToScreenX(p) - titleWidth / 2, pointToScreenY(p.up()));

            if (b.ready() && !b.occupied()) {
                int unoccupiedLabelWidth = g.getFontMetrics().stringWidth(title);
                g.drawString("(unoccupied)", pointToScreenX(p) - unoccupiedLabelWidth / 2, pointToScreenY(p.up()) - 40 + g.getFontMetrics().getHeight());
            }
        }
    }

    private void drawProjectiles(Graphics2D g) {

        for (Projectile projectile : map.getProjectiles()) {

            int xOrigOnScreen = pointToScreenX(projectile.getSource());
            int yOrigOnScreen = pointToScreenY(projectile.getSource());

            int xTargetOnScreen = pointToScreenX(projectile.getTarget());
            int yTargetOnScreen = pointToScreenY(projectile.getTarget());

            int directionX = xTargetOnScreen - xOrigOnScreen;
            int directionY = yTargetOnScreen - yOrigOnScreen;

            int xActualOnScreen = xOrigOnScreen + (int) (directionX * ((double) projectile.getProgress() / 100.0));
            int yActualOnScreen = yOrigOnScreen + (int) (directionY * ((double) projectile.getProgress() / 100.0));

            g.fillOval(xActualOnScreen, yActualOnScreen, 10, 10);
        }
    }

    private void drawWildAnimals(Graphics2D g, List<WildAnimal> animals, Player player) throws Exception {

        for (WildAnimal animal : animals) {

            double actualX = animal.getPosition().x;
            double actualY = animal.getPosition().y;            

            if (!animal.isExactlyAtPoint()) {
                Point next = null;

                next = animal.getNextPoint();

                Point last = animal.getLastPoint();

                if (!pointOnScreen(last) && !pointOnScreen(next)) {
                    continue;
                }

                if (!player.getDiscoveredLand().contains(last) &&
                    !player.getDiscoveredLand().contains(next)) {
                    continue;
                }

                if (next == null) {
                    actualX = last.x;
                    actualY = last.y;
                } else {
                    int percent = animal.getPercentageOfDistanceTraveled();

                    actualX = last.x + (next.x - last.x)*((double)percent/(double)100);
                    actualY = last.y + (next.y - last.y)*((double)percent/(double)100);
                }
            }

            g.setColor(Color.RED);
            g.fillOval(         (int)(actualX * scale + translateX) - (int)(20.0 * scale / 100.0),
                       height - (int)(actualY * scale + translateY) - (int)(50.0 * scale / 100.0), 
                       (int)(30 * scale / 100),
                       (int)(25 * scale / 100));

        }
    }

    void move(int changeX, int changeY) {
        translateX = translateX + changeX;
        translateY = translateY - changeY;
    }

    private boolean pointOnScreen(Point position) {
        int screenX = pointToScreenX(position);
        int screenY = pointToScreenY(position);

        return screenX > 0 && screenX < width && screenY > 0 && screenY < height;
    }

    void centerOn(Player controlledPlayer) {
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

        if (point != null) {
            translateX = (int)((width  / 2) - (point.x * scale));
            translateY = (int)((height / 2) - (point.y * scale));
        }
    }

    private class SpriteSorter implements Comparator<SpriteInfo> {

        @Override
        public int compare(SpriteInfo t, SpriteInfo t1) {

            if (t.getPosition().getY() > t1.getPosition().getY()) {
                return -1;
            } else if (t.getPosition().getY() < t1.getPosition().getY()) {
                return 1;
            } else {
                return 0;
            }
        }
    }

    private class SpriteInfo {
        private final int   offsetY;
        private final int   offsetX;
        private final int   spriteHeight;
        private final int   spriteWidth;
        private final Point position;
        private final Image sprite;

        private SpriteInfo(Image s, Point p, int w, int h, int ox, int oy) {
            sprite       = s;
            position     = p;
            spriteWidth  = w;
            spriteHeight = h;
            offsetX      = ox;
            offsetY      = oy;
        }

        public int getOffsetY() {
            return offsetY;
        }

        public int getOffsetX() {
            return offsetX;
        }

        public int getHeight() {
            return spriteHeight;
        }

        public int getWidth() {
            return spriteWidth;
        }

        public Point getPosition() {
            return position;
        }

        public Image getSprite() {
            return sprite;
        }

    }

    private void collectTreeSprites(GameMap map, Player player) {

        Collection<Tree> trees = map.getTrees();

        synchronized (trees) {

            for (Tree t : trees) {
                if (!pointOnScreen(t.getPosition())) {
                    continue;
                }

                if (!player.getDiscoveredLand().contains((t.getPosition()))) {
                    continue;
                }

                int base = 35;
                int treeHeight = 400;

                if (t.getSize() == SMALL) {
                    base = 20;
                    treeHeight = 200;
                } else if (t.getSize() == MEDIUM) {
                    base = 28;
                    treeHeight = 300;
                }

                SpriteInfo si = new SpriteInfo(treeImage, t.getPosition(), base * 5, treeHeight, -25, treeHeight);
                spritesToDraw.add(si);
            }
        }
    }

    private void collectStoneSprites(GameMap map, Player player) {

        Collection<Stone> stones = map.getStones();

        synchronized (stones) {
            for (Stone s : stones) {
                if (!pointOnScreen(s.getPosition())) {
                    continue;
                }

                if (!player.getDiscoveredLand().contains((s.getPosition()))) {
                    continue;
                }

                int stoneWidth = 250;
                int stoneHeight = 250;
                
                SpriteInfo si = new SpriteInfo(stoneImage, s.getPosition(), stoneWidth, stoneHeight, -(int)(stoneWidth / 2.0), -(int)(stoneHeight / 2.0));
                spritesToDraw.add(si);
            }
        }
    }

    private void drawCrops(Graphics2D g, Player player) {

        for (Crop c : map.getCrops()) {
            if (!pointOnScreen(c.getPosition())) {
                continue;
            }

            if (!player.getDiscoveredLand().contains((c.getPosition()))) {
                continue;
            }

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

            g.fillOval(pointToScreenX(c.getPosition(), -50),
                    pointToScreenY(c.getPosition(), -30),
                    scaleOffset(100),
                    scaleOffset(60));
        }
    }

    private void drawBorders(Graphics2D g, Player player) {
        for (Player playerIterator : map.getPlayers()) {
            for (Collection<Point> border : playerIterator.getBorders()) {
                g.setColor(playerIterator.getColor());

                for (Point p : border) {
                    if (!pointOnScreen(p)) {
                        continue;
                    }

                    if (!player.getDiscoveredLand().contains((p))) {
                        continue;
                    }

                    g.fillOval(pointToScreenX(p), pointToScreenY(p), scaleOffset(50), scaleOffset(50));
                }
            }
        }
    }

    private void drawSigns(Graphics2D g, Player player) {
        for (Sign s : map.getSigns()) {

            if (!pointOnScreen(s.getPosition())) {
                continue;
            }

            if (!player.getDiscoveredLand().contains((s.getPosition()))) {
                continue;
            }

            g.setColor(SIGN_BACKGROUND_COLOR);

            g.fillRect(pointToScreenX(s.getPosition(), 10),
                    pointToScreenY(s.getPosition(), 40),
                    scaleOffset(-5),
                    scaleOffset(-40));

            g.fillRect(pointToScreenX(s.getPosition(), 40),
                    pointToScreenY(s.getPosition(), 40),
                    scaleOffset(-20),
                    scaleOffset(-45));

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
                g.fillRect(
                        pointToScreenX(s.getPosition(), 30),
                        pointToScreenY(s.getPosition(), 30),
                        -15,
                        -70);
                break;
            case MEDIUM:
                g.fillRect(
                        pointToScreenX(s.getPosition(), 20),
                        pointToScreenY(s.getPosition(), 20),
                        -15,
                        -70);
                break;
            case SMALL:
                g.fillRect(
                        pointToScreenX(s.getPosition(), 10),
                        pointToScreenY(s.getPosition(), 10),
                        -15,
                        -70);
                break;
            }
        }
    }

    private void drawLastSelectedPoint(Graphics graphics, List<Point> ongoingRoadPoints, Player player) {
        Point selected = ongoingRoadPoints.get(ongoingRoadPoints.size() - 1);

        if (!pointOnScreen(selected)) {
            return;
        }

        if (!player.getDiscoveredLand().contains((selected))) {
            return;
        }

        graphics.setColor(Color.RED);
        graphics.fillOval(pointToScreenX(selected, -(int)(SPOT_WIDTH / 2.0)), pointToScreenY(selected, -(int)(SPOT_HEIGHT / 2.0)), SPOT_WIDTH, SPOT_HEIGHT);
    }

    private void drawFlags(Graphics2D g, Player player) {
        for (Flag f : map.getFlags()) {
            Point p = f.getPosition();

            if (!pointOnScreen(p)) {
                continue;
            }

            if (!player.getDiscoveredLand().contains((p))) {
                continue;
            }

            g.setColor(f.getPlayer().getColor());
            g.fillRect(pointToScreenX(p), pointToScreenY(p ,80), scaleOffset(40), scaleOffset(40));

            g.setColor(FLAG_POLE_COLOR);
            g.fillRect(pointToScreenX(p), pointToScreenY(p, 80), scaleOffset(10), scaleOffset(80));

            /* Draw any stacked cargo */
            if (!f.getStackedCargo().isEmpty()) {
                Color cargoColor = getColorForMaterial(f.getStackedCargo().get(f.getStackedCargo().size() - 1).getMaterial());

                g.setColor(cargoColor);
                g.fillRect(pointToScreenX(p, 10),
                           pointToScreenY(p, -10),
                           scaleOffset(CARGO_WIDTH),
                           scaleOffset(CARGO_HEIGHT));
            }
        }
    }

    private void drawPossibleRoadConnections(Graphics2D g, Player player, Point point, List<Point> roadPoints) throws Exception {

        for (Point p : map.getPossibleAdjacentRoadConnectionsIncludingEndpoints(player, point)) {
            if (!pointOnScreen(p)) {
                continue;
            }

            if (!player.getDiscoveredLand().contains(p)) {
                continue;
            }

            if (map.isFlagAtPoint(p)) {
                continue;
            }

            if (roadPoints.contains(p)) {
                continue;
            }

            g.setColor(POSSIBLE_WAYPOINT_COLOR);
            g.fillOval(pointToScreenX(p, (int)(-MARKER_WIDTH / 2.0)), pointToScreenY(p, (int)(MARKER_HEIGHT / 2.0)),
                    scaleOffset(MARKER_WIDTH), scaleOffset(MARKER_HEIGHT));

            g.setColor(Color.DARK_GRAY);
            g.drawOval(pointToScreenX(p, (int)(-MARKER_WIDTH / 2.0)), pointToScreenY(p, (int)(MARKER_HEIGHT / 2.0)),
                    scaleOffset(MARKER_WIDTH), scaleOffset(MARKER_HEIGHT));
        }
    }

    private void drawPreliminaryRoad(Graphics2D g, List<Point> ongoingRoadPoints, Player player) {
        Point previous = null;

        Stroke oldStroke = g.getStroke();
        g.setStroke(new BasicStroke(3));

        for (Point current : ongoingRoadPoints) {
            if (!pointOnScreen(current)) {
                continue;
            }

            if (!player.getDiscoveredLand().contains(current)) {
                continue;
            }

            if (previous == null) {
                previous = current;

                continue;
            }

            g.setColor(Color.YELLOW);

            g.drawLine(pointToScreenX(previous), pointToScreenY(previous), pointToScreenX(current), pointToScreenY(current));

            previous = current;
        }

        g.setStroke(oldStroke);
    }

    private void drawRoads(Graphics2D g, Player player) {
        g.setColor(Color.ORANGE);
        Stroke oldStroke = g.getStroke();

        for (Road r : map.getRoads()) {

            if (r.isMainRoad()) {
                g.setStroke(new BasicStroke(MAIN_ROAD_WIDTH));
                g.setColor(MAIN_ROAD_COLOR);
            } else {
                g.setStroke(new BasicStroke(SMALL_ROAD_WIDTH));
                g.setColor(SMALL_ROAD_COLOR);
            }

            Point previous = null;

            for (Point p : r.getWayPoints()) {
                if (previous == null) {
                    previous = p;
                    continue;
                }

                if (!pointOnScreen(p) && !pointOnScreen(previous)) {
                    continue;
                }

                if (!player.getDiscoveredLand().contains(p) &&
                    !player.getDiscoveredLand().contains(previous)) {
                    continue;
                }

                g.drawLine(pointToScreenX(previous), pointToScreenY(previous), pointToScreenX(p), pointToScreenY(p));

                previous = p;
            }
        }

        g.setStroke(oldStroke);
    }

    private void prepareToDrawVegetation(Tile t, Graphics2D g) {
        switch (t.getVegetationType()) {
        case GRASS:
            if (grassTexture != null) {
                g.setPaint(grassTexture);
            } else {
                g.setColor(GRASS_COLOR);
            }
            break;
        case SWAMP:
            g.setColor(Color.GRAY);
            break;
        case WATER:
            if (waterTexture != null) {
                g.setPaint(waterTexture);
            } else {
                g.setColor(Color.BLUE);
            }
            break;
        case MOUNTAIN:
            if (mountainTexture != null) {
                g.setPaint(mountainTexture);
            } else {
                g.setColor(MOUNTAIN_COLOR);
            }
        break;
        default:
            g.setColor(Color.GRAY);
        }
    }

    private void drawFilledTriangle(Graphics2D g, int x1, int y1, int x2, int y2, int x3, int y3) {
        Path2D.Double triangle = new Path2D.Double();
        triangle.moveTo(x1, y1);
        triangle.lineTo(x2, y2);
        triangle.lineTo(x3, y3);
        triangle.closePath();
        g.fill(triangle);
    }

    private void drawPersons(Graphics2D g, List<Worker> workers, Player player) {

        for (Worker w : workers) {
            if (w.isInsideBuilding()) {
                continue;
            }

            if (!pointOnScreen(w.getPosition())) {
                continue;
            }

            if (!player.getDiscoveredLand().contains((w.getPosition()))) {
                continue;
            }

            drawPerson(g, w);
        }
    }

    private void drawPerson(Graphics2D g, Worker w) {

        if (w instanceof Military) {
            g.setColor(w.getPlayer().getColor());
        } else {
            g.setColor(Color.BLACK);
        }

        if (w instanceof Donkey) {
            g.setColor(DONKEY_COLOR);
        }

        double actualX = w.getPosition().x;
        double actualY = w.getPosition().y;            

        if (!w.isExactlyAtPoint()) {
            Point next = null;

            try {
                next = w.getNextPoint();
            } catch (Exception ex) {
                Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);

                System.exit(1);
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

        g.fillOval(toScreenX(actualX), toScreenY(actualY, 100), scaleOffset(50), scaleOffset(100));

        /* Draw the cargo being carried (if any) */
        if (w.getCargo() != null ) {
            Color cargoColor = getColorForMaterial(w.getCargo().getMaterial());

            g.setColor(cargoColor);
            g.fillRect(toScreenX(actualX), toScreenY(actualY, 80), scaleOffset(CARGO_WIDTH), scaleOffset(CARGO_HEIGHT));
        }
    }

    private void drawAvailableSpots(Graphics2D g, Player player) throws Exception {
        Map<Point, Size> houses = map.getAvailableHousePoints(player);

        /* Draw the available houses */
        for (Map.Entry<Point, Size> pair : houses.entrySet()) {
            if (!pointOnScreen(pair.getKey())) {
                continue;
            }

            if (!player.getDiscoveredLand().contains(pair.getKey())) {
                continue;
            }

            drawAvailableHouse(g, pair.getKey(), pair.getValue());
        }

        /* Draw the available flags */
        for (Point p : map.getAvailableFlagPoints(player)) {
            if (!pointOnScreen(p)) {
                continue;
            }

            if (!player.getDiscoveredLand().contains(p)) {
                continue;
            }

            if (houses.keySet().contains(p)) {
                continue;
            }

            drawAvailableFlag(g, p);
        }

        /* Draw the available mines */
        for (Point p : map.getAvailableMinePoints(player)) {
            if (!pointOnScreen(p)) {
                continue;
            }

            if (!player.getDiscoveredLand().contains(p)) {
                continue;
            }

            drawAvailableMine(g, p);
        }
    }

    private void drawAvailableFlag(Graphics2D g, Point p) {

        g.setColor(Color.ORANGE);
        g.fillRect(pointToScreenX(p, 10), pointToScreenY(p, 50), 25, -25);

        g.setColor(Color.BLACK);
        g.fillRect(pointToScreenX(p, 10), pointToScreenY(p, 50), 25, -25);
    }


    private void drawAvailableHouse(Graphics2D g, Point key, Size value) {

        /* Draw box for small houses */
        g.setColor(AVAILABLE_CONSTRUCTION_COLOR);
        g.fillRect(pointToScreenX(key), pointToScreenY(key), AVAILABLE_SMALL_HOUSE_WIDTH, AVAILABLE_SMALL_HOUSE_HEIGHT);

        g.setColor(Color.BLACK);
        g.drawRect(pointToScreenX(key), pointToScreenY(key), AVAILABLE_SMALL_HOUSE_WIDTH, AVAILABLE_SMALL_HOUSE_HEIGHT);

        /* Draw box for medium houses */
        if (value != SMALL) {

            g.setColor(AVAILABLE_CONSTRUCTION_COLOR);
            g.fillRect(pointToScreenX(key), pointToScreenY(key), AVAILABLE_MEDIUM_HOUSE_WIDTH, AVAILABLE_MEDIUM_HOUSE_HEIGHT);

            g.setColor(Color.BLACK);
            g.drawRect(pointToScreenX(key), pointToScreenY(key), AVAILABLE_MEDIUM_HOUSE_WIDTH, AVAILABLE_MEDIUM_HOUSE_HEIGHT);
        }

        if (value == LARGE) {

            g.setColor(AVAILABLE_CONSTRUCTION_COLOR);
            g.fillRect(pointToScreenX(key), pointToScreenY(key), AVAILABLE_LARGE_HOUSE_WIDTH, AVAILABLE_LARGE_HOUSE_HEIGHT);

            g.setColor(Color.BLACK);
            g.drawRect(pointToScreenX(key), pointToScreenY(key), AVAILABLE_LARGE_HOUSE_WIDTH, AVAILABLE_LARGE_HOUSE_HEIGHT);
        }
    }

    private void drawSelectedPoint(Graphics2D g, Point p, Player player) {
        if (!pointOnScreen(p)) {
            return;
        }

        if (!player.getDiscoveredLand().contains(p)) {
            return;
        }

        g.setColor(SELECTED_POINT_COLOR);
        g.fillOval(pointToScreenX(p, -(int)(SPOT_WIDTH / 2.0)), pointToScreenY(p, (int)(SPOT_HEIGHT / 2.0)), scaleOffset(SPOT_WIDTH), scaleOffset(SPOT_HEIGHT));

        g.setColor(Color.DARK_GRAY);
        g.drawOval(pointToScreenX(p, -(int)(SPOT_WIDTH / 2.0)), pointToScreenY(p, (int)(SPOT_HEIGHT / 2.0)), scaleOffset(SPOT_WIDTH), scaleOffset(SPOT_HEIGHT));
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
        case PIG:
            return PIG_COLOR;
        case BEER:
            return BEER_COLOR;
        case COIN:
            return COIN_COLOR;
        default:
            return Color.RED;
        }
    }

    void recalculateScale(int w, int h) {
        width  = w;
        height = h;
    }

    void setMap(GameMap m) {
        map = m;

        widthInPoints = m.getWidth();
        heightInPoints = m.getHeight();
    }

    void zoomIn(int notches) throws Exception {
        // screen = game * scale + translate
        // screen/2 = game * scale + translate
        // translate = screen/2 - game * scale

        double oldGameX = ((width  / 2) - translateX) / scale;
        double oldGameY = ((height / 2) - translateY) / scale;

        scale = scale + 1;

        translateX = (int)((width  / 2) - oldGameX * scale);
        translateY = (int)((height / 2) - oldGameY * scale);
    }

    void zoomOut(int notches) throws Exception {
        // screen = game * scale + translate
        // screen/2 = game * scale + translate
        // translate = screen/2 - game * scale

        double oldGameX = ((width  / 2) - translateX) / scale;
        double oldGameY = ((height / 2) - translateY) / scale;

        scale = scale - 1;
        
        translateX = (int)((width  / 2) - oldGameX * scale);
        translateY = (int)((height / 2) - oldGameY * scale);
    }

    private TexturePaint createBrushFromImageResource(String res) throws IOException {

        /* Load the image from the resource string */
        URL url = Thread.currentThread().getContextClassLoader().getResource(res);

        /* Leave early and return null if the url isn't valid */
        if (url == null) {
            System.out.println("Failed to load " + res + ", invalid file");

            return null;
        }

        BufferedImage bi = ImageIO.read(url);

        /* bi will be null if the resource couldn't be located */
        if (bi == null) {
            System.out.println("Failed to load " + res);

            return null;
        }

        /* Create the brush */
        Rectangle r = new Rectangle(0, 0, 100, 100);

        System.out.println("Loaded " + res + " correctly " + new TexturePaint(bi, r));

        return new TexturePaint(bi, r);
    }

    private void loadBrushes() throws IOException {

        /* Load brushes */
        grassTexture    = createBrushFromImageResource(GRASS_TEXTURE);
        waterTexture    = createBrushFromImageResource(WATER_TEXTURE);
        mountainTexture = createBrushFromImageResource(MOUNTAIN_TEXTURE);

        /* Load images */
        stoneImage       = createImageFromImageResource(STONE_TEXTURE);
        houseImage       = createImageFromImageResource(HOUSE_TEXTURE);
        fireImage        = createImageFromImageResource(FIRE_TEXTURE);
        rubbleImage      = createImageFromImageResource(RUBBLE_TEXTURE);
        treeImage        = createImageFromImageResource(TREE_TEXTURE);
        headquarterImage = createImageFromImageResource(HEADQUARTER_IMAGE);
    }

    private BufferedImage createImageFromImageResource(String res) {
        try {

            /* Load the image from the file */
            BufferedImage bi = ImageIO.read(Thread.currentThread().getContextClassLoader().getResource(res));

            BufferedImage compatibleImage = Utils.createOptimizedBufferedImage(bi.getWidth(), bi.getHeight(), true);

            /* Write the loaded image to the optimized image */
            Graphics2D graphics = compatibleImage.createGraphics();

            graphics.drawImage(bi, null, null);

            /* Return the optimized image */
            return compatibleImage;
        } catch (IOException ex) {
            Logger.getLogger(GameDrawer.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }

    void setHoveringSpot(Point point) {
        hoveringSpot = point;
    }

    private void drawHoveringPoint(Graphics2D g, Point p, Player player) {

        if (!pointOnScreen(p)) {
            return;
        }

        if (!player.getDiscoveredLand().contains(p)) {
            return;
        }

        g.setColor(HOVERING_COLOR);
        g.fillOval(pointToScreenX(p, -(int)(SPOT_WIDTH / 2.0)), pointToScreenY(p, (int)(SPOT_HEIGHT / 2.0)), scaleOffset(SPOT_WIDTH), scaleOffset(SPOT_HEIGHT));

        g.setColor(Color.DARK_GRAY);
        g.drawOval(pointToScreenX(p, -(int)(SPOT_WIDTH / 2.0)), pointToScreenY(p, (int)(SPOT_HEIGHT / 2.0)), scaleOffset(SPOT_WIDTH), scaleOffset(SPOT_HEIGHT));
    }

    private void drawAvailableMine(Graphics2D g, Point p) {
        g.setColor(Color.ORANGE);
        g.fillOval(pointToScreenX(p, 30), pointToScreenY(p, 30), -15, -15);

        g.setColor(Color.BLACK);
        g.drawOval(pointToScreenX(p, 30), pointToScreenY(p, 30), -15, -15);
    }

    java.awt.Point gamePointToScreenPoint(java.awt.Point p, java.awt.Point gplowerLeft, java.awt.Point gpUpperRight, java.awt.Point upperLeft, java.awt.Point lowerRight) {

        /* Calculate ratios */
        double pixelsPerGpX = (lowerRight.x - upperLeft.x) / (gpUpperRight.x - gplowerLeft.x);
        double pixelsPerGpY = (lowerRight.y - upperLeft.y) / (gpUpperRight.y - gplowerLeft.y);

        /* Calculate anchor pixel */
        java.awt.Point fixedPoint = new java.awt.Point(
                (int)((p.x - gplowerLeft.x) * pixelsPerGpX + upperLeft.x),
                (int)((p.y - gplowerLeft.y) * pixelsPerGpY + upperLeft.y));

        return fixedPoint;
    }

    Dimension getPixelsPerGamePoint(java.awt.Point gpLowerLeft, java.awt.Point gpUpperRight, java.awt.Point upperLeft, java.awt.Point lowerRight) {
        double nrGamePointsX = gpUpperRight.x - gpLowerLeft.x;
        double nrGamePointsY = gpUpperRight.y - gpLowerLeft.y;

        double pixelsShownX = lowerRight.x - upperLeft.x;
        double pixelsShownY = upperLeft.y - lowerRight.y;

        return new Dimension((int) (pixelsShownX / nrGamePointsX), (int) (pixelsShownY / nrGamePointsY));
    }

    int pointToScreenX(Point p) {
        return (int)(p.x * scale + translateX);
    }

    int pointToScreenY(Point p) {
        return height - (int)(p.y * scale + translateY);
    }

    int toScreenX(double x) {
        return (int)(x * scale + translateX);
    }

    int toScreenY(double y) {
        return height - (int)(y * scale + translateY);
    }

    int pointToScreenX(Point p, int offset) {
        return (int)((p.x  + (offset / 100)) * scale + translateX);
    }

    int pointToScreenY(Point p, double offset) {
        return height - (int)((p.y  + (offset / 100)) * scale + translateY);
    }

    int toScreenX(double x, int offset) {
        return (int)((x  + (offset / 100)) * scale + translateX);
    }

    int toScreenY(double y, double offset) {
        return height - (int)((y + (offset / 100))* scale + translateY);
    }

    int scaleOffset(double d) {
        return (int)(d*scale/100);
    }
    
    Point screenPointToGamePoint(java.awt.Point point) {

        /* Go from surface coordinates to game points */
        double px = (double) (point.x - translateX) / scale;
        double py = (double) (height - point.y - translateY) / scale;

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
}
