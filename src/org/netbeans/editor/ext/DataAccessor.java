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

package org.netbeans.editor.ext;

import java.io.IOException;

/**
 *   DataAccessor for Code Completion DB files
 *
 *   @author  Martin Roskanin
 */
public interface DataAccessor {
    
    /** Opens DataAccessor file resource
     *  @param requestWrite if true, file is opened for read/write operation.
     */
    public void open(boolean requestWrite) throws IOException;
    
    /** Closes DataAccessor file resource */
    public void close() throws IOException;
    
    /** Reads up to len bytes of data from this file resource into an array of bytes.
     * @param buffer the buffer into which the data is read.
     * @param off the start offset of the data.
     * @param len the maximum number of bytes read.
     */
    public void read(byte buffer[], int off, int len) throws IOException;
    
    /** Appends exactly <code>len</code> bytes, starting at <code>off</code> of the buffer pointer
     *  to the end of file resource.
     *  @param  buffer the buffer from which the data is appended.
     *  @param  off    the start offset of the data in the buffer.
     *  @param  len    the number of bytes to append.
     *  @return        the actual file offset.
     */
    public void append(byte buffer[], int off, int len) throws IOException;
    
    /**
     * Returns the current offset in this file.
     *
     * @return     the offset from the beginning of the file, in bytes,
     *             at which the next read or write occurs.
     */
    public long getFilePointer() throws IOException;
    
    /** Clears the file and sets the offset to 0 */
    public void resetFile() throws IOException;
    
    /**
     * Sets the file-pointer offset, measured from the beginning of this
     * file, at which the next read or write occurs.
     */
    public void seek(long pos) throws IOException;
    
    public int getFileLength();
}

