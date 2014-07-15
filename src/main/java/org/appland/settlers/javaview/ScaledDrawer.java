/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.appland.settlers.javaview;

import java.awt.Graphics;
import java.awt.Graphics2D;
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

    public ScaledDrawer(int w, int h, int nrPtsW, int nrPtsH) {
        width = w;
        height = h;

        widthPoints = nrPtsW;
        heightPoints = nrPtsH;

        scaleX = width / widthPoints;
        scaleY = height / heightPoints;
    }

    int getHeight() {
        return height;
    }

    private int toScreenX(Point p) {
        return p.x * scaleX;
    }

    private int toScreenY(Point p) {
        return getHeight() - p.y * scaleY;
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

    void drawScaledOval(Graphics g, Point p, int i, int i0) {
        g.drawOval(toScreenX(p), toScreenY(p), i, i0);
    }

    void fillScaledRect(Graphics2D g, Point p, int w, int h) {
        g.fillRect(toScreenX(p), toScreenY(p), w, h);
    }

    void fillScaledOval(Graphics2D g, Point p, int w, int h) {
        g.fillOval(toScreenX(p), toScreenY(p), w, h);
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
    }
}
