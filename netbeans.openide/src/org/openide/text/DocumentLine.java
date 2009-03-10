/*
 *                 Sun Public License Notice
 * 
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 * 
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 1997-2000 Sun
 * Microsystems, Inc. All Rights Reserved.
 */

package org.openide.text;

import java.io.*;
import java.util.*;
import javax.swing.text.*;
import javax.swing.event.*;
import javax.swing.SwingUtilities;

import org.openide.ErrorManager;
import org.openide.loaders.DataObject;
import org.openide.util.WeakListener;
import org.openide.text.EnhancedChangeEvent;


/** Implementation of a line in a {@link StyledDocument}.
* One object
* of this class represents a line in the document by holding
* a {@link PositionRef}, which can represent a position in an open or
* closed document.
*
* @author Jaroslav Tulach, David Konecny
*/
public abstract class DocumentLine extends Line {
    /** reference to one position on the line */
    protected PositionRef pos;

    /** is breakpoint there - presistent state 
     @deprecated since 1.20 */
    private boolean breakpoint;

    /** error line  - transient state
     @deprecated since 1.20 */
    private transient boolean error;

    /** current line - transient state
     @deprecated since 1.20 */
    private transient boolean current;

    /** listener for changes of state of the document */
    private transient LR listener;

    /** weak document listener assigned to the document or null */
    private transient DocumentListener docL;

    /** weak map that assignes to editor supports whether they have current or error line
    * selected. (EditorSupport, DocumentLine[2]), where Line[0] is current and Line[1] is error */
    private static WeakHashMap assigned = new WeakHashMap (5);

    /** List of Line.Part which exist for this line*/
    private List lineParts = new ArrayList(3);
    
    static final long serialVersionUID =3213776466939427487L;
    /** Constructor.
    * @param obj data object we belong to
    * @param pos position on the line
    */
    public DocumentLine (DataObject obj, PositionRef pos) {
        super (obj);
        this.pos = pos;
    }

    /** Init listeners
    */
    void init () {
        listener = new LR ();
        pos.getCloneableEditorSupport ().addChangeListener (WeakListener.change (listener, pos.getCloneableEditorSupport ()));
    }

    /* Get the line number.
     * The number may change if the
    * text is modified.
    *
    * @return Returns current line number.
    */
    public int getLineNumber () {
        try {
            return pos.getLine ();
        } catch (IOException ex) {
            // what else?
            return 0;
        }
    }

    /* Shows the line.
    * @param kind one of SHOW_XXX constants.
    * @column the column of this line which should be selected
    */
    public abstract void show(int kind, int column);

    /* Sets the breakpoint. */
    public void setBreakpoint(boolean b) {
        if (breakpoint != b) {
            breakpoint = b;
            refreshState ();
        }
    }

    /* Tests if the breakpoint is set. */
    public boolean isBreakpoint () {
        return breakpoint;
    }

    /* Marks the error. */
    public void markError () {
        DocumentLine previous = registerLine (1, this);
        if (previous != null) {
            previous.error = false;
            previous.refreshState ();
        }

        error = true;

        refreshState ();
    }

    /* Unmarks error at this line. */
    public void unmarkError () {
        error = false;
        registerLine (1, null);

        refreshState ();
    }

    /* Marks this line as current. */
    public void markCurrentLine () {
        DocumentLine previous = registerLine (0, this);
        if (previous != null) {
            previous.current = false;
            previous.refreshState ();
        }

        current = true;
        refreshState ();
    }

    /* Unmarks this line as current. */
    public void unmarkCurrentLine () {
        current = false;
        registerLine (0, null);

        refreshState ();
    }

    /** Refreshes the current line.
     *
     * @deprecated since 1.20. */
    synchronized void refreshState () {
        StyledDocument doc = pos.getCloneableEditorSupport ().getDocument ();

        if (doc != null) {
            // the document is in memory, mark the state

            if (docL != null) {
                doc.removeDocumentListener (docL);
            }

            // error line
            if (error) {
                NbDocument.markError (doc, pos.getOffset ());

                doc.addDocumentListener (docL = WeakListener.document (listener, doc));

                return;
            }

            // current line
            if (current) {
                NbDocument.markCurrent (doc, pos.getOffset ());
                return;
            }

            // breakpoint line
            if (breakpoint) {
                NbDocument.markBreakpoint (doc, pos.getOffset ());
                return;
            }

            NbDocument.markNormal (doc, pos.getOffset ());
            return;
        }
    }

