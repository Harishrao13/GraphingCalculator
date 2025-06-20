package org.gcalc;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;

import org.apache.batik.svggen.SVGGraphics2D;
import org.w3c.dom.Document;
import org.w3c.dom.DOMImplementation;
import javax.xml.parsers.DocumentBuilderFactory;

public class Graph extends JLabel implements ComponentListener, EquationListener {
    public static final Color[] lineColours = {
            new Color(231, 76, 60), new Color(26, 188, 156), new Color(241, 196, 15),
            new Color(211, 84, 0), new Color(39, 174, 96), new Color(41, 128, 185),
            new Color(255, 0, 255)
    };

    protected static final int normInterval = 50;
    private int width, height;
    private BufferedImage img;
    private double scale = 1;
    private ArrayList<Equation> equations = new ArrayList<>();
    private ArrayList<EquationEditor> editors = new ArrayList<>();

    public Graph(int width, int height) {
        this.width = width;
        this.height = height;
        this.img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        this.setIcon(new ImageIcon(this.img));
        this.addComponentListener(this);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(this.width, this.height);
    }

    public void componentResized(ComponentEvent e) {
        Dimension size = this.getSize();
        this.width = size.width;
        this.height = size.height;
        this.img = new BufferedImage(this.width, this.height, BufferedImage.TYPE_INT_RGB);
        this.setIcon(new ImageIcon(this.img));
        this.redraw();
    }

    public void componentHidden(ComponentEvent e) {}
    public void componentMoved(ComponentEvent e) {}
    public void componentShown(ComponentEvent e) {}

    public void equationAdded(int id, Equation newEquation, EquationEditor editor) {
        while (equations.size() <= id) equations.add(null);
        while (editors.size() <= id) editors.add(null);
        this.equations.set(id, newEquation);
        this.editors.set(id, editor);
        this.redraw();
    }

    public void equationRemoved(int id) {
        if (id >= 0 && id < equations.size()) {
            equations.set(id, null);
        }
        if (id >= 0 && id < editors.size()) {
            editors.set(id, null);
        }
        this.redraw();
    }

    public void equationChanged(int id, Equation e) {
        if (id >= 0 && id < equations.size()) {
            this.equations.set(id, e);
        }
        this.redraw();
    }

    public void increaseScale() {
        this.setScale(this.getScale() * 1.5);
    }

    public void decreaseScale() {
        this.setScale(this.getScale() / 1.5);
    }

    public void setScale(double scale) {
        this.scale = scale;
        this.redraw();
    }

    public double getScale() {
        return this.scale;
    }

    protected void redraw() {
        Graphics2D g = this.img.createGraphics();
        g.setBackground(Color.WHITE);
        g.setColor(Color.WHITE);
        g.fill(new Rectangle2D.Double(0, 0, this.img.getWidth(), this.img.getHeight()));
        this.drawGrid(g, this.img.getWidth(), this.img.getHeight());

        int id = 0;
        for (Equation e : this.equations) {
            if (e == null) continue;
            try {
                this.drawEquation(g, id, e, this.img.getWidth(), this.img.getHeight());
            } catch (Exception ex) {
                EquationEditor editor = (id < editors.size()) ? editors.get(id) : null;
                if (editor != null) editor.setInvalid();
            }
            id++;
        }
        this.repaint();
    }

