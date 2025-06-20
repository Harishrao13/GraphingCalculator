package org.gcalc;

import javax.swing.*;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

public class EquationEditor extends JPanel implements AncestorListener, ActionListener {
    private int id, width;
    private boolean idSet = false;

    private JLabel title;
    private JPanel titleRow, buttonRow;
    private JButton deleteBtn;
    private JTextField editor;

    private Color editorNormalColor;

    private ArrayList<EquationEditorListener> listeners = new ArrayList<>();

    private Equation equation; // Changed to not initialize here, but in constructor

    public EquationEditor(int id, String initialEquationString) { // ADDED initialEquationString
        this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

        this.titleRow = new JPanel();
        this.titleRow.setLayout(new FlowLayout(FlowLayout.LEFT));
        this.title = new JLabel();
        this.titleRow.add(this.title);
        this.add(this.titleRow);

        this.editor = new JTextField();
        this.editor.setFont(new Font("monospaced", Font.PLAIN, 16));

        // Set initial text here, BEFORE adding the DocumentListener
        // This prevents the DocumentListener from firing immediately on setText
        // However, if you want it to fire and process the initial string, you can keep setText after.
        // For clean initialization, it's often better to create the Equation object directly.
        this.equation = new Equation(initialEquationString); // Initialize Equation here
        this.editor.setText(initialEquationString); // Set text field directly

        // Now add the listener. Any *subsequent* changes will trigger equationChanged().
        this.editor.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                EquationEditor.this.equationChanged();
            }

            public void removeUpdate(DocumentEvent e) {
                EquationEditor.this.equationChanged();
            }

            public void changedUpdate(DocumentEvent e) {
                EquationEditor.this.equationChanged();
            }
        });
        this.add(this.editor);

        this.editor.addAncestorListener(this);

        this.buttonRow = new JPanel();
        this.buttonRow.setLayout(new FlowLayout(FlowLayout.RIGHT));

        this.deleteBtn = new JButton("Delete");
        this.deleteBtn.addActionListener(this);
        this.buttonRow.add(this.deleteBtn);

        this.add(this.buttonRow);

        this.setID(id);

        // Call equationChanged() once after full setup to ensure initial state is processed
        // and listeners are notified. This is important if initialEquationString is not empty.
        if (!initialEquationString.isEmpty()) {
            this.equationChanged();
        }
    }

    // --- Original setEquationText method ---
    // You might still want to keep setEquationText if there are other scenarios
    // where you need to programmatically change the text *after* initial setup.
    // However, for initial loading, the constructor change is preferred.
    public void setEquationText(String text) {
        this.editor.setText(text);
        // DocumentListener will fire equationChanged() automatically
    }

    // ... (rest of EquationEditor methods remain the same) ...

    @Override
    public void ancestorAdded(AncestorEvent ancestorEvent) {
        final AncestorListener a = this;
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                final JComponent c = ancestorEvent.getComponent();
                c.requestFocusInWindow();
                c.removeAncestorListener(a);
            }
        });
    }

    @Override
    public void ancestorRemoved(AncestorEvent ancestorEvent) {
    }

    @Override
    public void ancestorMoved(AncestorEvent ancestorEvent) {
    }

    public void actionPerformed(ActionEvent actionEvent) {
        Object source = actionEvent.getSource();

        if (source.equals(this.deleteBtn)) {
            this.delete();
        }
    }

    public void setID(int newID) {
        if (this.idSet) {
            if (this.id % 2 == 1 && newID % 2 == 0) {
                this.lightenAllComponents();
                this.lightenAllComponents();
            } else if (this.id % 2 == 0 && newID % 2 == 1) {
                this.darkenAllComponents();
                this.darkenAllComponents();
            }
        } else {
            if (newID % 2 == 0) {
                this.lightenAllComponents();
            } else {
                this.darkenAllComponents();
            }
        }

        this.editorNormalColor = this.editor.getBackground();

        this.id = newID;
        this.idSet = true;

        this.title.setText("Expression " + Integer.toString(newID + 1));
        this.title.setForeground(Graph.lineColours[newID % Graph.lineColours.length]);

        this.repaint();
    }

    public int getID() {
        return this.id;
    }

    public void setWidth(int width) {
        this.width = width;
        this.setPreferredSize(new Dimension(width, 92));
        this.setMaximumSize(new Dimension(width, 92));
        this.editor.setMaximumSize(new Dimension(width - 10, 30));

        this.revalidate();
        this.repaint();
    }

    public void addEquationEditorListener(EquationEditorListener listener) {
        this.listeners.add(listener);
    }

    public void setValid() {
        this.editor.setBackground(this.editorNormalColor);
    }

    public void setInvalid() {
        this.editor.setBackground(new Color(228, 48, 0));
    }

    public Equation getEquation() {
        return this.equation;
    }

    public void delete() {
        for (EquationEditorListener l : this.listeners) {
            l.equationRemoved(this.id);
        }
    }

    protected void equationChanged() {
        try {
            this.equation = new Equation(this.editor.getText());
        } catch (Exception e) {
            this.setInvalid();
            return;
        }

        this.setValid();

        for (EquationEditorListener l : this.listeners) {
            l.equationEdited(this.id, this.equation);
        }
    }

    private void darkenAllComponents() {
        // darkenComponent(this);
        // darkenComponent(this.titleRow);
        // darkenComponent(this.editor);
        // darkenComponent(this.buttonRow);
        // darkenComponent(this.deleteBtn);
    }

    private void lightenAllComponents() {
        lightenComponent(this);
        lightenComponent(this.titleRow);
        lightenComponent(this.editor);
        lightenComponent(this.buttonRow);
        lightenComponent(this.deleteBtn);
    }

    private void darkenComponent(Component c) {
        this.hsvDecrease(c, 10);
    }

    private void lightenComponent(Component c) {
        this.hsvDecrease(c, -10);
    }

    private void hsvDecrease(Component c, int amount) {
        Color origColour = c.getBackground();
        int r = Math.min(255, origColour.getRed() - amount);
        int g = Math.min(255, origColour.getGreen() - amount);
        int b = Math.min(255, origColour.getBlue() - amount);
        c.setBackground(new Color(r, g, b));
    }
}