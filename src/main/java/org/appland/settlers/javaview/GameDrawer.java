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
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.TexturePaint;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
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
import org.appland.settlers.model.Terrain;
import org.appland.settlers.model.Tile;
import org.appland.settlers.model.Tree;
import org.appland.settlers.model.WildAnimal;
import org.appland.settlers.model.Worker;

/**
 *
 * @author johan
 */
public class GameDrawer {

    private final Color FOG_OF_WAR_COLOR        = Color.BLACK;
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
    private int           terrainPrerenderedWidthInPoints;
    private int           terrainPrerenderedHeightInPoints;
    private GameMap       map;
    private ScaledDrawer  drawer;
    private Point         hoveringSpot;
    private TexturePaint  grassTexture;
    private TexturePaint  waterTexture;
    private TexturePaint  mountainTexture;
    private BufferedImage terrainImage;
    private Image         houseImage;
    private Image         stoneImage;
    private Image         fireImage;
    private Image         rubbleImage;
    private Image         treeImage;
    private Image         headquarterImage;

    private final Map<Class, Image>      spriteMap;
    private final Map<Class, Dimension>  dimensionMap;
    private final List<SpriteInfo>       spritesToDraw;
    private final Comparator<SpriteInfo> spriteSorter;

    private final List<Worker>     workers;
    private final List<WildAnimal> animals;

    GameDrawer(int w, int h, int wP, int hP) {
        width  = w;
        height = h;

        widthInPoints  = wP;
        heightInPoints = hP;

        drawer = new ScaledDrawer(500, 500, w, h);

        /* No hovering spot exists on startup */
        hoveringSpot = null;

        /* Prepare brushes */
        try {
            loadBrushes();
        } catch (IOException ex) {
            Logger.getLogger(GameDrawer.class.getName()).log(Level.SEVERE, null, ex);
        }

        /* Create the list to store pieces to draw for each frame */
        spritesToDraw = new ArrayList<>();
        spriteSorter = new SpriteSorter();

        /* Set up a mapping from classes to images for pieces to draw */
        spriteMap = new HashMap<>();

        spriteMap.put(Tree.class, treeImage);
        spriteMap.put(Building.class, houseImage);
        spriteMap.put(Stone.class, stoneImage);

        /* Set up a mapping of dimensions for the pieces to draw */
        dimensionMap = new HashMap<>();

        dimensionMap.put(Tree.class, new Dimension(30, 10));
        dimensionMap.put(Building.class, new Dimension(20, 20));
        dimensionMap.put(Headquarter.class, new Dimension(30, 30));
        dimensionMap.put(Stone.class, new Dimension(10, 10));

        /* Create lists to store temporary copies to avoid synchronization issues */
        workers = new ArrayList<>();
        animals = new ArrayList<>();
    }

    double getScaleX() {
        return drawer.getScaleX();
    }

    double getScaleY() {
        return drawer.getScaleY();
    }

