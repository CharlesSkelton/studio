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
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.*;
import javax.swing.text.Position;
import javax.swing.text.StyledDocument;
import javax.swing.text.BadLocationException;

import org.openide.loaders.DataObject;
import org.openide.util.RequestProcessor;


/** Reference to one position in a document.
* This position is held as an integer offset, or as a {@link Position} object.
* There is also support for serialization of positions.
*
* @author Petr Hamernik
*/
public final class PositionRef extends Object implements Serializable {
    static final long serialVersionUID = -4931337398907426948L;

    /** Which type of position is currently holded - int X Position */
    transient private Manager.Kind kind;

    /** Manager for this position */
    private Manager manager;

    /** insert after? */
    private boolean insertAfter;

    /** Creates new <code>PositionRef</code> using the given manager at the specified
    * position offset.
    * @param manager manager for the position
    * @param offset - position in the document
    * @param bias the bias for the position
    */
    PositionRef (Manager manager, int offset, Position.Bias bias) {
        this (manager, manager.new OffsetKind (offset), bias);
    }

    /** Creates new <code>PositionRef</code> using the given manager at the specified
    * line and column.
    * @param manager manager for the position
    * @param line line number
    * @param column column number
    * @param bias the bias for the position
    */
    PositionRef (Manager manager, int line, int column, Position.Bias bias) {
        this (manager, manager.new LineKind (line, column), bias);
    }

    /** Constructor for everything.
    * @param manager manager that we are refering to
    * @param kind kind of position we hold
    * @param bias bias for the position
    */
    private PositionRef (Manager manager, Manager.Kind kind, Position.Bias bias) {
        this.manager = manager;
        this.kind = kind;
        insertAfter = (bias == Position.Bias.Backward);
        init ();
    }

    /** Initialize variables after construction and after deserialization. */
    private void init() {
        kind = manager.addPosition(this);
    }

