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
    private JPanel titleRow, buttonRow, calcButtonRow;
    private JButton deleteBtn, diffBtn, intBtn;
    private JTextField editor;
    private JTextField diffField, intField;

    private Color editorNormalColor;

    private ArrayList<EquationEditorListener> listeners = new ArrayList<>();

    private Equation equation;

    public EquationEditor(int id, String initialEquationString) {
        this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        
        this.setPreferredSize(new Dimension(width, 100));
        this.setMaximumSize(new Dimension(width, 100));

        // Title row
        this.titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        this.title = new JLabel();
        this.titleRow.add(this.title);
        this.add(this.titleRow);

        // Main equation editor
        this.editor = new JTextField();
        this.editor.setFont(new Font("monospaced", Font.PLAIN, 16));
        this.equation = new Equation(initialEquationString);
        this.editor.setText(initialEquationString);
        this.add(this.editor);

        // Calculation results fields (initially hidden)
        this.diffField = createResultField("Derivative will appear here");
        this.intField = createResultField("Integral will appear here");
        this.add(this.diffField);
        this.add(this.intField);

        // Single button row for all actions
        this.buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        this.buttonRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        
        // Smaller calculus buttons
        this.diffBtn = createSmallButton("d/dx");
        this.intBtn = createSmallButton("∫");
        this.deleteBtn = createSmallButton("x"); 
        
        this.diffBtn.addActionListener(this);
        this.intBtn.addActionListener(this);
        this.deleteBtn.addActionListener(this);
        
        // Add tooltips for better UX
        this.diffBtn.setToolTipText("Differentiate");
        this.intBtn.setToolTipText("Integrate");
        this.deleteBtn.setToolTipText("Delete");
        
        this.buttonRow.add(this.diffBtn);
        this.buttonRow.add(this.intBtn);
        this.buttonRow.add(this.deleteBtn);
        
        this.add(this.buttonRow);

        // Document listener for equation changes
        this.editor.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { equationChanged(); }
            public void removeUpdate(DocumentEvent e) { equationChanged(); }
            public void changedUpdate(DocumentEvent e) { equationChanged(); }
        });

        this.editor.addAncestorListener(this);
        this.setID(id);

        if (!initialEquationString.isEmpty()) {
            this.equationChanged();
        }
    }

    private JButton createSmallButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Dialog", Font.PLAIN, 12));
        button.setMargin(new Insets(0, 3, 0, 3));
        button.setPreferredSize(new Dimension(50, 22));
        return button;
    }

    private JTextField createResultField(String placeholder) {
        JTextField field = new JTextField(placeholder);
        field.setFont(new Font("monospaced", Font.PLAIN, 14));
        field.setEditable(false);
        field.setBackground(Color.WHITE);
        field.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
        field.setVisible(false);
        return field;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == this.deleteBtn) {
            this.delete();
        } else if (e.getSource() == this.diffBtn) {
            showDerivative();
        } else if (e.getSource() == this.intBtn) {
            showIntegral();
        }
    }

    private void showDerivative() {
        try {
            String result = Calculus.differentiate(this.editor.getText());
            diffField.setText("d/dx: " + result);
            diffField.setVisible(true);
            
            // Notify listeners to add the derivative as a new equation
            for (EquationEditorListener l : this.listeners) {
                l.addDerivative(this.id, result);
            }
            
            revalidate();
            repaint();
        } catch (Exception ex) {
            diffField.setText("Error in differentiation");
            diffField.setVisible(true);
        }
    }

    private void showIntegral() {
        try {
            String result = Calculus.integrate(this.editor.getText());
            intField.setText("∫dx: " + result);
            intField.setVisible(true);
            
            // Notify listeners to add the integral as a new equation
            for (EquationEditorListener l : this.listeners) {
                l.addIntegral(this.id, result);
            }
            
            revalidate();
            repaint();
        } catch (Exception ex) {
            intField.setText("Error in integration");
            intField.setVisible(true);
        }
    }

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


    public void setID(int newID) {

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
    this.setPreferredSize(new Dimension(width, 120)); // Increased height
    this.setMaximumSize(new Dimension(width, 120)); // Increased height
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

}