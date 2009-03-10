/*
 *                 Sun Public License Notice
 * 
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 * 
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 1997-2001 Sun
 * Microsystems, Inc. All Rights Reserved.
 */

package org.openide.nodes;

import java.util.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

/** Index cookie providing operations useful for reordering
* child nodes. {@link IndexedNode} is the common implementation.
*
* @author Jaroslav Tulach, Dafe Simonek
*/
public interface Index extends Node.Cookie {
  /** Get the number of nodes.
  * @return the count
  */
  public int getNodesCount ();

  /** Get the child nodes.
  * @return array of nodes that can be sorted by this index
  */
  public Node[] getNodes ();

  /** Get the index of a given node.
  * @param node node to find index of
  * @return index of the node, or <code>-1</code> if no such node was found
  */
  public int indexOf (final Node node);

  /** Invoke a dialog for reordering the children.
  */
  public void reorder ();

  /** Reorder all children with a given permutation.
  * @param perm permutation with the length of current nodes
  * @exception IllegalArgumentException if the permutation is not valid
  */
  public void reorder (int[] perm);

  /** Move the element at the <code>x</code>-th position to the <code>y</code>-th position. All
  * elements after the <code>y</code>-th position are moved down.
  *
  * @param x the position to remove the element from
  * @param y the position to insert the element to
  * @exception IndexOutOfBoundsException if an index is out of bounds
  */
  public void move (int x, int y);

  /** Exchange two elements.
  * @param x position of the first element
  * @param y position of the second element
  * @exception IndexOutOfBoundsException if an index is out of bounds
  */
  public void exchange (int x, int y);

  /** Move an element up.
  * @param x index of element to move up
  * @exception IndexOutOfBoundsException if an index is out of bounds
  */
  public void moveUp (int x);

  /** Move an element down.
  * @param x index of element to move down
  * @exception IndexOutOfBoundsException if an index is out of bounds
  */
  public void moveDown (int x);

  /** Add a new listener to the listener list. The listener will be notified of
  * any change in the order of the nodes.
  *
  * @param chl new listener
  */
  public void addChangeListener (final ChangeListener chl);

  /** Remove a listener from the listener list.
  *
  * @param chl listener to remove
  */
  public void removeChangeListener (final ChangeListener chl);

  /*********************** Inner classes ***********************/

  /** A support class implementing some methods of the <code>Index</code>
  * cookie.
  */
  public static abstract class Support implements Index {
    /** Registered listeners */
    private HashSet listeners;

    /** Default constructor. */
    public Support () {
    }

    /* Moves element at x-th position to y-th position. All
    * elements after the y-th position are moved down.
    *
    * @param x the position to remove the element from
    * @param y the position to insert the element to
    * @exception IndexOutOfBoundsException if an index is out of bounds
    */
    public void move (final int x, final int y) {
      int[] perm = new int[getNodesCount()];
      // if the positions are the same then no move
      if (x==y)
          return ;
      for (int i = 0; i < perm.length; i++) {
        if ((i<x && i<y) || (i>x && i>y)) {
            // this area w/o change
            perm[i] = i;
        } else {
            if (i>x && i<y) {
                // i-th element moves backward
                perm[i] = i-1;
            } else {
                // i-th element moves forward
                perm[i] = i+1;
            }
        }
      }
      // set new positions for the elemets on x-th and y-th position
      perm[x] = y;
      if (x<y) {
          perm[y] = y-1;
      } else {
          perm[y] = y+1;
      }

      reorder (perm);
    }

    /* Exchanges two elements.
    * @param x position of the first element
    * @param y position of the second element
    * @exception IndexOutOfBoundsException if an index is out of bounds
    */
    public void exchange (final int x, final int y) {
      int[] perm = new int[getNodesCount ()];
      for (int i = 0; i < perm.length; i++) {
        perm[i] = i;
      }
      perm[x] = y;
      perm[y] = x;

      reorder (perm);
    }

    /* Moves element up.
    * @param x index of element to move up
    * @exception IndexOutOfBoundsException if an index is out of bounds
    */
    public void moveUp (final int x) {
      exchange (x, x - 1);
    }

    /* Moves element down.
    * @param x index of element to move down
    * @exception IndexOutOfBoundsException if an index is out of bounds
    */
    public void moveDown (final int x) {
      exchange (x, x + 1);
    }

