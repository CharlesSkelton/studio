/*

 *                 Sun Public License Notice

 * 

 * The contents of this file are subject to the Sun Public License

 * Version 1.0 (the "License"). You may not use this file except in

 * compliance with the License. A copy of the License is available at

 * http://www.sun.com/

 * 

 * The Original Code is NetBeans. The Initial Developer of the Original

 * Code is Sun Microsystems, Inc. Portions Copyright 1997-2003 Sun

 * Microsystems, Inc. All Rights Reserved.

 */



package org.netbeans.editor;



import javax.swing.text.Segment;
import java.io.IOException;
import java.io.Reader;



/**

 * Converters handling the various line separators.

 *

 * @author Miloslav Metelka

 * @version 1.00

 */



public class LineSeparatorConversion {

    

    /**

     * Default size of the conversion buffers.

     */

    private static final int DEFAULT_CONVERSION_BUFFER_SIZE = 16384;

        

    private LineSeparatorConversion() {

        // no instances

    }

    

    /**

     * Convert all the occurrences of '\r' and '\r\n' in the text to '\n'.

     * @param text text being converted

     * @return converted text with '\n' instead of '\r' and '\r\n'.

     */

    public static String convertToLineFeed(String text) {

        StringBuffer output = new StringBuffer();

        convertToLineFeed(text, 0, text.length(), output);

        return output.toString();

    }

    

    /**

     * Convert all the occurrences of '\r' and '\r\n' in the text to '\n'.

     * @param text text being converted

     * @param offset offset of the first character in the text to be converted.

     * @param length number of characters to be converted.

     * @param output output buffer to which the converted characters are added.

     */

    public static void convertToLineFeed(String text, int offset, int length,

    StringBuffer output) {



        int endOffset = offset + length;

        boolean lastCharCR = false; // whether last char was '\r'



        while (offset < endOffset) {

            char ch = text.charAt(offset++);

            if (lastCharCR && ch == '\n') { // found CRLF sequence

                lastCharCR = false;



            } else { // not CRLF sequence

                if (ch == '\r') {

                    output.append('\n');

                    lastCharCR = true;



                } else { // current char not '\r'

                    lastCharCR = false;

                    output.append(ch);

                }

            }

        }

    }



    /**

     * Convert all the occurrences of '\n' in the given text

     * to the requested line separator.

     * @param text text being converted

     * @param lineFeedReplace characters that replace the '\n' character

     *  in the converted text.

     * @return converted text with replaced '\n' by characters from lineFeedReplace string

     */

    public static String convertFromLineFeed(String text, String lineFeedReplace) {

        StringBuffer output = new StringBuffer();

        convertFromLineFeed(text, 0, text.length(), lineFeedReplace, output);

        return output.toString();

    }



    /**

     * Convert all the occurrences of '\n' in the given text

     * to the requested line separator.

     * @param text text being converted

     * @param offset offset of the first character in the text to be converted.

     * @param length number of characters to be converted.

     * @param lineFeedReplace characters that replace the '\n' character in the output

     * @param output output buffer to which the converted characters are added.

     */

    public static void convertFromLineFeed(String text, int offset, int length,

    String lineFeedReplace, StringBuffer output) {

        int lineFeedReplaceLength = lineFeedReplace.length();

        int endOffset = offset + length;

        while (offset < endOffset) {

            char ch = text.charAt(offset++);

            if (ch == '\n') {

                for (int i = 0; i < lineFeedReplaceLength; i++) {

                    output.append(lineFeedReplace.charAt(i));

                }

            } else {

                output.append(ch);

            }

        }

    }



    /**

     * Convert all the occurrences of '\r' and '\r\n' in the text to '\n'.

     * This class does conversion in chunks of fixed size

     * and is therefore suitable for conversion of readers

     * where the size is unknown.

     */

    public static class ToLineFeed {

        

        private Reader reader;

        

        private Segment convertedText;

        

        private boolean lastCharCR;



        public ToLineFeed(Reader reader) {

            this(reader, DEFAULT_CONVERSION_BUFFER_SIZE);

        }



        public ToLineFeed(Reader reader, int convertBufferSize) {

            this.reader = reader;

            convertedText = new Segment();

            convertedText.array = new char[convertBufferSize];

        }

        