    void drawScene(Graphics2D g, Player player, Point selected, List<Point> ongoingRoadPoints, boolean showAvailableSpots) throws Exception {

        /* Remove previously copied pieces */
        workers.clear();
        animals.clear();

        /* Get copy of pieces to work on */
        copyListAtomically(map.getWorkers(), workers);
        copyListAtomically(map.getWildAnimals(), animals);

        /* Remove sprites drawn in the previous frame */
        spritesToDraw.clear();

        /* Draw the terrain */
        if (terrainImage != null) {
                int marginXInPoints = (terrainPrerenderedWidthInPoints-widthInPoints) / 2;
                int marginYInPoints = (terrainPrerenderedHeightInPoints-heightInPoints) / 2;
                
                int tw = terrainImage.getWidth();
                int th = terrainImage.getHeight();
                
                g.drawImage(terrainImage,
                        0,           //dx1
                        0,           //dy1
                        width,       //dx2
                        height,      //dy2
                        (marginXInPoints / terrainPrerenderedWidthInPoints) * tw, //sx1
                        (marginYInPoints / terrainPrerenderedHeightInPoints) * th, //sy1
                        (marginXInPoints + widthInPoints) / terrainPrerenderedWidthInPoints * tw,        //sx2
                        (marginYInPoints + heightInPoints) / terrainPrerenderedHeightInPoints * th,      //sy2
                        null);      //ImageObserver
        }

        /* Draw roads directly on the ground first */
        drawRoads(g);

        /* Collect sprites to draw */
        collectHouseSprites(map);
        collectTreeSprites(map);
        collectStoneSprites(map);

        drawCrops(g);

        drawPersons(g, workers);

        drawBorders(g);

        drawFlags(g);

        drawSigns(g);

        drawProjectiles(g);

        drawWildAnimals(g, animals);

        /* Draw the available spots for the next point for a road if a road is being built */
        if (showAvailableSpots) {
            drawAvailableSpots(g, player);
        }

        /* Draw the chosen points so far for a road being built if needed */
        if (ongoingRoadPoints != null && !ongoingRoadPoints.isEmpty()) {
            drawPreliminaryRoad(g, ongoingRoadPoints);

            drawLastSelectedPoint(g, ongoingRoadPoints);

            try {
                drawPossibleRoadConnections(g, player, ongoingRoadPoints.get(ongoingRoadPoints.size() - 1), ongoingRoadPoints);
            } catch (Exception ex) {
                Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else if (selected != null) {
            drawSelectedPoint(g, selected);
        }

        /* Sort the sprites so the front sprites are drawn first */
        Collections.sort(spritesToDraw, spriteSorter);

        /* Draw sprites */
        for (SpriteInfo si : spritesToDraw) {
            drawSprite(g, si);
        }

        /* Draw headings for the houses */
        drawHouseTitles(g, map.getBuildings());

        /* Draw the hovering spot on top */
        if (hoveringSpot != null) {
            drawHoveringPoint(g, hoveringSpot);
        }

        /* Draw the fog of war */
        drawFogOfWar(g, player);
    }

    private void drawSprite(Graphics2D g, SpriteInfo si) {

        if (si.getSprite() != null) {
            drawer.drawScaledImage(g, si.getSprite(), si.getPosition(), 
                    si.getWidth(), si.getHeight(), 
                    si.getOffsetX(), si.getOffsetY());
        } else {
            drawer.fillScaledRect(g, si.getPosition(), 
                    si.getWidth(), si.getHeight(), 
                    si.getOffsetX(), si.getOffsetY());
        }
    }

    private void collectHouseSprites(GameMap map) {

        List<Building> houses = map.getBuildings();

        synchronized (houses) {
            for (Building b : houses) {
                Point p = b.getPosition();

                if (b.burningDown()) {
                    spritesToDraw.add(new SpriteInfo(fireImage, p.upLeft(), 50, 60, -15, -25));

                    continue;
                }

                if (b.destroyed()) {
                    spritesToDraw.add(new SpriteInfo(rubbleImage, p.upLeft(), 50, 60, -15, -25));

                    continue;
                }

                if (b instanceof Headquarter) {
                    spritesToDraw.add(new SpriteInfo(headquarterImage, p.upLeft(), 50, 60, -15, -25));

                    continue;
                }

                spritesToDraw.add(new SpriteInfo(houseImage, p.upLeft(), 50, 60, -15, -25));
            }
        }
    }

    private void drawHouseTitles(Graphics2D g, List<Building> houses) {

        for (Building b : houses) {

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

            g.drawString(title, p.x*drawer.getScaleX() - 50, height - (p.y*drawer.getScaleY()) - 40);

            if (b.ready() && b.getWorker() == null && b.getHostedMilitary() == 0) {
                g.drawString("(unoccupied)", p.x*drawer.getScaleX() - 50, height- (p.y*drawer.getScaleY()) - 40 + g.getFontMetrics().getHeight());
            }
        }
    }

    private <T> void copyListAtomically(Collection<T> from, Collection<T> to) {

        synchronized (from) {
            to.addAll(from);
        }
    }

    private void drawProjectiles(Graphics2D g) {

        for (Projectile projectile : map.getProjectiles()) {

            int xOrigOnScreen = drawer.toScreenX(projectile.getSource());
            int yOrigOnScreen = drawer.toScreenY(projectile.getSource());

            int xTargetOnScreen = drawer.toScreenX(projectile.getTarget());
            int yTargetOnScreen = drawer.toScreenY(projectile.getTarget());

            int directionX = xTargetOnScreen - xOrigOnScreen;
            int directionY = yTargetOnScreen - yOrigOnScreen;

            int xActualOnScreen = xOrigOnScreen + (int) (directionX * ((double) projectile.getProgress() / 100.0));
            int yActualOnScreen = yOrigOnScreen + (int) (directionY * ((double) projectile.getProgress() / 100.0));

            g.fillOval(xActualOnScreen, yActualOnScreen, 10, 10);
        }
    }

    private void drawWildAnimals(Graphics2D g, List<WildAnimal> animals) {

        for (WildAnimal animal : animals) {

            double actualX = animal.getPosition().x;
            double actualY = animal.getPosition().y;            

            if (!animal.isExactlyAtPoint()) {
                Point next = null;

                try {
                    next = animal.getNextPoint();
                } catch (Exception ex) {
                    Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);

                    System.exit(1);
                }

                Point last = animal.getLastPoint();

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
            g.fillOval((int)(actualX*drawer.getScaleX()) - drawer.offsetScaleX(4), 
                       height - (int)(actualY*drawer.getScaleY()) - drawer.offsetScaleY(10), 
                       drawer.simpleScaleX(6), 
                       drawer.simpleScaleY(5));

            g.setColor(Color.BLACK);
            g.drawOval((int)(actualX*drawer.getScaleX()) - drawer.offsetScaleX(4), 
                       height - (int)(actualY*drawer.getScaleY()) - drawer.offsetScaleY(10), 
                       drawer.simpleScaleX(6), 
                       drawer.simpleScaleY(5));
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

    private void collectTreeSprites(GameMap map) {

        Collection<Tree> trees = map.getTrees();

        synchronized (trees) {

            for (Tree t : trees) {

                int base = 10;
                int treeHeight = 60;

                if (t.getSize() == SMALL) {
                    base = 4;
                    treeHeight = 20;
                } else if (t.getSize() == MEDIUM) {
                    base = 7;
                    treeHeight = 40;
                }

                SpriteInfo si = new SpriteInfo(treeImage, t.getPosition(), base * 2, treeHeight, -base, -treeHeight);
                spritesToDraw.add(si);
            }
        }
    }

    private void collectStoneSprites(GameMap map) {

        Collection<Stone> stones = map.getStones();

        synchronized (stones) {
            for (Stone s : stones) {
                SpriteInfo si = new SpriteInfo(stoneImage, s.getPosition(), 50, 60, -25, -35);
                spritesToDraw.add(si);
            }
        }
    }

    private void drawCrops(Graphics2D g) {

        for (Crop c : map.getCrops()) {

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
    }

    private void drawBorders(Graphics2D g) {
        for (Player playerIterator : map.getPlayers()) {
            for (Collection<Point> border : playerIterator.getBorders()) {
                g.setColor(playerIterator.getColor());
                    
                for (Point p : border) {
                    if (!isWithinScreen(p)) {
                        continue;
                    }

                    drawer.fillScaledOval(g, p, 6, 6, -3, -3);
                }
            }
        }
    }

    private void drawFogOfWar(Graphics2D g, Player player) {

        /* Avoid drawing if there is no field of view */
        if (player.getFieldOfView() == null || player.getFieldOfView().size() < 3) {
            return;
        }

        /* Create the area with the whole screen */
        Area area = new Area(new Rectangle(0, 0, width, height));

        List<Point> fov = player.getFieldOfView();

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

        g.fillRect(0, 0, width, height);

        g.setClip(oldClip);
    }

    private void drawSigns(Graphics2D g) {
        for (Sign s : map.getSigns()) {
            g.setColor(SIGN_BACKGROUND_COLOR);

            drawer.fillScaledRect(g, s.getPosition(), 2, 8, -1, -8);

            drawer.fillScaledRect(g, s.getPosition(), 8, 8, -4, -15);

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
                drawer.fillScaledRect(g, s.getPosition(), 6, 6, -3, -14);
                break;
            case MEDIUM:
                drawer.fillScaledRect(g, s.getPosition(), 4, 4, -3, -14);
                break;
            case SMALL:
                drawer.fillScaledRect(g, s.getPosition(), 2, 2, -3, -14);
                break;
            }
        }
    }

    private boolean isWithinScreen(Point p) {
        return p.x > 0 && p.x < widthInPoints && p.y > 0 && p.y < heightInPoints;
    }

    private void drawLastSelectedPoint(Graphics graphics, List<Point> ongoingRoadPoints) {
        Point selected = ongoingRoadPoints.get(ongoingRoadPoints.size() - 1);

        graphics.setColor(Color.RED);

        drawer.drawScaledFilledOval(graphics, selected, 7, 7);
    }

    private void drawFlags(Graphics2D g) {
        for (Flag f : map.getFlags()) {
            Point p = f.getPosition();

            g.setColor(f.getPlayer().getColor());
            drawer.fillScaledRect(g, f.getPosition(), 7, 7, 0, -15);

            g.setColor(FLAG_POLE_COLOR);
            drawer.fillScaledRect(g, f.getPosition(), 2, 16, -1, -16);

            if (!f.getStackedCargo().isEmpty()) {
                Color cargoColor = getColorForMaterial(f.getStackedCargo().get(f.getStackedCargo().size() - 1).getMaterial());

                g.setColor(cargoColor);

                g.fillRect((p.x*drawer.getScaleX()) - drawer.offsetScaleX(2),
                         height - (p.y*drawer.getScaleY()) - drawer.offsetScaleY(6),
                       drawer.simpleScaleX(5),
                       drawer.simpleScaleY(5));
            }
        }
    }

    private void drawPossibleRoadConnections(Graphics2D g, Player player, Point point, List<Point> roadPoints) throws Exception {

        for (Point p : map.getPossibleAdjacentRoadConnectionsIncludingEndpoints(player, point)) {
            if (map.isFlagAtPoint(p)) {
                continue;
            }

            if (roadPoints.contains(p)) {
                continue;
            }

            g.setColor(POSSIBLE_WAYPOINT_COLOR);
            drawer.fillScaledOval(g, p, 10, 10, -5, -5);

            g.setColor(Color.DARK_GRAY);
            drawer.drawScaledOval(g, p, 10, 10, -5, -5);
        }
    }

    private void drawPreliminaryRoad(Graphics2D g, List<Point> ongoingRoadPoints) {
        Point previous = null;

        Stroke oldStroke = g.getStroke();
        g.setStroke(new BasicStroke(3));

        for (Point current : ongoingRoadPoints) {
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

    private void drawRoads(Graphics2D g) {
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

                drawer.drawScaledLine(g, previous, p);

                previous = p;
            }
        }

        g.setStroke(oldStroke);
    }

    private BufferedImage createTerrainTexture(int w, int h, int wip, int hip) throws Exception {
        BufferedImage image = Utils.createOptimizedBufferedImage(w, h, false);
        Terrain terrain     = map.getTerrain();
        Graphics2D g        = image.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        boolean rowOffsetFlip = true;

        /* Place all possible flag points in the list */
        int x, y;

        for (y = 0; y < hip; y++) {

            int startX;
            if (rowOffsetFlip) {
                startX = 0;
            } else {
                startX = 1;
            }

            /* Draw upwards triangles */
            if (y < hip) {
                for (x = startX; x < wip; x+= 2) {
                    Point p1 = new Point(x, y);
                    Point p2 = new Point(x + 2, y);
                    Point p3 = new Point(x + 1, y + 1);

                    Tile t = terrain.getTile(p1, p2, p3);

                    drawTile(g, t, p1, p2, p3);
                }
            }

            /* Draw downwards triangles */
            if (y > 0) {
                for (x = startX; x < wip; x += 2) {
                    Point p1 = new Point(x, y);
                    Point p2 = new Point(x + 2, y);
                    Point p3 = new Point(x + 1, y - 1);

                    Tile t = terrain.getTile(p1, p2, p3);

                    drawTile(g, t, p1, p2, p3);
                }
            }

            rowOffsetFlip = !rowOffsetFlip;
        }

        terrainPrerenderedWidthInPoints = wip;
        terrainPrerenderedHeightInPoints = hip;
            
        return image;
    }

    private void drawTile(Graphics2D g, Tile t, Point p1, Point p2, Point p3) {
        Paint oldPaint = g.getPaint();

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

        drawer.fillScaledTriangle(g, p1, p2, p3);

        g.setPaint(oldPaint);
    }

    private void drawPersons(Graphics2D g, List<Worker> workers) {

        for (Worker w : workers) {
            if (w.isInsideBuilding()) {
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

        g.fillOval((int)(actualX*drawer.getScaleX()) - drawer.offsetScaleX(4),
                   height - (int)(actualY*drawer.getScaleY()) - drawer.offsetScaleY(10), 
                   drawer.simpleScaleX(5), 
                   drawer.simpleScaleY(15));

        if (w.getCargo() != null ) {
            Color cargoColor = getColorForMaterial(w.getCargo().getMaterial());

            g.setColor(cargoColor);
            g.fillRect((int)(actualX*drawer.getScaleX()) - drawer.offsetScaleX(2),
                       height - (int)(actualY*drawer.getScaleY()) - drawer.offsetScaleY(6),
                       drawer.simpleScaleX(5),
                       drawer.simpleScaleY(5));
        }
    }

    private void drawAvailableSpots(Graphics2D g, Player player) throws Exception {
        Map<Point, Size> houses = map.getAvailableHousePoints(player);

        /* Draw the available houses */
        for (Map.Entry<Point, Size> pair : houses.entrySet()) {
            drawAvailableHouse(g, pair.getKey(), pair.getValue());
        }

        /* Draw the available flags */
        for (Point p : map.getAvailableFlagPoints(player)) {
            if (houses.keySet().contains(p)) {
                continue;
            }

            drawAvailableFlag(g, p);
        }

        /* Draw the available mines */
        for (Point p : map.getAvailableMinePoints(player)) {
            drawAvailableMine(g, p);
        }
    }

    private void drawAvailableFlag(Graphics2D g, Point p) {

        g.setColor(Color.ORANGE);
        drawer.fillScaledRect(g, p, 2, 10, 5, -5);

        g.setColor(Color.BLACK);
        drawer.drawScaledRect(g, p, 2, 10, 5, -5);
    }


    private void drawAvailableHouse(Graphics2D g, Point key, Size value) {

        int houseWidth = 5;
        int houseHeight = 5;

        int verticalSpace = 2;

        /* Draw box for small houses */
        g.setColor(AVAILABLE_CONSTRUCTION_COLOR);
        drawer.fillScaledRect(g, key, houseWidth, houseHeight, -2, 0);

        g.setColor(Color.BLACK);
        drawer.drawScaledRect(g, key, houseWidth, houseHeight, -2, 0);

        /* Draw box for medium houses */
        if (value != SMALL) {

            g.setColor(AVAILABLE_CONSTRUCTION_COLOR);
            drawer.fillScaledRect(g, key, houseWidth, houseHeight, -2, -houseHeight - verticalSpace);

            g.setColor(Color.BLACK);
            drawer.drawScaledRect(g, key, houseWidth, houseHeight, -2, -houseHeight - verticalSpace);
        }

        if (value == LARGE) {

            g.setColor(AVAILABLE_CONSTRUCTION_COLOR);
            drawer.fillScaledRect(g, key, houseWidth, houseHeight, -2, -2*houseHeight - 2*verticalSpace);

            g.setColor(Color.BLACK);
            drawer.drawScaledRect(g, key, houseWidth, houseHeight, -2, -2*houseHeight - 2*verticalSpace);
        }
    }

    private void drawSelectedPoint(Graphics2D g, Point p) {

        g.setColor(SELECTED_POINT_COLOR);
        drawer.fillScaledOval(g, p, 10, 10, -5, -5);

        g.setColor(Color.DARK_GRAY);
        drawer.drawScaledOval(g, p, 10, 10, -5, -5);
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
        default:
            return Color.RED;
        }
    }

    void recalculateScale(int w, int h) {
        width  = w;
        height = h;

        drawer.recalculateScale(width, height);

        try {
            terrainImage = createTerrainTexture(width, height, widthInPoints, heightInPoints);
        } catch (Exception ex) {
            Logger.getLogger(GameDrawer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    void setMap(GameMap m) {
        map = m;

        try {
            terrainImage = createTerrainTexture(width, height, widthInPoints, heightInPoints);
        } catch (Exception ex) {
            Logger.getLogger(GameDrawer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    void zoomIn(int notches) throws Exception {
        if (widthInPoints < 10 || heightInPoints < 10) {
            return;
        }
        
        widthInPoints--;
        heightInPoints--;

        drawer.changeZoom(widthInPoints, heightInPoints);
        
        terrainImage = createTerrainTexture(width, height, widthInPoints, heightInPoints);
    }

    void zoomOut(int notches) throws Exception {
        if (widthInPoints > map.getWidth() || heightInPoints > map.getHeight()) {
            return;
        }
        
        widthInPoints++;
        heightInPoints++;

        drawer.changeZoom(widthInPoints, heightInPoints);

        terrainImage = createTerrainTexture(width, height, widthInPoints, heightInPoints);
    }

    private TexturePaint createBrushFromImageResource(String res) {
        try {

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
        } catch (IOException ex) {
            Logger.getLogger(GameDrawer.class.getName()).log(Level.SEVERE, null, ex);
        }

        /* Return null if the image didn't load correctly */
        return null;
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

    private void drawHoveringPoint(Graphics2D g, Point p) {

        g.setColor(HOVERING_COLOR);
        drawer.fillScaledOval(g, p, 10, 10, -5, -5);

        g.setColor(Color.DARK_GRAY);
        drawer.drawScaledOval(g, p, 10, 10, -5, -5);
    }

    private void drawAvailableMine(Graphics2D g, Point p) {
        g.setColor(Color.ORANGE);
        drawer.fillScaledOval(g, p, 6, 6, -3, -3);

        g.setColor(Color.BLACK);
        drawer.drawScaledOval(g, p, 6, 6, -3, -3);
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
}
