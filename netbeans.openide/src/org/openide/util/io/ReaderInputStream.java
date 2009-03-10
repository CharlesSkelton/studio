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

package org.openide.util.io;

import java.io.*;

/**
* This class convert Reader to InputStream. It works by converting
* the characters to the encoding specified in constructor parameter.
* Default enconding is ISO-8859-1.
*
* @author   Petr Hamernik, David Strupl
*/
public class ReaderInputStream extends InputStream {

    /** Input Reader class. */
    private Reader reader;

    private PipedInputStream pis;
    private PipedOutputStream pos;
    private OutputStreamWriter osw;
    
    /** Creates new input stream from the given reader.
    * @param reader Input reader
    */
    public ReaderInputStream(Reader reader) throws java.io.IOException {
        this(reader, "ISO-8859-1"); // TODO : is this correct ? // NOI18N
    }

    /** Creates new input stream from the given reader and encoding.
     * @param reader Input reader
     * @param encoding 
     */
    public ReaderInputStream(Reader reader, String encoding) throws java.io.IOException {
        this.reader = reader;
        pos = new PipedOutputStream();
        pis = new PipedInputStream(pos);
        osw = new OutputStreamWriter(pos, encoding);
    }
    
    /**
    * Reads the next byte of data from this input stream. The value
    * byte is returned as an <code>int</code> in the range
    * <code>0</code> to <code>255</code>. If no byte is available
    * because the end of the stream has been reached, the value
    * <code>-1</code> is returned. This method blocks until input data
    * is available, the end of the stream is detected, or an exception
    * is thrown.
    * <p>
    *
    * @return     the next byte of data, or <code>-1</code> if the end of the
    *             stream is reached.
    * @exception  IOException  if an I/O error occurs.
    */
    public int read() throws IOException {
        if (pis.available() > 0) {
            return pis.read();
        }
        int c = reader.read();
        if (c == -1) {
            return c;
        }
        osw.write(c);
        osw.flush();
        return pis.read();
    }

    /**
    * Skips over and discards <code>n</code> bytes of data from this
    * input stream. The <code>skip</code> method may, for a variety of
    * reasons, end up skipping over some smaller number of bytes,
    * possibly <code>0</code>. The actual number of bytes skipped is
    * returned.
    *
    * @param      n   the number of bytes to be skipped.
    * @return     the actual number of bytes skipped.
    * @exception  IOException  if an I/O error occurs.
    */
    public long skip(long n) throws IOException {
        return reader.skip(n);
    }

    /**
    * Returns the number of bytes that can be read from this input
    * stream without blocking.
    *
    * @return     the number of bytes that can be read from this input stream
    *             without blocking.
    * @exception  IOException  if an I/O error occurs.
    */
    public int available() throws IOException {
        // TODO: should we change this?!
        return 0;
    }

    /**
    * Closes this input stream and releases any system resources
    * associated with the stream.
    *
    * @exception  IOException  if an I/O error occurs.
    */
    public void close() throws IOException {
        reader.close();
        osw.close();
        pis.close();
    }
}
