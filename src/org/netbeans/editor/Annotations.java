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

import org.netbeans.editor.ext.ExtKit;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.EventListenerList;
import javax.swing.text.BadLocationException;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;
//cgs import org.openide.util.RequestProcessor;

/** Annotations class act as data model containing all annotations attached
 * to one document. Class uses instances of private class LineAnnotations for 
 * grouping of added annotations by line. These objects (LineAnnotations) are
 * referenced from two collections. First one is Map where the key is Mark. 
 * It is used during the drawing in DrawLayerFactory.AnnotationLayer - when
 * the mark appears in mark change, the LineAnnotations instance is found for 
 * it and the active annotation on the line can be queried.
 * Second is List where the LineAnnotations are sorted by line number. This
 * list is used for drawing the annotations in the gutter when the sequential
 * order is important.
 * 
 * The class also listen on document. It need to know how many lines where
 * removed or added to refresh the LineAnnotations.line property.
 *
 * @author David Konecny
 * @since 07/2001
 */
public class Annotations implements DocumentListener {
    
    /** Map of [Mark, LineAnnotations] */
    private HashMap lineAnnotationsByMark;
    
    /** List of [LineAnnotations] which is ordered by line number */
    private ArrayList lineAnnotationsArray;

    /** Drawing layer for drawing of annotations */
    private DrawLayerFactory.AnnotationLayer drawLayer;

    /** Reference to document */
    private BaseDocument doc;

    /** List of listeners on AnnotationsListener*/
    private EventListenerList listenerList;

    /** Property change listener on annotation type changes */
    private PropertyChangeListener l;
    
    /** Property change listener on AnnotationTypes changes */
    private PropertyChangeListener annoTypesListener;

    /** Whether the column with glyph icons is visible */
    private boolean glyphColumn = false;
    
    /** Whether the column with cycling button is visible*/
    private boolean glyphButtonColumn = false;
    
    /** Whether the gutter popup menu has been initialized */
    private boolean menuInitialized = false;

    public Annotations(BaseDocument doc) {
        lineAnnotationsByMark = new HashMap(30);
        lineAnnotationsArray = new ArrayList(20);
        listenerList =  new EventListenerList();
        
        drawLayer = null;
        this.doc = doc;

        // add annotation drawing layer
        doc.addLayer(new DrawLayerFactory.AnnotationLayer(doc),
                DrawLayerFactory.ANNOTATION_LAYER_VISIBILITY);

        // listener on document changes
        this.doc.addDocumentListener(this);
        
        l = new PropertyChangeListener() {
            public void propertyChange (PropertyChangeEvent evt) {
                if (evt.getPropertyName() == AnnotationDesc.PROP_ANNOTATION_TYPE) {
                    AnnotationDesc anno = (AnnotationDesc)evt.getSource();
                    LineAnnotations lineAnnos = (LineAnnotations)lineAnnotationsByMark.get(anno.getMark());
                    lineAnnos.refreshAnnotations();
                    refreshLine(lineAnnos.getLine());
                }
                if (evt.getPropertyName() == AnnotationDesc.PROP_MOVE_TO_FRONT) {
                    AnnotationDesc anno = (AnnotationDesc)evt.getSource();
                    frontAnnotation(anno);
                }
            }
        };

        AnnotationTypes.getTypes().addPropertyChangeListener( annoTypesListener = new PropertyChangeListener() {
            public void propertyChange (PropertyChangeEvent evt) {
                if (evt.getPropertyName() == AnnotationTypes.PROP_COMBINE_GLYPHS) {
                    LineAnnotations lineAnnos;
                    for( Iterator it = lineAnnotationsArray.iterator(); it.hasNext(); ) {
                        lineAnnos = (LineAnnotations)it.next();
                        lineAnnos.refreshAnnotations();
                    }
                }
                if (evt.getPropertyName() == AnnotationTypes.PROP_ANNOTATION_TYPES) {
                    LineAnnotations lineAnnos;
                    for( Iterator it = lineAnnotationsArray.iterator(); it.hasNext(); ) {
                        lineAnnos = (LineAnnotations)it.next();
                        for( Iterator it2 = lineAnnos.getAnnotations(); it2.hasNext(); ) {
                            AnnotationDesc anno = (AnnotationDesc)it2.next();
                            anno.updateAnnotationType();
                        }
                    }
                }
                fireChangedAll();
            }
        });
        
    }

