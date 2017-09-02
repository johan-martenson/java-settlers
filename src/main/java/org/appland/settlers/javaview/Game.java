/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.appland.settlers.javaview;

import static java.awt.Color.BLUE;
import static java.awt.Color.ORANGE;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.appland.settlers.computer.AttackPlayer;
import org.appland.settlers.computer.CoinProducer;
import org.appland.settlers.computer.CompositePlayer;
import org.appland.settlers.computer.ComputerPlayer;
import org.appland.settlers.computer.ConstructionPreparationPlayer;
import org.appland.settlers.computer.ExpandLandPlayer;
import org.appland.settlers.computer.FoodProducer;
import org.appland.settlers.computer.MiltaryProducer;
import org.appland.settlers.computer.PlayerType;
import static org.appland.settlers.computer.PlayerType.ATTACKING;
import static org.appland.settlers.computer.PlayerType.BUILDING;
import static org.appland.settlers.computer.PlayerType.COIN_PRODUCER;
import static org.appland.settlers.computer.PlayerType.COMPOSITE_PLAYER;
import static org.appland.settlers.computer.PlayerType.EXPANDING;
import static org.appland.settlers.computer.PlayerType.FOOD_PRODUCER;
import static org.appland.settlers.computer.PlayerType.MILITARY_PRODUCER;
import static org.appland.settlers.computer.PlayerType.MINERALS;
import org.appland.settlers.computer.SearchForMineralsPlayer;
import org.appland.settlers.maps.MapFile;
import org.appland.settlers.maps.MapLoader;
import org.appland.settlers.model.Building;
import org.appland.settlers.model.Flag;
import org.appland.settlers.model.GameMap;
import org.appland.settlers.model.Headquarter;
import org.appland.settlers.model.Player;
import org.appland.settlers.model.Road;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

/**
 *
 * @author johan
 */
public class Game {
    private final static String GAME_LOOP_THREAD_NAME = "Game loop timer";
    private final static int DEFAULT_WIDTH_IN_POINTS = 100;
    private final static int DEFAULT_HEIGHT_IN_POINTS = 100;

    protected final static int DEFAULT_TICK = 100;

    private final ScenarioCreator creator;
    private final List<View>      views;
    private Timer           gameLoopTimer;

    private Map<Player, ComputerPlayer> computerPlayers;
    private GameMap map;

    @Option(name="--no-graphics",
            usage="Run the game without graphics",
            required=false)
    boolean headless = false;

    @Option(name="--file", usage="Map file to load")
    String filename;

    @Option(name="--computer-player-one",
            usage="Computer player for player one",
            required=false)
    String computerPlayerOne;

    @Option(name="--computer-player-two",
            usage="Computer player for player two",
            required=false)
    String computerPlayerTwo;

    @Option(name="--tick",
            usage="The time (in milliseconds) between each step of the game",
            required=false)
    int tick = 30;

    @Option(name="--players",
            usage="The number of players (defaults to 2)",
            required=false)
    int numberOfPlayers = 2;

    @Option(name="--rest-server",
            usage="Enable REST server for remote players",
            required=false)
    boolean enableRestServer = false;

    @Option(name="--port",
            usage="Port to expose the REST server on",
            required=false)
    int port = 8080;

    public Game() throws Exception {

        views           = new ArrayList<>();
        creator         = new ScenarioCreator();
        computerPlayers = new HashMap<>();

        /* Create timer */
        gameLoopTimer   = new Timer(GAME_LOOP_THREAD_NAME);
    }

    public void resetGame() throws Exception {
        computerPlayers.clear();

        /* Create players */
        Player player0 = new Player("Player 0", BLUE);
        Player player1 = new Player("Player 1", ORANGE);

        List<Player> players = new LinkedList<>();

        players.add(player0);
        players.add(player1);

        /* Create game map */
        if (filename != null) {
            MapLoader mapLoader = new MapLoader();
            MapFile mf = mapLoader.loadMapFromFile(filename);
            map = mapLoader.convertMapFileToGameMap(mf);
            map.setPlayers(players);
        } else {

            /* Create the terrain */
            map = new GameMap(players, DEFAULT_WIDTH_IN_POINTS, DEFAULT_HEIGHT_IN_POINTS);
            creator.createInitialTerrain(map);
        }

        if (map.getStartingPoints() == null ||
            map.getStartingPoints().isEmpty()) {

            System.out.println("Placing players old-fashioned style");

            /* Place player to be controlled */
            if (numberOfPlayers > 0) {
                creator.placeInitialPlayer(player0, map);
            }

            /* Place the opponent */
            if (numberOfPlayers > 1) {
                creator.placeOpponent(player1, map);
            }
        } else {
            try {
                System.out.println("Placing players using starting points from map");
                if (numberOfPlayers > 0) {
                    map.placeBuilding(new Headquarter(player0), map.getStartingPoints().get(0));
                }

                if (numberOfPlayers > 1) {
                    map.placeBuilding(new Headquarter(player1), map.getStartingPoints().get(1));
                }
            } catch (Exception e) {
                e.printStackTrace(System.out);
            }
        }

        /* Give the updated map to the views */
        for (View view : views) {
            view.setMap(map);
        }
    }

