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

package org.netbeans.editor;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.JTextComponent;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;

/**
* All the documents and components register here so that
* they become available to the processing that crosses
* different components and documents such as cross document
* position stack or word matching.
*
* @author Miloslav Metelka
* @version 1.00
*/
public class Registry {

    private static final WeakReference[] EMPTY = new WeakReference[0];

    /** Array of weak references to documents */
    private static WeakReference[] docRefs = EMPTY;
    
    /** Number of the document references */
    private static int docRefsCount;

    /** Array of activated document numbers */
    private static final ArrayList docAct = new ArrayList();

    /** Array list of weak references to components */
    private static WeakReference[] compRefs = EMPTY;

    /** Number of the document references */
    private static int compRefsCount;

    /** Array of activated component numbers */
    private static final ArrayList compAct = new ArrayList();
    
    /** List of the registered changes listeners */
    private static final WeakEventListenerList listenerList
	= new WeakEventListenerList();
    
    private static int consolidateCounter;

    /** Add weak listener to listen to change of activity of documents or components.
     * The caller must
     * hold the listener object in some instance variable to prevent it
     * from being garbage collected.
     * @param l listener to add
     */
    public static void addChangeListener(ChangeListener l) {
        listenerList.add(SettingsChangeListener.class, l);
    }

    /** Remove listener for changes in activity. It's optional
     * to remove the listener. It would be done automatically
     * if the object holding the listener would be garbage collected.
     * @param l listener to remove
     */
    public static void removeChangeListener(SettingsChangeListener l) {
        listenerList.remove(SettingsChangeListener.class, l);
    }

    /** Get document ID from the document.
     * @return document id or -1 if document was not yet added to the registry
     *  by <code>addDocument()</code>.
     */
    public static synchronized int getID(BaseDocument doc) {
	Integer i = getIDInteger(doc);
	return (i != null) ? i.intValue() : -1;
    }
	
    /** Get component ID from the component.
     * @return component id or -1 if component was not yet added to the registry
     *  by <code>addComponent()</code>.
     */
    public static synchronized int getID(JTextComponent c) {
	return getIDImpl(c);
    }

    /** Get document when its ID is known.
     * It's rather cheap operation.
     * @param docID document ID. It can be retrieved from the document
     *  by <code>getID(doc)</code>.
     * @return document instance or null when document no longer exists
     */
    public static synchronized BaseDocument getDocument(int docID) {
        if (docID < 0 || docID >= docRefsCount) {
            return null;
        }

	WeakReference wr = docRefs[docID];
        return (wr != null) ? (BaseDocument)wr.get() : null;
    }

    /** Get component when its ID is known.
     * It's rather cheap operation.
     * @param compID component ID. It can be retrieved from the component
     *  by <code>getID(c)</code>.
     * @return component instance or null when document no longer exists
     */
    public static synchronized JTextComponent getComponent(int compID) {
        if (compID < 0 || compID >= compRefsCount) {
            return null;
        }

	WeakReference wr = compRefs[compID];
        return (wr != null) ? (JTextComponent)wr.get() : null;
    }

    /** Add document to registry. Doesn't search for repetitive
     * adding.
     * @return registry unique ID of the document
     */
    public static synchronized int addDocument(BaseDocument doc) {
        Integer docID = getIDInteger(doc);
        if (docID != null) { // already added
            return docID.intValue();
        }

	if (docRefsCount >= docRefs.length) {
	    docRefs = realloc(docRefs);
	}
	
        docRefs[docRefsCount] = new WeakReference(doc);
        doc.putProperty(BaseDocument.ID_PROP, new Integer(docRefsCount));
	return docRefsCount++;
    }

    /** Add component to registry. If the component is already registered
     * it returns the existing} ID. The document that is currently assigned
     * to the component is _not_ registered automatically.
     * @return ID of the component
     */
    public static synchronized int addComponent(JTextComponent c) {
        int compID = getIDImpl(c);
        if (compID != -1) {
            return compID; // already registered
        }
	
	if (compRefsCount >= compRefs.length) {
	    compRefs = realloc(compRefs);
	}

        compRefs[compRefsCount] = new WeakReference(c);
        ((BaseTextUI)c.getUI()).componentID = compRefsCount;
        return compRefsCount++;
    }

    /** Remove component from registry. It's usually done when
     * the UI of the component is being deinstalled.
     * @return ID that the component had in the registry. The possible
     *  new ID will be different from this one. -1 will be returned
     *  if the component was not yet added to the registry.
     */
    public static synchronized int removeComponent(JTextComponent c) {
        int compID = getIDImpl(c);
	
	if (compID != -1) {
	    compRefs[compID] = null;
	    // Search whether was activated
	    for (int i = compAct.size() - 1; i >= 0; i--) {
		if (((Integer)compAct.get(i)).intValue() == compID) {
		    compAct.remove(i);
		    break;
		}
	    }
	}
	
        return compID;
    }