    /** Finds the drawing layer for annotations */
    public synchronized DrawLayerFactory.AnnotationLayer getLayer() {
        if (drawLayer == null)
            drawLayer = (DrawLayerFactory.AnnotationLayer)doc.findLayer(DrawLayerFactory.ANNOTATION_LAYER_NAME);
        return drawLayer;
    }
    
    /** Add annotation */
    public void addAnnotation(AnnotationDesc anno) {

        // create mark for this annotation. One mark can be shared by more annotations
        MarkChain chain = getLayer().getMarkChain();
        try {
            chain.addMark(anno.getOffset());
        } catch (BadLocationException e) {
            return;
        }
        // attach created mark to annotation
        anno.setMark(chain.getAddedMark());
        
        // fine LineAnnotations instance corresponding to the line of this annotation
        // or create new LineAnnotations if this is first annotation on this line
        LineAnnotations lineAnnos = getLineAnnotations(anno.getLine());
        if (lineAnnos == null) {
            lineAnnos = new LineAnnotations();
            lineAnnos.addAnnotation(anno);
            lineAnnotationsByMark.put(anno.getMark(), lineAnnos);
            
            // insert newly created LineAnnotations into sorted array
            boolean inserted = false;
            for (int i=0; i < lineAnnotationsArray.size(); i++) {
                if (((LineAnnotations)lineAnnotationsArray.get(i)).getLine() > lineAnnos.getLine()) {
                    lineAnnotationsArray.add(i, lineAnnos);
                    inserted = true;
                    break;
                }
            }
            if (!inserted)
                    lineAnnotationsArray.add(lineAnnos);

        }
        else {
            lineAnnos.addAnnotation(anno);
            // check whether this mark is in lineAnnotationsByMark Map
            // it is possible that Line.Part annotations will have more marks
            // for one line
            if (lineAnnotationsByMark.get(anno.getMark()) == null)
                lineAnnotationsByMark.put(anno.getMark(), lineAnnos);
        }

        // add listener on changes of annotation type
        anno.addPropertyChangeListener(l);

        // ignore annotation types with default icon
        if (anno.isVisible() && (!anno.isDefaultGlyph() || (anno.isDefaultGlyph() && lineAnnos.getCount() > 1))) {
            glyphColumn = true;
        }
        
        if (lineAnnos.getCount() > 1)
            glyphButtonColumn = true;

        // notify view that it must be redrawn
        refreshLine(lineAnnos.getLine());
    }

    /** Remove annotation */
    public void removeAnnotation(AnnotationDesc anno) {

        // find LineAnnotations for the mark
        LineAnnotations lineAnnos = (LineAnnotations)lineAnnotationsByMark.get(anno.getMark());
        int line = lineAnnos.getLine();
        // remove annotation from the line
        lineAnnos.removeAnnotation(anno);
        
        // check if this mark is referenced or not. If not, remove it
        if (!lineAnnos.isMarkStillReferenced(anno.getMark())) {
            lineAnnotationsByMark.remove(anno.getMark());
            MarkChain chain = getLayer().getMarkChain();
            chain.removeMark(anno.getOffset());
        }
        
        // if there is no more annotations on the line, remove LineAnnotations
        if (lineAnnos.getCount() == 0) {
            lineAnnotationsArray.remove(lineAnnotationsArray.indexOf(lineAnnos));
        }

        // clear the mark from annotation
        anno.setMark(null);

        // remove listener on changes of annotation type
        anno.removePropertyChangeListener(l);

        // notify view that must be redrawn
        refreshLine(line);
    }
    
    /** Finds active annotation for the Mark. It is called from DrawLayer 
     * when it found the DrawMark */
    public AnnotationDesc getActiveAnnotation(Mark mark) {
        LineAnnotations annos;
        annos = (LineAnnotations)lineAnnotationsByMark.get(mark);
        if (annos == null) {
            return null;
        }
        AnnotationDesc anno = annos.getActive();
        // it is possible that some other mark on the line (means
        // some other annotations) is active
        if (anno.getMark() != mark) {
            return null;
        }
        return anno;
    }

