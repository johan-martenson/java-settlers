package org.appland;

import org.appland.settlers.model.GameMap;

import java.awt.*;
import javax.swing.*;
 
public class App {

    class GameCanvas extends Canvas {

	GameMap map;
	public GameCanvas(GameMap m){
	    map = m;
	}

        @Override
        public void paint (Graphics graphics){
	    
        }
    }

    public static void main( String[] args ) {
        System.out.println( "Create game map" );

        /* Create the initial game board */
    	map = new GameMap(200, 200);

        DrawingCanvas canvas = new DrawingCanvas();

        JFrame frame = new JFrame();

        frame.setSize(500, 500);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.getContentPane().add(canvas);

        frame.setVisible(true);
    }
}