    /* Adds new listener to the listener list. Listener is notified of
    * any change in ordering of nodes.
    *
    * @param chl new listener
    */
    public void addChangeListener (final ChangeListener chl) {
      if (listeners == null)
        listeners = new HashSet();
      listeners.add(chl);
    }

    /* Removes listener from the listener list.
    * Removed listener isn't notified no more.
    *
    * @param chl listener to remove
    */
    public void removeChangeListener (final ChangeListener chl) {
      if (listeners == null)
        return;
      listeners.remove(chl);
    }

    /** Fires notification about reordering to all
    * registered listeners.
    *
    * @param che change event to fire off
    */
    protected void fireChangeEvent (ChangeEvent che) {
      if (listeners == null)
        return;
      HashSet cloned;
      // clone listener list
      synchronized (this) {
        cloned = (HashSet)listeners.clone();
      }
      // fire on cloned list to prevent from modifications when firing
      for (Iterator iter = cloned.iterator(); iter.hasNext(); ) {
        ((ChangeListener)iter.next()).stateChanged(che);
      }
    }

    /** Get the nodes; should be overridden if needed.
    * @return the nodes
    */
    public abstract Node[] getNodes ();

    /** Get the index of a node. Simply scans through the array returned by {@link #getNodes}.
    * @param node the node
    * @return the index, or <code>-1</code> if the node was not found
    */
    public int indexOf (final Node node) {
      Node[] arr = getNodes ();
      for (int i = 0; i < arr.length; i++) {
        if (node.equals (arr[i])) {
          return i;
        }
      }
      return -1;
    }

    /** Reorder the nodes with dialog; should be overridden if needed.
    */
    public void reorder () {
        showIndexedCustomizer(this);
    }
    
    /** Utility method to create and show an indexed customizer.
     * Displays some sort of dialog permitting the index cookie to be reordered.
     * Blocks until the reordering is performed (or cancelled).
     * @param idx the index cookie to reorder based on
     * @since 1.37
     */
    public static void showIndexedCustomizer(Index idx) {
        TMUtil.showIndexedCustomizer(idx);
    }

    /** Get the node count. Subclasses must provide this.
    * @return the count
    */
    public abstract int getNodesCount ();

    /** Reorder by permutation. Subclasses must provide this.
    * @param perm the permutation
    */
    public abstract void reorder (int[] perm);

  } // end of Support inner class

  /** Reorderable children list stored in an array.
  */
  public static class ArrayChildren extends Children.Array implements Index {
    /** Support instance for delegation of some <code>Index</code> methods. */
    protected Index support;

    /** Constructor for the support.
    */
    public ArrayChildren () {
      this (null);
    }

    /** Constructor.
    * @param ar the array
    */
    private ArrayChildren (List ar) {
      super (ar);
      // create support instance for delegation of common tasks
      support = new Support() {
        public Node[] getNodes () {
          return ArrayChildren.this.getNodes ();
        }
        public int getNodesCount () {
          return ArrayChildren.this.getNodesCount();
        }
        public void reorder (int[] perm) {
          ArrayChildren.this.reorder(perm);
          fireChangeEvent(new ChangeEvent(ArrayChildren.this));
        }
      };
    }
    
    /** If default constructor is used, then this method is called to lazily create 
    * the collection. Even it claims that it returns Collection only subclasses
    * of List are valid values. 
    * <P>
    * This implementation returns ArrayList.
    *
    * @return any List collection.
    */
    protected java.util.Collection initCollection () {
      return new ArrayList ();
    }

    /* Reorders all children with given permutation.
    * @param perm permutation with the length of current nodes
    * @exception IllegalArgumentException if the perm is not valid permutation
    */
    public void reorder (final int[] perm) {
        try {
            PR.enterWriteAccess ();
            Object[] n = nodes.toArray ();
            List l = (List)nodes;
            for (int i = 0; i < n.length; i++) {
                l.set (perm[i], n[i]);
            }

            refresh ();
        } finally {
            PR.exitWriteAccess ();
        }
    }

    /** Invokes a dialog for reordering children using {@link IndexedCustomizer}.
    */
    public void reorder () {
        try {
            PR.enterReadAccess ();
            Support.showIndexedCustomizer(this);
        } finally {
            PR.exitReadAccess ();
        }
    }