    /** Returns the active annotation for the line given by Mark. */
    AnnotationDesc getLineActiveAnnotation(Mark mark) {
        LineAnnotations annos;
        annos = (LineAnnotations)lineAnnotationsByMark.get(mark);
        if (annos == null) {
            return null;
        }
        AnnotationDesc anno = annos.getActive();
        return anno;
    }
    
    /** Finds LineAnnotations for the given line number */
    protected LineAnnotations getLineAnnotations(int line) {
        LineAnnotations annos;
        // TODO: optimize searching
        for (int i=0; i<lineAnnotationsArray.size(); i++) {
            annos = (LineAnnotations)lineAnnotationsArray.get(i);
            if (annos.getLine() == line)
                return annos;
        }
        return null;
    }

    /** Returns the active annotation for the given line number.
     * It is called from the glyph gutter*/
    public AnnotationDesc getActiveAnnotation(int line) {
        LineAnnotations annos = getLineAnnotations(line);
        if (annos == null)
            return null;
        return annos.getActive();
    }

    /** Move annotation in front of others. The activated annotation
     * is moved in front of other annotations on the same line */
    public void frontAnnotation(AnnotationDesc anno) {
        int line = anno.getLine();
        LineAnnotations annos = getLineAnnotations(line);
        if (annos == null)
            return;
        annos.activate(anno);
        refreshLine(line);
    }

    /** Activate next annotation on the line. Used for cycling 
     * through the annotations */
    public AnnotationDesc activateNextAnnotation(int line) {
        LineAnnotations annos = getLineAnnotations(line);
        if (annos == null)
            return null;
        AnnotationDesc aa = annos.activateNext();
        refreshLine(line);
        return aa;
    }

    /** Get next line number with some annotation*/
    public int getNextLineWithAnnotation(int line) {
        LineAnnotations annos;
        // TODO: optimize searching
        for (int i=0; i<lineAnnotationsArray.size(); i++) {
            annos = (LineAnnotations)lineAnnotationsArray.get(i);
            if (annos.getLine() >= line)
                return annos.getLine();
        }
        return -1;
    }

    /** Get next line number with some annotation*/
    public AnnotationDesc getAnnotation(int line, String type) {
        return null;
    }
    
    /** Return list of pasive annotations which should be drawn on the backgorund */
    public AnnotationDesc[] getPasiveAnnotations(int line) {
        LineAnnotations annos = getLineAnnotations(line);
        if (annos == null)
            return null;
        if (annos.getCount() <= 1)
            return null;
        return annos.getPasive();
    }

    /** Returns number of visible annotations on the line*/
    public int getNumberOfAnnotations(int line) {
        LineAnnotations annos = getLineAnnotations(line);
        if (annos == null)
            return 0;
        return annos.getCount();
    }
    

    /** Notify view that it is necessary to redraw the line of the document  */
    protected void refreshLine(int line) {
        fireChangedLine(line);
        int start = Utilities.getRowStartFromLineOffset(doc, line);
        int end = Utilities.getRowStartFromLineOffset(doc, line+1);
        if (end == -1)
            end = doc.getLength();
        doc.repaintBlock(start, end);
    }
    
    /** Checks the number of removed lines and recalculate
     * LineAnnotations.line property */
    public void removeUpdate(DocumentEvent e) {
        BaseDocumentEvent be = (BaseDocumentEvent)e;
        int countOfDeletedLines = be.getLFCount();
        if (countOfDeletedLines == 0)
            return;
        
        int changedLine = be.getLine();

        LineAnnotations annos;
        for (int i=0; i<lineAnnotationsArray.size(); i++) {
            annos = (LineAnnotations)lineAnnotationsArray.get(i);
            if (annos.getLine() > changedLine && annos.getLine() < changedLine+countOfDeletedLines)
                annos.setLine(changedLine);
            if (annos.getLine() > changedLine)
                annos.setLine(annos.getLine()-countOfDeletedLines);
        }
        // fire event to AnnotationsListeners that everything should be redraw
        fireChangedAll();
    }
    