    /** Put the component to the first position in the array of last accessed
    * components. The activate of document is also called automatically.
    */
    public static synchronized void activate(JTextComponent c) {
        boolean activated = false;
        int compID = getIDImpl(c);
        if (compID == -1) { // c not registered
            return;
        }
        
        int actSize = compAct.size();
        int ind = 0;
        while (ind < actSize) {
            int id = ((Integer)compAct.get(ind)).intValue();
            if (id == compID) { // found
                if (ind == 0) {
                    break;
                }
                compAct.add(0, compAct.remove(ind));
                activated = true;
                break;
            }
            
            ind++;
        }
        
        if (ind == actSize) {
            compAct.add(0, new Integer(compID));
            activated = true;
        }

        // Try to activate component's document too
        Object doc = c.getDocument();
        if (doc instanceof BaseDocument) {
            if (doActivate((BaseDocument)doc)) {
                activated = true;
            }
        }
        
        if (activated) {
            fireChange();
        }
    }

    /** Put the document to the first position in the array of last accessed
     * documents. The document must be registered otherwise nothing
     * is done.
     * @param doc document to be activated
     */
    public static synchronized void activate(BaseDocument doc) {
	if (doActivate(doc)) {
	    fireChange();
	}
    }
    
    public static synchronized BaseDocument getMostActiveDocument() {
        return getValidDoc(0, true);
    }

    public static synchronized BaseDocument getLeastActiveDocument() {
        int lastInd = docAct.size() - 1;
        return getValidDoc(lastInd, false);
    }

    public static BaseDocument getLessActiveDocument(BaseDocument doc) {
        return getLessActiveDocument(getID(doc));
    }

    public static synchronized BaseDocument getLessActiveDocument(int docID) {
        return getNextActiveDoc(docID, true);
    }

    public static BaseDocument getMoreActiveDocument(BaseDocument doc) {
        return getMoreActiveDocument(getID(doc));
    }

    public static synchronized BaseDocument getMoreActiveDocument(int docID) {
        return getNextActiveDoc(docID, false);
    }

    /** Get the iterator over the active documents. It starts with
     * the most active document till the least active document.
     * It's just the current snapshot so the iterator will
     * not reflect future changes.
     */
    public static synchronized Iterator getDocumentIterator() {
        consolidate();

        ArrayList docList = new ArrayList();
        int actSize = docAct.size();
        for (int i = 0; i < actSize; i++) {
            int ind = ((Integer)docAct.get(i)).intValue();
            WeakReference wr = docRefs[ind];
            if (wr != null) {
                Object doc = wr.get();
                if (doc != null) {
                    docList.add(doc);
                }
            }
        }

        return docList.iterator();
    }

    public static synchronized JTextComponent getMostActiveComponent() {
        return getValidComp(0, true);
    }

    public static synchronized JTextComponent getLeastActiveComponent() {
        int lastInd = compAct.size() - 1;
        return getValidComp(lastInd, false);
    }

    public static JTextComponent getLessActiveComponent(JTextComponent c) {
        return getLessActiveComponent(getID(c));
    }

    public static synchronized JTextComponent getLessActiveComponent(int compID) {
        return getNextActiveComp(compID, true);
    }

    public static JTextComponent getMoreActiveComponent(JTextComponent c) {
        return getMoreActiveComponent(getID(c));
    }

    public static synchronized JTextComponent getMoreActiveComponent(int compID) {
        return getNextActiveComp(compID, false);
    }

    /** Get the iterator over the active components. It starts with
    * the most active component till the least active component.
    */
    public static synchronized Iterator getComponentIterator() {
	consolidate();

        ArrayList compList = new ArrayList();
        int actSize = compAct.size();
        for (int i = 0; i < actSize; i++) {
            int ind = ((Integer)compAct.get(i)).intValue();
            WeakReference wr = compRefs[ind];
            if (wr != null) {
                Object comp = wr.get();
                if (comp != null) {
                    compList.add(comp);
                }
            }
        }

        return compList.iterator();
    }

    private static WeakReference[] realloc(WeakReference[] refs) {
	WeakReference[] tmp = new WeakReference[refs.length * 2 + 4];
	System.arraycopy(refs, 0, tmp, 0, refs.length);
	return tmp;
    }
    
    private static void consolidate() {
	while (++consolidateCounter >= 20) { // after every 20th call
	    consolidateCounter = 0;
	    
	    // Remove empty document references
	    for (int i = docAct.size() - 1; i >= 0; i--) {
		int ind = ((Integer)docAct.get(i)).intValue();
		WeakReference wr = docRefs[ind];
		if (wr != null) {
		    if (wr.get() == null) { // empty reference
			docAct.remove(i);
			docRefs[ind] = null;
		    }
		}
	    }

	    // Remove empty component references
	    for (int i = compAct.size() - 1; i >= 0; i--) {
		int ind = ((Integer)compAct.get(i)).intValue();
		WeakReference wr = compRefs[ind];
		if (wr != null) {
		    if (wr.get() == null) { // empty reference
			compAct.remove(i);
			compRefs[ind] = null;
		    }
		}
	    }
	}
    }