    public int hashCode () {
        return pos.getCloneableEditorSupport ().hashCode ();
    }

    public boolean equals (Object o) {
        if (o instanceof DocumentLine) {
            DocumentLine dl = (DocumentLine)o;
            if (dl.pos.getCloneableEditorSupport () == pos.getCloneableEditorSupport ()) {
                return dl.getLineNumber () == getLineNumber ();
            }
        }
        return false;
    }


    //
    // Work with global hash table
    //

    /** Register this line as the one stored
    * under indx-index (0 = current, 1 = error).
    *
    * @param indx index to register
    * @param line value to add (this or null)
    * @return the previous value
    *
    * @deprecated since 1.20 */
    private DocumentLine registerLine (int indx, DocumentLine line) {
        DocumentLine prev;

        CloneableEditorSupport es = pos.getCloneableEditorSupport ();

        DocumentLine[] arr = (DocumentLine[])assigned.get (es);

        if (arr != null) {
            // remember the previous
            prev = arr[indx];
        } else {
            // create new array
            arr = new DocumentLine[2];
            assigned.put (es, arr);
            prev = null;
        }
        arr[indx] = line;
        return prev;

    }


    //
    // Serialization
    //

    /** Write fields.
    */
    private void writeObject (ObjectOutputStream oos) throws IOException {
        // do not do default read/write object
        oos.writeObject (pos);
        oos.writeBoolean (breakpoint);
    }

    /** Read important fields.
    */
    private void readObject (ObjectInputStream ois)
    throws IOException, ClassNotFoundException {
        pos = (PositionRef)ois.readObject ();
        setBreakpoint (ois.readBoolean ());
        lineParts = new ArrayList(3);
    }

    /** Register line.
    */
    Object readResolve() throws ObjectStreamException {
//        return Set.registerLine (this);
        //Set.registerPendingLine(this);
        return this.pos.getCloneableEditorSupport().getLineSet().registerLine(this);
    }

    
    /** Add annotation to this Annotatable class
     * @param anno annotation which will be attached to this class */
    protected void addAnnotation(Annotation anno) {
        super.addAnnotation(anno);
        StyledDocument doc = pos.getCloneableEditorSupport ().getDocument ();
        
        // document is not opened and so the annotation will be added to document later
        if (doc == null)
            return;

        pos.getCloneableEditorSupport().prepareDocument().waitFinished();
        
        try {
            if (!anno.isInDocument()) {
                anno.setInDocument(true);
                NbDocument.addAnnotation (doc, pos.getPosition(), -1, anno);
            }
        }  catch (IOException ex) {
            ErrorManager.getDefault ().notify ( ErrorManager.EXCEPTION, ex);
        }
    }
    
    /** Remove annotation to this Annotatable class
     * @param anno annotation which will be detached from this class  */
    protected void removeAnnotation(Annotation anno) {
        super.removeAnnotation(anno);
        StyledDocument doc = pos.getCloneableEditorSupport ().getDocument ();
        
        // document is not opened and so no annotation is attached to it
        if (doc == null)
            return;

        pos.getCloneableEditorSupport().prepareDocument().waitFinished();

        if (anno.isInDocument()) {
            anno.setInDocument(false);
            NbDocument.removeAnnotation(doc, anno);
        }
    }

    /** When document is opened or closed the annotations must be added or
     * removed.
     * @since 1.27 */
    void attachDetachAnnotations(StyledDocument doc, boolean closing) {
        java.util.List list = getAnnotations();
        for (int i=0; i<list.size(); i++) {
            Annotation anno = (Annotation)list.get(i);
            if (!closing) {
                try {
                    if (!anno.isInDocument()) {
                        anno.setInDocument(true);
                        NbDocument.addAnnotation (doc, pos.getPosition(), -1, anno);
                    }
                }  catch (IOException ex) {
                    ErrorManager.getDefault ().notify ( ErrorManager.EXCEPTION, ex);
                }
            } else {
                if (anno.isInDocument()) {
                    anno.setInDocument(false);
                    NbDocument.removeAnnotation(doc, anno);
                }
            }
        }
        
        // notify also all Line.Part attached to this Line
        for (int i=0; i<lineParts.size(); i++) {
            ((DocumentLine.Part)lineParts.get(i)).attachDetachAnnotations(doc, closing);
        }
    }
    
