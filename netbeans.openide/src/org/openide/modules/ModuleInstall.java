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

package org.openide.modules;

import org.openide.util.SharedClassObject;

/**
* Provides hooks for a custom module that may be inserted into the IDE.
* This interface should be implemented by the main class of a module.
*
* <p>Simple modules will likely not need a main class--just a few entries in the manifest file.
* Even modules with a main class need not do anything in it that is already covered by manifest entries;
* only additional special functionality need be handled here.
*
* <p>Specify this class in the manifest file with <code>OpenIDE-Module-Install</code>.
*
* <p>Modules wishing to keep state associated with the installation of the module
* may do so by implementing not only this class but also {@link java.io.Externalizable}.
* In this case, they are responsible for reading and writing their own state
* properly (probably using {@link java.io.ObjectOutput#writeObject} and {@link java.io.ObjectInput#readObject}).
* Note that state which is logically connected to the user's configuration of the module on
* a possibly project-specific basis should <em>not</em> be stored this way, but rather
* using a system option. (Even if this information is not to be displayed, it should
* still be stored as hidden properties of the system option, so as to be switched properly
* during project switches.)
* @author Petr Hamernik, Jaroslav Tulach, Jesse Glick
*/
public class ModuleInstall extends SharedClassObject {

    private static final long serialVersionUID = -5615399519545301432L;
    
    /** Called when a module is being considered for loading.
     * (This would be before {@link #installed}, {@link #restored},
     * or {@link #updated} are called.) If something is critically
     * wrong with the module (missing ad-hoc dependency, missing
     * license key, etc.) then <code>IllegalStateException</code>
     * may be thrown to prevent it from being loaded (preferably
     * with a localized annotation). The default implementation
     * does nothing. The module cannot assume much about when this
     * method will be called; specifically it cannot rely on layers
     * or manifest sections to be ready, nor for the module's classloader
     * to exist in the system class loader (so if loading bundles, icons,
     * and so on, specifically pass in the class loader of the install
     * class rather than relying on the default modules class loader).
     * @since 1.24
     */
    public void validate () throws IllegalStateException {
    }
    
    /**
     * Called when the module is first installed into the IDE.
     * Should perform whatever setup functions are required.
     * The default implementation calls restored.
     * <p>Typically, would do one-off functions, and then also call {@link #restored}.
     * @deprecated Better to check specific aspects of the module's installation.
     *             For example, a globally installed module might be used in several
     *             user directories. Only the module itself can know whether its
     *             special installation tasks apply to some part of the global installation,
     *             or whether they apply to the module's usage in the current user directory.
     *             For this reason, implementing this method cannot be guaranteed
     *             to have useful effects.
    */
    public void installed () {
        restored ();
    }

    /**
     * Called when an already-installed module is restored (during IDE startup).
     * Should perform whatever initializations are required.
     * <p>Note that it is possible for module code to be run before this method
     * is called, and that code must be ready nonetheless. For example, data loaders
     * might be asked to recognize a file before the module is "restored". For this
     * reason, but more importantly for general performance reasons, modules should
     * avoid doing anything here that is not strictly necessary - often by moving
     * initialization code into the place where the initialization is actually first
     * required (if ever). This method should serve as a place for tasks that must
     * be run once during every startup, and that cannot reasonably be put elsewhere.
     * <p>Basic programmatic services are available to the module at this stage -
     * for example, its class loader is ready for general use, any objects registered
     * declaratively to lookup (e.g. system options or services) are ready to be
     * queried, and so on.
     */
    public void restored () {}

    /**
     * Called when the module is loaded and the version is higher than
     * by the previous load
     * The default implementation calls {@link #restored}.
     * @param release The major release number of the <B>old</B> module code name or -1 if not specified.
     * @param specVersion The specification version of the this <B>old</B> module.
     * @deprecated Better to check specific aspects of the module's installation.
     *             For example, a globally installed module might be used in several
     *             user directories. Only the module itself can know whether its
     *             special installation tasks apply to some part of the global installation,
     *             or whether they apply to the module's usage in the current user directory.
     *             For this reason, implementing this method cannot be guaranteed
     *             to have useful effects.
    */
    public void updated ( int release, String specVersion ) {
        restored ();
    }

    /**
     * Called when the module is uninstalled (from a running IDE).
     * Should remove whatever functionality from the IDE that it had registered.
    */
    public void uninstalled () {}

    /**
     * Called when the IDE is about to exit. The default implementation returns <code>true</code>.
     * The module may cancel the exit if it is not prepared to be shut down.
    * @return <code>true</code> if it is ok to exit the IDE
    */
    public boolean closing () { return true; }

    /**
     * Called when all modules agreed with closing and the IDE will be closed.
    */
    public void close () {}

    protected boolean clearSharedData () {
        return false;
    }
}