        public Segment nextConverted() throws IOException {

            if (reader == null) { // no more chars to read

                return null;

            }



            int readOffset = 0;

            int readSize = readBuffer(reader, convertedText.array, readOffset, true);

            

            if (readSize == 0) { // no more chars in reader

                reader.close();

                reader = null;

                return null;

            }



            if (lastCharCR && readSize > 0 && convertedText.array[readOffset] == '\n') {

                /* the preceding '\r' was already converted to '\n'

                 * in the previous buffer so here just skip initial '\n'

                 */

                readOffset++;

                readSize--;

            }



            convertedText.offset = readOffset;

            convertedText.count = readSize;

            lastCharCR = convertSegmentToLineFeed(convertedText);

            return convertedText;

        }

        

        /**

         * Convert all the '\r\n' or '\r' to '\n' (linefeed).

         * This method 

         * @param text the text to be converted. Text is converted

         *  in the original array of the given segment.

         *  The <CODE>count</CODE> field

         *  of the text parameter will possibly be changed by the conversion

         *  if '\r\n' sequences are present.

         * @return whether the last character in the text was the '\r' character.

         *  That character was already converted to '\n' and is present

         *  in the segment. However this notification is important

         *  because if there would be '\n' at the begining

         *  of the next buffer then that character should be skipped.

         */

        private static boolean convertSegmentToLineFeed(Segment text) {

            char[] chars = text.array;

            int storeOffset = text.offset; // offset at which chars are stored

            int endOffset = storeOffset + text.count;

            boolean storeChar = false; // to prevent copying same chars to same offsets

            boolean lastCharCR = false; // whether last char was '\r'



            for (int offset = storeOffset; offset < endOffset; offset++) {

                char ch = chars[offset];



                if (lastCharCR && ch == '\n') { // found CRLF sequence

                    lastCharCR = false;

                    storeChar = true; // storeOffset now differs from offset



                } else { // not CRLF sequence

                    if (ch == '\r') {

                        lastCharCR = true;

                        chars[storeOffset++] = '\n'; // convert it to '\n'



                    } else { // current char not '\r'

                        lastCharCR = false;

                        if (storeChar) {

                            chars[storeOffset] = ch;

                        }

                        storeOffset++;

                    }

                }

            }



            text.count = storeOffset - text.offset;



            return lastCharCR;

        }



        private static int readBuffer(Reader reader, char[] buffer, int offset,

        boolean joinReads) throws IOException {

            int maxReadSize = buffer.length - offset;

            int totalReadSize = 0;



            do {

                int readSize = 0;

                while (readSize == 0) { // eliminate empty reads

                    readSize = reader.read(buffer, offset, maxReadSize);

                }



                if (readSize == -1) {

                    break; // no more chars in reader

                }



                totalReadSize += readSize;

                offset += readSize;

                maxReadSize -= readSize;



            } while (joinReads && maxReadSize > 0);



            return totalReadSize;

        }



    }



    /**

     * Convert all the occurrences of '\n' in the given text

     * to the requested line separator.

     * This class does conversion in chunks of fixed size

     * and is therefore suitable for conversion of large

     * texts.

     */

    public static class FromLineFeed {

        

        private Object charArrayOrSequence;

        

        private int offset;

        

        private int endOffset;

        

        private String lineFeedReplace;



        private Segment convertedText;

        

        public FromLineFeed(char[] source, int offset, int length,

        String lineFeedReplace) {

            this(source, offset, length, lineFeedReplace, DEFAULT_CONVERSION_BUFFER_SIZE);

        }



        public FromLineFeed(char[] source, int offset, int length,

        String lineFeedReplace, int conversionSegmentSize) {

            this((Object)source, offset, length, lineFeedReplace, conversionSegmentSize);

        }



        public FromLineFeed(String text, int offset, int length,

        String lineFeedReplace) {

            this(text, offset, length, lineFeedReplace, DEFAULT_CONVERSION_BUFFER_SIZE);

        }



        public FromLineFeed(String text, int offset, int length,

        String lineFeedReplace, int conversionSegmentSize) {

            this((Object)text, offset, length, lineFeedReplace, conversionSegmentSize);

        }



        private FromLineFeed(Object charArrayOrSequence, int offset, int length,

        String lineFeedReplace, int conversionSegmentSize) {

            

            if (conversionSegmentSize < lineFeedReplace.length()) {

                throw new IllegalArgumentException("conversionSegmentSize="

                    + conversionSegmentSize + " < lineFeedReplace.length()="

                    + lineFeedReplace.length()

                );

            }



            this.charArrayOrSequence = charArrayOrSequence;

            this.offset = offset;

            this.endOffset = offset + length;

            this.lineFeedReplace = lineFeedReplace;



            convertedText = new Segment();

            convertedText.array = new char[conversionSegmentSize];

        }