    public String getText() {
        StyledDocument doc = pos.getCloneableEditorSupport ().getDocument ();
        
        // document is not opened
        if (doc == null)
            return null;

        int lineNumber = getLineNumber();
        int lineStart = NbDocument.findLineOffset(doc, lineNumber);
        // #24434: Check whether the next line exists
        // (the current one could be the last one).
        int lineEnd;
        if((lineNumber + 1) 
        >= NbDocument.findLineRootElement(doc).getElementCount()) {
            lineEnd = doc.getLength();
        } else {
            lineEnd = NbDocument.findLineOffset(doc, lineNumber + 1);
        }
        
        try {
            return doc.getText(lineStart, lineEnd - lineStart);
        } catch (BadLocationException ex) {
            ErrorManager.getDefault ().notify ( ErrorManager.EXCEPTION, ex);
            return null;
        }
    }

    /** Attach created Line.Part to the parent Line */
    void addLinePart(DocumentLine.Part linePart) {
        lineParts.add(linePart);
    }

    /** Move Line.Part from this Line to a new one*/
    void moveLinePart(DocumentLine.Part linePart, DocumentLine newLine) {
        lineParts.remove(linePart);
        newLine.addLinePart(linePart);
        linePart.changeLine(newLine);
    }

    /** Notify Line.Part(s) that content of the line was changed and that Line.Part(s) may be affected by that*/
    void notifyChange(DocumentEvent p0, DocumentLine.Set set, StyledDocument doc) {
        DocumentLine.Part part;
        for (int i=0; i<lineParts.size(); ) {
            part = (DocumentLine.Part)lineParts.get(i);
            // notify Line.Part about the change
            part.handleDocumentChange(p0);
            // if necessary move Line.Part to new Line
            if (NbDocument.findLineNumber(doc, part.getOffset()) != part.getLine().getLineNumber()) {
                DocumentLine line = (DocumentLine)set.getCurrent(NbDocument.findLineNumber(doc, part.getOffset()));
                moveLinePart(part, line);
            } else {
                i++;
            }
        }
    }
    
    /** Notify Line.Part(s) that line was moved. */
    void notifyMove() {
        updatePositionRef();
        
        for (int i=0; i<lineParts.size(); i++) {
            ((DocumentLine.Part)lineParts.get(i)).firePropertyChange(Line.Part.PROP_LINE, null, null);
        }
    }
    
    /** Updates <code>pos</code> the way it points at the start of line. */
    private void updatePositionRef() {
        CloneableEditorSupport support = pos.getCloneableEditorSupport();
        int startOffset = NbDocument.findLineOffset(support.getDocument(),
            getLineNumber());
        if(pos.getOffset() != startOffset) {
            pos = new PositionRef(
                support.getPositionManager(), startOffset, Position.Bias.Forward
            );
        }
    }
    
    
    /** Implementation of Line.Part abstract class*/
    static class Part extends Line.Part {

        /** Reference of this part to the document*/
        private PositionRef position;
        
        /** Reference to Line to which this part belongs*/
        private Line line;
        
        /** Length of the annotated text*/
        private int length;
    
        /** Offset of this Part before the modification. This member is used in
         * listener on document changes and it is updated after each change. */
        private int previousOffset;
        
        public Part (Line line, PositionRef position, int length)
        {
            this.position = position;
            this.line = line;
            this.length = length;
            previousOffset = position.getOffset();
        }
        
        /** Start column of annotation */
        public int getColumn() {
            try {
                return position.getColumn();
            }  catch (IOException ex) {
                return 0; //TODO: change this
            }
        }
        
        /** Length of the annotated text. The length does not cross line end. If the annotated text is
         * split during the editing, the annotation is shorten till the end of the line. Modules can listen on
         * changes of this value*/
        public int getLength() {
            return length;
        }
        
        /** Line can change during editting*/
        public Line getLine() {
            return line;
        }
        
        /** Offset of the Line.Part*/
        int getOffset() {
            return position.getOffset();
        }
        
        /** Line can change during editting*/
        void changeLine(Line line) {
            this.line = line;
            // TODO: check whether there is really some change
            firePropertyChange (PROP_LINE_NUMBER, null, line);
        }
        
        /** Add annotation to this Annotatable class
         * @param anno annotation which will be attached to this class */
        protected void addAnnotation(Annotation anno) {
            super.addAnnotation(anno);
            StyledDocument doc = position.getCloneableEditorSupport ().getDocument ();

            // document is not opened and so the annotation will be added to document later
            if (doc == null)
                return;

            position.getCloneableEditorSupport().prepareDocument().waitFinished();

            try {
                if (!anno.isInDocument()) {
                    anno.setInDocument(true);
                    NbDocument.addAnnotation(doc, position.getPosition(), length, anno);
                }
            }  catch (IOException ex) {
                ErrorManager.getDefault ().notify ( ErrorManager.EXCEPTION, ex);
            }
        }