    /** Checks the number of inserted lines and recalculate
     * LineAnnotations.line property */
    public void insertUpdate(DocumentEvent e) {
        BaseDocumentEvent be = (BaseDocumentEvent)e;
        int countOfInsertedLines = be.getLFCount();
        if (countOfInsertedLines == 0)
            return;
        
        int changedLine = be.getLine();

        LineAnnotations annos;
        LineAnnotations current = null;
        for (int i=0; i<lineAnnotationsArray.size(); i++) {
            annos = (LineAnnotations)lineAnnotationsArray.get(i);
            if (annos.getLine() == changedLine && annos.getActive().getOffset() > e.getOffset())
                current = annos;
            if (annos.getLine() > changedLine)
                annos.setLine(annos.getLine()+countOfInsertedLines);
        }
        if (current != null)
            current.setLine(current.getLine()+countOfInsertedLines);
        
        // fire event to AnnotationsListeners that everything should be redraw
        fireChangedAll();
    }
    
    /**Gives notification that an attribute or set of attributes changed.*/
    public void changedUpdate(DocumentEvent e) {
    }
    
    /** Add AnnotationsListener listener */
    public void addAnnotationsListener(AnnotationsListener listener) {
	listenerList.add(AnnotationsListener.class, listener);
    }

    /** Remove AnnotationsListener listener */
    public void removeAnnotationsListener(AnnotationsListener listener) {
	listenerList.remove(AnnotationsListener.class, listener);
    }

    /** Fire AnnotationsListener.ChangedLine change*/
    protected void fireChangedLine(int line) {
	// Guaranteed to return a non-null array
	Object[] listeners = listenerList.getListenerList();
	// Process the listeners last to first, notifying
	// those that are interested in this event
	for (int i = listeners.length-2; i>=0; i-=2) {
	    if (listeners[i]==AnnotationsListener.class) {
		// Lazily create the event:
		// if (e == null)
		// e = new ListSelectionEvent(this, firstIndex, lastIndex);
		((AnnotationsListener)listeners[i+1]).changedLine(line);
	    }	       
	}
    }
   
    /** Fire AnnotationsListener.ChangedAll change*/
    protected void fireChangedAll() {
	// Guaranteed to return a non-null array
	Object[] listeners = listenerList.getListenerList();
	// Process the listeners last to first, notifying
	// those that are interested in this event
	for (int i = listeners.length-2; i>=0; i-=2) {
	    if (listeners[i]==AnnotationsListener.class) {
		// Lazily create the event:
		// if (e == null)
		// e = new ListSelectionEvent(this, firstIndex, lastIndex);
		((AnnotationsListener)listeners[i+1]).changedAll();
	    }	       
	}
    }

    /** Return whether this document has or had any glyph icon attached.
     * This method is called from glyph gutter to check whether the glyph column
     * should be drawn or not. */
    public boolean isGlyphColumn() {
        return glyphColumn;
    }
    
    /** Return whether this document has or had more annotations on one line.
     * This method is called from glyph gutter to check whether the glyph cycling 
     * column should be drawn or not. */
    public boolean isGlyphButtonColumn() {
        return glyphButtonColumn;
    }

    /** Creates menu item for the given action. It must handle the BaseActions, which
     * have localized name stored not in Action.NAME property. */
    private JMenuItem createMenuItem(Action action) {
        if (action instanceof BaseAction) {
            JMenuItem item = new JMenuItem( ((BaseAction)action).getPopupMenuText(null) );
            item.addActionListener(action);
            return item;
        } else {
            JMenuItem item = new JMenuItem( (String)action.getValue(Action.NAME) );
            item.addActionListener(action);
            return item;
        }
    }

    /** Creates popup menu with all actions for the given line. */
    public JPopupMenu createPopupMenu(BaseKit kit, int line) {
        return createMenu(kit, line, false).getPopupMenu();
    }
    
