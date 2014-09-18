/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.appland.settlers.javaview;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.Path2D;
import org.appland.settlers.model.Point;

/**
 *
 * @author johan
 */
public class ScaledDrawer {

    int width;
    int height;

    int widthPoints;
    int heightPoints;

    int scaleX;
    int scaleY;
    
    double offsetScaleX;
    double offsetScaleY;

    public ScaledDrawer(int w, int h, int nrPtsW, int nrPtsH) {
        width = w;
        height = h;

        widthPoints = nrPtsW;
        heightPoints = nrPtsH;

        scaleX = width / widthPoints;
        scaleY = height / heightPoints;
    
        offsetScaleX = scaleX / 20.0;
        offsetScaleY = scaleY / 20.0;
    }

    int getHeight() {
        return height;
    }

    int toScreenX(Point p) {
        return p.x * scaleX;
    }

    int toScreenX(Point p, int offset) {
        return toScreenX(p) + (int)(offset * offsetScaleX);
    }
    
    int toScreenY(Point p) {
        return getHeight() - p.y * scaleY;
    }

    int toScreenY(Point p, int offset) {
        return toScreenY(p) + (int)(offset * offsetScaleY);
    }
    
    int simpleScaleX(int x) {
        return (int)(x*offsetScaleX);
    }
    
    int simpleScaleY(int y) {
        return (int)(y*offsetScaleY);
    }
    
    void drawScaledLine(Graphics graphics, Point p1, Point p2) {
        graphics.drawLine(toScreenX(p1), toScreenY(p1), toScreenX(p2), toScreenY(p2));
    }

    void drawScaledFilledOval(Graphics graphics, Point p, int w, int h) {
        graphics.fillOval(toScreenX(p), toScreenY(p), w, h);
    }

    void drawScaledRect(Graphics g, Point p, int i, int i0) {
        g.drawRect(toScreenX(p), toScreenY(p), i, i0);
    }

    void drawScaledRect(Graphics2D g, Point p, int width, int height, int offsetX, int offsetY) {
        g.drawRect(toScreenX(p, offsetX), toScreenY(p, offsetY), simpleScaleX(width), simpleScaleY(height));
    }
    
    void drawScaledOval(Graphics g, Point p, int i, int i0) {
        g.drawOval(toScreenX(p), toScreenY(p), i, i0);
    }

    void drawScaledOval(Graphics g, Point p, int i, int i0, int offsetX, int offsetY) {
        g.drawOval(toScreenX(p, offsetX), toScreenY(p, offsetY), simpleScaleX(i), simpleScaleY(i0));
    }

    
    void fillScaledRect(Graphics2D g, Point p, int w, int h) {
        g.fillRect(toScreenX(p), toScreenY(p), simpleScaleX(w), simpleScaleY(h));
    }

    void fillScaledRect(Graphics2D g, Point p, int w, int h, int offsetX, int offsetY) {
        g.fillRect(toScreenX(p, offsetX), toScreenY(p, offsetY), simpleScaleX(w), simpleScaleY(h));
    }

    void fillScaledOval(Graphics2D g, Point p, int w, int h) {
        g.fillOval(toScreenX(p), toScreenY(p), simpleScaleX(w), simpleScaleY(h));
    }

    void fillScaledOval(Graphics2D g, Point p, int w, int h, int offsetX, int offsetY) {
        g.fillOval(toScreenX(p, offsetX), toScreenY(p, offsetY), simpleScaleX(w), simpleScaleY(h));
    }
    
    void fillScaledTriangle(Graphics2D g, Point p1, Point p2, Point p3) {
        Path2D.Double triangle = new Path2D.Double();
        triangle.moveTo(toScreenX(p1), toScreenY(p1));
        triangle.lineTo(toScreenX(p2), toScreenY(p2));
        triangle.lineTo(toScreenX(p3), toScreenY(p3));
        triangle.closePath();
        g.fill(triangle);
    }

    int getScaleX() {
        return scaleX;
    }

    int getScaleY() {
        return scaleY;
    }

    void recalculateScale(int newWidth, int newHeight) {
        width = newWidth;
        height = newHeight;
        
        scaleX = width / widthPoints;
        scaleY = height / heightPoints;

        offsetScaleX = scaleX / 20.0;
        offsetScaleY = scaleY / 20.0;
    }

    void drawScaledImage(Graphics2D g, Image img, Point p, int w, int h, int ox, int oy) {
        g.drawImage(img, 
                    toScreenX(p, ox), toScreenY(p, oy), 
                    toScreenX(p, ox) + simpleScaleX(w), toScreenY(p, oy) + simpleScaleY(h),
                    0, 0, img.getWidth(null), img.getHeight(null), null);
    }
}
