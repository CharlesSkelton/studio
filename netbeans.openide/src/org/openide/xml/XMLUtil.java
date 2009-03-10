/*
 *                 Sun Public License Notice
 * 
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 * 
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 1997-2002 Sun
 * Microsystems, Inc. All Rights Reserved.
 */

package org.openide.xml;

import java.io.*;

import javax.xml.parsers.*;

import org.w3c.dom.*;
import org.xml.sax.*;

/**
 * Utility class collecting library methods related to XML processing.
 *
 * <div class="nonnormative">
 *
 * <p>Remember that when parsing XML files you often want to set an explicit
 * entity resolver. For example, consider a file such as this:</p>
 *
 * <pre>
 * &lt;?<font class="keyword">xml</font> <font class="variable-name">version</font>=<font class="string">"1.0"</font> <font class="variable-name">encoding</font>=<font class="string">"UTF-8"</font>?&gt;
 * &lt;!<font class="keyword">DOCTYPE</font> <font class="type">root</font> <font class="keyword">PUBLIC</font> <font class="string">"-//NetBeans//DTD Foo 1.0//EN"</font> <font class="string">"http://www.netbeans.org/dtds/foo-1_0.dtd"</font>&gt;
 * &lt;<font class="function-name">root</font>/&gt;
 * </pre>
 *
 * <p>If you parse this with a null entity resolver, or you use the
 * default resolver ({@link EntityCatalog#getDefault}) but do not do
 * anything special with this DTD, you will probably find the parse
 * blocking to make a network connection <em>even when you are not
 * validating</em>. That is because DTDs can be used to define
 * entities and other XML oddities, and are not a pure constraint
 * language like Schema or RELAX-NG.</p>
 *
 * <p>There are three basic ways to avoid the network connection.</p>
 *
 * <ol>
 *
 * <li><p>Register the DTD. This is generally the best thing to do. See
 * {@link EntityCatalog}'s documentation for details, but for example
 * in your layer use:</p>
 *
 * <pre>
 * &lt;<font class="function-name">filesystem</font>&gt;
 *   &lt;<font class="function-name">folder</font> <font class="variable-name">name</font>=<font class="string">"xml"</font>&gt;
 *     &lt;<font class="function-name">folder</font> <font class="variable-name">name</font>=<font class="string">"entities"</font>&gt;
 *       &lt;<font class="function-name">folder</font> <font class="variable-name">name</font>=<font class="string">"NetBeans"</font>&gt;
 *         &lt;<font class="function-name">file</font> <font class="variable-name">name</font>=<font class="string">"DTD_Foo_1_0"</font>
 *               <font class="variable-name">url</font>=<font class="string">"nbres:/org/netbeans/modules/mymod/resources/foo-1_0.dtd"</font>&gt;
 *           &lt;<font class="function-name">attr</font> <font class="variable-name">name</font>=<font class="string">"hint.originalPublicID"</font>
 *                 <font class="variable-name">stringvalue</font>=<font class="string">"-//NetBeans//DTD Foo 1.0//EN"</font>/&gt;
 *         &lt;/<font class="function-name">file</font>&gt;
 *       &lt;/<font class="function-name">folder</font>&gt;
 *     &lt;/<font class="function-name">folder</font>&gt;
 *   &lt;/<font class="function-name">folder</font>&gt;
 * &lt;/<font class="function-name">filesystem</font>&gt;
 * </pre>
 *
 * <p>Now the default system entity catalog will resolve the public ID
 * to the local copy in your module, not the network copy.
 * Additionally, anyone who mounts the "NetBeans Catalog" in the XML
 * Entity Catalogs node in the Runtime tab will be able to use your
 * local copy of the DTD automatically, for validation, code
 * completion, etc. (The network URL should really exist, though, for
 * the benefit of other tools!)</p></li>
 *
 * <li><p>You can also set an explicit entity resolver which maps that
 * particular public ID to some local copy of the DTD, if you do not
 * want to register it globally in the system for some reason. If
 * handed other public IDs, just return null to indicate that the
 * system ID should be loaded.</p></li>
 *
 * <li><p>In some cases where XML parsing is very
 * performance-sensitive, and you know that you do not need validation
 * and furthermore that the DTD defines no infoset (there are no
 * entity or character definitions, etc.), you can speed up the parse.
 * Turn off validation, but also supply a custom entity resolver that
 * does not even bother to load the DTD at all:</p>
 *
 * <pre>
 * <font class="keyword">public</font> <font class="type">InputSource</font> <font class="function-name">resolveEntity</font>(<font class="type">String</font> <font class="variable-name">pubid</font>, <font class="type">String</font> <font class="variable-name">sysid</font>)
 *     <font class="keyword">throws</font> <font class="type">SAXException</font>, <font class="type">IOException</font> {
 *   <font class="keyword">if</font> (pubid.equals(<font class="string">"-//NetBeans//DTD Foo 1.0//EN"</font>)) {
 *     <font class="keyword">return</font> <font class="keyword">new</font> <font class="type">InputSource</font>(<font class="keyword">new</font> <font class="type">ByteArrayInputStream</font>(<font class="keyword">new</font> <font class="type">byte</font>[0]));
 *   } <font class="keyword">else</font> {
 *     <font class="keyword">return</font> EntityCatalog.getDefault().resolveEntity(pubid, sysid);
 *   }
 * }
 * </pre></li>
 *
 * </ol>
 *
 * </div>
 *
 * @author  Petr Kuzel
 * @since release 3.2 */
