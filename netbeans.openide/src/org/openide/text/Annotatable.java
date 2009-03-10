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

/** Classes which are capable of holding annotations must 
 * extend this abstract class. The examples of these classes are 
 * Line or Line.Part. It allows to add/remove 
 * Annotation(s) to this class. There is also support for
 * listening on changes of the properties like deleted or
 * count of attached annotations.
 *
 * @author David Konecny, Jaroslav Tulach
 * @since 1.20
 */
public abstract class Annotatable extends Object {

    /** Property name of the count of annotations  */
    public static final String PROP_ANNOTATION_COUNT = "annotationCount"; // NOI18N

    /** Property name for the deleted attribute */
    public static final String PROP_DELETED = "deleted"; // NOI18N

    /** Property name for the content of the annotatable
     * @since 1.35
     */
    public static final String PROP_TEXT = "text"; // NOI18N
    
    /** Support for property change listeners*/
    private java.beans.PropertyChangeSupport propertyChangeSupport;
    
    /** Count of all annotations attached to this instance. */
    private int annotationCount;

    /** List of all annotations attached to this annotatable object */
    private java.util.List attachedAnnotations;
    
    /** Whether the Annotatable object was deleted during 
     * the editting of document or not. */
    private boolean deleted;
    
    public Annotatable() {
        deleted = false;
        annotationCount = 0;
        propertyChangeSupport = new java.beans.PropertyChangeSupport(this);
        attachedAnnotations = new java.util.LinkedList();
    }
    
    /** Add annotation to this Annotatable class
     * @param anno annotation which will be attached to this class */
    protected void addAnnotation(Annotation anno) {
        annotationCount++;
        attachedAnnotations.add(anno);
        propertyChangeSupport.firePropertyChange (PROP_ANNOTATION_COUNT, annotationCount-1, annotationCount);
    }
    
    /** Remove annotation to this Annotatable class
     * @param anno annotation which will be detached from this class  */
    protected void removeAnnotation(Annotation anno) {
        annotationCount--;
        attachedAnnotations.remove(anno);
        propertyChangeSupport.firePropertyChange (PROP_ANNOTATION_COUNT, annotationCount+1, annotationCount);
    }

    /** Gets the list of all annotations attached to this annotatable object 
     * @since 1.27 */
    java.util.List getAnnotations() {
        return attachedAnnotations;
    }
    
    /** Add listeners on changes of annotatable properties
     * @param l change listener*/
    final public void addPropertyChangeListener(java.beans.PropertyChangeListener l) {
        propertyChangeSupport.addPropertyChangeListener (l);
    }
    
    /** Remove listeners on changes of annotatable properties
     * @param l change listener*/
    final public void removePropertyChangeListener(java.beans.PropertyChangeListener l) {
        propertyChangeSupport.removePropertyChangeListener (l);
    }

    /** Fire property change to registered listeners. */
    final protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        propertyChangeSupport.firePropertyChange(propertyName, oldValue, newValue);
    }

    /** Whether this Annotatable object was removed or not. 
     * @return whether the Annotatable object was removed or not
     */
    final public boolean isDeleted() {
        return deleted;
    }

    /** Get content of the annotatable. The listeners can listen
     * on changes of PROP_TEXT property to learn that content of Annotatable
     * is changing.
     * @return text representing the content of annotatable. The return value can be null, 
     * what means that document is closed.
     * @since 1.35
     */
    abstract public String getText();
    
    /** Setter for property deleted.
     * @param deleted New value of property deleted.
     */
    void setDeleted(boolean deleted) {
        if (this.deleted != deleted)
        {
            this.deleted = deleted;
            propertyChangeSupport.firePropertyChange (PROP_DELETED, !deleted, deleted);
        }
    }    
    
    /** The count of already attached annotations. Modules can use
     * this property to learn whether to this instance are
     * already attached some annotations or not. 
     * @return count of attached annotations
     */
    final public int getAnnotationCount() {
        return annotationCount;
    }
    
}
