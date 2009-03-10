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

/** Description of annotation. The class is extended by modules
 * which creates annotations and defines annotation type and tooltip. 
 * The annotation can be attached to Annotatable object. Editors which
 * displays Annotations listen on PropertyChangeListner for changes of
 * annotation type or tooltip text. The tooltip text can be evaluated 
 * asynchronously. It means that editors after the getShortDescription call
 * must listen on PropertyChangeListner while the tooltip is visible.
 * If the tooltip text property is changed, the tooltip value must be updated.
 *
 * @author David Konecny, Jaroslav Tulach
 * @since 1.20
 */
public abstract class Annotation extends Object {

    /** Property name of the tip text */
    public static final String PROP_SHORT_DESCRIPTION = "shortDescription"; // NOI18N

    /** Property name of the annotation type */
    public static final String PROP_ANNOTATION_TYPE = "annotationType"; // NOI18N

    /** Virtual property which does not have getter/setter. When change on this
     * property is fired, listeners (= editors) must ensure that this annotation is not covered
     * by some other annotation which is on the same line - annotation must be moved in front
     * of others. See also moveToFront() method.
     * @since 1.27 */
    public static final String PROP_MOVE_TO_FRONT = "moveToFront"; // NOI18N
    
    /** Support for property change listeners*/
    private java.beans.PropertyChangeSupport support;
    
    /** Holding the reference to Annotatable object to
     * which is this Annotation attached*/
    private Annotatable attached;
    
    /** Whether the annotation was added into document or not.
     * Annotation is added to document only in case it is opened.
     * WARNING: It is highly probable that this implementation 
     * will change and maybe will be completely removed 
     * in next version. It is used only during the attaching
     * and detaching of annotations to document. If the code
     * for loading/closing of document and add/remove of 
     * annotations would be synchronized, this variable would not be
     * necessary. However, some refactoring on side of DocumentLine
     * and CloneableEditorSupport will be necessary to achieve this.
     */
    private boolean inDocument = false;

    public Annotation() {
        support = new java.beans.PropertyChangeSupport(this);
    }

    /** Returns name of the file which describes the annotation type. 
     * The file must be defined in module installation layer in the
     * directory "Editors/AnnotationTypes"
     * @return  name of the anotation type*/
    public abstract String getAnnotationType();
    
    /** Returns the tooltip text for this annotation.
     * @return  tooltip for this annotation*/
    public abstract String getShortDescription();
    
    /** Attach annotation to Annotatable object.
     * @param anno annotatable class to which this annotation will be attached */
    public final void attach(Annotatable anno) {
        if (attached != null)
            detach();
        
        attached = anno;
        
        attached.addAnnotation(this);
        notifyAttached(attached);
    }
    
    /** Notifies the annotation that it was attached
     * to the annotatable.
     * @param toAnno annotatable to which the annotation
     *  was attached.
     * @since 1.38
     */
    protected void notifyAttached(Annotatable toAnno) {
    }
    
    /** Detach annotation.*/
    public final void detach() {
        if (attached != null)
        {
            attached.removeAnnotation(this);
            Annotatable old = attached;
            attached = null;
            notifyDetached(old);
        }
    }
    
    /** Notifies the annotation that it was detached
     * from the annotatable.
     * @param fromAnno annotatable from which the annotation
     *  was detached.
     * @since 1.38
     */
    protected void notifyDetached(Annotatable fromAnno) {
    }
    
    /** Gets annotatable object to which this annotation is attached.
     * @return null if annotation is not attached or reference to annotatable object.
     * @since 1.27 */
    final public Annotatable getAttachedAnnotatable() {
        return attached;
    }
    
    /** Add listeners on changes of annotation properties
     * @param l  change listener*/
    final public void addPropertyChangeListener(java.beans.PropertyChangeListener l) {
        support.addPropertyChangeListener (l);
    }
    
    /** Remove listeners on changes of annotation properties
     * @param l  change listener*/
    final public void removePropertyChangeListener(java.beans.PropertyChangeListener l) {
        support.removePropertyChangeListener (l);
    }

    /** Fire property change to registered listeners. */
    final protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        support.firePropertyChange(propertyName, oldValue, newValue);
    }

    /** Helper method for moving annotation which is covered by other annotations
     * on the same line in front of others. The method fires change on PROP_MOVE_TO_FRONT
     * property on which editors must listen and do the fronting of the annotation. Whether the annotation
     * is visible in editor or not is not guaranteed by this method - use Line.show instead.
     * @since 1.27 */
    final public void moveToFront() {
        support.firePropertyChange(PROP_MOVE_TO_FRONT, null, null);
    }

    /** Getter for the inDocument property
     * @return is in document or not */
    final boolean isInDocument() {
        return inDocument;
    }
    
    /** Setter for the inDocument property
     * @param b is in document or not */
    final void setInDocument(boolean b) {
        inDocument = b;
    }
        
}