    /** Writes the manager and the offset (int). */
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeBoolean (insertAfter);
        out.writeObject (manager);
        kind.write (out);
    }

    /** Reads the manager and the offset (int). */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        insertAfter = in.readBoolean ();
        manager = (Manager)in.readObject();
        kind = manager.readKind (in);
        init ();
    }

    /** @return the appropriate manager for this position ref.
    */
    public CloneableEditorSupport getCloneableEditorSupport () {
        return manager.getCloneableEditorSupport ();
    }
    
    /** Getter for the editor support this position ref is associated with.
    * But because the possition ref can be associated with any CloneableEditorSupport
    * there can be situations when the editor support cannot be found.
    * That is why this method can ClassCastException.
    *
    * @return editor support 
    * @exception ClassCastException if the position is attached to CloneableEditorSupport
    *    that is not subclass of EditorSupport
    * @deprecated Please use {@link #getCloneableEditorSupport} instead.
    */
    public EditorSupport getEditorSupport () {
        return EditorSupport.extract (getCloneableEditorSupport ());
    }

    /** @return the bias of the position
    */
    public Position.Bias getPositionBias() {
        return insertAfter ? Position.Bias.Backward : Position.Bias.Forward;
    }

    /** @return the position as swing.text.Position object.
    * @exception IOException when an exception occured during reading the file.
    */
    public Position getPosition() throws IOException {
        if(manager.getCloneableEditorSupport().getDocument() == null) {
            manager.getCloneableEditorSupport ().openDocument ();
        }
        
        synchronized(manager) {
            Manager.PositionKind p = (Manager.PositionKind)kind;
            return p.pos;
        }
    }

    /** @return the position as offset index in the file.
    */
    public int getOffset() {
        return kind.getOffset ();
    }

    /** Get the line number where this position points to.
    * @return the line number for this position
    * @throws IOException if the document could not be opened to check the line number
    */
    public int getLine() throws IOException {
        return kind.getLine ();
    }

    /** Get the column number where this position points to.
    * @return the column number within a line (counting starts from zero)
    * @exception IOException if the document could not be opened to check the column number
    */
    public int getColumn() throws IOException {
        return kind.getColumn ();
    }

    public String toString() {
        return "Pos[" + getOffset () + "]"; // NOI18N
    }

    /** This class is responsible for the holding the Document object
    * and the switching the status of PositionRef (Position X offset)
    * objects which depends to this manager.
    * It has one abstract method for the creating the StyledDocument.
    */
    static final class Manager extends Object
    implements Runnable, Serializable {
        /** Head item of data structure replacing linked list here.
         * @see ChainItem */
        private transient ChainItem head;
        /** ReferenceQueue where all <code>ChainedItem</code>'s will be enqueued to. */
        private transient ReferenceQueue queue;
        /** Counter which counts enqued items and after reaching
         * number 100 schedules sweepTask. */
        private transient int counter;
        /** Task which is run in RequestProcessor thread and provides
         * full pass sweep, i.e. removes items with garbaged referents from
         * data strucure. */
        private transient RequestProcessor.Task sweepTask;
        
        /** support for the editor */
        transient private CloneableEditorSupport support;

        /** the document for this manager or null if the manager is not in memory */
        transient private StyledDocument doc;

        static final long serialVersionUID =-4374030124265110801L;
        /** Creates new manager
        * @param supp support to work with
        */
        public Manager(CloneableEditorSupport supp) {
            support = supp;
            init();
        }

        /** Initialize the variables to the default values. */
        protected void init() {
            queue = new ReferenceQueue();
	    
	    // A stable mark used to simplify operations with the list
    	    head = new ChainItem(null, null, null);
        }

        /** Reads the object and initialize */
        private void readObject (ObjectInputStream in) throws IOException, ClassNotFoundException {
            Object firstObject = in.readObject();

            if (firstObject instanceof DataObject) {
                DataObject obj = (DataObject)firstObject;
                support = (CloneableEditorSupport) obj.getCookie(CloneableEditorSupport.class);
            } else {
                // first object is environment
                CloneableEditorSupport.Env env = (CloneableEditorSupport.Env)firstObject;
                support = (CloneableEditorSupport)env.findCloneableOpenSupport ();
            }


            if (support == null) {
                //PENDING - what about now ? does exist better way ?
                throw new IOException();
            }
        }

        final Object readResolve () {
            return support.getPositionManager ();
        }

        private void writeObject(ObjectOutputStream out) throws IOException {
// old serialization version            out.writeObject(support.findDataObject());
            out.writeObject (support.env ());
        }

        /** @return the styled document or null if the document is not loaded.
        */
        public CloneableEditorSupport getCloneableEditorSupport () {
            return support;
        }

        /** Converts all positions into document one.
        */
        void documentOpened (StyledDocument doc) {
            this.doc = doc;

            processPositions(true);
        }

        /** Closes the document and switch all positionRefs to the offset (int)
        * holding status (Position objects willbe forgotten.
        */
        void documentClosed () {
            processPositions(false);

            doc = null;
        }
        
        /** Puts/gets positions to/from memory. It also provides full
         * pass sweep of the data structure (inlined in the code).
         * @param toMemory puts positions to memory if <code>true</code>,
         * from memory if <code>false</code> */
        private void processPositions(boolean toMemory) {
	    // clear the queue, we'll do the sweep inline anyway
            while(queue.poll() != null);
            counter = 0;
            
            synchronized(this) {
		ChainItem previous = head;
		ChainItem ref = previous.next;
		
                while(ref != null) {
                    PositionRef pos = (PositionRef)ref.get();
                    if(pos == null) {
                        // Remove the item from data structure.
                        previous.next = ref.next;
                    } else {
                        // Process the PostionRef.
                        if(toMemory) {
                            pos.kind = pos.kind.toMemory(pos.insertAfter);
                        } else {
                            pos.kind = pos.kind.fromMemory();
                        }
                        
                        previous = ref;
                    }

                    ref = ref.next;
                }
            }
        }

        /** Polls queue and increases the <code>counter</code> accordingly.
         * Schedule full sweep task if counter exceedes 100. */
        private void checkQueue() {
            while(queue.poll() != null) {
                counter++;
            }

            if(counter > 100) {
                counter = 0;

                if(sweepTask == null) {
            	    sweepTask = RequestProcessor.getDefault().post(this);
        	} else if(sweepTask.isFinished()) {
            	    sweepTask.schedule(0);
        	}

            }
        }
        
        /** Implements <code>Runnable</code> interface. 
         * Does full pass sweep in <code>RequestProcessor</code> thread. */
        public synchronized void run() {
	    ChainItem previous = head;
	    ChainItem ref = previous.next;

            while(ref != null) {
                if(ref.get() == null) {
                        // Remove the item from data structure.
                    previous.next = ref.next;
                } else {
                    previous = ref;
                }

                ref = ref.next;
            }
        }

        /** Adds the position to this manager. */
        Kind addPosition(PositionRef pos) {
            Kind kind;
            synchronized(this) {
                head.next = new ChainItem(pos, queue, head.next);
                
                kind = (doc == null ? 
                            pos.kind : 
                            pos.kind.toMemory(pos.insertAfter));
            }

            checkQueue();

            return kind;
        }

        //
        // Kinds
        //

        /** Loads the kind from the stream */
        Kind readKind (DataInput is) throws IOException {
            int offset = is.readInt ();
            int line = is.readInt ();
            int column = is.readInt ();

            if (offset == -1) {
                // line and column must be valid
                return new LineKind (line, column);
            }

            if (line == -1 || column == -1) {
                // offset kind
                return new OffsetKind (offset);
            }

            // out of memory representation
            return new OutKind (offset, line, column);
        }

        // #19694. Item of special data structure replacing
        // for our purposed LinkedList due to performance reasons.
        /** One item which chained instanced provides data structure
         * keeping positions for this Manager. */
        private static class ChainItem extends WeakReference {
            /** Next reference keeping the position. */
            ChainItem next;


            /** Cointructs chanined item. 
             * @param position <code>PositionRef</code> as referent for this 
             * instance
             * @param queue <code>ReferenceQueue</code> to be used for this instance
             * @param next next chained item */
            public ChainItem(PositionRef position, ReferenceQueue queue, ChainItem next) {
                super(position, queue);
                
                this.next = next;
            }
        } // End of class ChainItem.


        /** Base kind with all methods */
        private abstract class Kind extends Object {
	    Kind() {}

            /** Offset */
            public abstract int getOffset ();

            /** Get the line number */
            public abstract int getLine() throws IOException;

            /** Get the column number */
            public abstract int getColumn() throws IOException;

            /** Writes the kind to stream */
            public abstract void write (DataOutput os) throws IOException;

            /** Converts the kind to representation in memory */
            public PositionKind toMemory (boolean insertAfter) {
                // try to find the right position
                Position p;
                try {
                    p = NbDocument.createPosition (doc, getOffset (), insertAfter ? Position.Bias.Forward : Position.Bias.Backward);
                } catch (BadLocationException e) {
                    p = doc.getEndPosition ();
                }
                return new PositionKind (p);
            }

            /** Converts the kind to representation out from memory */
            public Kind fromMemory () {
                return this;
            }
        }

        /** Kind for representing position when the document is
        * in memory.
        */
        private final class PositionKind extends Kind {
            /** position */
            private Position pos;

            /** Constructor */
            public PositionKind (Position pos) {
                this.pos = pos;
            }

            /** Offset */
            public int getOffset () {
                return pos.getOffset ();
            }

            /** Get the line number */
            public int getLine() {
                return NbDocument.findLineNumber(doc, getOffset());
            }

            /** Get the column number */
            public int getColumn() {
                return NbDocument.findLineColumn(doc, getOffset());
            }

            /** Writes the kind to stream */
            public void write (DataOutput os) throws IOException {
                int offset = getOffset();
                int line = getLine();
                int column = getColumn();
                
                if(offset < 0 || line < 0 || column < 0) {
                    throw new IOException(
                        "Illegal PositionKind: " + pos + "[offset=" // NOI18N
                        + offset + ",line=" // NOI18N
                        + line + ",column=" + column + "] in " // NOI18N
                        + doc + " used by " + support + "." // NOI18N
                    );
                }
                
                os.writeInt(offset);
                os.writeInt(line);
                os.writeInt(column);
            }

            /** Converts the kind to representation in memory */
            public PositionKind toMemory (boolean insertAfter) {
                return this;
            }

            /** Converts the kind to representation out from memory */
            public Kind fromMemory () {
                return new OutKind (this);
            }

        }

        /** Kind for representing position when the document is
        * out from memory. There are all infomation about the position,
        * including offset, line and column.
        */
        private final class OutKind extends Kind {
            private int offset;
            private int line;
            private int column;

            /** Constructs the out kind from the position kind.
            */
            public OutKind (PositionKind kind) {
                int offset = kind.getOffset();
                int line = kind.getLine();
                int column = kind.getColumn();
                
                if(offset < 0 || line < 0 || column < 0) {
                    throw new IndexOutOfBoundsException(
                        "Illegal OutKind[offset=" // NOI18N
                        + offset + ",line=" // NOI18N
                        + line + ",column=" + column + "] in " // NOI18N
                        + doc + " used by " + support + "." // NOI18N
                    );
                }
                
                this.offset = offset;
                this.line = line;
                this.column = column;
            }

            /** Constructs the out kind.
            */
            OutKind (int offset, int line, int column) {
                this.offset = offset;
                this.line = line;
                this.column = column;
            }

            /** Offset */
            public int getOffset () {
                return offset;
            }

            /** Get the line number */
            public int getLine() {
                return line;
            }

            /** Get the column number */
            public int getColumn() {
                return column;
            }

            /** Writes the kind to stream */
            public void write (DataOutput os) throws IOException {
                if(offset < 0 || line < 0 || column < 0) {
                    throw new IOException(
                        "Illegal OutKind[offset=" // NOI18N
                        + offset + ",line=" // NOI18N
                        + line + ",column=" + column + "] in " // NOI18N
                        + doc + " used by " + support + "." // NOI18N
                    );
                }
                
                os.writeInt (offset);
                os.writeInt (line);
                os.writeInt (column);
            }
        } // OutKind

        /** Kind for representing position when the document is
        * out from memory. Represents only offset in the document.
        */
        private final class OffsetKind extends Kind {
            private int offset;

            /** Constructs the out kind from the position kind.
            */
            public OffsetKind (int offset) {
                if(offset < 0) {
                    throw new IndexOutOfBoundsException(
                        "Illegal OffsetKind[offset=" // NOI18N
                        + offset + "] in " + doc + " used by " // NOI18N
                        + support + "." // NOI18N
                    );
                }
                
                this.offset = offset;
            }

            /** Offset */
            public int getOffset () {
                return offset;
            }

            /** Get the line number */
            public int getLine() throws IOException {
                return NbDocument.findLineNumber(getCloneableEditorSupport().openDocument(), offset);
            }

            /** Get the column number */
            public int getColumn() throws IOException {
                return NbDocument.findLineColumn (getCloneableEditorSupport().openDocument(), offset);
            }

            /** Writes the kind to stream */
            public void write (DataOutput os) throws IOException {
                if(offset < 0) {
                    throw new IOException(
                        "Illegal OffsetKind[offset=" // NOI18N
                        + offset + "] in " + doc + " used by " // NOI18N
                        + support + "." // NOI18N
                    );
                }
                
                os.writeInt (offset);
                os.writeInt (-1);
                os.writeInt (-1);
            }
        }

        /** Kind for representing position when the document is
        * out from memory. Represents only line and column in the document.
        */
        private final class LineKind extends Kind {
            private int line;
            private int column;

            /** Constructor.
            */
            public LineKind (int line, int column) {
                if(line < 0 || column < 0) {
                    throw new IndexOutOfBoundsException(
                        "Illegal LineKind[line=" // NOI18N
                        + line + ",column=" + column + "] in " // NOI18N
                        + doc + " used by " + support + "." // NOI18N
                    );
                }
                
                this.line = line;
                this.column = column;
            }

            /** Offset */
            public int getOffset () {
                try {
                    StyledDocument doc = getCloneableEditorSupport().getDocument();
                    if (doc == null) {
                        doc = getCloneableEditorSupport().openDocument();
                    }
                    return NbDocument.findLineOffset (doc, line) + column;
                } catch (IOException e) {
                    // what to do? hopefully unlikelly
                    return 0;
                }
            }

            /** Get the line number */
            public int getLine() throws IOException {
                return line;
            }

            /** Get the column number */
            public int getColumn() throws IOException {
                return column;
            }

            /** Writes the kind to stream */
            public void write (DataOutput os) throws IOException {
                if(line < 0 || column < 0) {
                    throw new IOException(
                        "Illegal LineKind[line=" // NOI18N
                        + line + ",column=" + column + "] in " // NOI18N
                        + doc + " used by " + support + "." // NOI18N
                    );
                }
                
                os.writeInt (-1);
                os.writeInt (line);
                os.writeInt (column);
            }

            /** Converts the kind to representation in memory */
            public PositionKind toMemory (boolean insertAfter) {
                // try to find the right position
                Position p;
                try {
                    p = NbDocument.createPosition (doc, NbDocument.findLineOffset (doc, line) + column, insertAfter ? Position.Bias.Forward : Position.Bias.Backward);
                } catch (BadLocationException e) {
                    p = doc.getEndPosition ();
                }
                return new PositionKind (p);
            }

        }

    }


}