    /* Moves element at x-th position to y-th position. All
    * elements after the y-th position are moved down.
    * Delegates functionality to Index.Support.
    *
    * @param x the position to remove the element from
    * @param y the position to insert the element to
    * @exception IndexOutOfBoundsException if an index is out of bounds
    */
    public void move (final int x, final int y) {
      support.move(x, y);
    }

    /* Exchanges two elements.
    * Delegates functionality to Index.Support.
    * @param x position of the first element
    * @param y position of the second element
    * @exception IndexOutOfBoundsException if an index is out of bounds
    */
    public void exchange (final int x, final int y) {
      support.exchange(x, y);
    }

    /* Moves element up.
    * Delegates functionality to Index.Support.
    * @param x index of element to move up
    * @exception IndexOutOfBoundsException if an index is out of bounds
    */
    public void moveUp (final int x) {
      support.exchange(x, x - 1);
    }

    /* Moves element down.
    * Delegates functionality to Index.Support.
    * @param x index of element to move down
    * @exception IndexOutOfBoundsException if an index is out of bounds
    */
    public void moveDown (final int x) {
      support.exchange(x, x + 1);
    }

    /* Returns the index of given node.
    * @param node Node to find index of.
    * @return Index of the node, -1 if no such node was found.
    */
    public int indexOf (final Node node) {
        try {
            PR.enterWriteAccess ();
            return ((List)nodes).indexOf(node);
        } finally {
            PR.exitWriteAccess ();
        }
    }

    /* Adds new listener to the listener list. Listener is notified of
    * any change in ordering of nodes.
    *
    * @param chl new listener
    */
    public void addChangeListener (final ChangeListener chl) {
      support.addChangeListener(chl);
    }

    /* Removes listener from the listener list.
    * Removed listener isn't notified no more.
    *
    * @param chl listener to remove
    */
    public void removeChangeListener (final ChangeListener chl) {
      support.removeChangeListener(chl);
    }

  } // End of ArrayChildren inner class


  /** Implementation of index interface that operates on an list of
  * objects that are presented by given nodes.
  */
  public abstract class KeysChildren extends Children.Keys {
   /** Support instance for delegation of some <code>Index</code> methods. */
    private Index support; // JST: Maybe made protected 

    /** list of objects that should be manipulated with this keys */
    protected final List list;

    /** Constructor.
    * @param ar the array of any objects
    */
    public KeysChildren (List ar) {
      list = ar;
      update ();
    }

    /** Getter for the index that works with this children.
    * @return the index
    */
    public Index getIndex () {
      synchronized (this) {
        if (support == null) {
          support = createIndex ();
        }
        return support;
      }
    }

    /** The method that creates the supporting index for this
    * children object.
      */
    protected Index createIndex () { 
      // create support instance for delegation of common tasks
      return new Support() {
        /** Returns only nodes that are indexable. Not any fixed ones.
        */
        public Node[] getNodes () {
          List l = Arrays.asList(KeysChildren.this.getNodes ());
          if (KeysChildren.this.nodes != null) {
            l.removeAll (KeysChildren.this.nodes);
          }
          return (Node[])l.toArray (new Node[l.size ()]);
        }
        public int getNodesCount () {
          return list.size();
        }
        public void reorder (int[] perm) {
          KeysChildren.this.reorder (perm);
          update ();
          fireChangeEvent(new ChangeEvent(this));
        }
      };
    }
    
    /* Reorders the list with given permutation.
    * @param perm permutation with the length of current nodes
    * @exception IllegalArgumentException if the perm is not valid permutation
    */
    protected void reorder (final int[] perm) {
        synchronized (lock ()) {
            Object[] n = list.toArray ();
            for (int i = 0; i < n.length; i++) {
              list.set (perm[i], n[i]);
            }
        }
    }
    
    /** The lock to use when accessing the list.
     * By default this implementation returns the list itself, but can
     * be changed to lock more properly.
     *
     * @return lock to use for accessing the list
     */
    protected Object lock () {
        return list;
    }

    /** Update the status of the list if it has changed.
    */
    public final void update () {
        Object[] keys;
        synchronized (lock ()) {
            keys = list.toArray ();
        }
        
        super.setKeys (keys);
    }

  }
}