public final class XMLUtil extends Object {

    /** Forbids creating new XMLUtil */
    private XMLUtil() {
    }

    
    // ~~~~~~~~~~~~~~~~~~~~~ SAX related ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        
    /** Create a simple parser.
      * @return <code>createXMLReader(false, false)</code>
      */
    public static XMLReader createXMLReader() throws SAXException {
        return createXMLReader(false, false);
    }

    /** Create a simple parser, possibly validating.
     * @param validate if true, a validating parser is returned
     * @return <code>createXMLReader(validate, false)</code>
     */
    public static XMLReader createXMLReader(boolean validate) throws SAXException{
        return createXMLReader(validate, false);
    }

    /** Create a SAX parser from the JAXP factory.
     * The result can be used to parse XML files.
     * 
     * <p>See class Javadoc for hints on setting an entity resolver.
     * This parser has its entity resolver set to the system entity resolver chain.
     *
     * @param validate if true, a validating parser is returned
     * @param namespaceAware if true, a namespace aware parser is returned
     *
     * @throws FactoryConfigurationError Application developers should never need to directly catch errors of this type.
     * @throws SAXException if a parser fulfilling given parameters can not be created
     *
     * @return XMLReader configured according to passed parameters
     */
    public static XMLReader createXMLReader(boolean validate, boolean namespaceAware) throws SAXException {
        
        SAXParserFactory factory = SAXParserFactory.newInstance();

        factory.setValidating(validate);
        factory.setNamespaceAware(namespaceAware);

        try {
             return factory.newSAXParser().getXMLReader();
        } catch (ParserConfigurationException ex) {
            throw new SAXException("Cannot create parser satisfying configuration parameters", ex);  //NOI18N                        
        }
        
    }

    
    // ~~~~~~~~~~~~~~~~~~~~~ DOM related ~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    
    
    /** 
     * Creates empty DOM Document using JAXP factoring. E.g.:
     * <p><pre>
     * Document doc = createDocument("book", null, null, null);
     * </pre><p>
     * creates new DOM of a well-formed document with root element named book.
     *
     * @param rootQName qualified name of root element. e.g. <code>myroot</code> or <code>ns:myroot</code>
     * @param namespaceURI URI of root element namespace or <code>null</code>
     * @param doctypePublicID public ID of DOCTYPE or <code>null</code>
     * @param doctypeSystemID system ID of DOCTYPE or <code>null</code> if no DOCTYPE 
     *        required and doctypePublicID is also <code>null</code>
     *
     * @throws DOMException if new DOM with passed parameters can not be created
     * @throws FactoryConfigurationError Application developers should never need to directly catch errors of this type.     
     *
     * @return new DOM Document
     */        
    public static Document createDocument(String rootQName, String namespaceURI, String doctypePublicID, String doctypeSystemID) throws DOMException {
        
        DOMImplementation impl = getDOMImplementation();

        if (doctypePublicID != null && doctypeSystemID == null) {
            throw new IllegalArgumentException("System ID cannot be null if public ID specified. ");  //NOI18N
        }
        
        DocumentType dtd = null;
        if (doctypeSystemID != null) {
            dtd = impl.createDocumentType(rootQName, doctypePublicID, doctypeSystemID);
        }
        
        return impl.createDocument(namespaceURI, rootQName, dtd);        
    }

