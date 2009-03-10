/*
 *                 Sun Public License Notice
 * 
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 * 
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 1997-2003 Sun
 * Microsystems, Inc. All Rights Reserved.
 */

package org.netbeans.editor;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Caret;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
* Status bar support
*
* @author Miloslav Metelka
* @version 1.00
*/

public class StatusBar implements PropertyChangeListener, SettingsChangeListener, DocumentListener {

    public static final String CELL_MAIN = "main"; // NOI18N

    public static final String CELL_POSITION = "position"; // NOI18N

    public static final String CELL_TYPING_MODE = "typing-mode"; // NOI18N

    public static final String INSERT_LOCALE = "status-bar-insert"; // NOI18N

    public static final String OVERWRITE_LOCALE = "status-bar-overwrite"; // NOI18N

    private static final String[] POS_MAX_STRINGS = new String[] { "99999:999" }; // NOI18N

    private static final Insets NULL_INSETS = new Insets(0, 0, 0, 0);

    static final Border CELL_BORDER = 
    BorderFactory.createCompoundBorder(
        BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1,0,0,0,UIManager.getDefaults().getColor("control")),   // NOI18N
            BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0,0,1,1,UIManager.getDefaults().getColor("controlHighlight")),   // NOI18N
                BorderFactory.createLineBorder(UIManager.getDefaults().getColor("controlDkShadow"))   // NOI18N
            )
        ),
        BorderFactory.createEmptyBorder(0, 2, 0, 2)
    );

    protected EditorUI editorUI;

    /** The status bar panel into which the cells are added. */
    private JPanel panel;

    private boolean visible;

    private Coloring coloring;

    private Coloring boldColoring;

    private List cellList = new ArrayList();

    private Caret caret;

    private CaretListener caretL;

    private int caretDelay;

    private boolean overwriteModeDisplayed;

    private String insText;

    private String ovrText;

    static final long serialVersionUID =-6266183959929157349L;

    public StatusBar(EditorUI editorUI) {
        this.editorUI = editorUI;

        caretDelay = 10;
        caretL = new CaretListener(caretDelay);
        insText = LocaleSupport.getString(INSERT_LOCALE);
        ovrText = LocaleSupport.getString(OVERWRITE_LOCALE);

        Settings.addSettingsChangeListener(this);

        synchronized (editorUI.getComponentLock()) {
            // if component already installed in EditorUI simulate installation
            JTextComponent component = editorUI.getComponent();
            if (component != null) {
                propertyChange(new PropertyChangeEvent(editorUI,
                                                       EditorUI.COMPONENT_PROPERTY, null, component));
            }

            editorUI.addPropertyChangeListener(this);
        }
    }

    public void settingsChange(SettingsChangeEvent evt) {
        Class kitClass = Utilities.getKitClass(editorUI.getComponent());
        String settingName = (evt != null) ? evt.getSettingName() : null;
        if (kitClass != null) {
            Coloring dc = editorUI.getDefaultColoring();
            coloring = editorUI.getColoring(SettingsNames.STATUS_BAR_COLORING);
            boldColoring = editorUI.getColoring(SettingsNames.STATUS_BAR_BOLD_COLORING);

            // assign coloring
            if (coloring != null) {
                coloring = coloring.apply(dc);
            } else {
                coloring = dc;
            }

            // assign bold coloring
            if (boldColoring != null) {
                boldColoring = boldColoring.apply(dc);
            } else {
                boldColoring = dc;
            }

            refreshPanel();

            if (settingName == null || SettingsNames.STATUS_BAR_CARET_DELAY.equals(settingName)) {
                caretDelay = SettingsUtil.getInteger(kitClass, SettingsNames.STATUS_BAR_CARET_DELAY,
                                                     SettingsDefaults.defaultStatusBarCaretDelay);
                if (caretL != null) {
                    caretL.setDelay(caretDelay);
                }
            }

            if (settingName == null || SettingsNames.STATUS_BAR_VISIBLE.equals(settingName)) {
                boolean wantVisible = SettingsUtil.getBoolean(kitClass,
                                      SettingsNames.STATUS_BAR_VISIBLE, SettingsDefaults.defaultStatusBarVisible);
                setVisible(wantVisible);
            }

        }
    }
    
    private void documentUndo(DocumentEvent evt) {
        Utilities.runInEventDispatchThread(new Runnable() {
            public void run() {
                // Clear the main cell
                setText(CELL_MAIN, "");
            }
        });
    }

    public void insertUpdate(DocumentEvent evt) {
        if (evt.getType() == DocumentEvent.EventType.REMOVE) { // undo
            documentUndo(evt);
        }
    }

    public void removeUpdate(DocumentEvent evt) {
        if (evt.getType() == DocumentEvent.EventType.INSERT) { // undo
            documentUndo(evt);
        }
    }

    public void changedUpdate(DocumentEvent evt) {
    }


    protected JPanel createPanel() {
        return new JPanel(new GridBagLayout());
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean v) {
        if (v != visible) {
            visible = v;

            if (panel != null || visible) {
                if (visible) { // need to refresh first
                    refreshPanel();
                }
                // fix for issue 13842
                if (SwingUtilities.isEventDispatchThread()) {
                    getPanel().setVisible(visible);
                } else {
                    SwingUtilities.invokeLater(
                        new Runnable() {
                            public void run() {
                                getPanel().setVisible(visible);
                            }
                        }
                    );
                }
            }
        }
    }

    public final JPanel getPanel() {
        if (panel == null) {
            panel = createPanel();
            initPanel();
        }
        return panel;
    }

    protected void initPanel() {
        addCell(CELL_POSITION, POS_MAX_STRINGS).setHorizontalAlignment(
            SwingConstants.CENTER);
        addCell(CELL_TYPING_MODE, new String[] { insText, ovrText }).setHorizontalAlignment(
            SwingConstants.CENTER);
        setText(CELL_TYPING_MODE, insText);
        addCell(CELL_MAIN, null);
    }

    public void propertyChange(PropertyChangeEvent evt) {
        String propName = evt.getPropertyName();

        if (EditorUI.COMPONENT_PROPERTY.equals(propName)) {
            JTextComponent component = (JTextComponent)evt.getNewValue();
            if (component != null) { // just installed
                component.addPropertyChangeListener(this);

                caret = component.getCaret();
                if (caret != null) {
                    caret.addChangeListener(caretL);
                }
                
                Document doc = component.getDocument();
                if (doc != null) {
                    doc.addDocumentListener(this);
                }

                settingsChange(null);
                refreshPanel();

            } else { // just deinstalled
                component = (JTextComponent)evt.getOldValue();

                component.removePropertyChangeListener(this);

                caret = component.getCaret();
                if (caret != null) {
                    caret.removeChangeListener(caretL);
                }

            }

        } else if ("caret".equals(propName)) {
            if (caret != null) {
                caret.removeChangeListener(caretL);
            }

            caret = (Caret)evt.getNewValue();
            if (caret != null) {
                caret.addChangeListener(caretL);
            }
        } else if ("document".equals(propName)) {
            Document old = (Document)evt.getOldValue();
            Document cur = (Document)evt.getNewValue();
            if (old != null) {
                old.removeDocumentListener(this);
            }
            if (cur != null) {
                cur.addDocumentListener(this);
            }
        }

        // Refresh the panel after each property-change
        if (EditorUI.OVERWRITE_MODE_PROPERTY.equals(propName)) {
            caretL.actionPerformed(null); // refresh immediately

        } else { // not overwrite mode change
            caretL.stateChanged(null);
        }
    }

    private void applyColoring() {
    }

    public int getCellCount() {
        return cellList.size();
    }

    public JLabel addCell(String name, String[] widestStrings) {
        return addCell(-1, name, widestStrings);
    }

    public JLabel addCell(int i, String name, String[] widestStrings) {
        Cell c = new Cell(name, widestStrings);
        addCellImpl(i, c);
        return c;
    }

    public void addCustomCell(int i, JLabel c) {
        addCellImpl(i, c);
    }

    private void addCellImpl(int i, JLabel c) {
        synchronized (cellList) {
            ArrayList newCellList = new ArrayList(cellList);
            int cnt = newCellList.size();
            if (i < 0 || i > cnt) {
                i = cnt;
            }
            newCellList.add(i, c);

            cellList = newCellList;
        }

        refreshPanel();
    }

    public JLabel getCellByName(String name) {
        Iterator i = cellList.iterator();
        while (i.hasNext()) {
            JLabel c = (JLabel)i.next();
            if (name.equals(c.getName())) {
                return c;
            }
        }
        return null;
    }

    public String getText(String cellName) {
        JLabel cell = getCellByName(cellName);
        return (cell != null) ? cell.getText() : null;
    }

    public void setText(String cellName, String text) {
        setText(cellName, text, null);
    }

    public void setBoldText(String cellName, String text) {
        setText(cellName, text, boldColoring);
    }

    public void setText(String cellName, String text,
                        Coloring extraColoring) {
        JLabel cell = getCellByName(cellName);
        if (cell != null) {
            Coloring c = coloring;
            if (extraColoring != null) {
                c = extraColoring.apply(c);
            }
            cell.setText(text);
            c.apply(cell);            
        }
    }

    /* Refresh the whole panel by removing all the components
    * and adding only those that appear in the cell-list.
    */
    private void refreshPanel() {
        SwingUtilities.invokeLater(
            new Runnable() {
                public void run() {
                    if (isVisible()) { // refresh only if visible
                        // Apply coloring to all cells
                        Iterator it = cellList.iterator();
                        while (it.hasNext()) {
                            JLabel c = (JLabel)it.next();
                            if (c instanceof Cell) {
                                coloring.apply(c);
                            }
                        }

                        // Layout cells
                        GridBagConstraints gc = new GridBagConstraints();
                        gc.gridx = GridBagConstraints.RELATIVE;
                        gc.gridwidth = 1;
                        gc.gridheight = 1;

                        it = cellList.iterator();
                        while (it.hasNext()) {
                            JLabel c = (JLabel)it.next();
                            boolean main = CELL_MAIN.equals(c.getName());
                            if (main) {
                                gc.fill = GridBagConstraints.HORIZONTAL;
                                gc.weightx = 1.0;
                            }
                            getPanel().add(c, gc);
                            if (main) {
                                gc.fill = GridBagConstraints.NONE;
                                gc.weightx = 0;
                            }
                        }
                    }
                }
            }
        );
    }

    class CaretListener implements ChangeListener, ActionListener {

        Timer timer;

        CaretListener(int delay) {
            timer = new Timer(delay, new WeakTimerListener(this));
            timer.setRepeats(false);
        }

        void setDelay(int delay) {
            timer.setInitialDelay(delay);
        }

        public void stateChanged(ChangeEvent evt) {
            timer.restart();
        }

        public void actionPerformed(ActionEvent evt) {
            Caret c = caret;
            JTextComponent component = editorUI.getComponent();

            if (component != null) {
                if (c != null) {
                    BaseDocument doc = Utilities.getDocument(editorUI.getComponent());
                    if (doc != null) {
                        int pos = c.getDot();
                        String s = Utilities.debugPosition(doc, pos);
                        setText(CELL_POSITION, s);
                    }
                }

                Boolean b = (Boolean)editorUI.getProperty(EditorUI.OVERWRITE_MODE_PROPERTY);
                boolean om = (b != null && b.booleanValue());
                if (om != overwriteModeDisplayed) {
                    overwriteModeDisplayed = om;
                    setText(CELL_TYPING_MODE, overwriteModeDisplayed ? ovrText : insText);
                }
            }
        }

    }

    static class Cell extends JLabel {

        Dimension maxDimension;

        String[] widestStrings;

        static final long serialVersionUID =-2554600362177165648L;

        Cell(String name, String[] widestStrings) {
            setName(name);
            setBorder(CELL_BORDER);
            setOpaque(true);
            this.widestStrings = widestStrings;
        }

        private void updateSize() {
            Font f = getFont();
            if (maxDimension == null) {
                maxDimension = new Dimension();
            }
            if (f != null) {
                Border b = getBorder();
                Insets ins = (b != null) ? b.getBorderInsets(this) : NULL_INSETS;
                FontMetrics fm = getFontMetrics(f);
                String text=this.getText();

                if( text == null)
                {
                    text="";
                }

                int mw = fm.stringWidth(text);

                maxDimension.height = fm.getHeight() + ins.top + ins.bottom;
                if (widestStrings != null) {
                    for (int i = 0; i < widestStrings.length; i++)
                    {
                        text= widestStrings[i];
                        if( text == null)
                        {
                            text="";
                        }

                        mw = Math.max(mw, fm.stringWidth(text));
                    }
                }
                maxDimension.width = mw + ins.left + ins.right;
            }
        }

        public Dimension getPreferredSize() {
            if (maxDimension == null) {
                maxDimension = new Dimension();
            }
            return new Dimension(maxDimension);
        }
        
        public Dimension getMinimumSize(){
            if (maxDimension == null) {
                maxDimension = new Dimension();
            }
            return new Dimension(maxDimension);
        }

        public void setFont(Font f) {
            super.setFont(f);
            updateSize();
        }

    }

}