        public Segment nextConverted() {

            if (offset == endOffset) { // no more chars to convert

                return null;

            }



            // [PENDING-PERF] optimization for '\n' -> arraycopy



            char[] convertedArray = convertedText.array;

            int convertedArrayLength = convertedArray.length;

            int convertedOffset = 0;



            /* Determine whether the source is char-sequence

             * or char buffer.

             * Assign either sourceText or sourceArray but not both.

             */

            String sourceText;

            char[] sourceArray;

            if (charArrayOrSequence instanceof String) {

                sourceText = (String)charArrayOrSequence;

                sourceArray = null;



            } else {

                sourceArray = (char[])charArrayOrSequence;

                sourceText = null;

            }



            int lineFeedReplaceLength = lineFeedReplace.length();

            while (offset < endOffset

                && convertedArrayLength - convertedOffset >= lineFeedReplaceLength

            ) {

                char ch = (sourceText != null)

                    ? sourceText.charAt(offset++)

                    : sourceArray[offset++];



                if (ch == '\n') {

                    for (int i = 0; i < lineFeedReplaceLength; i++) {

                        convertedArray[convertedOffset++] = lineFeedReplace.charAt(i);

                    }

                    



                } else {

                    convertedArray[convertedOffset++] = ch;

                }

            }



            convertedText.offset = 0;

            convertedText.count = convertedOffset;



            return convertedText;

        }

        

    }

    

    public static class InitialSeparatorReader extends Reader {

        

        private static final int AFTER_CR_STATUS = -1;

        

        private static final int INITIAL_STATUS = 0;

        

        private static final int CR_SEPARATOR = 1;

        

        private static final int LF_SEPARATOR = 2;

        

        private static final int CRLF_SEPARATOR = 3;

        

        private Reader delegate;

        

        private int status = INITIAL_STATUS;

        

        public InitialSeparatorReader(Reader delegate) {

            this.delegate = delegate;

        }

        

        public String getInitialSeparator() {

            String separator;

            switch (status) {

                case CR_SEPARATOR:

                    separator = "\r";

                    break;

                    

                case LF_SEPARATOR:

                    separator = "\n";

                    break;

                    

                case CRLF_SEPARATOR:

                    separator = "\r\n";

                    break;

                    

                case AFTER_CR_STATUS: // '\r' was last char

                    separator = "\r";

                    break;

                    

                default:

                    separator = "\n"; // default

                    break;

            }



            return separator;

        }

        

        private void resolveSeparator(char ch) {

            switch (status) {

                case INITIAL_STATUS:

                    switch (ch) {

                        case '\r':

                            status = AFTER_CR_STATUS;

                            break;

                        case '\n':

                            status = LF_SEPARATOR;

                            break;

                    }

                    break;

                    

                case AFTER_CR_STATUS:

                    switch (ch) {

                        case '\n':

                            status = CRLF_SEPARATOR;

                            break;

                        default:

                            status = CR_SEPARATOR;

                            break;

                    }

                    break;



                default:

                    switch (ch) {

                        case '\r':

                            status = AFTER_CR_STATUS;

                            break;

                        case '\n':

                            status = LF_SEPARATOR;

                            break;

                    }

                    break;

            }

        }

        

        private boolean isSeparatorResolved() {

            return (status > 0);

        }

        

        public void close() throws IOException {

            if (delegate == null) {

                return;

            }



            delegate.close();

            delegate = null;

        }        

        

        public int read(char[] cbuf, int off, int len) throws IOException {

            if (delegate == null) {

                throw new IOException("Reader already closed.");

            }



            int readLen = delegate.read(cbuf, off, len);



            for (int endOff = off + readLen;

                off < endOff && !isSeparatorResolved();

                off++

            ) {

                resolveSeparator(cbuf[off]);

            }



            return readLen;

        }



        public int read() throws IOException {

            if (delegate == null) {

                throw new IOException("Reader already closed.");

            }



            int r = delegate.read();

            if (r != -1 && !isSeparatorResolved()) {

                resolveSeparator((char)r);

            }

            

            return r;

        }



    }



}