    /**
     * Obtains DOMImpementaton interface providing a number of methods for performing 
     * operations that are independent of any particular DOM instance. 
     *
     * @throw DOMException <code>NOT_SUPPORTED_ERR</code> if cannot get DOMImplementation
     * @throw FactoryConfigurationError Application developers should never need to directly catch errors of this type.          
     *
     * @return DOMImplementation implementation
     */
    private static DOMImplementation getDOMImplementation() throws DOMException { //can be made public
        
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    
        try {
            return factory.newDocumentBuilder().getDOMImplementation();
        } catch (ParserConfigurationException ex) {
            throw new DOMException(DOMException.NOT_SUPPORTED_ERR , "Cannot create parser satisfying configuration parameters");  //NOI18N
        }        
    }

    /** 
     * Create from factory a DocumentBuilder and let it create a org.w3c.dom.Document.
     * This method takes InputSource. After successful finish the document tree is returned.
     *
     * @param input a parser input (for URL users use: <code>new InputSource(url.toExternalForm())</code>
     * @param validate if true validating parser is used
     * @param namespaceAware if true DOM is created by namespace aware parser
     * @param errorHandler a error handler to notify about exception or <code>null</code>
     * @param entityResolver SAX entity resolver or <code>null</code>; see class Javadoc for hints
     *
     * @throws IOException if an I/O problem during parsing occurs
     * @throws SAXException is thrown if a parser error occurs
     * @throws FactoryConfigurationError Application developers should never need to directly catch errors of this type.
     *
     * @return document representing given input, or null if a parsing error occurs
     */
    public static Document parse (
            InputSource input, 
            boolean validate, 
            boolean namespaceAware,
            ErrorHandler errorHandler,             
            EntityResolver entityResolver
        ) throws IOException, SAXException {
        
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();        
        factory.setValidating(validate);
        factory.setNamespaceAware(namespaceAware);            
    
        DocumentBuilder builder = null;
        try {
             builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException ex) {
            throw new SAXException("Cannot create parser satisfying configuration parameters", ex);  //NOI18N
        }
            
        if (errorHandler != null) {
            builder.setErrorHandler(errorHandler);
        }
        
        if (entityResolver != null) {
            builder.setEntityResolver(entityResolver);
        }

        return builder.parse(input);            
    }
    
    /**
     * Write Document into OutputStream using given encoding. 
     * It is a shortcut for writing configurations etc. It guarantees
     * just that data will be written. Structure and indentation may change.
     *
     * @param doc DOM Document to be written
     * @param out data sink     
     * @param enc XML defined encoding name (i.e. IANA defined, one of UTF-8, UNICODE, ASCII)
     *
     * @throws IOException if an I/O exception occurs
     */
    public static void write(Document doc, OutputStream out, String enc) throws IOException {
        XMLUtilImpl.write(doc, out, enc);
    }

    /** 
     * Escape passed string as XML attibute value
     * (<code>&lt;</code>, <code>&amp;</code>, <code>'</code> and <code>"</code>
     * will be escaped.
     * Note: An XML processor returns normalized value that can be different.
     *
     * @param val a string to be escaped
     *
     * @return escaped value
     * @throws CharConversionException if val contains an improper XML character
     *
     * @since 1.40
     */ 
    public static String toAttributeValue(String val) throws CharConversionException {
        
        if (val == null) throw new CharConversionException("null"); // NOI18N

        if (checkAttributeCharacters(val)) return val;
        
        StringBuffer buf = new StringBuffer();
        
        for (int i = 0; i<val.length(); i++) {
            char ch = val.charAt(i);
            if ('<' == ch) {
                buf.append("&lt;");
                continue;
            } else if ('&' == ch) {
                buf.append("&amp;");
                continue;
            } else if ('\'' == ch) {
                buf.append("&apos;");
                continue;
            } else if ('"' == ch) {
                buf.append("&quot;");
                continue;
            }   
            buf.append(ch);
        }        
        return buf.toString();

    }