    private void initMenu(JMenu pm, BaseKit kit, int line){
        LineAnnotations annos = getLineAnnotations(line);
        Map types = new HashMap(AnnotationTypes.getTypes().getVisibleAnnotationTypeNamesCount() * 4/3);

        Action[] actions;
        boolean separator = false;
        boolean added = false;
        JMenu subMenu;

        if (annos != null) {
            
            // first, add actions for active annotation
            AnnotationDesc anno = annos.getActive();
            if (anno != null) {
                actions = anno.getActions();
                if (actions != null) {
                    for (int i=0; i<actions.length; i++) {
                        pm.add(createMenuItem(actions[i]));
                    }
                    separator = true;
                    types.put(anno.getAnnotationType(), anno.getAnnotationType());
                }
            }

            // second, add submenus for all pasive annotations
            AnnotationDesc[] pasiveAnnos = annos.getPasive();
            added = false;
            if (pasiveAnnos != null) {
                for (int i=0; i < pasiveAnnos.length; i++) {
                    actions = pasiveAnnos[i].getActions();
                    if (actions != null) {
                        subMenu = new JMenu(pasiveAnnos[i].getAnnotationTypeInstance().getDescription());
                        for (int j=0; j<actions.length; j++)
                            subMenu.add(createMenuItem(actions[j]));
                        if (separator) {
                            separator = false;
                            pm.addSeparator();
                        }
                        pm.add(subMenu);
                        added = true;
                        types.put(pasiveAnnos[i].getAnnotationType(), pasiveAnnos[i].getAnnotationType());
                    }
                }
                if (added)
                    separator = true;
            }
        }

        // third, add all remaining possible actions to the end of the list
        added = false;
        AnnotationType type;
        for (Iterator i = AnnotationTypes.getTypes().getAnnotationTypeNames(); i.hasNext(); ) {
            type = AnnotationTypes.getTypes().getType((String)i.next());
            if (type == null || !type.isVisible())
                continue;
            if (types.get(type.getName()) != null)
                continue;
            actions = type.getActions();
            if (actions != null) {
                subMenu = new JMenu(type.getDescription());
                for (int j=0; j<actions.length; j++)
                    subMenu.add(createMenuItem(actions[j]));
                if (separator) {
                    separator = false;
                    pm.addSeparator();
                }
                pm.add(subMenu);
                added = true;
            }
        }
        if (added)
            separator = true;

        if (separator)
            pm.addSeparator();
        
        // add checkbox for enabling/disabling of line numbers
        BaseAction action = (BaseAction)kit.getActionByName(BaseKit.toggleLineNumbersAction);
        pm.add(action.getPopupMenuItem(null));
        
        BaseAction action2 = (BaseAction)kit.getActionByName(ExtKit.toggleToolbarAction);
        if (action2 != null){
            pm.add(action2.getPopupMenuItem(null));
        }
        menuInitialized = true;
    }

    private static class DelayedMenu extends JMenu{
        
//cgs         RequestProcessor.Task task;
        
        public DelayedMenu(String s){
            super(s);
        }
        
        public JPopupMenu getPopupMenu() {
//cgs             if (task!=null && (!task.isFinished())){
//cgs                 task.waitFinished();
//cgs             }
            return super.getPopupMenu();
        }
        
  //cgs       public void addTask(RequestProcessor.Task task){
//cgs             this.task = task;
//cgs         }
    }
    
    private JMenu createMenu(BaseKit kit, int line, boolean backgroundInit){
        final DelayedMenu pm = new DelayedMenu(LocaleSupport.getString("generate-gutter-popup"));
        final BaseKit fKit = kit;
        final int fLine = line;

        if (backgroundInit){
 //cgs            RequestProcessor.Task task = RequestProcessor.postRequest(new Runnable(){
 //cgs                public void run(){
  //cgs                   initMenu(pm, fKit, fLine);
 //cgs                }
  //cgs           });
  //cgs           pm.addTask(task);
        }else{
            initMenu(pm, fKit, fLine);
        }

        return pm;
    }
    
    /** Creates popup menu with all actions for the given line. */
    public JMenu createMenu(BaseKit kit, int line) {
        boolean bkgInit = menuInitialized;
        menuInitialized = true;
        return createMenu(kit, line, !bkgInit);
    }
    