    public static void main(String[] args) {
        try {
            Game game = new Game();
            CmdLineParser parser = new CmdLineParser(game);
            
            parser.parseArgument(args);
            
            game.startGame();
        } catch (Exception ex) {
            Logger.getLogger(Game.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    void setSpeed(int tick) {
        this.tick = tick;

        System.out.println("Set speed to " + tick);

        gameLoopTimer.cancel();

        gameLoopTimer = new Timer("Game loop");

        gameLoopTimer.schedule(new GameLoopTask(), tick, tick);
    }

    int getSpeed() {
        return this.tick;
    }

    private void startGame() throws Exception {

        /* Create the initial game board */
        resetGame();

        /* Start computer players if they have been configured */
        System.out.println("Computer player one: " + computerPlayerOne);
        System.out.println("Computer player two: " + computerPlayerTwo);

        /* Create and add the GUI if it's enabled */
        if (!this.headless) {
            try {
                views.add(new App(this, map));
                System.out.println("Enabled GUI");
            } catch (Exception e) {
                System.out.println("Failed to enable the GUI");
                e.printStackTrace();
            }
        }
        
        /* Start REST server if it's enabled */
        if (this.enableRestServer) {
            RestServer restServer = new RestServer(map, port, this);
            try {
                restServer.startServer();
            } catch (Exception e) {
                System.out.println("Failed to enable the REST API");
                e.printStackTrace();
            }

            views.add(restServer);
        }

        /* Enable computer players if they are enabled */
        if (computerPlayerOne != null) {
            enableComputerPlayer(map.getPlayers().get(0), PlayerType.playerTypeFromString(computerPlayerOne));
        }

        if (computerPlayerTwo != null) {
            enableComputerPlayer(map.getPlayers().get(0), PlayerType.playerTypeFromString(computerPlayerOne));
        }

        /* Share the map with the views */
        for (View view : views) {
            view.setMap(map);
        }

        /* Tell the views that the game has started */
        for (View view : views) {
            view.onGameStarted();
        }

        /* Start game tick */
        TimerTask task = new GameLoopTask();

        if (tick == 0) {
            while (true) {
                task.run();
            }
        } else {
            gameLoopTimer.schedule(task, tick, tick);
        }
    }

    void enableComputerPlayer(Player player, PlayerType type) {

        ComputerPlayer computerPlayer = null;
        
        switch (type) {
            case BUILDING:
                computerPlayer = new ConstructionPreparationPlayer(player, map);
                break;
            case EXPANDING:
                computerPlayer = new ExpandLandPlayer(player, map);
                break;
            case ATTACKING:
                computerPlayer = new AttackPlayer(player, map);
                break;
            case MINERALS:
                computerPlayer = new SearchForMineralsPlayer(player, map);
                break;
            case FOOD_PRODUCER:
                computerPlayer = new FoodProducer(player, map);
                break;
            case COIN_PRODUCER:
            	computerPlayer = new CoinProducer(player, map);
            	break;
            case MILITARY_PRODUCER:
            	computerPlayer = new MiltaryProducer(player, map);
                break;
            case COMPOSITE_PLAYER:
                computerPlayer = new CompositePlayer(player, map);
            }

        this.computerPlayers.put(player, computerPlayer);
    }

    private class GameLoopTask extends TimerTask {

        @Override
        public void run() {

            /* Call any computer players if available */
            for (ComputerPlayer computerPlayer : computerPlayers.values()) {
                try {
                    synchronized (map) {
                        computerPlayer.turn();
                    }
                } catch (Exception ex) {

                    printTroubleshootingInformation(ex);

                    System.exit(1);
                }
            }

            /* Run the game logic one more step */
            try {
                synchronized(map) {
                    map.stepTime();
                }
            } catch (Exception ex) {

                /* Print API recording to make the fault reproducable */
                try {
                    ((GameMapRecordingAdapter)map).printRecordingOnConsole();
                } catch (Exception e) {

                }

                /* Ask the views to provide troubleshooting information if available */
                for (View view : views) {
                    view.onSaveTroubleshootingInformation();
                }

                printTroubleshootingInformation(ex);

                System.exit(1);
            }
        }

        private void printTroubleshootingInformation(Exception ex) {
            /* Print API recording to make the fault reproducable */
            try {
                ((GameMapRecordingAdapter)map).printRecordingOnConsole();
            } catch (Exception e) {}

            for (Flag flag : map.getFlags()) {
                System.out.println("FLAG: " + flag.getPosition());
            }

            for (Road road : map.getRoads()) {
                System.out.println("" + road.getWayPoints());
            }

            for (Building building : map.getBuildings()) {
                System.out.println("" + building.getClass() + " " + building.getPosition());
            }

            for (View view : views) {
                view.onSaveTroubleshootingInformation();
            }

            /* Print exception and backtrace */
            Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