    /**
     * Escape passed string as XML element content (<code>&lt;</code>, 
     * <code>&amp;</code> and <code>><code> in <code>]]></code> sequences).
     *
     * @param val a string to be escaped
     *
     * @return escaped value
     * @throws CharConversionException if val contains an improper XML character
     *
     * @since 1.40
     */    
    public static String toElementContent(String val) throws CharConversionException {
        if (val == null) throw new CharConversionException("null");  // NOI18N
        
        if (checkContentCharacters(val)) return val;
        
        StringBuffer buf = new StringBuffer();
                
        for (int i = 0; i<val.length(); i++) {
            char ch = val.charAt(i);
            if ('<' == ch) {
                buf.append("&lt;");
                continue;
            } else if ('&' == ch) {
                buf.append("&amp;");
                continue;
            } else if ('>' == ch && i>1 && val.charAt(i-2) == ']' && val.charAt(i-1) == ']') {
                buf.append("&gt;");
                continue;
            }
            buf.append(ch);            
        }
        return buf.toString();        

    }
                
    
/*        
    public static String toCDATA(String val) throws IOException {
        
    }
*/        
    private static final char[] DEC2HEX = {'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'};
    
    /**
     * Can be used to encode values that contain invalid XML characters.
     * At SAX parser end must be used pair method to get original value.
     *
     * @param val data to be converted
     * @param start offset
     * @param len count
     *
     * @since 1.29
     */
    public static String toHex(byte[] val, int start, int len) {
                        
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i<len; i++) {
            byte b = val[start + i];
            buf.append(DEC2HEX[(b & 0xf0) >> 4]);
            buf.append(DEC2HEX[b & 0x0f]);
        }
        return buf.toString();
    }

    /**
     * Decodes data encoded using {@link #toHex(byte[],int,int) toHex}.
     *
     * @param hex data to be converted
     * @param start offset
     * @param len count
     *
     * @throws IOException if input does not represent hex encoded value
     *
     * @since 1.29
     */
    public static byte[] fromHex(char[] hex, int start, int len) throws IOException {

        if (hex == null) throw new IOException("null");
        
        int i = hex.length;
        if (i % 2 != 0) throw new IOException("odd length");
        byte[] magic = new byte[i/2];
        for (;i>0; i-=2) {
            String g = new String(hex, i-2, 2);
            try {
                magic[(i/2) -1] = (byte) Integer.parseInt(g, 16);
            } catch (NumberFormatException ex) {
                throw new IOException(ex.getLocalizedMessage());
            }
        }                        
        
        return magic;
    }
    
    
    /**
     * Check if all passed characters match XML expression [2].
     * @return true if no escaping necessary
     * @throws CharConversionException if contains invalid chars
     */
    private static boolean checkAttributeCharacters(String chars) throws CharConversionException {
        boolean escape = false;
        for (int i = 0; i<chars.length(); i++) {
            char ch = chars.charAt(i);
            if (((int)ch) <= 93) { // we are UNICODE ']'
                switch (ch) {
                    case 0x9:
                    case 0xA:
                    case 0xD:
                        continue;
                    case '\'':
                    case '"':
                    case '<':
                    case '&':
                        escape = true;
                        continue;
                    default:
                        if (((int) ch) < 0x20) {
                            throw new CharConversionException("Invalid XML character &#" + ((int)ch) + ";.");
                        }
                }
            }
        }
        return escape == false;
    }
    
    /**
     * Check if all passed characters match XML expression [2].
     * @return true if no escaping necessary
     * @throws CharConversionException if contains invalid chars
     */
    private static boolean checkContentCharacters(String chars) throws CharConversionException {
        boolean escape = false;
        for (int i = 0; i<chars.length(); i++) {
            char ch = chars.charAt(i);
            if (((int)ch) <= 93) { // we are UNICODE ']'
                switch (ch) {
                    case 0x9:
                    case 0xA:
                    case 0xD:
                        continue;
                    case '>':       // only ]]> is dangerous
                        if (escape) continue;
                        escape =  i > 0 && (chars.charAt(i - 1) == ']');
                        continue;
                    case '<':
                    case '&':
                        escape = true;
                        continue;                        
                    default:
                        if (((int) ch) < 0x20) {
                            throw new CharConversionException("Invalid XML character &#" + ((int)ch) + ";.");
                        }
                }
            }
        }
        return escape == false;
    }
    
}