        /** Remove annotation to this Annotatable class
         * @param anno annotation which will be detached from this class  */
        protected void removeAnnotation(Annotation anno) {
            super.removeAnnotation(anno);
            StyledDocument doc = position.getCloneableEditorSupport ().getDocument ();

            // document is not opened and so no annotation is attached to it
            if (doc == null)
                return;

            position.getCloneableEditorSupport().prepareDocument().waitFinished();

            if (anno.isInDocument()) {
                anno.setInDocument(false);
                NbDocument.removeAnnotation(doc, anno);
            }
        }

        public String getText() {
            StyledDocument doc = position.getCloneableEditorSupport ().getDocument ();

            // document is not opened
            if (doc == null)
                return null;

            try {
                return doc.getText(position.getOffset(), getLength());
            } catch (BadLocationException ex) {
                ErrorManager.getDefault ().notify ( ErrorManager.EXCEPTION, ex);
                return null;
            }
        }
        
        /** When document is opened or closed the annotations must be added or
         * removed.*/
        void attachDetachAnnotations(StyledDocument doc, boolean closing) {
            java.util.List list = getAnnotations();
            for (int i=0; i<list.size(); i++) {
                Annotation anno = (Annotation)list.get(i);
                if (!closing) {
                    try {
                        if (!anno.isInDocument()) {
                            anno.setInDocument(true);
                            NbDocument.addAnnotation (doc, position.getPosition(), getLength(), anno);
                        }
                    }  catch (IOException ex) {
                        ErrorManager.getDefault ().notify ( ErrorManager.EXCEPTION, ex);
                    }
                } else {
                    if (anno.isInDocument()) {
                        anno.setInDocument(false);
                        NbDocument.removeAnnotation(doc, anno);
                    }
                }
            }
        }

        /** Handle DocumentChange event. If the change affect this Part, fire
         * the PROP_TEXT event. */
        void handleDocumentChange(DocumentEvent p0) {
            if (p0.getType().equals(DocumentEvent.EventType.INSERT)) {
                if (p0.getOffset() >= previousOffset &&
                    p0.getOffset() < (previousOffset+getLength()) ) {
                    firePropertyChange(Annotatable.PROP_TEXT, null, null);
                }
            }
            if (p0.getType().equals(DocumentEvent.EventType.REMOVE)) {
                if ( (p0.getOffset() >= previousOffset && p0.getOffset() < previousOffset+getLength()) ||
                    (p0.getOffset() < previousOffset && p0.getOffset()+p0.getLength() > previousOffset) ) {
                    firePropertyChange(Annotatable.PROP_TEXT, null, null);
                }
            }
            if ((p0.getType().equals(DocumentEvent.EventType.INSERT) ||
                p0.getType().equals(DocumentEvent.EventType.REMOVE)) &&
                p0.getOffset() < previousOffset) {
                firePropertyChange(Line.Part.PROP_COLUMN, null, null);
            }
            previousOffset = position.getOffset();
        }

    }    
      

    /** Definition of actions performed in Listener */
    private final class LR implements Runnable, ChangeListener, DocumentListener {
        private static final int REFRESH = 0;
        private static final int UNMARK = 1;
        private static final int ATTACH_DETACH = 2;

        private int actionId;
        private EnhancedChangeEvent ev;
        
	public LR() {}

        public LR (int actionId) {
            this.actionId = actionId;
        }
        
        public LR (EnhancedChangeEvent ev) {
            this.actionId = ATTACH_DETACH;
            this.ev = ev;
        }

        public void run () {
            switch (actionId) {
                case REFRESH: refreshState (); break;
                case UNMARK:  unmarkError (); break;
                case ATTACH_DETACH: attachDetachAnnotations(ev.getDocument(), ev.isClosingDocument()); ev = null; break;
            }
        }

	private void invoke(int op) {
            SwingUtilities.invokeLater(new LR(op));
	}
        
	private void invoke(EnhancedChangeEvent ev) {
            SwingUtilities.invokeLater(new LR(ev));
	}

        public void stateChanged (ChangeEvent ev) {
            invoke(REFRESH);
            invoke((EnhancedChangeEvent)ev);
        }