    /** Manager of all annotations attached to one line. Class stores
     * the references to all annotations from one line in List and also
     * stores which annotation is active, count of visible annotations 
     * and line number. */
    static public class LineAnnotations extends Object {

        /** List with all annotations in this LineAnnotations */
        private LinkedList annos;

        /** List with all visible annotations in this LineAnnotations */
        private LinkedList annosVisible;
        
        /** Active annotation. Used only in case there is more than one
         * annotation on the line */
        private AnnotationDesc active;
        
        /** Line number */
        private int lineNumber;
        
        protected LineAnnotations() {
            annos = new LinkedList();
            annosVisible = new LinkedList();
            lineNumber = -1;
        }

        /** Add annotation to this line and activate it. */
        public void addAnnotation(AnnotationDesc anno) {
            if (lineNumber == -1)
                lineNumber = anno.getLine();
            annos.add(anno);
            if (anno.isVisible()) {
                active = anno;
            }
            refreshAnnotations();
        }
        
        /** Remove annotation from this line. Refresh the active one
         * and count of visible. */
        public void removeAnnotation(AnnotationDesc anno) {
            if (anno == active)
                activateNext();
            annos.remove(anno);
            if (active == anno)
                active = null;
            refreshAnnotations();
        }

        /** Return the active line annotation. */
        public AnnotationDesc getActive() {
            return active;
        }

        /** Getter for the line number property */
        public int getLine() {
            return lineNumber;
        }

        /** Setter for the line number property */
        public void setLine(int line) {
            lineNumber = line;
        }

        /** Gets the array of all pasive and visible annotations */
        public AnnotationDesc[] getPasive() {
            AnnotationDesc[] pasives = new AnnotationDesc[getCount()-1];
            AnnotationDesc anno;
            int startIndex = annosVisible.indexOf(getActive());
            int index = startIndex;
            int i=0;
            while (true) {
                index++;
                if (index >= annosVisible.size())
                    index = 0;
                if (index == startIndex)
                    break;

                pasives[i] = (AnnotationDesc)annosVisible.get(index);
                i++;
            }
            return pasives;
        }

        /** Make the given annotation active. */
        public boolean activate(AnnotationDesc anno) {
            
            int i,j;
            i = annosVisible.indexOf(anno);

            if (i == -1) {
                // was anno combined by some type ??
                for(j=0; j < annosVisible.size(); j++) {
                    if (annosVisible.get(j) instanceof AnnotationCombination) {
                        if (((AnnotationCombination)annosVisible.get(j)).isAnnotationCombined(anno)) {
                            i = j;
                            anno = (AnnotationCombination)annosVisible.get(j);
                            break;
                        }
                    }
                }
            }
            
            if (i == -1)
                return false;
            
            if (annosVisible.get(i) == null)
                return false;
            
            if (anno == active || !anno.isVisible())
                return false;
            
            active = anno;
            
            return true;
        }

        /** Get count of visible annotations on the line */
        public int getCount() {
            return annosVisible.size();
        }

        /** Activate next annoation on the line. Used during the cycling. */
        public AnnotationDesc activateNext() {
            if (getCount() <= 1)
                return active;
            
            int current = annosVisible.indexOf(active);
            current++;
            if (current >= getCount())
                current = 0;
            active = (AnnotationDesc)annosVisible.get(current);
            return active;
        }
        
        /** Searches all combination annotation type and sort them
         * by getCombinationOrder into combTypes array
         * which is passed as paramter. */
        private void fillInCombinationsAndOrderThem(LinkedList combTypes) {
            AnnotationType type;
            AnnotationType.CombinationMember[] combs;
            
            for (Iterator it = AnnotationTypes.getTypes().getAnnotationTypeNames(); it.hasNext(); ) {
                type = AnnotationTypes.getTypes().getType((String)it.next());
                if (type == null)
                    continue;
                combs = type.getCombinations();
                if (combs != null && type.isWholeLine() &&
                    (combs.length >= 2 || (combs.length == 1 && combs[0].isAbsorbAll())) ) {
                    if (type.getCombinationOrder() == 0) {
                        combTypes.add(type);
                    } else {
                        boolean inserted = false;
                        for (int i=0; i < combTypes.size(); i++) {
                            if ( ((AnnotationType)combTypes.get(i)).getCombinationOrder() > type.getCombinationOrder()) {
                                combTypes.add(i, type);
                                inserted = true;
                                break;
                            }
                        }
                        if (!inserted)
                            combTypes.add(type);
                    }
                }
            }
        }

