package org.gcalc;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Sidebar extends JScrollPane implements ComponentListener, EquationEditorListener {
    private ArrayList<EquationListener> listeners = new ArrayList<>();
    private ArrayList<EquationEditor> editors = new ArrayList<>();

    int width, height;
    JPanel container;

    private static final String WORKSPACE_FILE_NAME = "workspace.txt";

    public Sidebar(int width, int height) {
        super(new JPanel());
        this.setPreferredSize(new Dimension(width, height));
        this.getVerticalScrollBar().setUnitIncrement(16);
        this.width = width;
        this.height = height;

        this.container = (JPanel) this.getViewport().getView();
        this.container.setLayout(new BoxLayout(this.container, BoxLayout.PAGE_AXIS));
        container.setMaximumSize(new Dimension(width - 10, Integer.MAX_VALUE));

        this.container.addComponentListener(this);
        this.addComponentListener(this);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(this.width, this.height);
    }

    @Override
    public void componentResized(ComponentEvent componentEvent) {
        Dimension size = this.getViewport().getSize();
        this.container.setMaximumSize(new Dimension(size.width, Integer.MAX_VALUE));
        for (EquationEditor e : this.editors) {
            e.setWidth(size.width);
        }
        this.revalidate();
        this.repaint();
    }

    @Override public void componentMoved(ComponentEvent componentEvent) { }
    @Override public void componentShown(ComponentEvent componentEvent) { }
    @Override public void componentHidden(ComponentEvent componentEvent) { }

    @Override
    public void equationEdited(int id, Equation equation) {
        for (EquationListener l : this.listeners) {
            l.equationChanged(id, equation);
        }
    }

    @Override
    public void equationRemoved(int id) {
        if (id >= 0 && id < this.editors.size()) {
            this.container.remove(this.editors.get(id));
            this.editors.remove(id);
        } else {
            System.err.println("Attempted to remove non-existent equation editor with ID: " + id);
            return;
        }

        int newID = 0;
        for (EquationEditor e : this.editors) {
            e.setID(newID);
            newID++;
        }

        for (int i = listeners.size() - 1; i >= 0; i--) {
            listeners.get(i).equationRemoved(id);
        }

        int width = this.getViewport().getSize().width;
        for (EquationEditor f : this.editors) {
            f.setWidth(width);
        }

        this.revalidate();
        this.repaint();
    }

    public void newEquation() {
        int id = this.editors.size();
        EquationEditor e = new EquationEditor(id, "");
        e.addEquationEditorListener(this);
        this.container.add(e);
        this.editors.add(e);

        int width = this.getViewport().getSize().width;
        for (EquationEditor f : this.editors) {
            f.setWidth(width);
        }

        this.revalidate();
        this.repaint();

        for (EquationListener l : this.listeners) {
            l.equationAdded(id, e.getEquation(), e);
        }
    }

    public void loadEquation(String equationString) {
        int id = this.editors.size();
        EquationEditor e = new EquationEditor(id, equationString);
        e.addEquationEditorListener(this);
        this.container.add(e);
        this.editors.add(e);

        int width = this.getViewport().getSize().width;
        for (EquationEditor f : this.editors) {
            f.setWidth(width);
        }

        this.revalidate();
        this.repaint();

        for (EquationListener l : this.listeners) {
            l.equationAdded(id, e.getEquation(), e);
        }
    }

    public void loadWorkspace() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Load Workspace");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setSelectedFile(new File(WORKSPACE_FILE_NAME));

        int userSelection = fileChooser.showOpenDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToLoad = fileChooser.getSelectedFile();

            this.deleteAllEquations();

            try (BufferedReader reader = new BufferedReader(new FileReader(fileToLoad))) {
                StringBuilder equationBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("Expression ")) {
                        if (equationBuilder.length() > 0) {
                            loadEquationFromBlock(equationBuilder.toString());
                            equationBuilder.setLength(0);
                        }
                    }
                    equationBuilder.append(line).append("\n");
                }
                if (equationBuilder.length() > 0) {
                    loadEquationFromBlock(equationBuilder.toString());
                }
                System.out.println("Workspace loaded from " + fileToLoad.getAbsolutePath());

                if (this.editors.isEmpty()) {
                    this.newEquation();
                }

                this.revalidate();
                this.repaint();

            } catch (IOException e) {
                System.err.println("Error loading workspace: " + e.getMessage());
                e.printStackTrace();
                if (this.editors.isEmpty()) {
                    this.newEquation();
                }
            }
        } else {
            System.out.println("Workspace load cancelled by user.");
            if (this.editors.isEmpty()) {
                this.newEquation();
            }
        }
    }

    private void loadEquationFromBlock(String block) {
        String titleLine = block.split("\n", 2)[0];
        if (titleLine.startsWith("Expression ")) {
            int firstQuote = titleLine.indexOf('"');
            int lastQuote = titleLine.lastIndexOf('"');
            if (firstQuote != -1 && lastQuote != -1 && lastQuote > firstQuote) {
                String expression = titleLine.substring(firstQuote + 1, lastQuote);
                loadEquation(expression);
            }
        }
    }

    public void saveEquationsToWorkspace() {
        List<String> equationStrings = new ArrayList<>();
        for (EquationEditor editor : this.editors) {
            equationStrings.add(editor.getEquation().toString());
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(WORKSPACE_FILE_NAME))) {
            for (String equation : equationStrings) {
                writer.write(equation);
                writer.newLine();
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "Error saving equations: " + e.getMessage(),
                    "Save Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public void deleteAllEquations() {
        for (int i = this.editors.size() - 1; i >= 0; i--) {
            this.editors.get(i).delete();
        }
        this.container.removeAll();
        this.revalidate();
        this.repaint();
    }

    public void addEquationListener(EquationListener listener) {
        this.listeners.add(listener);

        for (EquationEditor e : this.editors) {
            listener.equationAdded(e.getID(), e.getEquation(), e);
        }
    }

    public List<Equation> getAllEquations() {
        List<Equation> currentEquations = new ArrayList<>();
        for (EquationEditor editor : this.editors) {
            Equation eq = editor.getEquation();
            if (eq != null && !eq.isEmpty) {
                currentEquations.add(eq);
            }
        }
        return currentEquations;
    }
}