        public void removeUpdate(final javax.swing.event.DocumentEvent p0) {
            invoke(UNMARK);
        }

        public void insertUpdate(final javax.swing.event.DocumentEvent p0) {
            invoke(UNMARK);
        }

        public void changedUpdate(final javax.swing.event.DocumentEvent p0) {
        }
    }
    
    /** Abstract implementation of {@link Line.Set}.
     *  Defines
    * ways to obtain a line set for documents following
    * NetBeans conventions.
    */
    public static abstract class Set extends Line.Set {
        /** listener on document changes */
        private final LineListener listener;
        /** all lines in the set or null */
        private java.util.List list;

        
        /** Constructor.
        * @param doc document to work on
        */
        public Set (StyledDocument doc) {
            this(doc, null);
        }

        Set (StyledDocument doc, CloneableEditorSupport support) {
            listener = new LineListener (doc, support);
        }

        
        /** Find the line given as parameter in list of all lines attached to this set
         * and if the line exist in the list, notify it about being edited. */
        void linesChanged(int startLineNumber, int endLineNumber, DocumentEvent p0) {
            List changedLines = getLinesFromRange(startLineNumber, endLineNumber);
            
            for(Iterator it = changedLines.iterator(); it.hasNext(); ) {
                Line line = (Line)it.next();
                
                line.firePropertyChange(Annotatable.PROP_TEXT, null, null);

                // revalidate all parts attached to this line
                // that they are still part of the line
                if(line instanceof DocumentLine) {
                    ((DocumentLine)line).notifyChange(p0, this, listener.doc);
                }
            }
        }
        
        /** Find the line given as parameter in list of all lines attached to this set
         * and if the line exist in the list, notify it about being moved. */
        void linesMoved(int startLineNumber, int endLineNumber) {
            List movedLines = getLinesFromRange(startLineNumber, endLineNumber);
            
            for(Iterator it = movedLines.iterator(); it.hasNext(); ) {
                Line line = (Line)it.next();
                line.firePropertyChange(Line.PROP_LINE_NUMBER, null, null);

                // notify all parts attached to this line
                // that they were moved
                if (line instanceof DocumentLine) {
                    ((DocumentLine)line).notifyMove();
                } 
            }
        }
        
        /** Gets the lines with line number whitin the range inclusive.
         * @return <code>List</code> of lines from range inclusive */
        private List getLinesFromRange(int startLineNumber, int endLineNumber) {
            List linesInRange = new ArrayList(10);

            synchronized(lines) {
                for(Iterator it = lines.keySet().iterator(); it.hasNext(); ) {
                    Line line = (Line)it.next();
                    int lineNumber = line.getLineNumber();
                    if(startLineNumber <= lineNumber
                    && lineNumber <= endLineNumber) {
                        linesInRange.add(line);
                    }
                }
            }
            
            return linesInRange;
        }

        /* Returns an unmodifiable set of Lines sorted by their
        * line numbers that contains all lines holded by this
        * Line.Set.
        *
        * @return list of Line objects
        */
        public java.util.List getLines () {
            if (list == null) {
                int cnt = listener.getOriginalLineCount ();
                java.util.List l = new java.util.LinkedList ();
                for (int i = 0; i < cnt; i++) {
                    l.add (getOriginal (i));
                }
                list = l;
            }
            return list;
        }

        /* Finder method that for the given line number finds right
        * Line object that represent as closely as possible the line number
        * in the time when the Line.Set has been created.
        *
        * @param line is a number of the line (text line) we want to acquire
        * @exception IndexOutOfBoundsException if <code>line</code> is invalid.
        */
        public Line getOriginal (int line) throws IndexOutOfBoundsException {
            int newLine = listener.getLine (line);
            int offset = NbDocument.findLineOffset (listener.doc, newLine);

            return this.registerLine(createLine(offset));
        }

        /* Creates current line.
        *
        * @param line is a number of the line (text line) we want to acquire
        * @exception IndexOutOfBoundsException if <code>line</code> is invalid.
        */
        public Line getCurrent (int line) throws IndexOutOfBoundsException {
            int offset = NbDocument.findLineOffset (listener.doc, line);

            return this.registerLine(createLine(offset));
        }

        /** Creates a {@link Line} for a given offset.
        * @param offset the beginning offset of the line
        * @return line object representing the line at this offset
        */
        protected abstract Line createLine (int offset);

    }


}
