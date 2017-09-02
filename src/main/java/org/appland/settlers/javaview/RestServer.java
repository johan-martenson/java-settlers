package org.appland.settlers.javaview;

import java.awt.Color;
import java.io.BufferedReader;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.appland.settlers.model.Bakery;
import org.appland.settlers.model.Barracks;
import org.appland.settlers.model.Building;
import org.appland.settlers.model.Catapult;
import org.appland.settlers.model.CoalMine;
import org.appland.settlers.model.DonkeyFarm;
import org.appland.settlers.model.Farm;
import org.appland.settlers.model.Fishery;
import org.appland.settlers.model.Flag;
import org.appland.settlers.model.ForesterHut;
import org.appland.settlers.model.Fortress;
import org.appland.settlers.model.GameMap;
import org.appland.settlers.model.GoldMine;
import org.appland.settlers.model.GraniteMine;
import org.appland.settlers.model.GuardHouse;
import org.appland.settlers.model.Headquarter;
import org.appland.settlers.model.HunterHut;
import org.appland.settlers.model.IronMine;
import org.appland.settlers.model.Material;
import org.appland.settlers.model.Mill;
import org.appland.settlers.model.Mint;
import org.appland.settlers.model.PigFarm;
import org.appland.settlers.model.Player;
import org.appland.settlers.model.Point;
import org.appland.settlers.model.Quarry;
import org.appland.settlers.model.Road;
import org.appland.settlers.model.Sawmill;
import org.appland.settlers.model.Sign;
import org.appland.settlers.model.Size;
import org.appland.settlers.model.SlaughterHouse;
import org.appland.settlers.model.Stone;
import org.appland.settlers.model.Terrain;
import org.appland.settlers.model.Tile;
import org.appland.settlers.model.Tile.Vegetation;
import org.appland.settlers.model.Tree;
import org.appland.settlers.model.WatchTower;
import org.appland.settlers.model.Well;
import org.appland.settlers.model.WildAnimal;
import org.appland.settlers.model.Woodcutter;
import org.appland.settlers.model.Worker;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class RestServer extends AbstractHandler implements View {

    private final String PLAYER_PARAM = "player";

    private GameMap map;
    private final int port;
    private final Pattern INDIVIDUAL_FLAG  = Pattern.compile("/flags/([0-9A-Za-z]+)/?");
    private final Pattern INDIVIDUAL_HOUSE = Pattern.compile("/houses/([0-9A-Za-z]+)/?");
    private Map<Object, Integer> objectToId;
    private int ids;
    private Map<Integer, Object> idToObject;
    private final Game game;
    private final Map<Pattern, String[]> allowedMethods;

    public RestServer(GameMap map, int port, Game game) {
        this.map = map;
        this.port = port;
        this.game = game;

        this.objectToId = new HashMap<>();
        this.idToObject = new HashMap<>();
        this.ids = 0;
        this.allowedMethods = new HashMap<>();

        /* Define the allowed methods for each endpoint*/
        this.allowedMethods.put(Pattern.compile("/terrain"),       new String[] {"GET"});
        this.allowedMethods.put(Pattern.compile("/buildings"),     new String[] {"GET"});
        this.allowedMethods.put(Pattern.compile("/points"),        new String[] {"GET", "PUT"});
        this.allowedMethods.put(Pattern.compile("/flags"),         new String[] {"POST"});
        this.allowedMethods.put(Pattern.compile("/flags/.*"),      new String[] {"DELETE"});
        this.allowedMethods.put(Pattern.compile("/players"),       new String[] {"GET"});
        this.allowedMethods.put(Pattern.compile("/roads"),         new String[] {"POST"});
        this.allowedMethods.put(Pattern.compile("/game"),          new String[] {"PUT", "GET"});
        this.allowedMethods.put(Pattern.compile("/houses"),        new String[] {"POST"});
        this.allowedMethods.put(Pattern.compile("/houses/.*"),     new String[] {"DELETE", "PUT"});
        this.allowedMethods.put(Pattern.compile("/viewForPlayer"), new String[] {"GET"});
    }

    @Override
    public void handle(String target,
                       Request baseRequest,
                       HttpServletRequest request,
                       HttpServletResponse response)
        throws IOException, ServletException {

        /* Answer OPTIONS requests */
        if (request.getMethod().equals("OPTIONS")) {
            System.out.println("Getting options for " + target);

            for (Pattern pattern : this.allowedMethods.keySet()) {
                if (pattern.matcher(target).matches()) {
                    String methods = joinArray(this.allowedMethods.get(pattern), ", ");
                    System.out.println("Answering with " + methods);

                    response.setContentType("application/json;charset=utf-8");
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.setHeader("Access-Control-Allow-Origin", "*");
                    response.setHeader("Access-Control-Allow-Methods", methods);
                    baseRequest.setHandled(true);
                    response.getWriter().println(messageToJson("Request is OK"));

                    return;
                }

                response.setContentType("application/json;charset=utf-8");
                response.setStatus(HttpServletResponse.SC_OK);
                response.setHeader("Access-Control-Allow-Origin", "*");
                response.setHeader("Access-Control-Allow-Methods", "");
                baseRequest.setHandled(true);
                response.getWriter().println(messageToJson("Request is OK"));
            }

            return;
        }
        
        /* Return the terrain. Provide the full terrain to avoid sending it more than once */
        if (target.equals("/terrain")) {
            JSONObject jsonTerrain = new JSONObject();

            JSONArray jsonTrianglesBelow = new JSONArray();
            JSONArray jsonTrianglesBelowRight = new JSONArray();

            jsonTerrain.put("straightBelow", jsonTrianglesBelow);
            jsonTerrain.put("belowToTheRight", jsonTrianglesBelowRight);

            int start = 1;

            synchronized (map) {
                jsonTerrain.put("width", map.getWidth());
                jsonTerrain.put("height", map.getHeight());

                Terrain terrain = map.getTerrain();

                for (int y = 1; y < map.getHeight(); y++) {
                    for (int x = start; x + 1 < map.getWidth(); x += 2) {
                        Point p = new Point(x, y);

                        Tile below = terrain.getTile(p.downLeft(), p, p.downRight());
                        Tile belowRight = terrain.getTile(p, p.downRight(), p.right());

                        jsonTrianglesBelow.add(vegetationToJson(below.getVegetationType()));
                        jsonTrianglesBelowRight.add(vegetationToJson(belowRight.getVegetationType()));
                    }

                    if (start == 1) {
                        start = 2;
                    } else {
                        start = 1;
                    }
                }
            }

            replyWithJson(response, baseRequest, jsonTerrain);

            return;
        }

        /* Return information about the given building */
        if (target.equals("/buildings") && request.getMethod().equals("GET")) {
            int houseId = Integer.parseInt(request.getParameter("buildingId"));

            String playerIdString = request.getParameter("playerId");

            Player player = null;

            if (playerIdString != null) {
                player = (Player)getObjectFromId(Integer.parseInt(playerIdString));
            }

            Building building = (Building)getObjectFromId(houseId);
            int playerId = getId(building.getPlayer());

            /* Instantiate JSON objects outside the synchronized scope */
            JSONObject jsonHouse = null;
            JSONObject jsonInventory = new JSONObject();

            synchronized (map) {
                jsonHouse = houseToJson(building, playerId);

                jsonHouse.put("inventory", jsonInventory);

                for (Material m : Material.values()) {
                    jsonInventory.put(m.name(), building.getAmount(m));
                }

                /* Fill in attack information if the player is included */
                if (player != null && building.isMilitaryBuilding()) {
                    try {
                        jsonHouse.put("maxAttackers", player.getAvailableAttackersForBuilding(building));
                    } catch (Exception ex) {
                        Logger.getLogger(RestServer.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }

            replyWithJson(response, baseRequest, jsonHouse);

            return;
        }

        /* Handle update for the given point */
        if (target.equals("/points") && request.getMethod().equals("PUT")) {
            Point point = getPointFromParameters(request);

            try {
                JSONObject jsonPointUpdate = requestBodyToJson(request);

                synchronized (map) {
                    if (jsonPointUpdate.containsKey("geologistNeeded") &&
                        (Boolean)jsonPointUpdate.get("geologistNeeded")) {
                        map.getFlagAtPoint(point).callGeologist();

                        System.out.println("Called geologist");
                    } else if (jsonPointUpdate.containsKey("scoutNeeded") &&
                               (Boolean)jsonPointUpdate.get("scoutNeeded")) {
                        map.getFlagAtPoint(point).callScout();

                        System.out.println("Called scout");
                    }
                }
            } catch (Exception ex) {
                Logger.getLogger(RestServer.class.getName()).log(Level.SEVERE, null, ex);
            }

            replyWithJson(response, baseRequest, messageToJson("Updated point"));

            return;            
        }

        /* Return information about the given point */
        if (target.equals("/points") && request.getMethod().equals("GET")) {
            int playerId = Integer.parseInt(request.getParameter("playerId"));
            Point point = getPointFromParameters(request);

            JSONObject jsonPointInfo = new JSONObject();

            putPointAsJson(jsonPointInfo, point);

            Player player = (Player)getObjectFromId(playerId);

            synchronized (map) {
                if (player.getDiscoveredLand().contains(point)) {

                    if (map.isBuildingAtPoint(point)) {
                        Building building = map.getBuildingAtPoint(point);
                        jsonPointInfo.put("building", houseToJson(building, playerId));
                        jsonPointInfo.put("is", "building");
                    }

                    if (map.isFlagAtPoint(point)) {
                        jsonPointInfo.put("is", "flag");
                    }

                    JSONArray canBuild = new JSONArray();
                    jsonPointInfo.put("canBuild", canBuild);

                    try {
                        if (map.isAvailableFlagPoint(player, point)) {
                            canBuild.add("flag");
                        }

                        if (map.isAvailableMinePoint(player, point)) {
                            canBuild.add("mine");
                        }

                        Size size = map.isAvailableHousePoint(player, point);

                        if (size != null) {
                            if (size == Size.LARGE) {
                                canBuild.add("large");
                                canBuild.add("medium");
                                canBuild.add("small");
                            } else if (size == Size.MEDIUM) {
                                canBuild.add("medium");
                                canBuild.add("small");
                            } else if (size == Size.SMALL) {
                                canBuild.add("small");
                            }
                        }

                        /* Fill in available connections for a new road */
                        JSONArray jsonPossibleConnections = new JSONArray();
                        jsonPointInfo.put("possibleRoadConnections", jsonPossibleConnections);
                        for (Point possibleConnection : map.getPossibleAdjacentRoadConnectionsIncludingEndpoints(player, point)) {
                            jsonPossibleConnections.add(pointToJson(possibleConnection));
                        }
                    } catch (Exception ex) {
                        Logger.getLogger(RestServer.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }

                replyWithJson(response, baseRequest, jsonPointInfo);

                return;
            }
        }

        Matcher individualFlagMatcher = INDIVIDUAL_FLAG.matcher(target);

        /* Remove the given flag */
        if (individualFlagMatcher.matches() && request.getMethod().equals("DELETE")) {
            
            String flagIdString = individualFlagMatcher.group(1);
            System.out.println("Try to remove flag at " + flagIdString);

            if (flagIdString != null && !flagIdString.equals("")) {
                int flagId = Integer.parseInt(flagIdString);

                Flag flag = (Flag)getObjectFromId(flagId);
                System.out.println("Got flag");
                try {
                    synchronized (map) {
                        map.removeFlag(flag);
                    }
                    System.out.println("Removed flag");
                } catch (Exception ex) {
                    Logger.getLogger(RestServer.class.getName()).log(Level.SEVERE, null, ex);
                }

                replyWithJson(response, baseRequest, messageToJson("Removed flag " + flagId));

                return;
            }
        }

        Matcher individualHouseMatcher = INDIVIDUAL_HOUSE.matcher(target);

        /* Handle requests for an individual house */
        if (individualHouseMatcher.matches()) {

            String houseIdString = individualHouseMatcher.group(1);

            if (houseIdString != null && !houseIdString.equals("")) {
                int houseId = Integer.parseInt(houseIdString);

                Building building = (Building)getObjectFromId(houseId);

                /* Remove the given house */
                if (request.getMethod().equals("DELETE")) {

                    try {
                        synchronized (map) {
                            building.tearDown();
                        }
                    } catch (Exception ex) {
                        Logger.getLogger(RestServer.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }

                /* Update the given house (can only be used to trigger attacks) */
                if (request.getMethod().equals("PUT")) {
                    try {
                        JSONObject body = requestBodyToJson(request);

                        JSONObject attackJson = (JSONObject) body.get("attacked");

                        System.out.println("Attacker " + attackJson.get("by"));
                        System.out.println(((Long)attackJson.get("by")).intValue());
                        System.out.println("Attackers " + attackJson.get("attackers"));
                        System.out.println(((Long)attackJson.get("attackers")).intValue());
                    } catch (ParseException ex) {
                        Logger.getLogger(RestServer.class.getName()).log(Level.SEVERE, null, ex);
                    }

                }

                replyWithJson(response, baseRequest, messageToJson("Removed building " + houseIdString));

                return;
            }
        }

        /* Return the list of players */
        if (target.equals("/players") && request.getMethod().equals("GET")) {
            JSONArray jsonPlayers = playersToJson();

            replyWithJson(response, baseRequest, jsonPlayers);

            return;
        }

        /* Handle create road */
        if (target.equals("/roads") && request.getMethod().equals("POST")) {
            try {
                JSONObject jsonRoad = requestBodyToJson(request);

                /* Get the points */
                JSONArray jsonPoints = (JSONArray) jsonRoad.get("points");

                List<Point> points = new ArrayList<Point>();

                for (int i = 0; i < jsonPoints.size(); i++) {
                    JSONObject jsonPoint = (JSONObject) jsonPoints.get(i);
                    Point point = jsonToPoint(jsonPoint);

                    points.add(point);
                }

                /* Get the player */
                int playerId = ((Long)jsonRoad.get("playerId")).intValue();

                Player player = (Player)getObjectFromId(playerId);

                /* Create the road */
                synchronized (map) {
                    if (points.size() == 2) {
                        map.placeAutoSelectedRoad(player, points.get(0), points.get(1));
                    } else {
                        map.placeRoad(player, points);
                    }
                }

                replyWithJson(response, baseRequest, messageToJson("Created road OK"));

                return;
            } catch (Exception ex) {
                Logger.getLogger(RestServer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        /* Handle raise flag */
        if (target.equals("/flags") && request.getMethod().equals("POST")) {
            try {
                JSONObject jsonFlag = requestBodyToJson(request);

                Point point = jsonToPoint(jsonFlag);

                int playerId = ((Long)jsonFlag.get("playerId")).intValue();

                Player player = (Player)getObjectFromId(playerId);

                synchronized (map) {
                    map.placeFlag(player, point);
                }

                replyWithJson(response, baseRequest, messageToJson("Raised flag OK"));

                return;
            } catch (Exception ex) {
                Logger.getLogger(RestServer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        /* Return the game */
        if (target.equals("/game") && request.getMethod().equals("GET")) {

            JSONObject jsonGame = new JSONObject();

            jsonGame.put("tickLength", game.getSpeed());

            replyWithJson(response, baseRequest, jsonGame);

            return;
        }

        /* Handle update game */
        if (target.equals("/game") && request.getMethod().equals("PUT")) {
            try {
                JSONObject jsonGame = requestBodyToJson(request);
                int speed = ((Long)jsonGame.get("tickLength")).intValue();

                game.setSpeed(speed);
            } catch (ParseException ex) {
                Logger.getLogger(RestServer.class.getName()).log(Level.SEVERE, null, ex);
            }

            replyWithJson(response, baseRequest, messageToJson("Set speed OK"));

            return;
        }

        /* Handle create house */
        if (target.equals("/houses") && request.getMethod().equals("POST")) {
            try {
                JSONObject jsonHouse = requestBodyToJson(request);

                Point point = jsonToPoint(jsonHouse);
                int playerId = ((Long)jsonHouse.get("playerId")).intValue();
                Player player = (Player)getObjectFromId(playerId);

                Building building = buildingFactory(jsonHouse, player);

                synchronized (map) {
                    map.placeBuilding(building, point);
                }

                replyWithJson(response, baseRequest, messageToJson("Created building OK"));

                return;
            } catch (Exception ex) {
                Logger.getLogger(RestServer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        /* Give the whole world as the given player sees it */
        if (target.equals("/viewForPlayer")) {
            int playerId = Integer.parseInt(request.getParameterValues(PLAYER_PARAM)[0]);

            Player player = (Player)getObjectFromId(playerId);

            if (player == null) {
                replyWithInvalidParameter(response, baseRequest, "Player does not exist " + playerId);

                return;
            }

            /* Create instances outside the synchronized block when possible */
            JSONObject view = new JSONObject();

            JSONArray jsonHouses = new JSONArray();
            JSONArray trees = new JSONArray();
            JSONArray jsonStones = new JSONArray();
            JSONArray workers = new JSONArray();
            JSONArray jsonFlags = new JSONArray();
            JSONArray jsonRoads = new JSONArray();
            JSONArray jsonDiscoveredPoints = new JSONArray();
            JSONArray jsonBorders = new JSONArray();
            JSONArray jsonSigns = new JSONArray();
            JSONArray jsonAnimals = new JSONArray();

            view.put("trees", trees);
            view.put("houses", jsonHouses);
            view.put("stones", jsonStones);
            view.put("workers", workers);
            view.put("flags", jsonFlags);
            view.put("roads", jsonRoads);
            view.put("discoveredPoints", jsonDiscoveredPoints);
            view.put("borders", jsonBorders);
            view.put("signs", jsonSigns);
            view.put("animals", jsonAnimals);

            /* Protect access to the map to avoid interference */
            synchronized(map) {
                Set<Point> discoveredLand = player.getDiscoveredLand();

                /* Fill in houses */
                for (Building building : map.getBuildings()) {

                    if (!discoveredLand.contains(building.getPosition())) {
                        continue;
                    }

                    jsonHouses.add(houseToJson(building, playerId));
                }

                /* Fill in trees */
                for (Tree tree : map.getTrees()) {
                    if (!discoveredLand.contains(tree.getPosition())) {
                        continue;
                    }

                    trees.add(treeToJson(tree));
                }

                /* Fill in stones */
                for (Stone stone : map.getStones()) {

                    if (!discoveredLand.contains(stone.getPosition())) {
                        continue;
                    }

                    jsonStones.add(stoneToJson(stone));
                }

                /* Fill in workers */
                for (Worker worker : map.getWorkers()) {

                    if (!discoveredLand.contains(worker.getPosition())) {
                        continue;
                    }

                    if (worker.isInsideBuilding()) {
                        continue;
                    }

                    workers.add(workerToJson(worker));
                }

                /* Fill in flags */
                for (Flag flag : map.getFlags()) {

                    if (!discoveredLand.contains(flag.getPosition())) {
                        continue;
                    }

                    jsonFlags.add(flagToJson(flag, getId(flag), playerId));
                }

                /* Fill in roads */
                for (Road road : map.getRoads()) {

                    boolean inside = false;

                    /* Filter roads the player cannot see */
                    for (Point p : road.getWayPoints()) {
                        if (discoveredLand.contains(p)) {
                            inside = true;

                            break;
                        }
                    }

                    if (!inside) {
                        continue;
                    }

                    jsonRoads.add(roadToJson(road));
                }

                /* Fill in the points the player has discovered */
                for (Point point : discoveredLand) {
                    jsonDiscoveredPoints.add(pointToJson(point));
                }

                jsonBorders.add(borderToJson(player, playerId));

                /* Fill in the signs */
                for (Sign sign : map.getSigns()) {

                    if (!discoveredLand.contains(sign.getPosition())) {
                        continue;
                    }

                    jsonSigns.add(signToJson(sign));
                }

                /* Fill in wild animals */
                for (WildAnimal animal : map.getWildAnimals()) {

                    if (!discoveredLand.contains(animal.getPosition())) {
                        continue;
                    }

                    /* Animal is an extension of worker so the same method is used */

                    jsonAnimals.add(workerToJson(animal));
                }
            }

            replyWithJson(response, baseRequest, view);

            return;
        }

        System.out.println("SHOULD NOT END UP HERE");
        System.out.println("Target: " + target);
        System.out.println("Method: " + request.getMethod());
        
        while (request.getAttributeNames().hasMoreElements()) {
            System.out.println("  " + request.getAttributeNames().nextElement());
        }

        for (Entry<String, String[]> pair : request.getParameterMap().entrySet()) {
            System.out.println("  - " + pair.getKey() + " " + Arrays.asList(pair.getValue()));
        }

        System.exit(1);
    }

    private void replyWithJson(HttpServletResponse response, Request baseRequest, JSONArray jsonArray) throws IOException {
        response.setContentType("application/json;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        response.setHeader("Access-Control-Allow-Origin", "*");
        baseRequest.setHandled(true);
        response.getWriter().println(jsonArray.toJSONString());
    }
    
    private void replyWithJson(HttpServletResponse response, Request baseRequest, JSONObject jsonObject) throws IOException {
        response.setContentType("application/json;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        response.setHeader("Access-Control-Allow-Origin", "*");
        baseRequest.setHandled(true);
        response.getWriter().println(jsonObject.toJSONString());
    }

    private void putPointAsJson(JSONObject jsonPointInfo, Point point) {
        jsonPointInfo.put("x", point.x);
        jsonPointInfo.put("y", point.y);
    }

    private JSONObject borderToJson(Player player, int playerId) {
        /* Fill in borders */
        JSONObject jsonBorder = new JSONObject();
        jsonBorder.put("color", colorToHexString(player.getColor()));
        jsonBorder.put("playerId", playerId);
        JSONArray jsonBorderPoints = new JSONArray();
        jsonBorder.put("points", jsonBorderPoints);
        for (Collection<Point> border : player.getBorders()) {
            for (Point point : border) {
                jsonBorderPoints.add(pointToJson(point));
            }
        }
        return jsonBorder;
    }

    private JSONObject signToJson(Sign sign) {
        JSONObject jsonSign = new JSONObject();

        if (sign.isEmpty()) {
            jsonSign.put("type", null);
        } else {
            switch (sign.getType()) {
                case GOLD:
                    jsonSign.put("type", "gold");
                    break;
                case IRON:
                    jsonSign.put("type", "iron");
                    break;
                case COAL:
                    jsonSign.put("type", "coal");
                    break;
                case STONE:
                    jsonSign.put("type", "granite");
                    break;
                case WATER:
                    jsonSign.put("type", "water");
                    break;
                default:
                    System.out.println("Cannot have " + sign.getType());
                    System.exit(1);
            }
        }

        putPointAsJson(jsonSign, sign.getPosition());
        return jsonSign;
    }

    private Building buildingFactory(JSONObject jsonHouse, Player player) {
        Building building = null;
        switch((String)jsonHouse.get("type")) {
            case "Forester":
                building = new ForesterHut(player);
                break;
            case "Woodcutter":
                building = new Woodcutter(player);
                break;
            case "Quarry":
                building = new Quarry(player);
                break;
            case "Headquarter":
                building = new Headquarter(player);
                break;
            case "Sawmill":
                building = new Sawmill(player);
                break;
            case "Farm":
                building = new Farm(player);
                break;
            case "Barracks":
                building = new Barracks(player);
                break;
            case "Well":
                building = new Well(player);
                break;
            case "Mill":
                building = new Mill(player);
                break;
            case "Bakery":
                building = new Bakery(player);
                break;
            case "Fishery":
                building = new Fishery(player);
                break;
            case "Goldmine":
                building = new GoldMine(player);
                break;
            case "Ironmine":
                building = new IronMine(player);
                break;
            case "Coalmine":
                building = new CoalMine(player);
                break;
            case "Granitemine":
                building = new GraniteMine(player);
                break;
            case "Pigfarm":
                building = new PigFarm(player);
                break;
            case "Mint":
                building = new Mint(player);
                break;
            case "Slaughterhouse":
                building = new SlaughterHouse(player);
                break;
            case "Donkeyfarm":
                building = new DonkeyFarm(player);
                break;
            case "Guardhouse":
                building = new GuardHouse(player);
                break;
            case "Watchtower":
                building = new WatchTower(player);
                break;
            case "Fortress":
                building = new Fortress(player);
                break;
            case "Catapult":
                building = new Catapult(player);
                break;
            case "Hunterhut":
                building = new HunterHut(player);
                break;
            default:
                System.exit(1);
        }
        return building;
    }

    private JSONArray playersToJson() {
        JSONArray jsonPlayers = new JSONArray();

        synchronized (map) {
            for (Player player : map.getPlayers()) {
                JSONObject jsonPlayer = playerToJson(player, getId(player));

                jsonPlayers.add(jsonPlayer);
            }
        }

        return jsonPlayers;
    }

    private Point getPointFromParameters(HttpServletRequest request) throws NumberFormatException {
        int x = Integer.parseInt(request.getParameter("x"));
        int y = Integer.parseInt(request.getParameter("y"));
        Point point = new Point(x, y);
        return point;
    }

    private JSONObject workerToJson(Worker worker) {
        JSONObject jsonWorker = new JSONObject();
        putPointAsJson(jsonWorker, worker.getPosition());
        jsonWorker.put("type", worker.getClass().getSimpleName());
        jsonWorker.put("inside", worker.isInsideBuilding());
        jsonWorker.put("betweenPoints", !worker.isExactlyAtPoint());

        if (!worker.isExactlyAtPoint()) {
            JSONObject jsonPrevious = new JSONObject();
            jsonWorker.put("previous", jsonPrevious);

            putPointAsJson(jsonPrevious, worker.getLastPoint());
            
            try {
                JSONObject jsonNext = new JSONObject();
                jsonWorker.put("next", jsonNext);

                putPointAsJson(jsonNext, worker.getNextPoint());
            } catch(Exception e) {
                System.out.println("" + e);
            }
            
            jsonWorker.put("percentageTraveled", worker.getPercentageOfDistanceTraveled());
            jsonWorker.put("speed", 10); // TODO: dynamically look up speed
        } else {
            jsonWorker.put("percentageTraveled", 0);
        }
        return jsonWorker;
    }

    private JSONObject houseToJson(Building building, int playerId) {
        JSONObject jsonHouse = new JSONObject();
            
        jsonHouse.put("x", building.getPosition().getX());
        jsonHouse.put("y", building.getPosition().getY());
            
        jsonHouse.put("type", building.getClass().getSimpleName());
        jsonHouse.put("playerId", playerId);
        jsonHouse.put("houseId", getId(building));

        if (building.underConstruction()) {
            jsonHouse.put("state", "unfinished");
        } else if (building.ready() && !building.occupied()) {
            jsonHouse.put("state", "unoccupied");
        } else if (building.ready() && building.occupied()) {
            jsonHouse.put("state", "occupied");
        } else if (building.burningDown()) {
            jsonHouse.put("state", "burning");
        } else if (building.destroyed()) {
            jsonHouse.put("state", "destroyed");
        }

        return jsonHouse;
    }

    protected void startServer() throws Exception {

        final RestServer handler = this;

        Thread thread = new Thread(new Runnable() {
        
            @Override
            public void run() {
                Server server = new Server(port);
                server.setHandler(handler);

                try {
                    server.start();
                    server.join();
                } catch (Exception ex) {
                    Logger.getLogger(RestServer.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });

        thread.start();
    }

    private JSONObject treeToJson(Tree tree) {
        JSONObject jsonTree = new JSONObject();

        putPointAsJson(jsonTree, tree.getPosition());

        return jsonTree;
    }

    private JSONObject playerToJson(Player player, int i) {
        JSONObject jsonPlayer = new JSONObject();

        jsonPlayer.put("name", player.getName());
        jsonPlayer.put("color", colorToHexString(player.getColor()));
        jsonPlayer.put("id", i);

        /* Get the player's "center spot" */
        for (Building building : player.getBuildings()) {
            if (building instanceof Headquarter) {
                jsonPlayer.put("centerPoint", pointToJson(building.getPosition()));

                break;
            }
        }
 
        /* Fill in the points the player has discovered */
        JSONArray jsonDiscoveredPoints = new JSONArray();
        jsonPlayer.put("discoveredPoints", jsonDiscoveredPoints);

        for (Point point : player.getDiscoveredLand()) {
            jsonDiscoveredPoints.add(pointToJson(point));
        }

        return jsonPlayer;
    }
    private JSONObject pointToJson(Point point) {
        JSONObject jsonPoint = new JSONObject();

        putPointAsJson(jsonPoint, point);

        return jsonPoint;
    }

    private JSONObject flagToJson(Flag flag, int flagId, int playerId) {
        JSONObject jsonFlag = new JSONObject();

        putPointAsJson(jsonFlag, flag.getPosition());

        jsonFlag.put("flagId", flagId);
        jsonFlag.put("playerId", playerId);

        return jsonFlag;
    }

    private JSONObject roadToJson(Road road) {
        JSONObject jsonRoad = new JSONObject();

        JSONArray jsonPoints = new JSONArray();

        for (Point p : road.getWayPoints()) {
            JSONObject jsonPoint = new JSONObject();

            putPointAsJson(jsonPoint, p);

            jsonPoints.add(jsonPoint);
        }

        jsonRoad.put("points", jsonPoints);

        return jsonRoad;
    }

    private String colorToHexString(Color c) {

        String hex = Integer.toHexString(c.getRGB() & 0xffffff);

        while(hex.length() < 6){
              hex = "0" + hex;
        }

        hex = "#" + hex;
    
        return hex;
    }

    private String vegetationToJson(Vegetation v) {
        switch (v) {
            case GRASS:
                return "G";
            case WATER:
                return "W";
            case SWAMP:
                return "S";
            case MOUNTAIN:
                return "M";
            default:
                System.out.println("Cannot handle this vegetation " + v);
                System.exit(1);
        }

        return ""; // Should never be reached but the compiler complains
    }

    private JSONObject messageToJson(String message) {
        JSONObject jsonMessage = new JSONObject();

        jsonMessage.put("statusCode", "0");
        jsonMessage.put("status", "OK");
        jsonMessage.put("message", message);

        return jsonMessage;
    }
    private Point jsonToPoint(JSONObject jsonPoint) {
        System.out.println("" + jsonPoint.get("x").getClass() );
        return new Point(((Long)jsonPoint.get("x")).intValue(),
                         ((Long)jsonPoint.get("y")).intValue());
    }

    private JSONObject stoneToJson(Stone stone) {
        JSONObject jsonStone = new JSONObject();

        putPointAsJson(jsonStone, stone.getPosition());
        jsonStone.put("amount", stone.getAmount());

        return jsonStone;
    }

    private JSONObject requestBodyToJson(HttpServletRequest request) throws IOException, ParseException {
        StringBuilder sb = new StringBuilder();
        String line = null;

        BufferedReader br = new BufferedReader(new InputStreamReader(request.getInputStream()));
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }

        return (JSONObject)(new JSONParser()).parse(sb.toString());
    }

    private int getId(Object o) {
        synchronized (objectToId) {
            if (!objectToId.containsKey(o)) {
                ids++;

                objectToId.put(o, ids);
                idToObject.put(ids, o);
            }   
        }

        return objectToId.get(o);
    }

    private Object getObjectFromId(int id) {
        return idToObject.get(id);
    }

    private String joinArray(String[] get, String separator) {
        StringBuilder sb = new StringBuilder();

        if (get.length == 0) {
            return "";
        }

        for (int i = 0; i < get.length; i++) {
            sb.append(get[i]);

            if (i -1 < get.length) {
                sb.append(separator);
            }
        }

        return sb.toString();
    }

    @Override
    public void onSaveTroubleshootingInformation() {}

    @Override
    public void setMap(GameMap map) {
        this.map = map;
    }

    @Override
    public void onGameStarted() {}

    private void replyWithInvalidParameter(HttpServletResponse response, Request baseRequest, String string) throws IOException {
        response.setContentType("application/json;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setHeader("Access-Control-Allow-Origin", "*");
        baseRequest.setHandled(true);
        response.getWriter().println(messageToJson(string));
   }
}