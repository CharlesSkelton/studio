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
/*
 * SettingsEntry.java
 *
 * Created on June 6, 2001, 5:23 PM
 */

package org.openide.loaders;

import org.openide.filesystems.*;
import org.openide.loaders.*;
import java.io.IOException;

/** Handles settings folder attached as a secondary entry to MultiDataObjects.
 * It performs operations on the folder and all its children. Direct children
 * of settings folder must be folders otherwise they are ignored.
 * @author  Vita Stejskal
 */
class SettingsEntry extends FileEntry {

    /** Creates new SettingsEntry */
    public SettingsEntry(MultiDataObject obj, FileObject file) {
        super(obj, file);
    }

    /** Copies settings folder including whole subfolders tree. Configurations and their settings
     * are copied using Data Systems API.
     * @param f the folder to create this entry in
     * @param name the new name to use
     * @return the copied <code>FileObject</code> or <code>null</code> if it cannot be copied
     * @exception IOException when the operation fails
     */
    public FileObject copy(FileObject f, String suffix) throws IOException {
        return copy_internal(f, suffix);
    }
    
    /** Moves settings, it uses safe copy-and-delete mechanism.
     * @param f the folder to move this entry to
     * @param suffix the suffix to use
     * @return the moved <code>FileObject</code> or <code>null</code> if it has been deleted
     * @exception IOException when the operation fails
     */
    public FileObject move(FileObject f, String suffix) throws IOException {
        FileObject nue = copy_internal(f, suffix);
        super.delete();
        return nue;
    }
    
    /** Creates settings from template, all configuration folder are created and
     * createFromTemplate is delegated to their children.
     * @param f the folder to create this entry in
     * @param name the new name to use
     * @return the copied <code>FileObject</code> or <code>null</code> if it cannot be copied
     * @exception IOException when the operation fails
     */
    public FileObject createFromTemplate(FileObject f, String name) throws IOException {
        String ext = getExt (getFile ());
        String newName = ext == null ? name : name + "." + ext; // NOI18N
        FileObject nue = FileUtil.createFolder (f, newName);
        DataObject children [] = null;
        
        try {
            children = DataFolder.findContainer (getFile ()).getChildren ();
        } catch (Exception e) {
            children = null;
        }
        
        if (children != null) {
            for(int i = 0; i < children.length; i++) {
                // get all configurations folders only
                DataFolder data = (DataFolder)children[i].getCookie (DataFolder.class);
                if (data != null) {
                    FileObject dataf = FileUtil.createFolder(nue, data.getPrimaryFile ().getName ());
                    createFromTemplate_datafolder (data, DataFolder.findFolder (dataf));
                }
            }
        }
        return nue;
    }
    
    /** Renames settings folder, keeps the name format appropriate for settings folder.
     * @param name the new name
     * @return the renamed <code>FileObject</code> or <code>null</code> if it has been deleted
     * @exception IOException when the operation fails
     */
    public FileObject rename(String name) throws IOException {
        String ext = getExt (getFile ());
        String newName = ext == null ? name : name + "." + ext; // NOI18N
        boolean locked = isLocked ();
        
        FileLock lock = takeLock ();
        try {
            getFile ().rename (lock, newName, getFile ().getExt ());
        } finally {
            if (!locked)
                lock.releaseLock ();
        }
        return getFile ();
    }
    
    private FileObject copy_internal(FileObject f, String suffix) throws IOException {
        FileObject orig = getFile();
        FileObject nue = FileUtil.createFolder(f, orig.getName() + suffix);
        DataObject children [] = null;
        
        try {
            children = DataFolder.findContainer(orig).getChildren();
        } catch (Exception e) {
            children = null;
        }
        
        if (children != null) {
            for(int i = 0; i < children.length; i++) {
                // get all configurations folders only
                DataFolder data = (DataFolder)children[i].getCookie(DataFolder.class);
                if (data != null) {
                    FileObject dataf = FileUtil.createFolder(nue, data.getPrimaryFile().getName());
                    copy_datafolder(data, DataFolder.findFolder(dataf));
                }
            }
        }
        return nue;
    }
    
    private void copy_datafolder(DataFolder src, DataFolder trg) throws IOException {
        DataObject children [] = src.getChildren ();
        
        if (children != null) {
            for (int i = 0; i < children.length; i++) {
                children[i].copy (trg);
            }
        }
    }

    private void createFromTemplate_datafolder(DataFolder src, DataFolder trg) throws IOException {
        DataObject children [] = src.getChildren ();
        
        if (children != null) {
            for (int i = 0; i < children.length; i++) {
                children[i].createFromTemplate (trg);
            }
        }
    }
    
    private String getExt (FileObject f) {
        String name = f.getNameExt();
        int i = name.lastIndexOf('.');
        if (i == -1)
            return null;
        return name.substring(i + 1);
    }
}
