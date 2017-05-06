package org.appland.settlers.javaview;

import java.io.BufferedReader;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.appland.settlers.model.Building;
import org.appland.settlers.model.GameMap;
import org.appland.settlers.model.Player;
import org.appland.settlers.model.Point;
import org.appland.settlers.model.Road;
import org.appland.settlers.model.Terrain;
import org.appland.settlers.model.Tile;
import org.appland.settlers.model.Worker;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class RestServer extends AbstractHandler {

    private final String PLAYER_PARAM = "player";

    private GameMap map;
    private final int port;
    private int speed;

    public RestServer(GameMap map, int port) {
        this.map = map;
        this.port = port;
    }

    void setGameSpeed(int speed) {
        this.speed = speed;
    }

    @Override
    public void handle(String target,
                       Request baseRequest,
                       HttpServletRequest request,
                       HttpServletResponse response)
        throws IOException, ServletException {
        System.out.println("Target: " + target);
        System.out.println("Method: " + request.getMethod());
        
        while (request.getAttributeNames().hasMoreElements()) {
            System.out.println("  " + request.getAttributeNames().nextElement());
        }

        for (Entry<String, String[]> pair : request.getParameterMap().entrySet()) {
            System.out.println("  - " + pair.getKey() + " " + Arrays.asList(pair.getValue()));
        }

        /* Return the terrain. Provide the full terrain to avoid sending it more than once */
        if (target.equals("/terrain")) {
            JSONObject jsonTerrain = new JSONObject();

            jsonTerrain.put("width", map.getWidth());
            jsonTerrain.put("height", map.getHeight());

            JSONArray jsonTrianglesBelow = new JSONArray();
            JSONArray jsonTrianglesBelowRight = new JSONArray();

            jsonTerrain.put("straightBelow", jsonTrianglesBelow);
            jsonTerrain.put("belowToTheRight", jsonTrianglesBelowRight);

            int start = 1;
            Terrain terrain = map.getTerrain();

            for (int y = 1; y < map.getHeight(); y++) {
                for (int x = start; x + 1 < map.getWidth(); x+=2) {
                    Point p = new Point(x, y);

                    Tile below = terrain.getTile(p.downLeft(), p, p.downRight());
                    Tile belowRight = terrain.getTile(p, p.downRight(), p.right());

                    jsonTrianglesBelow.add(below.getVegetationType().name());
                    jsonTrianglesBelowRight.add(belowRight.getVegetationType().name());
                }

                if (start == 1) {
                    start = 2;
                } else {
                    start = 1;
                }
            }

            response.setContentType("application/json;charset=utf-8");
            response.setStatus(HttpServletResponse.SC_OK);
            response.setHeader("Access-Control-Allow-Origin", "*");
            baseRequest.setHandled(true);
            response.getWriter().println(jsonTerrain.toJSONString());
            System.out.println("Responding with " + jsonTerrain.toJSONString());

            return;
        }

        /* Give the whole world as the given player sees it */
        if (target.equals("/viewForPlayer")) {
            int playerId = Integer.parseInt(request.getParameterValues(PLAYER_PARAM)[0]);

            Player player = map.getPlayers().get(playerId);

            JSONObject view = new JSONObject();

            /* Fill in houses */
            view.put("houses", getBuildingsAsJsonList(player.getBuildings()));

            /* Fill in workers */
            JSONArray workers = new JSONArray();
            view.put("workers", workers);

            for (Worker worker : map.getWorkers()) {
                if (worker.getPlayer().equals(player)) {
                    JSONObject jsonWorker = workerToJson(worker);

                    workers.add(jsonWorker);
                }
            }

            /* Fill in roads */
            JSONArray jsonRoads = new JSONArray();
            view.put("roads", jsonRoads);

            for (Road road : map.getRoads()) {

                /* Filter roads the player cannot see */
                if (!player.getDiscoveredLand().contains(road.getStart()) ||
                    !player.getDiscoveredLand().contains(road.getEnd())) {
                    continue;
                }

                JSONObject jsonRoad = new JSONObject();

                JSONArray jsonPoints = new JSONArray();

                for (Point p : road.getWayPoints()) {
                    JSONObject jsonPoint = new JSONObject();

                    jsonPoint.put("x", p.x);
                    jsonPoint.put("y", p.y);

                    jsonPoints.add(jsonPoint);
                }

                jsonRoad.put("points", jsonPoints);

                jsonRoads.add(jsonRoad);
            }

            /* Return some metadata about the game */
            view.put("speed", speed);

            response.setContentType("application/json;charset=utf-8");
            response.setStatus(HttpServletResponse.SC_OK);
            response.setHeader("Access-Control-Allow-Origin", "*");
            baseRequest.setHandled(true);
            response.getWriter().println(view.toJSONString());
            System.out.println("Responding with " + view.toJSONString());
            return;

        /* Return houses */
        } else if (target.equals("/houses")) {
            JSONArray list = getBuildingsAsJsonList(map.getBuildings());

            response.setContentType("application/json;charset=utf-8");
            response.setStatus(HttpServletResponse.SC_OK);
            response.setHeader("Access-Control-Allow-Origin", "*");
            baseRequest.setHandled(true);
            response.getWriter().println(list.toJSONString());

            return;
        }

        BufferedReader br = request.getReader();

        while (true) {
            String s = br.readLine();

            if (s == null) {
                break;
            }

            System.out.println("Body: " + s);
        }

        response.setContentType("text/html;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        baseRequest.setHandled(true);
        response.getWriter().println("<h1>Hello World</h1>");
    }

    private JSONObject workerToJson(Worker worker) {
        JSONObject jsonWorker = new JSONObject();
        jsonWorker.put("x", worker.getPosition().x);
        jsonWorker.put("y", worker.getPosition().y);
        jsonWorker.put("type", worker.getClass().getSimpleName());
        jsonWorker.put("inside", worker.isInsideBuilding());
        jsonWorker.put("between_points", !worker.isExactlyAtPoint());
        if (!worker.isExactlyAtPoint()) {
            jsonWorker.put("previous_x", worker.getLastPoint().x);
            jsonWorker.put("previous_y", worker.getLastPoint().y);
            
            try {
                jsonWorker.put("next_x", worker.getNextPoint().x);
                jsonWorker.put("next_y", worker.getNextPoint().y);
            } catch(Exception e) {
                System.out.println("" + e);
            }
            
            jsonWorker.put("percentage_traveled", worker.getPercentageOfDistanceTraveled());
            jsonWorker.put("speed", 10); // TODO: dynamically look up speed
        } else {
            jsonWorker.put("percentage_traveled", 0);
        }
        return jsonWorker;
    }

    private JSONArray getBuildingsAsJsonList(List<Building> buildings) {
        JSONArray list = new JSONArray();
        for (Building building : buildings) {
            JSONObject obj = new JSONObject();
            
            obj.put("x", building.getPosition().getX());
            obj.put("y", building.getPosition().getY());
            
            obj.put("type", building.getClass().getSimpleName());
            obj.put("player", building.getPlayer().getName());
            
            list.add(obj);
        }
        return list;
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
}