        /** For the given combination annotation type and list of annotations
         * it finds all annotations which are combined by this combination
         * and inserts into list of annotations new combined annotation which
         * wraps combined annotations. The result list of annotations can
         * contain null values for annotations which were combined. */
        private boolean combineType(AnnotationType combType, LinkedList annosDupl) {

            int i, j, k;
            boolean matchedType;
            int countOfAnnos = 0;
            int valid_optional_count = 0;
            
            LinkedList combinedAnnos = new LinkedList();

            AnnotationType.CombinationMember[] combs = combType.getCombinations();
            
            // check that there is match between line annos & all types specified in combination
            boolean matchedComb = true;
            AnnotationType.CombinationMember comb;
            AnnotationDesc anno;
            for (i=0; i < combs.length; i++) {

                comb = combs[i];
                matchedType = false;
                
                // check that for one specified combination type there exist some annotation
                for (j=0; j < annosDupl.size(); j++) {
                    
                    anno = (AnnotationDesc)annosDupl.get(j);
                    
                    if (anno == null)
                        continue;

                    // check whether this annotation matches the specified combination type
                    if (comb.getName().equals( anno.getAnnotationType() )) {
                        countOfAnnos++;

                        // now check if the combination has specified some minimum count of annos
                        if (comb.getMinimumCount() == 0) {
                            matchedType = true;
                            countOfAnnos++;
                            combinedAnnos.add(anno);
                            if (!comb.isAbsorbAll())
                                break;
                        } else {
                            int requiredCount = comb.getMinimumCount() - 1;
                            for (k=j+1; (k < annosDupl.size()) && (requiredCount > 0); k++) {
                                if (annosDupl.get(k) == null)
                                    continue;
                                if (comb.getName().equals( ((AnnotationDesc)annosDupl.get(k)).getAnnotationType() )) {
                                    requiredCount--;
                                }
                            }
                            if (requiredCount == 0) {
                                matchedType = true;
                                
                                combinedAnnos.add(anno);
                                for (k=j+1; k < annosDupl.size(); k++) {
                                    if (annosDupl.get(k) == null)
                                        continue;
                                    if (comb.getName().equals( ((AnnotationDesc)annosDupl.get(k)).getAnnotationType() )) {
                                        countOfAnnos++;
                                        combinedAnnos.add(annosDupl.get(k));
                                    }
                                }
                            }
                            break;
                        }
                        
                    }
                    
                }
                
                if (matchedType) {
                    if (comb.isOptional())
                        valid_optional_count++;
                } else {
                    if (!comb.isOptional()) {
                        matchedComb = false;
                        break;
                    }
                }
                
            }
            if (combType.getMinimumOptionals() > valid_optional_count)
                matchedComb = false;

            AnnotationCombination annoComb = null;
            if (matchedComb) {

                boolean activateComb = false;
                
                for (i=0; i<combinedAnnos.size(); i++) {
                    if (combinedAnnos.get(i) == active)
                        activateComb = true;
                    
                    if (annoComb == null) {
                        annoComb = new AnnotationCombination(combType.getName(), (AnnotationDesc)combinedAnnos.get(i));
                        annosDupl.set(annosDupl.indexOf(combinedAnnos.get(i)),annoComb);  // replace the original annotation by the new Combined one
                    } else {
                        annoComb.addCombinedAnnotation((AnnotationDesc)combinedAnnos.get(i));
                        annosDupl.set(annosDupl.indexOf(combinedAnnos.get(i)),null);  // remove annotations which were combined form the array
                    }
                }
                if (activateComb)
                    active = annoComb;
                
                return true;
            }
            
            return false;
        }
    
        
        
        
        /** Refresh the active annotation and count of visible annotations. 
         * This method is used after change of annotation type of some annotation
         * on this line */
        public void refreshAnnotations() {
            int i, j, k, count;
            
            if (!AnnotationTypes.getTypes().isCombineGlyphs().booleanValue()) {
                
                // combinations are disabled
                annosVisible = new LinkedList();
                for (i=0; i < annos.size(); i++) {
                    if ( ! ((AnnotationDesc)annos.get(i)).isVisible() )
                        continue;
                    annosVisible.add(annos.get(i));
                }
                
            } else {
                
                // combination are enabled
                LinkedList annosDupl = (LinkedList)annos.clone();
    
                // List of all annotation types
                LinkedList combTypes = new LinkedList();
                
                // first, fill in the array with combination types sorted by the order
                fillInCombinationsAndOrderThem(combTypes);
                
                for (int ct=0; ct < combTypes.size(); ct++) {
                    combineType((AnnotationType)combTypes.get(ct), annosDupl);
                }

                annosVisible = new LinkedList();

                // add remaining not combined annotations into the line annotations array
                for (i=0; i < annosDupl.size(); i++) {
                    if (annosDupl.get(i) != null && ((AnnotationDesc)annosDupl.get(i)).isVisible() )
                        annosVisible.add(annosDupl.get(i));
                }
            }

            // update the active annotation
            if (annosVisible.indexOf(active) == -1) {
                if (annosVisible.size() > 0)
                    active = (AnnotationDesc)annosVisible.get(0);
                else
                    active = null;
            }
        }