    private static int getIDImpl(JTextComponent c) {
        if (c == null) {
            return -1;
        }
        return ((BaseTextUI)c.getUI()).componentID;
    }

    private static Integer getIDInteger(BaseDocument doc) {
	if (doc == null) {
            return null;
        }

        return (Integer)doc.getProperty(BaseDocument.ID_PROP);
    }

    private static boolean doActivate(BaseDocument doc) {
	Integer docIDInteger = getIDInteger(doc);
	
	if (docIDInteger == null) {
	    return false; // document not added to registry
	}

        int docID = (docIDInteger != null) ? docIDInteger.intValue() : -1;

	int size = docAct.size();
	for (int ind = 0; ind < size; ind++) {
	    int id = ((Integer)docAct.get(ind)).intValue();
	    if (id == docID) {
		if (ind == 0) { // no change
		    return false;
		}

		docAct.add(0, docAct.remove(ind));
		return true;
	    }
	}
	
        docAct.add(0, docIDInteger);
	return true;
    }

    private static BaseDocument getValidDoc(int ind, boolean forward) {
	consolidate();

	int actSize = docAct.size();
        while (ind >= 0 && ind < actSize) {
            int docID = ((Integer)docAct.get(ind)).intValue();
	    WeakReference wr = docRefs[docID];
            BaseDocument doc = (wr != null) ? (BaseDocument)wr.get() : null;
            if (doc != null) {
                return doc;
            }
            ind += forward ? +1 : -1;
        }
        return null;
    }

    private static BaseDocument getNextActiveDoc(int docID, boolean forward) {
	consolidate();

        int actSize = docAct.size();
        int ind = forward ? 0 : (actSize - 1);
        while (ind >= 0 && ind < actSize) {
            if (((Integer)docAct.get(ind)).intValue() == docID) {
                ind += forward ? +1 : -1; // get next one
                return getValidDoc(ind, forward);
            }
            ind += forward ? +1 : -1;
        }
        return null;
    }

    private static JTextComponent getValidComp(int ind, boolean forward) {
	consolidate();

	int actSize = compAct.size();
        while (ind >= 0 && ind < actSize) {
            int compID = ((Integer)compAct.get(ind)).intValue();
	    WeakReference wr = compRefs[compID];
            JTextComponent c = (wr != null) ? (JTextComponent)wr.get() : null;
            if (c != null) {
                return c;
            }
            ind += forward ? +1 : -1;
        }
        return null;
    }

    private static JTextComponent getNextActiveComp(int compID, boolean forward) {
        int actSize = compAct.size();
        int ind = forward ? 0 : (actSize - 1);
        while (ind >= 0 && ind < actSize) {
            if (((Integer)compAct.get(ind)).intValue() == compID) {
                ind += forward ? +1 : -1;
                return getValidComp(ind, forward);
            }
            ind += forward ? +1 : -1;
        }
        return null;
    }

    private static void fireChange() {
	ChangeListener[] listeners
	    = (ChangeListener[])listenerList.getListeners(ChangeListener.class);
	ChangeEvent evt = new ChangeEvent(Registry.class);
	for (int i = 0; i < listeners.length; i++) {
	    listeners[i].stateChanged(evt);
	}
    }

    /** Debug the registry into string. */
    public static synchronized String registryToString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Document References:\n"); // NOI18N
        for (int i = 0; i < docRefsCount; i++) {
            WeakReference wr = docRefs[i];
            sb.append("docRefs[" + i + "]=" + ((wr != null) ? wr.get() : "null") + "\n"); // NOI18N
        }
        sb.append("Component References:\n"); // NOI18N
        for (int i = 0; i < compRefsCount; i++) {
            WeakReference wr = (WeakReference)compRefs[i];
            sb.append("compRefs[" + i + "]=" + ((wr != null) ? wr.get() : "null") + "\n"); // NOI18N
        }
        sb.append("\nActive Document Indexes:\n"); // NOI18N
        for (int i = 0; i < docAct.size(); i++) {
            sb.append(docAct.get(i));
            if (i != docAct.size() - 1) {
                sb.append(", "); // NOI18N
            }
        }
        sb.append("\nActive Component Indexes:\n"); // NOI18N
        for (int i = 0; i < compAct.size(); i++) {
            sb.append(compAct.get(i));
            if (i != compAct.size() - 1) {
                sb.append(", "); // NOI18N
            }
        }

        return sb.toString();
    }

}
