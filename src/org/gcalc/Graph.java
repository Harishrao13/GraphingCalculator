package org.gcalc;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    private List<Point2D.Double> clickedGraphPoints;
    private List<Point> clickedPixelPoints;
    private Set<Point2D.Double> intersectionPoints;
    private Point hoveredPoint = null;

    private double offsetX = 0;
    private double offsetY = 0;
    private Point lastDragPoint = null;


    public Graph(int width, int height) {
        this.width = width;
        this.height = height;
        this.img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        this.setIcon(new ImageIcon(this.img));
        this.addComponentListener(this);

        this.clickedGraphPoints = new ArrayList<>();
        this.clickedPixelPoints = new ArrayList<>();
        this.intersectionPoints = new HashSet<>();

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int mouseX = e.getX();
                int mouseY = e.getY();
                Point2D.Double graphPoint = convertPixelToGraph(mouseX, mouseY);
                clickedGraphPoints.add(graphPoint);
                clickedPixelPoints.add(new Point(mouseX, mouseY));
                System.out.printf("Clicked at Pixel: (%d, %d) -> Graph: (x=%.2f, y=%.2f)\n", mouseX, mouseY, graphPoint.x, graphPoint.y);
                redraw();
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                hoveredPoint = e.getPoint();
                redraw();
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                lastDragPoint = e.getPoint();
            }
        
            @Override
            public void mouseReleased(MouseEvent e) {
                lastDragPoint = null;
            }
        });
        
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (lastDragPoint != null) {
                    int dx = e.getX() - lastDragPoint.x;
                    int dy = e.getY() - lastDragPoint.y;
                    lastDragPoint = e.getPoint();
        
                    // Convert pixel drag to graph-space drag
                    offsetX -= dx / (normInterval * scale);
                    offsetY += dy / (normInterval * scale);
        
                    redraw();
                }
            }
        });
        
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
        if (id >= 0 && id < equations.size()) equations.set(id, null);
        if (id >= 0 && id < editors.size()) editors.set(id, null);
        this.redraw();
    }

    public void equationChanged(int id, Equation e) {
        if (id >= 0 && id < equations.size()) this.equations.set(id, e);
        this.redraw();
    }

    public void increaseScale() { this.setScale(this.getScale() * 1.5); }
    public void decreaseScale() { this.setScale(this.getScale() / 1.5); }
    public void setScale(double scale) { this.scale = scale; this.redraw(); }
    public double getScale() { return this.scale; }

    protected Point convertGraphToPixel(double graphX, double graphY) {
        int pixelX = (int) (this.img.getWidth() / 2.0 + (graphX - offsetX) * (normInterval * this.scale));
        int pixelY = (int) (this.img.getHeight() / 2.0 - (graphY - offsetY) * (normInterval * this.scale));
        return new Point(pixelX, pixelY);
    }
    
    protected Point2D.Double convertPixelToGraph(int pixelX, int pixelY) {
        double graphX = offsetX + (pixelX - this.img.getWidth() / 2.0) / (normInterval * this.scale);
        double graphY = offsetY + (this.img.getHeight() / 2.0 - pixelY) / (normInterval * this.scale);
        return new Point2D.Double(graphX, graphY);
    }

    protected void redraw() {
        Graphics2D g = this.img.createGraphics();
        g.setBackground(Color.WHITE);
        g.setColor(Color.WHITE);
        g.fill(new Rectangle2D.Double(0, 0, this.img.getWidth(), this.img.getHeight()));
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        this.drawGrid(g, this.img.getWidth(), this.img.getHeight());
        intersectionPoints.clear();

        int id = 0;
        for (Equation e : this.equations) {
            if (e == null) continue;
            try {
                this.drawEquation(g, id, e, this.img.getWidth(), this.img.getHeight());
            } catch (Exception ex) {
                EquationEditor editor = (id < editors.size()) ? editors.get(id) : null;
                if (editor != null) editor.setInvalid();
                System.err.printf("Error drawing equation %d: %s\n", id, ex.getMessage());
                ex.printStackTrace();
            }
            id++;
        }

        g.setColor(Color.MAGENTA);
        int clickedDotSize = 8;
        for (Point2D.Double graphP : clickedGraphPoints) {
            Point pixelP = convertGraphToPixel(graphP.x, graphP.y);
            g.fillOval(pixelP.x - clickedDotSize / 2, pixelP.y - clickedDotSize / 2, clickedDotSize, clickedDotSize);
            String coordStr = String.format("(%.2f, %.2f)", graphP.x, graphP.y);
            g.drawString(coordStr, pixelP.x + clickedDotSize, pixelP.y - clickedDotSize);
        }

        g.setColor(Color.RED);
        int intersectionDotSize = 10;
        for (Point2D.Double graphP : intersectionPoints) {
            Point pixelP = convertGraphToPixel(graphP.x, graphP.y);
            g.fillOval(pixelP.x - intersectionDotSize / 2, pixelP.y - intersectionDotSize / 2, intersectionDotSize, intersectionDotSize);
            String coordStr = String.format("(%.2f, %.2f)", graphP.x, graphP.y);
            g.drawString(coordStr, pixelP.x + intersectionDotSize, pixelP.y - intersectionDotSize);
        }

        if (hoveredPoint != null) {
            Point2D.Double gp = convertPixelToGraph(hoveredPoint.x, hoveredPoint.y);
            g.setColor(Color.BLUE);
            String text = String.format("(%.2f, %.2f)", gp.x, gp.y);
            g.drawString(text, hoveredPoint.x + 10, hoveredPoint.y - 10);
        }

        this.repaint();
    }

    protected void drawEquation(Graphics2D g, int id, Equation e, int imgWidth, int imgHeight) {
        g.setColor(lineColours[id % lineColours.length]);
        g.setStroke(new BasicStroke(2));

        double xGraphMin = -imgWidth / 2.0 / (normInterval * this.scale);
        double xGraphMax = imgWidth / 2.0 / (normInterval * this.scale);
        double step = (xGraphMax - xGraphMin) / imgWidth;

        Point lastPixelPoint = null;
        for (int pixelX = 0; pixelX < imgWidth; pixelX++) {
            double graphX = convertPixelToGraph(pixelX, 0).x;
            double[] yValues = e.evaluate(graphX);

            if (yValues.length > 0 && !Double.isNaN(yValues[0])) {
                double graphY = yValues[0];
                Point currentPixelPoint = convertGraphToPixel(graphX, graphY);

                if (lastPixelPoint != null) {
                    g.draw(new Line2D.Double(lastPixelPoint.x, lastPixelPoint.y, currentPixelPoint.x, currentPixelPoint.y));
                }
                lastPixelPoint = currentPixelPoint;
            } else {
                lastPixelPoint = null;
            }
        }

        if (id > 0) {
            for (int prevId = 0; prevId < id; prevId++) {
                Equation prevEquation = equations.get(prevId);
                if (prevEquation == null) continue;
        
                Point2D.Double lastIntersection = null;
                double minDistanceBetweenIntersections = 0.3; 
                
                for (int pixelX = 0; pixelX < imgWidth; pixelX += 2) {
                    double graphX = convertPixelToGraph(pixelX, 0).x;
                    double[] y1Values = e.evaluate(graphX);
                    double[] y2Values = prevEquation.evaluate(graphX);
        
                    if (y1Values.length > 0 && !Double.isNaN(y1Values[0]) &&
                        y2Values.length > 0 && !Double.isNaN(y2Values[0])) {
        
                        double y1 = y1Values[0];
                        double y2 = y2Values[0];
                        
                        // More reasonable tolerance based on scale
                        double tolerance = Math.max(0.01 / this.scale, 0.01);
        
                        if (Math.abs(y1 - y2) < tolerance) {
                            Point2D.Double candidate = new Point2D.Double(
                                Math.round(graphX * 100) / 100.0, 
                                Math.round(((y1 + y2) / 2.0) * 100) / 100.0
                            );
                            
                            // Check if this intersection is far enough from the last one
                            boolean shouldAdd = true;
                            if (lastIntersection != null) {
                                double distance = Math.sqrt(
                                    Math.pow(candidate.x - lastIntersection.x, 2) + 
                                    Math.pow(candidate.y - lastIntersection.y, 2)
                                );
                                if (distance < minDistanceBetweenIntersections) {
                                    shouldAdd = false;
                                }
                            }
                            
                            // Also check against existing intersection points
                            for (Point2D.Double existing : intersectionPoints) {
                                double distance = Math.sqrt(
                                    Math.pow(candidate.x - existing.x, 2) + 
                                    Math.pow(candidate.y - existing.y, 2)
                                );
                                if (distance < minDistanceBetweenIntersections) {
                                    shouldAdd = false;
                                    break;
                                }
                            }
                            
                            if (shouldAdd) {
                                intersectionPoints.add(candidate);
                                lastIntersection = candidate;
                            }
                        }
                    }
                }
            }
        }
        }

        protected void drawGrid(Graphics2D g, int imgWidth, int imgHeight) {
            float[] dashPattern = new float[]{10 * (float) this.scale, 5 * (float) this.scale};
            g.setColor(new Color(48, 48, 48));
        
            int centerX = imgWidth / 2;
            int centerY = imgHeight / 2;
        
            g.setStroke(new BasicStroke(2));
            // Axes (adjusted for offset)
            int axisX = (int) (centerX - offsetX * normInterval * scale);
            int axisY = (int) (centerY + offsetY * normInterval * scale);
            g.draw(new Line2D.Double(0, axisY, imgWidth, axisY)); // X axis
            g.draw(new Line2D.Double(axisX, 0, axisX, imgHeight)); // Y axis
        
            // Grid lines
            g.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, dashPattern, 0)); 
            int interval = (int) Math.round(normInterval * this.scale);
            if (interval < 1) interval = 1;
        
            // Calculate start points in graph space
            double graphXMin = convertPixelToGraph(0, 0).x;
            double graphXMax = convertPixelToGraph(imgWidth, 0).x;
            double graphYMin = convertPixelToGraph(0, imgHeight).y;
            double graphYMax = convertPixelToGraph(0, 0).y;
        
            // Draw vertical grid lines
            for (int i = (int) Math.floor(graphXMin); i <= (int) Math.ceil(graphXMax); i++) {
                int x = convertGraphToPixel(i, 0).x;
                g.draw(new Line2D.Double(x, 0, x, imgHeight));
                if (Math.abs(i) > 1e-6) // skip 0 label on X-axis
                    g.drawString(Integer.toString(i), x + 2, axisY + 14);
            }
        
            // Draw horizontal grid lines
            for (int j = (int) Math.floor(graphYMin); j <= (int) Math.ceil(graphYMax); j++) {
                int y = convertGraphToPixel(0, j).y;
                g.draw(new Line2D.Double(0, y, imgWidth, y));
                if (Math.abs(j) > 1e-6) // skip 0 label on Y-axis
                    g.drawString(Integer.toString(j), axisX + 2, y + 14);
            }
        }

    public void saveWorkspace() {
        String filename = "workspace.txt";

        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            for (Equation e : this.equations) {
                if (e != null) {
                    writer.println(e.toString());
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

            svgGenerator.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

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

            // Draw clicked points on SVG
            svgGenerator.setColor(Color.MAGENTA);
            int clickedDotSize = 8;
            for (Point pixelP : clickedPixelPoints) {
                // SVG coordinates are typically top-left origin, matching Swing pixel (x,y)
                svgGenerator.fillOval(pixelP.x - clickedDotSize / 2, pixelP.y - clickedDotSize / 2, clickedDotSize, clickedDotSize);
                String coordStr = String.format("(%.2f, %.2f)",
                                                convertPixelToGraph(pixelP.x, pixelP.y).x,
                                                convertPixelToGraph(pixelP.x, pixelP.y).y);
                svgGenerator.drawString(coordStr, pixelP.x + clickedDotSize, pixelP.y - clickedDotSize);
            }

            // Draw intersection points on SVG
            svgGenerator.setColor(Color.RED);
            int intersectionDotSize = 10;
            for (Point2D.Double graphP : intersectionPoints) {
                Point pixelP = convertGraphToPixel(graphP.x, graphP.y);
                svgGenerator.fillOval(pixelP.x - intersectionDotSize / 2, pixelP.y - intersectionDotSize / 2, intersectionDotSize, intersectionDotSize);
                String coordStr = String.format("(%.2f, %.2f)", graphP.x, graphP.y);
                svgGenerator.drawString(coordStr, pixelP.x + intersectionDotSize, pixelP.y - intersectionDotSize);
            }


            try (Writer out = new OutputStreamWriter(new FileOutputStream(file), "UTF-8")) {
                svgGenerator.stream(out, true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}