        /** Is this given mark still referenced by some annotation or it
         * can be removed from the draw mark chain */
        public boolean isMarkStillReferenced(Mark mark) {
            AnnotationDesc anno;
            for( Iterator it = annos.listIterator(); it.hasNext(); ) {
                anno = (AnnotationDesc)it.next();
                if (anno.getMark() == mark)
                   return true;
            }
            return false;
        }
        
        public Iterator getAnnotations() {
            return annos.iterator();
        }
        
    }
    

    /** Listener for listening on changes in Annotations object.*/
    public interface AnnotationsListener extends EventListener {

        /** This method is fired when annotations on the line are changed - 
         * annotation was added, removed, changed, etc. */
        public void changedLine(int Line);

        /** It is not possible to trace what have changed and so the listeners
         * are only informed that something has changed and that the change
         * must be reflected somehow (most probably by complete redraw). */
        public void changedAll();

    }

    /** Annotation which is used for representation of combined annotations. 
     * Some basic operations like getLine etc. are delegated to one of the
     * annotations which are representd by this combined annotation. The only
     * added functionality is for tooltip text and annotation type.
     */
    private static class AnnotationCombination extends AnnotationDesc {
        
        /** Delegate annotaiton */
        private AnnotationDesc delegate;

        /** Annotation type */
        private String type;
        
        /** List of annotations which are combined */
        private LinkedList list;
        
        public AnnotationCombination(String type, AnnotationDesc delegate) {
            super(delegate.getOffset(), delegate.getLength());
            this.delegate = delegate;
            this.type = type;
            updateAnnotationType();
            list = new LinkedList();
            list.add(delegate);
        }
        
        /** Getter for offset of this annotation  */
        public int getOffset() {
            return delegate.getOffset();
        }
        
        /** Getter for line number of this annotation  */
        public int getLine() {
            return delegate.getLine();
        }
        
        /** Getter for localized tooltip text for this annotation  */
        public String getShortDescription() {
            return getAnnotationTypeInstance().getDescription();
        }
        
        /** Getter for annotation type name  */
        public String getAnnotationType() {
            return type;
        }

        /** Add the annotation to this combination */
        public void addCombinedAnnotation(AnnotationDesc anno) {
            list.add(anno);
        }

        /** Is the given annotation part of this combination */
        public boolean isAnnotationCombined(AnnotationDesc anno) {
            if (list.indexOf(anno) == -1)
                return false;
            else
                return true;
        }

        /** Get Mark which represent this annotation in document */
        Mark getMark() {
            return delegate.getMark();
        }
        
    }
}