    protected void drawGrid(Graphics2D g, int imgWidth, int imgHeight) {
        float[] dashPattern = new float[]{10 * (float) this.scale, 5 * (float) this.scale};
        g.setColor(new Color(48, 48, 48));
        g.setStroke(new BasicStroke(2));
        g.draw(new Line2D.Double(0, imgHeight / 2, imgWidth, imgHeight / 2));
        g.draw(new Line2D.Double(imgWidth / 2, 0, imgWidth / 2, imgHeight));

        g.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, dashPattern, (29 - (imgHeight % 30)) / 2.0f + 20));
        int xScaledInt = (int) Math.round(normInterval * this.scale);
        int xMultiplier = Math.max(Math.round((float) normInterval / (float) xScaledInt), 1);
        int xInterval = xMultiplier * xScaledInt;
        int xNumInterval = xMultiplier, xCurrent = 0;

        for (int x = imgWidth / 2 - xInterval; x >= 0; x -= xInterval) {
            g.draw(new Line2D.Double(x, imgHeight, x, 0));
            g.draw(new Line2D.Double(imgWidth - x, imgHeight, imgWidth - x, 0));
            xCurrent += xNumInterval;
            g.drawString("-" + xCurrent, x + 2, imgHeight / 2 + 14);
            g.drawString(Integer.toString(xCurrent), imgWidth - x + 2, imgHeight / 2 + 14);
        }

        g.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, dashPattern, (29 - (imgWidth % 30)) / 2.0f + 20));
        int yInterval = xInterval;
        int yNumInterval = xNumInterval, yCurrent = 0;

        for (int y = imgHeight / 2 - yInterval; y >= 0; y -= yInterval) {
            g.draw(new Line2D.Double(imgWidth, y, 0, y));
            g.draw(new Line2D.Double(imgWidth, imgHeight - y, 0, imgHeight - y));
            yCurrent += yNumInterval;
            g.drawString(Integer.toString(yCurrent), imgWidth / 2 + 2, y + 14);
            g.drawString("-" + yCurrent, imgWidth / 2 + 2, imgHeight - y + 14);
        }
    }

    protected void drawEquation(Graphics2D g, int id, Equation e, int imgWidth, int imgHeight) {
        g.setColor(lineColours[id % lineColours.length]);
        double bounds = (imgWidth / 2.0) / (normInterval * this.scale);
        boolean lastValSet = false;
        double lastVal = 0;
        int drawX = 0;

        for (double x = -bounds; x < bounds; x += (2 * bounds) / imgWidth) {
            if (!lastValSet) {
                lastVal = e.evaluate(x)[0];
                lastValSet = true;
                continue;
            }
            double currentVal = e.evaluate(x)[0];
            g.setStroke(new BasicStroke(2));
            g.draw(new Line2D.Double(drawX - 1, imgHeight / 2.0 - lastVal * (normInterval * this.scale),
                    drawX, imgHeight / 2.0 - currentVal * (normInterval * this.scale)));
            lastVal = currentVal;
            drawX++;
        }
    }

    public void saveWorkspace() {
        String filename = "workspace.txt"; // Default workspace file name

        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            for (Equation e : this.equations) {
                if (e != null) {
                    // Assuming Equation has a meaningful toString() representation
                    // or a method like toSaveString()
                    writer.println(e.getRightHandSide()+"\n");

                }
            }
            System.out.println("Workspace saved to " + filename);
        } catch (IOException e) {
            System.err.println("Error saving workspace: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void saveAsSVG(File file) {
        try {
            DOMImplementation domImpl = DocumentBuilderFactory.newInstance().newDocumentBuilder().getDOMImplementation();
            Document document = domImpl.createDocument(null, "svg", null);
            SVGGraphics2D svgGenerator = new SVGGraphics2D(document);
            svgGenerator.setSVGCanvasSize(new Dimension(this.width, this.height));

            this.drawGrid(svgGenerator, this.width, this.height);
            int id = 0;
            for (Equation e : this.equations) {
                if (e == null) continue;
                try {
                    this.drawEquation(svgGenerator, id, e, this.width, this.height);
                } catch (Exception ex) {
                    EquationEditor editor = (id < editors.size()) ? editors.get(id) : null;
                    if (editor != null) editor.setInvalid();
                }
                id++;
            }

            try (Writer out = new OutputStreamWriter(new FileOutputStream(file), "UTF-8")) {
                svgGenerator.stream(out, true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
